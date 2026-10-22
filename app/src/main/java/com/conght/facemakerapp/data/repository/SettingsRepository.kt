package com.conght.facemakerapp.data.repository

import com.conght.facemakerapp.data.local.datastore.SettingsDataStore
import com.conght.facemakerapp.data.model.RatePromptState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface SettingsRepository {
    val languageCode: Flow<String?>
    suspend fun setLanguage(code: String)
    val onboardingDone: Flow<Boolean>
    suspend fun setOnboardingDone(done: Boolean)
    val rated: Flow<Boolean>
    suspend fun setRated(rated: Boolean)

    /** Instruction của Cosplay đã tự hiện lần đầu chưa. */
    val cosplayInstructionShown: Flow<Boolean>
    suspend fun setCosplayInstructionShown()

    /** Số lần lưu creation thành công (cho Rate us trigger). */
    val creationSaveCount: Flow<Int>
    suspend fun incrementCreationSaveCount()

    val ratePromptState: Flow<RatePromptState>
    suspend fun setRatePromptState(state: RatePromptState)
}

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore,
) : SettingsRepository {
    override val languageCode = dataStore.languageCode
    override suspend fun setLanguage(code: String) = dataStore.setLanguage(code)
    override val onboardingDone = dataStore.onboardingDone
    override suspend fun setOnboardingDone(done: Boolean) = dataStore.setOnboardingDone(done)
    override val rated = dataStore.rated
    override suspend fun setRated(rated: Boolean) = dataStore.setRated(rated)
    override val cosplayInstructionShown = dataStore.cosplayInstructionShown
    override suspend fun setCosplayInstructionShown() = dataStore.setCosplayInstructionShown()
    override val creationSaveCount = dataStore.creationSaveCount
    override suspend fun incrementCreationSaveCount() = dataStore.incrementCreationSaveCount()
    override val ratePromptState = dataStore.ratePromptState
    override suspend fun setRatePromptState(state: RatePromptState) = dataStore.setRatePromptState(state)
}
