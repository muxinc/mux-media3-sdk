package com.mux.player.media

import androidx.annotation.GuardedBy
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.DrmConfiguration
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.DrmSessionManager
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.drm.ExoMediaDrm
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.MediaDrmCallback
import java.util.UUID

@OptIn(UnstableApi::class)
class MuxDrmSessionManagerProvider(
  val drmHttpDataSourceFactory: HttpDataSource.Factory,
) : DrmSessionManagerProvider {

  private val lock = Any()
  private var mediaItem: MediaItem? = null
  private var sessionManager: DrmSessionManager? = null

  override fun get(mediaItem: MediaItem): DrmSessionManager {
    synchronized(lock) {
      val currentSessionManager = sessionManager
      if (currentSessionManager != null && this.mediaItem == mediaItem) {
        return currentSessionManager
      } else {
        return createSessionManager(mediaItem)
      }
    }
  }

  private fun createSessionManager(mediaItem: MediaItem): DrmSessionManager {

    // todo - MuxDrmCallback needs playback id and drm key

    return DefaultDrmSessionManager.Builder()
      .setUuidAndExoMediaDrmProvider(C.WIDEVINE_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
      .setMultiSession(false)
      //.setPlayClearSamplesWithoutKeys(true) // todo - right?? Well probably not
      .build(MuxDrmCallback(drmHttpDataSourceFactory))
  }
}


// TO WORRY ABOUT
// custom domain + drm token

// iOS - App Certificate fetched
// DRMToday.swift

@OptIn(UnstableApi::class)
class MuxDrmCallback(
  val drmHttpDataSourceFactory: HttpDataSource.Factory
) : MediaDrmCallback {

  companion object {
  }

  override fun executeProvisionRequest(
    uuid: UUID,
    request: ExoMediaDrm.ProvisionRequest
  ): ByteArray {
    TODO("Not yet implemented")
  }

  override fun executeKeyRequest(uuid: UUID, request: ExoMediaDrm.KeyRequest): ByteArray {
    TODO("Not yet implemented")
  }

  private fun createLicenseUri(playbackId: String, drmToken: String, domain: String): String {
    return "https://license.${domain}/license/widevine/${playbackId}?token=${drmToken}"
  }

  private fun createKeyUri(playbackId: String, drmToken: String, domain: String): String {
    // todo - assmption that the keys are at key.mux.com (or whatever)
    return "https://key.${domain}/license/widevine/${playbackId}?token=${drmToken}"
  }
}
