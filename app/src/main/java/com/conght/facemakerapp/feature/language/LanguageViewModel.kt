package com.conght.facemakerapp.feature.language

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conght.facemakerapp.core.common.AppLanguage
import com.conght.facemakerapp.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Mã ngôn ngữ đang chọn – lưu trong SavedStateHandle để sống sót recreate/process death. */
    val selected: StateFlow<AppLanguage> = savedStateHandle
        .getStateFlow(KEY_SELECTED, AppLanguage.current().code)
        .map { AppLanguage.fromCode(it) ?: AppLanguage.DEFAULT }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.current())

    private val _confirmed = Channel<AppLanguage>(Channel.BUFFERED)

    /** Phát ra một lần khi đã lưu xong lựa chọn. */
    val confirmed: Flow<AppLanguage> = _confirmed.receiveAsFlow()

    fun select(language: AppLanguage) {
        savedStateHandle[KEY_SELECTED] = language.code
    }

    fun confirm() {
        val language = selected.value
        viewModelScope.launch {
            settingsRepository.setLanguage(language.code)
            _confirmed.send(language)
        }
    }

    private companion object {
        const val KEY_SELECTED = "selected_language"
    }
}
