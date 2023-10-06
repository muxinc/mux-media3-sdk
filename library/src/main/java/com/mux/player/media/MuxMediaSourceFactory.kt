package com.mux.player.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.CmcdConfiguration

/**
 * A [MediaSource.Factory] configured to work best with Mux Video. Prefer to use this over
 * [DefaultMediaSourceFactory] so we can provide our own defaults (eg, turning on CMCD, etc)
 *
 * It's backed by a [DefaultMediaSourceFactory] that you can configure further using [innerFactory]
 *
 * @see innerFactory
 */
@OptIn(UnstableApi::class)
class MuxMediaSourceFactory private constructor(
  @Suppress("MemberVisibilityCanBePrivate") val innerFactory: DefaultMediaSourceFactory
): MediaSource.Factory by innerFactory {

  constructor(context: Context): this(DefaultMediaSourceFactory(context))

  init {
    setCmcdConfigurationFactory(CmcdConfiguration.Factory.DEFAULT)
    // TODO: Figure out what else to put here/if I need to configure the CmcdConfigFactory
  }
}