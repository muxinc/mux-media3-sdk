package com.mux.player.media

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata

/**
 * Creates instances of [MediaItem] or [MediaItem.Builder] configured for easy use with
 * `MuxExoPlayer`
 *
 * TODO: Alternative spelling: MuxMediaItems
 */
object MediaItems {

  /**
   * Default domain + tld for Mux Video
   */
  @Suppress("MemberVisibilityCanBePrivate")
  const val MUX_VIDEO_DEFAULT_DOMAIN = "mux.com"

  private const val MUX_VIDEO_SUBDOMAIN = "stream"
  private const val EXTRA_VIDEO_DATA = "com.mux.video.customerdata"

  /**
   * Creates a new [MediaItem] that points to a given Mux Playback ID.
   *
   * @param playbackId A playback ID for a Mux Asset
   * @param domain Optional custom domain for Mux Video. The default is [MUX_VIDEO_DEFAULT_DOMAIN]
   *
   * @see builderFromMuxPlaybackId
   */
  @JvmStatic
  @JvmOverloads
  fun fromMuxPlaybackId(
    playbackId: String,
    maxResolution: PlaybackMaxResolution? = null,
    domain: String = MUX_VIDEO_DEFAULT_DOMAIN,
    playbackToken: String? = null,
  ): MediaItem = builderFromMuxPlaybackId(
    playbackId,
    maxResolution,
    domain,
    playbackToken,
  ).build()

  /**
   * Creates a new [MediaItem.Builder] that points to a given Mux Playback ID. You can add
   * additional configuration to the `MediaItem` before you build it
   *
   * @param playbackId A playback ID for a Mux Asset
   * @param domain Optional custom domain for Mux Video. The default is [MUX_VIDEO_DEFAULT_DOMAIN]
   *
   * @see fromMuxPlaybackId
   */
  @JvmStatic
  @JvmOverloads
  fun builderFromMuxPlaybackId(
    playbackId: String,
    maxResolution: PlaybackMaxResolution? = null,
    domain: String = MUX_VIDEO_DEFAULT_DOMAIN,
    playbackToken: String? = null,
  ): MediaItem.Builder {
    return MediaItem.Builder()
      .setUri(
        createPlaybackUrl(
          playbackId = playbackId,
          domain = domain,
          maxResolution = maxResolution,
          playbackToken = playbackToken,
        )
      )
      .setRequestMetadata(
        RequestMetadata.Builder()
          .build()
      )
  }

  private fun createPlaybackUrl(
    playbackId: String,
    domain: String = MUX_VIDEO_DEFAULT_DOMAIN,
    subdomain: String = MUX_VIDEO_SUBDOMAIN,
    maxResolution: PlaybackMaxResolution? = null,
    playbackToken: String? = null,
  ): String {
    val base = Uri.parse("https://$subdomain.$domain/$playbackId.m3u8").buildUpon()

    maxResolution?.let { base.appendQueryParameter("max_resolution", resolutionValue(it)) }
    playbackToken?.let { base.appendQueryParameter("token", it) }

    return base.build().toString()
  }

  private fun resolutionValue(playbackMaxResolution: PlaybackMaxResolution): String {
    return when (playbackMaxResolution) {
      PlaybackMaxResolution.HD_720 -> "720p"
      PlaybackMaxResolution.FHD_1080 -> "1080p"
      PlaybackMaxResolution.QHD_1440 -> "1440p"
      PlaybackMaxResolution.FOUR_K_2160 -> "2160p"
    }
  }
}

/**
 * A resolution for playing back Mux assets. If specified in [MediaItems.fromMuxPlaybackId], or
 * similar methods, the video's resolution will be limited to the given value
 */
enum class PlaybackMaxResolution {
  HD_720,
  FHD_1080,
  QHD_1440,
  FOUR_K_2160,
}
