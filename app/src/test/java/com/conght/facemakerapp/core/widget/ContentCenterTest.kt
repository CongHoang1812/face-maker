package com.fantasy.oc.maker.customize.core.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentCenterTest {

    @Test
    fun centerOfOpaqueBox_inTopLeftCorner() {
        // Nội dung chiếm x 0..19, y 10..29 trong ảnh 100×100
        val center = contentCenter(100, 100) { x, y -> if (x < 20 && y in 10 until 30) 255 else 0 }!!
        assertEquals(0.1f, center.first, 0.0001f)
        assertEquals(0.2f, center.second, 0.0001f)
    }

    @Test
    fun fullyTransparent_returnsNull() {
        assertNull(contentCenter(100, 100) { _, _ -> 0 })
    }

    @Test
    fun faintPixels_areIgnored() {
        val center = contentCenter(100, 100) { x, _ -> if (x < 10) 255 else 1 }!!
        assertEquals(0.05f, center.first, 0.0001f)
    }

    @Test
    fun largeImage_isSampled_notScannedPixelByPixel() {
        var reads = 0
        contentCenter(2048, 2048) { _, _ ->
            reads++
            255
        }
        assertEquals(128 * 128, reads)
    }
}
