package com.conght.facemakerapp.core.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.conght.facemakerapp.core.ui.MockImageLoader
import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.CharacterTransform
import com.conght.facemakerapp.data.model.TemplateDetail

/**
 * Vẽ nhân vật từ nhiều layer ảnh (mỗi [com.conght.facemakerapp.data.model.PartCategory] một layer,
 * xếp theo zIndex) và áp [CharacterTransform] (dịch / phóng to / xoay / lật) lên cả nhóm layer.
 *
 * Khi [gesturesEnabled] (chế độ Move): kéo 1 ngón để dịch, pinch để zoom → báo qua [onGesture].
 */
class CharacterCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    /** dx, dy (dp) và hệ số zoom (nhân) từ thao tác tay. */
    var onGesture: ((dxDp: Float, dyDp: Float, scaleFactor: Float) -> Unit)? = null
    var gesturesEnabled = false

    private val layerContainer = FrameLayout(context)
    private val layers = mutableMapOf<String, ImageView>()
    private val layerUrls = mutableMapOf<String, String?>()
    private val density = resources.displayMetrics.density

    private var lastX = 0f
    private var lastY = 0f
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            onGesture?.invoke(0f, 0f, detector.scaleFactor)
            return true
        }
    })

    init {
        clipChildren = true
        addView(layerContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    /** Hiển thị [config] theo [detail]: chọn layer url theo item + màu, None → ẩn layer. */
    fun render(detail: TemplateDetail, config: CharacterConfig) {
        val categories = detail.categories.sortedBy { it.zIndex }
        val wanted = categories.map { it.id }.toSet()
        (layers.keys - wanted).forEach { id ->
            layers.remove(id)?.let(layerContainer::removeView)
            layerUrls.remove(id)
        }
        categories.forEachIndexed { index, category ->
            val view = layers.getOrPut(category.id) {
                ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
            }
            if (layerContainer.indexOfChild(view) != index) {
                layerContainer.removeView(view)
                layerContainer.addView(view, index, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            }
            val selection = config.selections[category.id]
            val item = category.items.firstOrNull { it.id == selection?.itemId }
            val color = item?.colors?.firstOrNull { it.id == selection?.colorId } ?: item?.colors?.firstOrNull()
            // TODO(API): item không có màu hiện dùng thumbnailUrl làm layer – đổi khi API có field layer riêng
            val url = color?.layerUrl ?: item?.thumbnailUrl
            setLayer(category.id, view, url)
        }
        applyTransform(config.transform)
    }

    private fun setLayer(categoryId: String, view: ImageView, url: String?) {
        if (layerUrls.containsKey(categoryId) && layerUrls[categoryId] == url) return
        layerUrls[categoryId] = url
        if (url == null) {
            Glide.with(view).clear(view)
            view.setImageDrawable(null)
            view.visibility = INVISIBLE
            return
        }
        view.visibility = VISIBLE
        if (url.startsWith(MockImageLoader.MOCK_COLOR_PREFIX)) {
            Glide.with(view).clear(view)
            view.setImageDrawable(url.removePrefix(MockImageLoader.MOCK_COLOR_PREFIX).toColorInt().toDrawable())
        } else {
            // Không dùng HARDWARE bitmap để exportBitmap() vẽ được ra canvas phần mềm
            Glide.with(view).load(url).disallowHardwareConfig().into(view)
        }
    }

    fun applyTransform(transform: CharacterTransform) {
        layerContainer.translationX = transform.offsetX * density
        layerContainer.translationY = transform.offsetY * density
        layerContainer.scaleX = transform.scale * if (transform.flipped) -1f else 1f
        layerContainer.scaleY = transform.scale
        layerContainer.rotation = transform.rotation
    }

    /** Vẽ nhân vật (kèm transform) ra bitmap vuông [size]px – lấy ô vuông giữa view. Gọi trên main thread. */
    fun exportBitmap(size: Int = EXPORT_SIZE): Bitmap {
        val bitmap = createBitmap(size, size)
        if (width == 0 || height == 0) return bitmap
        val canvas = Canvas(bitmap)
        val side = minOf(width, height).toFloat()
        canvas.scale(size / side, size / side)
        canvas.translate(-(width - side) / 2f, -(height - side) / 2f)
        draw(canvas)
        return bitmap
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = gesturesEnabled

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!gesturesEnabled) return super.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> {
                // Đổi số ngón -> lấy lại mốc để không nhảy
                val index = if (event.actionIndex == 0 && event.pointerCount > 1) 1 else 0
                lastX = event.getX(index)
                lastY = event.getY(index)
            }

            MotionEvent.ACTION_MOVE -> if (event.pointerCount == 1 && !scaleDetector.isInProgress) {
                val dx = (event.x - lastX) / density
                val dy = (event.y - lastY) / density
                lastX = event.x
                lastY = event.y
                onGesture?.invoke(dx, dy, 1f)
            }
        }
        return true
    }

    private companion object {
        const val EXPORT_SIZE = 1024
    }
}
