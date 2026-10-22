package com.conght.facemakerapp.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.conght.facemakerapp.data.model.RatePromptState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.settingsDataStore

    val languageCode: Flow<String?> = dataStore.data.map { it[KEY_LANGUAGE] }
    val onboardingDone: Flow<Boolean> = dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    val rated: Flow<Boolean> = dataStore.data.map { it[KEY_RATED] ?: false }
    val cosplayInstructionShown: Flow<Boolean> = dataStore.data.map { it[KEY_COSPLAY_INSTRUCTION] ?: false }
    val sampleCreationsSeeded: Flow<Boolean> = dataStore.data.map { it[KEY_SAMPLE_SEEDED] ?: false }
    val creationSaveCount: Flow<Int> = dataStore.data.map { it[KEY_SAVE_COUNT] ?: 0 }
    val ratePromptState: Flow<RatePromptState> = dataStore.data.map {
        RatePromptState(
            lastPromptDay = it[KEY_RATE_LAST_DAY] ?: -1L,
            afterSavePrompted = it[KEY_RATE_SAVE_PROMPTED] ?: false,
            homeExitPrompted = it[KEY_RATE_HOME_PROMPTED] ?: false,
        )
    }

    suspend fun setLanguage(code: String) {
        dataStore.edit { it[KEY_LANGUAGE] = code }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setRated(rated: Boolean) {
        dataStore.edit { it[KEY_RATED] = rated }
    }

    suspend fun setCosplayInstructionShown() {
        dataStore.edit { it[KEY_COSPLAY_INSTRUCTION] = true }
    }

    suspend fun setSampleCreationsSeeded() {
        dataStore.edit { it[KEY_SAMPLE_SEEDED] = true }
    }

    suspend fun incrementCreationSaveCount() {
        dataStore.edit { it[KEY_SAVE_COUNT] = (it[KEY_SAVE_COUNT] ?: 0) + 1 }
    }

    suspend fun setRatePromptState(state: RatePromptState) {
        dataStore.edit {
            it[KEY_RATE_LAST_DAY] = state.lastPromptDay
            it[KEY_RATE_SAVE_PROMPTED] = state.afterSavePrompted
            it[KEY_RATE_HOME_PROMPTED] = state.homeExitPrompted
        }
    }

    private companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language_code")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_RATED = booleanPreferencesKey("rated")
        val KEY_COSPLAY_INSTRUCTION = booleanPreferencesKey("cosplay_instruction_shown")
        val KEY_SAVE_COUNT = intPreferencesKey("creation_save_count")
        val KEY_SAMPLE_SEEDED = booleanPreferencesKey("sample_creations_seeded")
        val KEY_RATE_LAST_DAY = longPreferencesKey("rate_last_prompt_day")
        val KEY_RATE_SAVE_PROMPTED = booleanPreferencesKey("rate_after_save_prompted")
        val KEY_RATE_HOME_PROMPTED = booleanPreferencesKey("rate_home_exit_prompted")
    }
}
