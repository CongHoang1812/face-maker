package com.conght.facemakerapp.core.common

import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.PartSelection
import org.junit.Assert.assertEquals
import org.junit.Test

class CosplayScorerTest {

    private fun config(vararg selections: Pair<String, PartSelection>) =
        CharacterConfig(templateId = "t", selections = selections.toMap())

    private val answer = config(
        "hair" to PartSelection("hair_1", "red"),
        "eyes" to PartSelection("eyes_2", null),
        "hat" to PartSelection("hat_3", null),
        "cape" to PartSelection("cape_1", "blue"),
    )

    @Test
    fun allMatch_is100() {
        assertEquals(100, CosplayScorer.score(answer, answer))
    }

    @Test
    fun noneMatch_is0() {
        val user = config(
            "hair" to PartSelection("hair_9", "red"),
            "eyes" to PartSelection("eyes_9", null),
            "hat" to PartSelection(null, null),
        )
        assertEquals(0, CosplayScorer.score(answer, user))
    }

    @Test
    fun halfMatch_is50() {
        val user = config(
            "hair" to PartSelection("hair_1", "red"),
            "eyes" to PartSelection("eyes_2", "any"), // answer không có colorId -> bỏ qua màu
            "hat" to PartSelection("hat_9", null),
            "cape" to PartSelection("cape_1", "green"), // sai màu
        )
        assertEquals(50, CosplayScorer.score(answer, user))
    }

    @Test
    fun noneVsNone_matches() {
        val noneAnswer = config("hat" to PartSelection(null, null), "hair" to PartSelection("hair_1", null))
        val user = config("hair" to PartSelection("hair_1", null)) // thiếu "hat" = None
        assertEquals(100, CosplayScorer.score(noneAnswer, user))
    }

    @Test
    fun noneVsNone_notCounted_extraItemPenalized() {
        val sparseAnswer = config(
            "hair" to PartSelection("hair_1", null),
            "hat" to PartSelection(null, null),
            "cape" to PartSelection(null, null),
            "mask" to PartSelection(null, null),
        )
        assertEquals(0, CosplayScorer.score(sparseAnswer, config()))
        val user = config("hair" to PartSelection("hair_1", null), "mask" to PartSelection("mask_1", null))
        assertEquals(50, CosplayScorer.score(sparseAnswer, user))
    }

    @Test
    fun emptyAnswer_is0() {
        assertEquals(0, CosplayScorer.score(config(), answer))
    }
}
