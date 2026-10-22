package com.conght.facemakerapp.feature.maker.move

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.conght.facemakerapp.core.ui.setOnRepeatClickListener
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.databinding.ViewMovePanelBinding

/**
 * Panel chế độ Move (Figma 8:24). Các nút xoay / di chuyển / zoom giữ để lặp (80ms).
 * Bước: dịch [MOVE_STEP_DP] dp, xoay [ROTATE_STEP_DEG]°, zoom [ZOOM_STEP].
 */
class MovePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onExit()
        fun onMove(dxDp: Float, dyDp: Float)
        fun onRotate(degrees: Float)
        fun onZoom(delta: Float)
    }

    var listener: Listener? = null

    private val binding: ViewMovePanelBinding

    init {
        orientation = VERTICAL
        binding = ViewMovePanelBinding.inflate(LayoutInflater.from(context), this)
        binding.btnExit.setOnSingleClickListener { listener?.onExit() }
        binding.btnRotateLeft.setOnRepeatClickListener { listener?.onRotate(-ROTATE_STEP_DEG) }
        binding.btnRotateRight.setOnRepeatClickListener { listener?.onRotate(ROTATE_STEP_DEG) }
        binding.btnUp.setOnRepeatClickListener { listener?.onMove(0f, -MOVE_STEP_DP) }
        binding.btnDown.setOnRepeatClickListener { listener?.onMove(0f, MOVE_STEP_DP) }
        binding.btnLeft.setOnRepeatClickListener { listener?.onMove(-MOVE_STEP_DP, 0f) }
        binding.btnRight.setOnRepeatClickListener { listener?.onMove(MOVE_STEP_DP, 0f) }
        binding.btnZoomIn.setOnRepeatClickListener { listener?.onZoom(ZOOM_STEP) }
        binding.btnZoomOut.setOnRepeatClickListener { listener?.onZoom(-ZOOM_STEP) }
    }

    companion object {
        const val MOVE_STEP_DP = 10f
        const val ROTATE_STEP_DEG = 15f
        const val ZOOM_STEP = 0.1f
    }
}
