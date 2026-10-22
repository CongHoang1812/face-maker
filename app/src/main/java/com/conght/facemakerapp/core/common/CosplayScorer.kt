package com.conght.facemakerapp.core.common

import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.PartSelection
import kotlin.math.roundToInt

object CosplayScorer {

    private val NONE = PartSelection(itemId = null, colorId = null)

    /**
     * % category khớp so với đáp án. Một category khớp khi itemId bằng nhau
     * và (nếu đáp án có colorId) colorId cũng bằng nhau. None vs None = khớp.
     */
    fun score(answer: CharacterConfig, user: CharacterConfig): Int {
        val total = answer.selections.size
        if (total == 0) return 0
        val matched = answer.selections.count { (categoryId, expected) ->
            val actual = user.selections[categoryId] ?: NONE
            expected.itemId == actual.itemId &&
                (expected.colorId == null || expected.colorId == actual.colorId)
        }
        return (matched * 100.0 / total).roundToInt()
    }
}
