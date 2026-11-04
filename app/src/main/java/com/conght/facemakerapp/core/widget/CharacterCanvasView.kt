package com.conght.facemakerapp.core.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.conght.facemakerapp.core.ui.MockImageLoader
import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.CharacterTransform
import com.conght.facemakerapp.data.model.PartSelection
import com.conght.facemakerapp.data.model.TemplateDetail
import com.conght.facemakerapp.data.model.layerUrlFor

/**
 * Vẽ nhân vật từ nhiều layer ảnh (mỗi [com.conght.facemakerapp.data.model.PartCategory] một layer,
 * xếp theo zIndex). [CharacterConfig.transform] áp lên cả nhóm layer, [CharacterConfig.partTransforms]
 * áp lên từng layer.
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
    private val layers = mutableMapOf<String, LayerView>()
    private val layerUrls = mutableMapOf<String, String?>()
    private var layerOrder: List<String> = emptyList()

    /** Layer mới đang chờ hiện: categoryId → url đích (null = None) / view ẩn đang tải ảnh. */
    private val pendingUrls = mutableMapOf<String, String?>()
    private val pendingViews = mutableMapOf<String, LayerView>()
    private val loadingIds = mutableSetOf<String>()
    private var staging = false

    /** Đầu vào layer của lần [render] trước: kéo / zoom chỉ đổi transform nên khỏi dựng lại layer. */
    private var renderedDetail: TemplateDetail? = null
    private var renderedSelections: Map<String, PartSelection>? = null

    /** false = đang chờ layer mới (màn hình vẫn là nhân vật cũ) → chưa nên exportBitmap(). */
    val isSettled: Boolean get() = pendingUrls.isEmpty()

    /** Gọi sau khi layer mới đã hiện xong, lúc này exportBitmap() ra đúng nhân vật mới. */
    var onSettled: (() -> Unit)? = null
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

    /**
     * Hiển thị [config] theo [detail]: chọn layer url theo item + màu, None → ẩn layer.
     *
     * Lần đầu: tải thẳng vào layer. Các lần sau (random, đổi item / màu): layer mới tải vào view ẩn
     * nằm ngay trên layer cũ, nhân vật cũ giữ nguyên; khi mọi layer mới xong thì đổi cùng lúc
     * ([commitPending]) → không bị cảnh nhân vật thiếu bộ phận / hiện dần từng layer.
     */
    fun render(detail: TemplateDetail, config: CharacterConfig) {
        if (detail !== renderedDetail || config.selections != renderedSelections) {
            renderedDetail = detail
            renderedSelections = config.selections
            renderLayers(detail, config)
        }
        layerContainer.applyTransform(config.transform, density)
        // Gồm cả layer đang chờ để lúc đổi layer bộ phận không bị nhảy về vị trí gốc
        layers.forEach { (id, view) -> view.applyPartTransform(config.partTransform(id), density) }
        pendingViews.forEach { (id, view) -> view.applyPartTransform(config.partTransform(id), density) }
    }

    private fun renderLayers(detail: TemplateDetail, config: CharacterConfig) {
        val categories = detail.categories.sortedBy { it.zIndex }
        val ids = categories.map { it.id }
        if (ids != layerOrder) {
            // Đổi bộ category (template khác): sắp lại layer, bỏ mọi layer đang chờ
            cancelPending(pendingUrls.keys.toList())
            (layers.keys - ids.toSet()).forEach { id ->
                layers.remove(id)?.let(layerContainer::removeView)
                layerUrls.remove(id)
            }
            ids.forEachIndexed { index, id ->
                val view = layers.getOrPut(id) { newLayerView() }
                if (layerContainer.indexOfChild(view) != index) {
                    layerContainer.removeView(view)
                    layerContainer.addView(view, index, layerParams())
                }
            }
            layerOrder = ids
        }
        staging = true
        categories.forEach { category ->
            val id = category.id
            val url = category.layerUrlFor(config.selections[id])
            when {
                // Layer chưa từng hiện: tải thẳng
                !layerUrls.containsKey(id) && id !in pendingUrls -> setLayer(id, layers.getValue(id), url)
                pendingUrls.containsKey(id) && pendingUrls[id] == url -> Unit // đang tải đúng url
                else -> {
                    cancelPending(listOf(id))
                    if (layerUrls[id] != url) stageLayer(id, url)
                }
            }
        }
        staging = false
        commitIfReady()
    }

    /** Tải [url] vào view ẩn ngay trên layer hiện tại của [categoryId]; hiện ở [commitPending]. */
    private fun stageLayer(categoryId: String, url: String?) {
        pendingUrls[categoryId] = url
        if (url == null || url.startsWith(MockImageLoader.MOCK_COLOR_PREFIX)) return // không cần tải
        val current = layers.getValue(categoryId)
        val view = newLayerView().apply { visibility = INVISIBLE }
        layerContainer.addView(view, layerContainer.indexOfChild(current) + 1, layerParams())
        pendingViews[categoryId] = view
        loadingIds += categoryId
        val onDone = {
            if (pendingViews[categoryId] === view && loadingIds.remove(categoryId)) commitIfReady()
        }
        Glide.with(view).load(url).disallowHardwareConfig()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean,
                ): Boolean {
                    onDone()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    onDone()
                    return false
                }
            })
            .into(view)
    }

    /** Callback có thể chạy ngay trong [stageLayer] (ảnh có sẵn trong RAM) → chờ hết vòng render mới đổi. */
    private fun commitIfReady() {
        if (!staging && loadingIds.isEmpty() && pendingUrls.isNotEmpty()) commitPending()
    }

    /** Đổi mọi layer đang chờ cùng lúc. */
    private fun commitPending() {
        pendingUrls.forEach { (id, url) ->
            val old = layers.getValue(id)
            val replacement = pendingViews[id]
            if (replacement == null) {
                setLayer(id, old, url)
            } else {
                Glide.with(old).clear(old)
                layerContainer.removeView(old)
                replacement.visibility = VISIBLE
                layers[id] = replacement
                layerUrls[id] = url
            }
        }
        pendingUrls.clear()
        pendingViews.clear()
        // post: listener của Glide chạy trước khi ảnh được gán vào view, báo ngay thì export thiếu layer
        post { onSettled?.invoke() }
    }

    /** Bỏ layer đang chờ của [ids] (url đích đã đổi / đổi template). */
    private fun cancelPending(ids: List<String>) {
        ids.forEach { id ->
            pendingUrls.remove(id)
            loadingIds.remove(id)
            pendingViews.remove(id)?.let { view ->
                Glide.with(view).clear(view)
                layerContainer.removeView(view)
            }
        }
    }

    private fun newLayerView() = LayerView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }

    private fun layerParams() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

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

private fun View.applyTransform(transform: CharacterTransform, density: Float) {
    translationX = transform.offsetX * density
    translationY = transform.offsetY * density
    scaleX = transform.scale * if (transform.flipped) -1f else 1f
    scaleY = transform.scale
    rotation = transform.rotation
}

/**
 * Layer của 1 bộ phận. Ảnh layer thường rộng bằng cả canvas còn bộ phận chỉ chiếm một góc, nên phóng to / xoay
 * quanh tâm phần ảnh có nội dung (không phải tâm view) để bộ phận đổi kích thước tại chỗ.
 */
private class LayerView(context: Context) : AppCompatImageView(context) {

    private var hasTransform = false

    /** Tâm phần có nội dung của ảnh hiện tại (tỉ lệ 0..1), tính khi cần rồi giữ đến lúc đổi ảnh. */
    private var cachedCenter: Pair<Float, Float>? = null
    private var cachedCenterValid = false

    fun applyPartTransform(transform: CharacterTransform, density: Float) {
        applyTransform(transform, density)
        hasTransform = transform.scale != 1f || transform.rotation != 0f
        updatePivot()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        cachedCenterValid = false
        updatePivot()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updatePivot()
    }

    /** Scale 1 và không xoay thì pivot không ảnh hưởng gì → khỏi quét ảnh của những layer chưa chỉnh. */
    private fun updatePivot() {
        if (!hasTransform || width == 0 || height == 0) return
        if (!cachedCenterValid) {
            cachedCenter = findContentCenter()
            cachedCenterValid = true
        }
        val center = cachedCenter
        val image = drawable
        if (center == null || image == null) {
            pivotX = width / 2f
            pivotY = height / 2f
            return
        }
        // FIT_CENTER: ảnh được thu theo cạnh chạm khung trước và nằm giữa view
        val fit = minOf(width.toFloat() / image.intrinsicWidth, height.toFloat() / image.intrinsicHeight)
        val shownWidth = image.intrinsicWidth * fit
        val shownHeight = image.intrinsicHeight * fit
        pivotX = (width - shownWidth) / 2f + center.first * shownWidth
        pivotY = (height - shownHeight) / 2f + center.second * shownHeight
    }

    private fun findContentCenter(): Pair<Float, Float>? {
        val bitmap = (drawable as? BitmapDrawable)?.bitmap?.takeUnless { it.isRecycled } ?: return null
        return contentCenter(bitmap.width, bitmap.height) { x, y -> Color.alpha(bitmap.getPixel(x, y)) }
    }
}

private const val MAX_CONTENT_SAMPLES = 128
private const val MIN_CONTENT_ALPHA = 16

/**
 * Tâm khung bao phần có nội dung của ảnh [width]×[height], theo tỉ lệ 0..1; null nếu ảnh trong suốt hoàn toàn.
 * Chỉ lấy mẫu theo lưới tối đa [MAX_CONTENT_SAMPLES] điểm mỗi chiều vì hàm chạy trên main thread.
 */
internal fun contentCenter(width: Int, height: Int, alphaAt: (x: Int, y: Int) -> Int): Pair<Float, Float>? {
    val step = maxOf(1, maxOf(width, height) / MAX_CONTENT_SAMPLES)
    var left = Int.MAX_VALUE
    var top = Int.MAX_VALUE
    var right = -1
    var bottom = -1
    for (y in 0 until height step step) {
        for (x in 0 until width step step) {
            if (alphaAt(x, y) < MIN_CONTENT_ALPHA) continue
            if (x < left) left = x
            if (x > right) right = x
            if (y < top) top = y
            if (y > bottom) bottom = y
        }
    }
    if (right < 0) return null
    return (left + right + 1) / 2f / width to (top + bottom + 1) / 2f / height
}
