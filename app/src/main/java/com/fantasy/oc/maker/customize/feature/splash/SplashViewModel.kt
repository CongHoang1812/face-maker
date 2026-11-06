package com.fantasy.oc.maker.customize.feature.splash

import androidx.lifecycle.ViewModel
import com.fantasy.oc.maker.customize.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    suspend fun isOnboardingDone(): Boolean = settingsRepository.onboardingDone.first()
}
