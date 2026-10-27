package com.conght.facemakerapp.core.common

import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.PartSelection
import kotlin.math.roundToInt

object CosplayScorer {

    private val NONE = PartSelection(itemId = null, colorId = null)

    /**
     * % category khớp so với đáp án, chỉ tính các category mà đáp án hoặc người chơi có chọn item
     * (None vs None bị bỏ qua để đáp án nhiều None không làm điểm cao sẵn). Một category khớp khi
     * itemId bằng nhau và (nếu đáp án có colorId) colorId cũng bằng nhau.
     */
    fun score(answer: CharacterConfig, user: CharacterConfig): Int {
        val counted = (answer.selections.keys + user.selections.keys).filter { categoryId ->
            answer.selections[categoryId]?.itemId != null || user.selections[categoryId]?.itemId != null
        }
        if (counted.isEmpty()) return 0
        val matched = counted.count { categoryId ->
            val expected = answer.selections[categoryId] ?: NONE
            val actual = user.selections[categoryId] ?: NONE
            expected.itemId == actual.itemId &&
                (expected.colorId == null || expected.colorId == actual.colorId)
        }
        return (matched * 100.0 / counted.size).roundToInt()
    }
}
