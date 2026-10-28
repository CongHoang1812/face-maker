package com.conght.facemakerapp.feature.luckyroll

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conght.facemakerapp.core.common.NetworkMonitor
import com.conght.facemakerapp.core.common.retryWhenBackOnline
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.data.model.LuckyRollResult
import com.conght.facemakerapp.data.repository.LuckyRollRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface LuckyRollUiState {
    /** Chưa bấm Random (Figma 3:11). */
    data object Idle : LuckyRollUiState

    /** Đã bấm Random: Loading (chờ API) / Success (Figma 3:24) / Error. */
    data class Rolled(val result: Resource<LuckyRollResult>) : LuckyRollUiState
}

/** Lucky roll: kết quả random từ dữ liệu API (data source random + ghép ảnh). */
@HiltViewModel
class LuckyRollViewModel @Inject constructor(
    private val luckyRollRepository: LuckyRollRepository,
    private val gson: Gson,
    private val savedStateHandle: SavedStateHandle,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    /** Số lần bấm Random; 0 = Idle. Lưu để xoay màn không quay về Idle. */
    private val rollCount = savedStateHandle.getStateFlow(KEY_ROLL_COUNT, 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<LuckyRollUiState> = rollCount
        .flatMapLatest { count ->
            if (count == 0) flowOf(LuckyRollUiState.Idle)
            else luckyRollRepository.roll().map { LuckyRollUiState.Rolled(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LuckyRollUiState.Idle)

    init {
        // Tải + ghép sẵn kết quả trong lúc người dùng còn ở màn Idle
        luckyRollRepository.prepare()
        retryWhenBackOnline(
            networkMonitor,
            isError = { (state.value as? LuckyRollUiState.Rolled)?.result is Resource.Error },
            retry = ::roll,
        )
    }

    /** Random (sau Watch Ad nếu [com.conght.facemakerapp.core.common.AppConfig.LUCKY_ROLL_REQUIRES_AD]). */
    fun roll() {
        savedStateHandle[KEY_ROLL_COUNT] = rollCount.value + 1
    }

    fun configJson(result: LuckyRollResult): String = gson.toJson(result.config)

    private companion object {
        const val KEY_ROLL_COUNT = "roll_count"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
