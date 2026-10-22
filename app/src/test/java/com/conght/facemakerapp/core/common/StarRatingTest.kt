package com.conght.facemakerapp.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class StarRatingTest {

    @Test
    fun thresholds() {
        assertEquals(3, StarRating.fromPercent(100))
        assertEquals(3, StarRating.fromPercent(90))
        assertEquals(2, StarRating.fromPercent(89))
        assertEquals(2, StarRating.fromPercent(60))
        assertEquals(1, StarRating.fromPercent(59))
        assertEquals(1, StarRating.fromPercent(30))
        assertEquals(0, StarRating.fromPercent(29))
        assertEquals(0, StarRating.fromPercent(0))
    }
}
