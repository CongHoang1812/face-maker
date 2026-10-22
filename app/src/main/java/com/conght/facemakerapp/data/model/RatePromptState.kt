package com.conght.facemakerapp.data.model

/** Trạng thái đã nhắc Rate us ([lastPromptDay] = số ngày kể từ epoch, -1 = chưa nhắc). */
data class RatePromptState(
    val lastPromptDay: Long = -1L,
    val afterSavePrompted: Boolean = false,
    val homeExitPrompted: Boolean = false,
)
