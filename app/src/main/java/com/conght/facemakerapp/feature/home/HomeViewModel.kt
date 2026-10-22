package com.conght.facemakerapp.feature.home

import androidx.lifecycle.ViewModel
import com.conght.facemakerapp.core.common.RatePrompter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ratePrompter: RatePrompter,
) : ViewModel() {

    /** Back ở Home lần đầu (chưa rate): hiện Rate us trước khi thoát. */
    suspend fun shouldPromptRateOnExit(): Boolean = ratePrompter.shouldPromptOnHomeExit()
}
