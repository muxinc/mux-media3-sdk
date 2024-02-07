package com.mux.player.internal.cache

import android.database.Cursor
import com.mux.player.cacheing.CacheConstants
import com.mux.player.cacheing.CacheController
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@JvmSynthetic
@Throws(IOException::class)
internal fun Cursor.getStringOrThrow(name: String): String {
  val idx = getColumnIndex(name)
  return if (idx >= 0) {
    getString(idx)
  } else {
    throw IOException("Could not find expected column: $name")
  }
}

@JvmSynthetic
@Throws(IOException::class)
internal fun Cursor.getLongOrThrow(name: String): Long {
  val idx = getColumnIndex(name)
  return if (idx >= 0) {
    getLong(idx)
  } else {
    throw IOException("Could not find expected column: $name")
  }
}

@JvmSynthetic
@Throws(IOException::class)
internal fun InputStream.consumeInto(outputStream: OutputStream, readSize: Int = 32 * 1024) {
  val buf = ByteArray(readSize)
  while (true) {
    val readBytes = read(buf)
    if (readBytes == -1) {
      // done
      break
    } else {
      outputStream.write(buf, 0, readBytes)
    }
  }
}

@JvmSynthetic
internal fun isContentTypeSegment(contentTypeHeader: String?): Boolean {
  return contentTypeHeader.equals(CacheConstants.MIME_TS, true)
          || contentTypeHeader.equals(CacheConstants.MIME_M4S, true)
          || contentTypeHeader.equals(CacheConstants.MIME_M4S_ALT, true)
}

@JvmSynthetic
internal fun isContentTypePlaylist(contentTypeHeader: String?): Boolean {
  return (contentTypeHeader.equals("application/vnd.apple.mpegurl", true)
          || contentTypeHeader.equals("audio/mpegurl", true)
          || contentTypeHeader.equals("application/mpegurl", true)
          || contentTypeHeader.equals("application/x-mpegurl", true)
          || contentTypeHeader.equals("audio/x-mpegurl", true)
          )
}

// todo - make a Headers model
internal fun Map<String, List<String>>.getCacheControl(): String? =
  mapKeys { it.key.lowercase() }["cache-control"]?.last()

// todo - make a Headers model
internal fun Map<String, List<String>>.getETag(): String? =
  mapKeys { it.key.lowercase() }["etag"]?.last()

// todo - make a Headers model
internal fun Map<String, List<String>>.getAge(): String? =
  mapKeys { it.key.lowercase() }["age"]?.last()

// todo - make a Headers model
internal fun Map<String, List<String>>.getContentType(): String? =
  mapKeys { it.key.lowercase() }["content-type"]?.last()

@JvmSynthetic
internal fun parseSMaxAge(cacheControl: String): Long? {
  val matchResult = CacheController.RX_S_MAX_AGE.matchEntire(cacheControl)
  return if (matchResult == null) {
    null
  } else {
    val maxAgeSecs = matchResult.groupValues[1]
    maxAgeSecs.toLongOrNull()
  }
}

@JvmSynthetic
internal fun parseMaxAge(cacheControl: String): Long? {
  val matchResult = CacheController.RX_MAX_AGE.matchEntire(cacheControl)
  return if (matchResult == null) {
    null
  } else {
    val maxAgeSecs = matchResult.groupValues[1]
    maxAgeSecs.toLongOrNull()
  }
}
