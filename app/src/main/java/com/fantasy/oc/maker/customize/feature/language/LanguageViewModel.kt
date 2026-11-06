package com.fantasy.oc.maker.customize.feature.language

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fantasy.oc.maker.customize.core.common.AppLanguage
import com.fantasy.oc.maker.customize.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * Ngôn ngữ đang chọn – lưu trong SavedStateHandle để sống sót recreate/process death.
     * Lần đầu mở app: null cho đến khi người dùng chọn. Từ setting: ngôn ngữ hiện tại.
     */
    val selected: StateFlow<AppLanguage?> = savedStateHandle.getStateFlow(
        KEY_SELECTED,
        if (savedStateHandle.get<Boolean>(LanguageFragment.ARG_FROM_SETTING) == true) AppLanguage.current() else null,
    )

    private val _confirmed = Channel<AppLanguage>(Channel.BUFFERED)

    /** Phát ra một lần khi đã lưu xong lựa chọn. */
    val confirmed: Flow<AppLanguage> = _confirmed.receiveAsFlow()

    fun select(language: AppLanguage) {
        savedStateHandle[KEY_SELECTED] = language
    }

    fun confirm() {
        val language = selected.value ?: return
        viewModelScope.launch {
            settingsRepository.setLanguage(language.code)
            _confirmed.send(language)
        }
    }

    private companion object {
        const val KEY_SELECTED = "selected_language"
    }
}
