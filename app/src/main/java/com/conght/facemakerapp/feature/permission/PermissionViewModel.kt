package com.conght.facemakerapp.feature.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conght.facemakerapp.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _onboardingCompleted = Channel<Unit>(Channel.BUFFERED)

    /** Phát ra một lần sau khi đã lưu onboardingDone = true. */
    val onboardingCompleted: Flow<Unit> = _onboardingCompleted.receiveAsFlow()

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingDone(true)
            _onboardingCompleted.send(Unit)
        }
    }
}
