package com.fantasy.oc.maker.customize.feature.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fantasy.oc.maker.customize.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val appRated: Flow<Boolean> = settingsRepository.appRated

    /** Lưu ở đây chứ không ở dialog: dialog đóng ngay sau khi rate, scope của nó bị huỷ có thể làm mất lần ghi. */
    fun markAppRated() {
        viewModelScope.launch { settingsRepository.setAppRated() }
    }
}
