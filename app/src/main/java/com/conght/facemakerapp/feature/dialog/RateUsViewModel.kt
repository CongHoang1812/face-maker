package com.conght.facemakerapp.feature.dialog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.conght.facemakerapp.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class RateUsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Số sao 0..5 (0 = chưa chọn), giữ qua recreate. */
    val stars: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_STARS, 0)

    fun setStars(value: Int) {
        savedStateHandle[KEY_STARS] = value.coerceIn(0, MAX_STARS)
    }

    suspend fun markRated() = settingsRepository.setRated(true)

    companion object {
        const val MAX_STARS = 5
        private const val KEY_STARS = "stars"
    }
}
