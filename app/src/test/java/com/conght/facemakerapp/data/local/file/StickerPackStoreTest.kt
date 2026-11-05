package com.conght.facemakerapp.data.local.file

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StickerPackStoreTest {

    @Test
    fun packId_isStableAndDependsOnImages() {
        val id = stickerPackId(listOf("/c/a.png", "/c/b.png"))
        assertEquals(id, stickerPackId(listOf("/c/a.png", "/c/b.png")))
        assertNotEquals(id, stickerPackId(listOf("/c/b.png", "/c/a.png")))
        assertTrue(id, Regex("pack_[0-9a-f]{32}").matches(id))
    }

    @Test
    fun compress_lowersQualityUntilWithinLimit() {
        val qualities = mutableListOf<Int>()
        val result = compressWithinLimit(limitBytes = 50) { quality ->
            qualities += quality
            ByteArray(quality)
        }
        assertEquals(listOf(90, 80, 70, 60, 50), qualities)
        assertEquals(50, result.size)
    }

    @Test(expected = IllegalStateException::class)
    fun compress_failsWhenLimitCannotBeMet() {
        compressWithinLimit(limitBytes = 1) { ByteArray(100) }
    }
}
