package com.conght.facemakerapp.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    val cosplayInstructionShown: Flow<Boolean> = dataStore.data.map { it[KEY_COSPLAY_INSTRUCTION] ?: false }

    suspend fun setLanguage(code: String) {
        dataStore.edit { it[KEY_LANGUAGE] = code }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setCosplayInstructionShown() {
        dataStore.edit { it[KEY_COSPLAY_INSTRUCTION] = true }
    }

    private companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language_code")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_COSPLAY_INSTRUCTION = booleanPreferencesKey("cosplay_instruction_shown")
    }
}
