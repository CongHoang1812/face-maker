package com.fantasy.oc.maker.customize.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
    val appRated: Flow<Boolean> = dataStore.data.map { it[KEY_APP_RATED] ?: false }

    suspend fun setLanguage(code: String) {
        dataStore.edit { it[KEY_LANGUAGE] = code }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setCosplayInstructionShown() {
        dataStore.edit { it[KEY_COSPLAY_INSTRUCTION] = true }
    }

    suspend fun setAppRated() {
        dataStore.edit { it[KEY_APP_RATED] = true }
    }

    /** Tăng số lần bấm Back ở Home, trả về giá trị mới. */
    suspend fun incrementHomeBackCount(): Int =
        dataStore.edit { it[KEY_HOME_BACK_COUNT] = (it[KEY_HOME_BACK_COUNT] ?: 0) + 1 }[KEY_HOME_BACK_COUNT] ?: 0

    private companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language_code")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_COSPLAY_INSTRUCTION = booleanPreferencesKey("cosplay_instruction_shown")
        val KEY_APP_RATED = booleanPreferencesKey("app_rated")
        val KEY_HOME_BACK_COUNT = intPreferencesKey("home_back_count")
    }
}
