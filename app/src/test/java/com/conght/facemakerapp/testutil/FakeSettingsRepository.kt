package com.conght.facemakerapp.testutil

import com.conght.facemakerapp.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** SettingsRepository trong bộ nhớ cho unit test. */
class FakeSettingsRepository : SettingsRepository {
    override val languageCode = MutableStateFlow<String?>(null)
    override suspend fun setLanguage(code: String) {
        languageCode.value = code
    }

    override val onboardingDone = MutableStateFlow(false)
    override suspend fun setOnboardingDone(done: Boolean) {
        onboardingDone.value = done
    }

    override val cosplayInstructionShown = MutableStateFlow(false)
    override suspend fun setCosplayInstructionShown() {
        cosplayInstructionShown.value = true
    }
}
