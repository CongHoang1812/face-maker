package com.conght.facemakerapp.core.ui

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View

/**
 * Bấm 1 lần = 1 lần [action]; giữ nút = lặp lại mỗi [intervalMs] sau [initialDelayMs].
 * Vẫn hỗ trợ click qua accessibility (performClick).
 */
@SuppressLint("ClickableViewAccessibility")
fun View.setOnRepeatClickListener(
    intervalMs: Long = 80L,
    initialDelayMs: Long = 300L,
    action: () -> Unit,
) {
    val repeater = object : Runnable {
        override fun run() {
            action()
            postDelayed(this, intervalMs)
        }
    }
    setOnClickListener { action() }
    setOnTouchListener { v, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.isPressed = true
                action()
                v.postDelayed(repeater, initialDelayMs)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.isPressed = false
                v.removeCallbacks(repeater)
            }
        }
        true
    }
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) = Unit
        override fun onViewDetachedFromWindow(v: View) {
            v.removeCallbacks(repeater)
        }
    })
}
