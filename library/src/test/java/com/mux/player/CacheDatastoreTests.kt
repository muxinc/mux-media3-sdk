package com.mux.player

import android.content.Context
import com.mux.player.internal.cache.CacheDatastore
import com.mux.player.internal.createNoLogger
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.net.URL

class CacheDatastoreTests : AbsRobolectricTest() {

  private lateinit var cacheDatastore: CacheDatastore

  @Before
  fun setUp() {
    val mockContext = mockk<Context> {
      every { applicationContext } returns mockk<Context> {

      }
    }

    cacheDatastore = CacheDatastore(mockContext, logger = createNoLogger())
  }

  @Test
  fun `generateCacheKey generates special keys for segments`() {
    val notASegmentUrl = "https://manifest-gcp-us-east4-vop1.cfcdn.mux.com/efg456hjk/rendition.m3u8"
    val hlsSegmentUrl = "https://chunk-gcp-us-east4-vop1.cfcdn.mux.com/v1/chunk/hls123abc/0.ts"
    val cmafSegmentUrl = "https://chunk-gcp-us-east4-vop1.cfcdn.mux.com/v1/chunk/cmaf456def/146.m4s"

    val notASegmentKey =
      cacheDatastore.generateCacheKey(URL(notASegmentUrl))
    val hlsKey = cacheDatastore.generateCacheKey(URL(hlsSegmentUrl))
    val cmafKey = cacheDatastore.generateCacheKey(URL(cmafSegmentUrl))

    Assert.assertEquals(
      "Non-segment URLs key on the entire URL",
      notASegmentUrl, notASegmentKey
    )
    Assert.assertNotEquals(
      "HLS segment URLs have a special key",
      hlsKey, hlsSegmentUrl
    )
    Assert.assertNotEquals(
      "CMAF segment URLs have a special key",
      cmafKey, cmafSegmentUrl
    )
  }

  @Test
  fun `generateCacheKey generates cache keys for segments correctly`() {
    val segmentUrl = "https://chunk-gcp-us-east4-vop1.cfcdn.mux.com/v1/chunk/abc12345xyz/0.ts"
    val expectedKey = "/v1/chunk/abc12345xyz/0.ts"

    val key = cacheDatastore.generateCacheKey(URL(segmentUrl))
    Assert.assertEquals(
      "cache key should be constructed properly",
      expectedKey, key
    )
  }
}
