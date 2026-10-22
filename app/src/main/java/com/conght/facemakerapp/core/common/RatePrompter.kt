package com.conght.facemakerapp.core.common

import com.conght.facemakerapp.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quyết định khi nào hiện Rate us (khi người dùng chưa rate, tối đa 1 lần/ngày):
 * - sau lần lưu creation thành công thứ 2 (kiểm tra ở màn Successful) – 1 lần;
 * - lần đầu bấm back ở Home – 1 lần.
 */
@Singleton
class RatePrompter internal constructor(
    private val settings: SettingsRepository,
    private val today: () -> Long,
) {
    @Inject
    constructor(settings: SettingsRepository) : this(settings, { System.currentTimeMillis() / MILLIS_PER_DAY })

    suspend fun shouldPromptAfterSave(): Boolean {
        if (settings.rated.first()) return false
        val state = settings.ratePromptState.first()
        if (state.afterSavePrompted || state.lastPromptDay == today()) return false
        if (settings.creationSaveCount.first() < SAVES_BEFORE_PROMPT) return false
        settings.setRatePromptState(state.copy(afterSavePrompted = true, lastPromptDay = today()))
        return true
    }

    suspend fun shouldPromptOnHomeExit(): Boolean {
        if (settings.rated.first()) return false
        val state = settings.ratePromptState.first()
        if (state.homeExitPrompted || state.lastPromptDay == today()) return false
        settings.setRatePromptState(state.copy(homeExitPrompted = true, lastPromptDay = today()))
        return true
    }

    private companion object {
        const val SAVES_BEFORE_PROMPT = 2
        const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }
}
