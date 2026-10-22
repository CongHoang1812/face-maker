package com.conght.facemakerapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.conght.facemakerapp.data.local.db.SampleCreationSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FaceMakerApp : Application() {

    @Inject lateinit var sampleCreationSeeder: SampleCreationSeeder

    private val appScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        // App không có dark theme -> khoá light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        appScope.launch { sampleCreationSeeder.seedIfNeeded() }
    }
}
