package com.mux.player.media3.examples

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.mux.stats.sdk.core.model.CustomData
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.core.model.CustomerVideoData
import com.mux.stats.sdk.core.model.CustomerViewData
import com.mux.stats.sdk.core.util.UUID
import com.mux.player.MuxPlayer
import com.mux.player.media.MediaItems
import com.mux.player.media.PlaybackResolution
import com.mux.player.media3.PlaybackIds
import com.mux.player.media3.databinding.ActivityBasicPlayerBinding
import com.mux.stats.sdk.core.model.CustomerPlayerData

/**
 * A simple example that uses the normal media3 player UI to play a video in the foreground from
 * Mux Video, using a Playback ID
 */
class DrmPlaybackActivity : AppCompatActivity() {

  data class DrmExample(
    val title: String,
    val playbackId: String,
    val playbackToken: String,
    val drmToken: String
  )

  private lateinit var binding: ActivityBasicPlayerBinding
  private val playerView get() = binding.player

  private var player: MuxPlayer? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityBasicPlayerBinding.inflate(layoutInflater)
    setContentView(binding.root)
  }

  override fun onStart() {
    super.onStart()

    playSomething()
  }

  override fun onStop() {
    tearDownPlayer()

    super.onStop()
  }

  private fun tearDownPlayer() {
    playerView.player = null
    player?.release()
  }

  private fun playSomething() {
    val INDEX = 0

    val player = createPlayer(this)
    val mediaItem = MediaItems.builderFromMuxPlaybackId(
      playbackId = DRM_EXAMPLES[INDEX].playbackId,
      playbackToken = DRM_EXAMPLES[INDEX].playbackToken,
      drmToken = DRM_EXAMPLES[INDEX].drmToken
    )
      .setMediaMetadata(
        MediaMetadata.Builder()
          .setTitle("Basic MuxPlayer Example")
          .build()
      )
      .build()
    player.setMediaItem(mediaItem)
    player.prepare()
    player.playWhenReady = true

    this.playerView.player = player
    this.player = player
  }

  @OptIn(UnstableApi::class)
  private fun createPlayer(context: Context): MuxPlayer {
    val out: MuxPlayer = MuxPlayer.Builder(context)
      .addMonitoringData(
        CustomerData().apply {
          customerViewData = CustomerViewData().apply {
            viewSessionId = UUID.generateUUID()
          }
          customerVideoData = CustomerVideoData().apply {
            videoTitle = "DRM Playback Example"
            videoSeries = "Mux Player for Android"
            videoId = "abc1234zyxw"
          }
        }
      )
      .build()

    out.addListener(object : Player.Listener {
      override fun onPlayerError(error: PlaybackException) {
        Log.e(TAG, "player error!", error)
        Toast.makeText(
          this@DrmPlaybackActivity,
          "Playback error! ${error.localizedMessage}",
          Toast.LENGTH_LONG
        ).show()
      }
    })

    return out
  }

  // todo - temporary object with hard-coded playbackIDs and tokens
  companion object {
    val TAG = DrmPlaybackActivity::class.simpleName
    val DRM_EXAMPLES = listOf(
      DrmExample(
        title = "Staging test PlaybackID 1",
        playbackId = "UHMpUMz4l00SmDcgAAQPd4Yk01200IDwD4uD7K24GPp01yg",
        playbackToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJ2IiwiZXhwIjoxNzIyNjE2OTc0LCJraWQiOiJFelE2SkI1ZkQwMmd5TmVxUmE4MDJYT2xnMDE0SzAxckxwdXNDbklRSjJobEtYbyIsInN1YiI6IlVITXBVTXo0bDAwU21EY2dBQVFQZDRZazAxMjAwSUR3RDR1RDdLMjRHUHAwMXlnIn0.rDymk2prKKDqlSKMnWDl24YNQ_LfnPlEzkBFr2-M3Yb_0mABE_bp-a2NeIKgwmqPxSvS0VAXpJApMbNa1j43yzW8oQxyZZXWnw0NLTQfdKfafDs83JVJB7uhL7MeEXcs1lpJGwLPSDYwdIPt2dKNzbATjqRViYbUO4GF14cq_35xsCb-kZy4D42_pdn62K6XnDUqccEeUmBav8W7m8ZILZ4eBJJJdCGVB9B85uGse_YTokGrXZ-chVO-uZ328B6ns_ehnhbJPtpstmUHvaqo0Xf0qF1J7pKkpbVoMwyhERB6M70m3oijP2GM1kLKAayrh1ujmNRNTXLcRJeBobqmPg",
        drmToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJsIiwiZXhwIjoxNzIyNjE2OTE0LCJraWQiOiJFelE2SkI1ZkQwMmd5TmVxUmE4MDJYT2xnMDE0SzAxckxwdXNDbklRSjJobEtYbyIsInN1YiI6IlVITXBVTXo0bDAwU21EY2dBQVFQZDRZazAxMjAwSUR3RDR1RDdLMjRHUHAwMXlnIn0.tHmqMgHf3pY2adP9QVvx9VIUVZvaxzWZP8Qf4DSUBnT4Zxac-tRPBsHDtBlFIILhmPhjBa2IAmD2PdqgHopSxw_zDp9ktTl6QAKCGgw40ZUKt4GD4aZKubKzAyfPm5q0-7f8aW8oNDbejQ1VjN5QqIBb50ytyPc4NkIzwqJ3P3azrr4TSlo-NiXbXhwWuiMHGqspoNPk8BGBcXpSML7vghlncxwKWYAwbpPaz5q5AEMmN5sqKo7woSVsXBxoe78al6cfT2SRdDR6bu92kMf5zSZ9600boNSjmNn2Dx5IidFAZMYy9qVj22W1T-7rCthmc37c9OcUGK9g0unHEAFE6A"
      ),
      DrmExample(
        title = "Staging Test PlaybackID 2",
        playbackId = "OZYvDVHsfLebZw00En9vOO8Ta1pcIeuPO4Esbv2yCv4E",
        playbackToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJ2IiwiZXhwIjoxNzIyNjE4ODQxLCJraWQiOiJFelE2SkI1ZkQwMmd5TmVxUmE4MDJYT2xnMDE0SzAxckxwdXNDbklRSjJobEtYbyIsInN1YiI6Ik9aWXZEVkhzZkxlYlp3MDBFbjl2T084VGExcGNJZXVQTzRFc2J2MnlDdjRFIn0.p4D33mKmiHZYiO4Zhihx48MNcJQu0orZkezy1Wubrr3rTMInJSSlBEqaqEKgfSo505qXHx9n-zabIuM4hbGmpVNPY2aX8L3jDZU-o076NuYCjpiB87eQd6ilimOw5U-n55uCeYDXO6WYENmsy3trq-8hBMTmdloNeFXnCx1aECETU4ZmXXo3GnZBkWEWpRHyVqhFFOYxkeEWWHMvgrGoqkZHvLhHC93H9maz3KKCrqFqJeFrEo_idoJ-AsBqYhTGKhO2uGV_fhGUda6Qetc9QrqEK0WuxHwqpRbjR1cyvTbWDwCcvES1gXx4UDiWs1wdpZuyC3j2Y4LuPGAiLVWatA",
        drmToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJsIiwiZXhwIjoxNzIyNjE4ODI4LCJraWQiOiJFelE2SkI1ZkQwMmd5TmVxUmE4MDJYT2xnMDE0SzAxckxwdXNDbklRSjJobEtYbyIsInN1YiI6Ik9aWXZEVkhzZkxlYlp3MDBFbjl2T084VGExcGNJZXVQTzRFc2J2MnlDdjRFIn0.K6CCI-RGsTXGK0y-u2SseXea33tR5SbbX9wvucF7j0UictV6_VB0TsZe3SPlU_3ST0ecedLegbyJu-_4I6h7-XDApfXCGslYFoqM5iZnQ_5YtL0Zkdeh2iHJZKyS-mH_z6lyojggbFPLFGgRC0gZVfXJwdDtAUi33wOnlvkvGOdzNXmJCrRInkg7OfRKvLzxkQnQ0kTagKtq74Uv5JpG6XeascSi6tXExM8KVxG-4VEWHvBqCQvrpV6xlZmSnlOvoLT7E2oQB-6rwvy4cFnA_1ZASxHxiAZTNppPTmdmDYxDUd8qwJiL7MtF73sfqcrooH-z9p35u9t7eqiUGlslcA"
      )
    )
  }
}
