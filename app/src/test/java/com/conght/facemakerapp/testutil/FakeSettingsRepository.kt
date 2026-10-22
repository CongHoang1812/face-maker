package com.conght.facemakerapp.testutil

import com.conght.facemakerapp.data.model.RatePromptState
import com.conght.facemakerapp.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** SettingsRepository trong bộ nhớ cho unit test. */
class FakeSettingsRepository(
    rated: Boolean = false,
    saveCount: Int = 0,
) : SettingsRepository {
    override val languageCode = MutableStateFlow<String?>(null)
    override suspend fun setLanguage(code: String) {
        languageCode.value = code
    }

    override val onboardingDone = MutableStateFlow(false)
    override suspend fun setOnboardingDone(done: Boolean) {
        onboardingDone.value = done
    }

    override val rated = MutableStateFlow(rated)
    override suspend fun setRated(rated: Boolean) {
        this.rated.value = rated
    }

    override val cosplayInstructionShown = MutableStateFlow(false)
    override suspend fun setCosplayInstructionShown() {
        cosplayInstructionShown.value = true
    }

    override val creationSaveCount = MutableStateFlow(saveCount)
    override suspend fun incrementCreationSaveCount() {
        creationSaveCount.value++
    }

    override val ratePromptState = MutableStateFlow(RatePromptState())
    override suspend fun setRatePromptState(state: RatePromptState) {
        ratePromptState.value = state
    }
}
