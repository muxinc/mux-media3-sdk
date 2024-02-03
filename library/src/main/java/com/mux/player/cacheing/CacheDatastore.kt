package com.mux.player.cacheing

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.util.Base64
import android.util.Log
import com.mux.player.internal.cache.CachedResourceRecord
import com.mux.player.oneOf
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.concurrent.CancellationException
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicReference

/**
 * Represents the on-disk datastore for the cache. This class provides methods that allow for
 * reading and writing from the cache, as well as methods for obtaining files for the Proxy to
 * download into.
 *
 * You should keep an instance of this class open as long as the cache could likely be accessed.
 * CacheController is immediately responsible for deciding this (though in this in-dev iteration it
 * simply keeps one and doesn't close it, which we should change before 1.0)
 */
internal class CacheDatastore(val context: Context) : Closeable {

  companion object {
    private val openTask: AtomicReference<FutureTask<DbHelper>> = AtomicReference(null)
  }

  private val RX_CHUNK_URL =
    Regex("""^https://[^/]*/v1/chunk/([^/]*)/([^/]*)\.(m4s|ts)""")

  private val dbHelper: DbHelper get() = awaitDbHelper()

  /**
   * Opens the datastore, blocking until it is ready to use
   *
   * If you use the cache before opening then it will open itself. But opening after a crash or
   * something may take longer due to internal bookkeeping stuff, so this method is exposed for now
   *
   * Internally, this method will ensure that the cache directories and index database exist
   * on-disk, and the index db is updated. It will also clean-up orphaned files and do an eviction
   * pass and whatever other cleanup tasks are required.
   *
   * This method is safe to call multiple times. Subsequent calls will await the same task,
   * or do nothing if the db is already open.
   *
   * @throws IOException if there was an error opening the cache, or if
   */
  @Throws(IOException::class)
  fun open() {
    try {
      awaitDbHelper()
    } catch (_: CancellationException) {
      // swallow cancellation errors, they are not that important
    }
  }

  /**
   * Closes the datastore. This will close the index database and revert the datastore to a closed
   * state. You can reopen it by calling [open] again.
   */
  override fun close() {
    // em - it's definitely all copacetic to call close() to handle errors from open(), or to
    // close() during opening. if you immediately call open() after close(), your second open() may
    // fail intermittently. But maybe that's just a theoretical risk, so todo - test cranking this
    //  (and maybe don't worry about it overly much)
    val openFuture = openTask.get()
    try {
      if (openFuture != null) {
        val openDbHelper = if (openFuture.isDone) openFuture.get() else null
        openFuture.cancel(true)
        openDbHelper?.close()
      }
    } catch (_: Exception) {
    } finally {
      // calls made to open() start failing after cancel() and keep failing until after this line
      openTask.compareAndSet(openFuture, null)
    }
  }

  /**
   * Create a temporary file for downloading purposes. This file will be within a temporary dir,
   * and is guaranteed not to have existed before this method was called. When the download is
   * finished, call [moveFromTempFile] to move the file to more-permanent cache storage.
   *
   * The temp directory is purged every time the datastore is reopened, and temp files are deleted
   * automatically whenever the JVM shuts down gracefully
   */
  fun createTempDownloadFile(remoteUrl: URL): File {
    return createTempMediaFile(remoteUrl.path.split("/").last())
      .also { it.deleteOnExit() }
  }

  /**
   * Move a completed download from the temp file to the cache where it will live until it falls out
   */
  fun moveFromTempFile(
    tempFile: File,
    remoteUrl: URL,
    contentRange: CacheController.ContentRange? = null
  ): File {
    // todo - we need to add more to the filename, since we can have many per segment
    val cacheFile = createCacheFile(remoteUrl, contentRange)
    tempFile.renameTo(cacheFile)
    return cacheFile
  }

  fun writeResourceRecord(fileRecord: CachedResourceRecord): Result<Unit> {
    val rowId = dbHelper.writableDatabase.insertWithOnConflict(
      IndexSchema.ResourcesTable.name, null,
      fileRecord.toContentValues(),
      SQLiteDatabase.CONFLICT_REPLACE
    )

    return if (rowId >= 0) {
      Result.success(Unit)
    } else {
      Result.failure(IOException("Failed to write to cache index"))
    }
  }

  fun readRecord(url: String): CachedResourceRecord? {
    // todo - try to read a record for the given URL, returning it if there's a hit
    return null
  }

  /**
   * Mux Video segments have special cache keys because their URLs follow a known format even
   * across CDNs.
   *
   * For segments specifically from Mux Video, this key will be generated in such a way that
   * requests for the same segment from one CDN can hit cached entries for the same segment from a
   * different CDN
   *
   * Unless you are writing a test, use [safeCacheKey], which encodes the output of this
   */
  @JvmSynthetic
  internal fun generateCacheKey(
    requestUrl: URL,
  ): String {
    val urlStr = requestUrl.toString()
    val matchResult = RX_CHUNK_URL.find(urlStr)

    val key = if (matchResult == null) {
      urlStr
    } else {
      val extension = matchResult.groups[3]!!.value
      val isSegment = extension.oneOf(CacheConstants.EXT_TS, CacheConstants.EXT_M4S)

      if (isSegment) {
        // todo - we can pull out more-specific key parts using groups 2 and 1 if we want, but the
        //  paths in the urls are standardized in mux video so this implementation just relies on
        //  that for simplicity
        requestUrl.path
      } else {
        urlStr
      }
    }

    return key
  }

  /**
   * Generates a URL-safe cache key for a given URL. Delegates to [generateCacheKey] but encodes it
   * into something else
   */
  @JvmSynthetic
  internal fun safeCacheKey(url: URL): String = Base64.encodeToString(
    generateCacheKey(url).toByteArray(Charsets.UTF_8),
    Base64.URL_SAFE
  )

  private fun ensureDirs() {
    fileTempDir().mkdirs()
    fileCacheDir().mkdirs()
    indexDbDir().mkdirs()
  }

  /**
   * Deletes all temporary files. Under normal circumstances, temp files are moved to cache files
   * once the download is complete. There are mechanisms to delete temp files in the event of
   * errors but nothing is 100% guaranteed.
   *
   * As such, this method should be called at least once per Process, while blocking for the
   * database to be opened. If our error-handling is thorough, we shouldn't need to call this method
   * more times than that. Safely calling this once the datastore can be used would be complex.
   */
  private fun clearTempFiles() {
    val fileTempDir = fileTempDir()

    if (fileTempDir.exists() && fileTempDir.isDirectory) {
      fileTempDir.listFiles()?.onEach { tempFile ->
        if (tempFile.isDirectory) { // probably not, but it's easy to catch
          tempFile.deleteRecursively()
        } else {
          tempFile.delete()
        }
      }
    } else {
      fileTempDir.delete()
      fileTempDir.mkdirs()
    }
  }

  private fun fileTempDir(): File = File(context.cacheDir, CacheConstants.TEMP_FILE_DIR)
  private fun fileCacheDir(): File = File(context.cacheDir, CacheConstants.CACHE_FILES_DIR)
  private fun indexDbDir(): File =
    File(context.filesDirNoBackupCompat, CacheConstants.CACHE_BASE_DIR)

  /**
   * Creates a new temp file for downloading-into. Temp files go in a special dir that gets cleared
   * out when the datastore is opened
   */
  private fun createTempMediaFile(fileBasename: String): File {
    return File.createTempFile("mux-download-$fileBasename", ".part", fileTempDir())
  }

  /**
   * Creates a new cache file named after its associated cache key. If a file with that name already
   * existed, it will be deleted.
   */
  private fun createCacheFile(url: URL, contentRange: CacheController.ContentRange? = null): File {
    val key = safeCacheKey(url)
    val basename = if (contentRange == null) {
      key
    } else {
      // we don't ever read the start/end from the name, but it makes the file unique enough
      key + "-${contentRange.startByte}-${contentRange.endByte}"
    }
    val cacheFile = File(fileCacheDir(), basename)
    cacheFile.delete()
    cacheFile.createNewFile()
    return cacheFile
  }

  // Starts opening the DB unless it's open or being opened. If it's open, you get the DbHelper.
  //  If it's still being opened on another thread, this method will block until the db has been
  //  opened.
  // If the db failed to open, this method will throw. Opening can be re-attempted after resolving
  @Throws(IOException::class)
  private fun awaitDbHelper(): DbHelper {
    fun closeIfInterrupted(dbHelper: DbHelper?) {
      if (Thread.interrupted()) {
        dbHelper?.close()
        throw CancellationException("open interrupted")
      }
    }

    fun doOpen(): DbHelper {
      // todo - we should also consider getting our cacheQuota here, that will take a long time
      //  so maybe do it async & only consider the cache quota once we have it(..?)
      closeIfInterrupted(null)
      clearTempFiles()
      closeIfInterrupted(null)
      ensureDirs()

      val helper = DbHelper(context, indexDbDir())
      closeIfInterrupted(helper)
      val db = helper.writableDatabase
      // todo- eviction pass with that db
      closeIfInterrupted(helper)
      return helper;
    }

    val needToStart = openTask.compareAndSet(null, FutureTask { doOpen() })
    try {
      val actualTask = openTask.get()!!
      if (needToStart) {
        actualTask.run()
      }
      return actualTask.get()
    } catch (e: Exception) {
      // todo - get a Logger down here
      Log.e("CacheDatastore", "failed to open cache", e)

      // subsequent calls can attempt again
      openTask.set(null)

      throw IOException(e)
    }
  }
}

@JvmSynthetic
internal fun CachedResourceRecord.toContentValues(): ContentValues {
  val values = ContentValues()

  values.apply {
    put(IndexSchema.ResourcesTable.Columns.lookupKey, lookupKey)
    put(IndexSchema.ResourcesTable.Columns.etag, etag)
    put(IndexSchema.ResourcesTable.Columns.remoteUrl, url)
    put(IndexSchema.ResourcesTable.Columns.downloadedAtUnixTime, downloadedAtUtcSecs)
    put(IndexSchema.ResourcesTable.Columns.maxAgeUnixTime, cacheMaxAge)
    put(IndexSchema.ResourcesTable.Columns.resourceAgeUnixTime, resourceAge)
    put(IndexSchema.ResourcesTable.Columns.cacheControl, cacheControl)
  }

  return values
}

private class DbHelper(
  appContext: Context,
  directory: File,
) : SQLiteOpenHelper(
  /* context = */ appContext,
  /* name = */ File(directory, DB_FILE).path,
  null,
  IndexSchema.version
) {

  companion object {
    private const val DB_FILE = "mux-player-cache.db"
  }

  init {
    setWriteAheadLoggingEnabled(true)
    // todo - this is the modern way to enable WAL but requires very recent Android
//    setOpenParams(
//      SQLiteDatabase.OpenParams.Builder()
//        .setOpenFlags(SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING)
//        .build()
//    )
  }

  override fun onCreate(db: SQLiteDatabase?) {
    db?.execSQL(
      """
        create table if not exists ${IndexSchema.ResourcesTable.name} (
            ${IndexSchema.ResourcesTable.Columns.lookupKey} text not null primary key,
            ${IndexSchema.ResourcesTable.Columns.remoteUrl} text not null,
            ${IndexSchema.ResourcesTable.Columns.etag} text not null,
            TOTAL_SEGMENT_SIZE,
            ${IndexSchema.ResourcesTable.Columns.downloadedAtUnixTime} integer not null,
            ${IndexSchema.ResourcesTable.Columns.maxAgeUnixTime} integer not null,
            ${IndexSchema.ResourcesTable.Columns.resourceAgeUnixTime} integer not null default 0,
            ${IndexSchema.ResourcesTable.Columns.cacheControl} text not null
        )
        """
    )
    db?.execSQL(
      """
        create table if not exists ${IndexSchema.FilesTable.name} (
            ${IndexSchema.FilesTable.Columns.lookupKey} text not null,
            ${IndexSchema.FilesTable.Columns.filePath} text not null,
            ${IndexSchema.FilesTable.Columns.startOffset} integer not null,
            ${IndexSchema.FilesTable.Columns.endOffset} integer not null,
            ${IndexSchema.FilesTable.Columns.lastAccessedUtc} integer not null
        )
      """.trimIndent()
    )
  }

  override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    // todo - After we release this to people, we will need to start handling migrations (if we need
    //  update the schema)
    // for now, we just have to reinstall our apps
  }
}

/**
 * Returns this app's no-backup internal files dir, or the regular files dir on older api levels
 */
internal val Context.filesDirNoBackupCompat: File
  @SuppressLint("ObsoleteSdkInt") get() {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      noBackupFilesDir
    } else {
      filesDir
    }
  }

/**
 * Schema for the cache index
 */
internal object IndexSchema {

  const val version = 1

  object FilesTable {
    const val name = "file"

    object Columns {
      const val lookupKey = "lookup_key"
      const val filePath = "file_path"
      const val fileSize = "file_size"
      const val startOffset = "start_offset"
      const val endOffset = "end_offset"
      const val lastAccessedUtc = "last_access"
    }
  }

  object ResourcesTable {
    const val name = "resources"

    object Columns {
      /**
       * Key for matching URLs. Since we need to support multi-cdn without caching redundantly,
       * some files (segments) are keyed using a strategy other than hashing the entire URL.
       * Use [CacheDatastore.generateCacheKey] to calculate a cache key for a given URL
       */
      const val lookupKey = "lookup_key"

      /**
       * The URL of the remote resource
       */
      const val remoteUrl = "remote_url"

      /**
       * The etag of the response we cached for this resource
       */
      const val etag = "etag"

      /**
       * The time the resource was downloaded in unix time
       */
      const val downloadedAtUnixTime = "downloaded_at_unix_time"

      /**
       * The path of the cached copy of the file. This path is relative to the app's cache dir
       */
      const val filePath = "file_path"

      /**
       * Total size of the resource being cached
       */
      const val totalSize = "total_size"

      /**
       * Age of the resource as described by the `Age` header
       */
      const val resourceAgeUnixTime = "resource_age"

      /**
       * The `max-age` of the cache entry, as required by cache control.
       * `max-age` gets its own column because we must perform queries based on it
       */
      const val maxAgeUnixTime = "max_age_unix_time"

      /**
       * The 'Cache-Control' header that came down with the response we cached
       * Data found in this header may be duplicated in the table schema if it is required for
       * queries (like max-age)
       */
      const val cacheControl = "cache_control"
    }
  }
}
