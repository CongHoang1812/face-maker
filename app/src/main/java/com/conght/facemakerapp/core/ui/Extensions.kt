package com.conght.facemakerapp.core.ui

import android.content.res.Resources
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Collect [flow] trên viewLifecycleOwner khi ở trạng thái STARTED. */
fun <T> Fragment.collectWhenStarted(flow: Flow<T>, action: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { action(it) }
        }
    }
}

val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

val Float.dp: Float get() = this * Resources.getSystem().displayMetrics.density

/**
 * Click chống bấm liên tục (debounce [intervalMs]).
 * [haptic] = true: rung nhẹ khi chọn item (phụ kiện, màu, tab…).
 */
fun View.setOnSingleClickListener(intervalMs: Long = 500L, haptic: Boolean = false, action: (View) -> Unit) {
    var lastClick = 0L
    setOnClickListener { v ->
        val now = SystemClock.elapsedRealtime()
        if (now - lastClick >= intervalMs) {
            lastClick = now
            if (haptic) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            action(v)
        }
    }
}
