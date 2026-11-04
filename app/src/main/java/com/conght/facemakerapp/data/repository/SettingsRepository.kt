package com.conght.facemakerapp.data.repository

import com.conght.facemakerapp.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface SettingsRepository {
    val languageCode: Flow<String?>
    suspend fun setLanguage(code: String)
    val onboardingDone: Flow<Boolean>
    suspend fun setOnboardingDone(done: Boolean)

    /** Instruction của Cosplay đã tự hiện lần đầu chưa. */
    val cosplayInstructionShown: Flow<Boolean>
    suspend fun setCosplayInstructionShown()
}

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore,
) : SettingsRepository {
    override val languageCode = dataStore.languageCode
    override suspend fun setLanguage(code: String) = dataStore.setLanguage(code)
    override val onboardingDone = dataStore.onboardingDone
    override suspend fun setOnboardingDone(done: Boolean) = dataStore.setOnboardingDone(done)
    override val cosplayInstructionShown = dataStore.cosplayInstructionShown
    override suspend fun setCosplayInstructionShown() = dataStore.setCosplayInstructionShown()
}
