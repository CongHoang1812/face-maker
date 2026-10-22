package com.conght.facemakerapp.feature.decor.editor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.loadImage
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Canvas editor 328×328: nền (màu / ảnh) → ảnh nhân vật → các phần tử sticker / speech / text.
 *
 * Phần tử đang chọn có viền nét đứt `brown_primary` + 3 handle: flip (trên-phải), delete (dưới-trái),
 * resize/rotate (dưới-phải). Kéo thân để di chuyển, 2 ngón để scale/xoay, chạm 2 lần vào
 * speech/text để sửa chữ, chạm vùng trống để bỏ chọn. Mọi thay đổi báo qua [listener]
 * (state nằm ở ViewModel, canvas chỉ hiển thị).
 */
class DecorCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onSelect(id: String?)
        fun onTransform(id: String, x: Float, y: Float, scale: Float, rotation: Float)
        fun onFlip(id: String)
        fun onDelete(id: String)
        fun onEditText(id: String)
    }

    var listener: Listener? = null

    private val backgroundView = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
    private val characterView = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
    private val elementLayer = FrameLayout(context)
    private val elementViews = LinkedHashMap<String, DecorElementView>()

    private var elements: List<DecorElement> = emptyList()
    private var selectedId: String? = null
    private var backgroundKey: DecorBackground? = null
    private var exporting = false

    // ---- Vẽ viền chọn + handle ----
    private val handleSize = resources.getDimension(R.dimen.decor_handle)
    private val handleRadius = resources.getDimension(R.dimen.radius_small)
    private val iconSize = resources.getDimension(R.dimen.small_icon_glyph)
    private val selectionPadding = resources.getDimension(R.dimen.decor_selection_padding)
    private val brown = ContextCompat.getColor(context, R.color.brown_primary)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.getDimension(R.dimen.stroke_medium)
        color = brown
        val dash = resources.getDimension(R.dimen.decor_dash)
        pathEffect = DashPathEffect(floatArrayOf(dash, dash), 0f)
    }
    private val handleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.white) }
    private val handleStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.getDimension(R.dimen.stroke_medium)
        color = brown
    }
    private val iconFlip: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_flip)
    private val iconDelete: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_trash_small)
    private val iconResize: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_move)
    private val path = Path()
    private val rect = RectF()

    // ---- Cử chỉ ----
    private enum class Mode { NONE, DRAG, RESIZE, PINCH }
    private enum class Handle { FLIP, DELETE, RESIZE }

    private var mode = Mode.NONE
    private var dragOffset = PointF()
    private var startScale = 1f
    private var startRotation = 0f
    private var startDistance = 1f
    private var startAngle = 0f
    private var lastTapTime = 0L
    private var lastTapId: String? = null
    private val doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout().toLong()

    init {
        val match = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(backgroundView, match)
        addView(characterView, LayoutParams(match))
        addView(elementLayer, LayoutParams(match))
        backgroundView.setImageDrawable(ContextCompat.getColor(context, R.color.white).toDrawable())
        setWillNotDraw(false)
    }

    fun setCharacter(path: String?) {
        characterView.loadImage(path, R.color.transparent, forExport = true)
    }

    fun render(data: EditorData) {
        if (backgroundKey != data.background) {
            backgroundKey = data.background
            val bg = data.background
            when {
                bg.color != null -> backgroundView.setImageDrawable(bg.color.toDrawable())
                bg.imagePath != null -> backgroundView.loadImage(bg.imagePath, R.color.white, forExport = true)
                else -> backgroundView.setImageDrawable(ContextCompat.getColor(context, R.color.white).toDrawable())
            }
        }
        elements = data.elements
        selectedId = data.selectedId
        val ids = elements.map { it.id }.toSet()
        (elementViews.keys - ids).forEach { id -> elementViews.remove(id)?.let(elementLayer::removeView) }
        elements.forEachIndexed { index, element ->
            val view = elementViews.getOrPut(element.id) {
                DecorElementView(context, element.type).also {
                    elementLayer.addView(it, DecorElementView.layoutParamsFor(context, element.type))
                }
            }
            if (elementLayer.indexOfChild(view) != index) {
                elementLayer.removeView(view)
                elementLayer.addView(view, index, DecorElementView.layoutParamsFor(context, element.type))
            }
            view.bind(element)
        }
        applyTransforms()
        invalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        applyTransforms()
    }

    private fun applyTransforms() {
        if (width == 0) return
        elements.forEach { e ->
            val view = elementViews[e.id] ?: return@forEach
            view.translationX = (e.x - 0.5f) * width
            view.translationY = (e.y - 0.5f) * height
            view.scaleX = e.scale * if (e.flipped) -1f else 1f
            view.scaleY = e.scale
            view.rotation = e.rotation
        }
    }

    /** Vẽ thiết kế (KHÔNG có viền chọn / handle) ra bitmap vuông [size]px. Gọi trên main thread. */
    fun exportBitmap(size: Int = EXPORT_SIZE): Bitmap {
        val bitmap = createBitmap(size, size)
        if (width == 0 || height == 0) return bitmap
        exporting = true
        try {
            val canvas = Canvas(bitmap)
            canvas.scale(size / width.toFloat(), size / height.toFloat())
            draw(canvas)
        } finally {
            exporting = false
        }
        return bitmap
    }

    // ---------- Hình học ----------

    private fun centerOf(e: DecorElement) = PointF(e.x * width, e.y * height)

    /** 4 góc (TL, TR, BR, BL) của khung chọn quanh phần tử, đã xoay. */
    private fun cornersOf(e: DecorElement): List<PointF>? {
        val view = elementViews[e.id] ?: return null
        if (view.width == 0) return null
        val c = centerOf(e)
        val hw = view.width * e.scale / 2 + selectionPadding
        val hh = view.height * e.scale / 2 + selectionPadding
        val rad = Math.toRadians(e.rotation.toDouble())
        val cos = cos(rad).toFloat()
        val sin = sin(rad).toFloat()
        return listOf(-hw to -hh, hw to -hh, hw to hh, -hw to hh).map { (dx, dy) ->
            PointF(c.x + dx * cos - dy * sin, c.y + dx * sin + dy * cos)
        }
    }

    private fun handlesOf(e: DecorElement): Map<Handle, PointF> {
        val c = cornersOf(e) ?: return emptyMap()
        return mapOf(Handle.FLIP to c[1], Handle.RESIZE to c[2], Handle.DELETE to c[3])
    }

    private fun hit(e: DecorElement, x: Float, y: Float): Boolean {
        val view = elementViews[e.id] ?: return false
        val c = centerOf(e)
        val rad = Math.toRadians(-e.rotation.toDouble())
        val dx = x - c.x
        val dy = y - c.y
        val lx = (dx * cos(rad) - dy * sin(rad)).toFloat()
        val ly = (dx * sin(rad) + dy * cos(rad)).toFloat()
        return kotlin.math.abs(lx) <= view.width * e.scale / 2 && kotlin.math.abs(ly) <= view.height * e.scale / 2
    }

    // ---------- Vẽ khung chọn ----------

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (exporting) return
        val e = elements.firstOrNull { it.id == selectedId } ?: return
        val corners = cornersOf(e) ?: return
        path.reset()
        path.moveTo(corners[0].x, corners[0].y)
        corners.drop(1).forEach { path.lineTo(it.x, it.y) }
        path.close()
        canvas.drawPath(path, borderPaint)
        handlesOf(e).forEach { (handle, p) ->
            val icon = when (handle) {
                Handle.FLIP -> iconFlip
                Handle.DELETE -> iconDelete
                Handle.RESIZE -> iconResize
            }
            drawHandle(canvas, p, icon)
        }
    }

    private fun drawHandle(canvas: Canvas, p: PointF, icon: Drawable?) {
        val half = handleSize / 2
        rect.set(p.x - half, p.y - half, p.x + half, p.y + half)
        canvas.drawRoundRect(rect, handleRadius, handleRadius, handleFill)
        canvas.drawRoundRect(rect, handleRadius, handleRadius, handleStroke)
        icon ?: return
        val i = iconSize / 2
        icon.setBounds((p.x - i).toInt(), (p.y - i).toInt(), (p.x + i).toInt(), (p.y + i).toInt())
        icon.draw(canvas)
    }

    // ---------- Chạm ----------

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> onDown(event.x, event.y)
            MotionEvent.ACTION_POINTER_DOWN -> if (event.pointerCount == 2) startPinch(event)
            MotionEvent.ACTION_MOVE -> onMove(event)
            MotionEvent.ACTION_POINTER_UP -> mode = Mode.NONE
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> mode = Mode.NONE
        }
        return true
    }

    private fun selected() = elements.firstOrNull { it.id == selectedId }

    private fun onDown(x: Float, y: Float) {
        parent?.requestDisallowInterceptTouchEvent(true)
        selected()?.let { e ->
            val touched = handlesOf(e).entries.firstOrNull { (_, p) -> hypot(p.x - x, p.y - y) <= handleSize }
            when (touched?.key) {
                Handle.FLIP -> return listener?.onFlip(e.id) ?: Unit
                Handle.DELETE -> return listener?.onDelete(e.id) ?: Unit
                Handle.RESIZE -> {
                    val c = centerOf(e)
                    mode = Mode.RESIZE
                    startScale = e.scale
                    startRotation = e.rotation
                    startDistance = hypot(x - c.x, y - c.y).coerceAtLeast(1f)
                    startAngle = angle(c.x, c.y, x, y)
                    return
                }

                null -> Unit
            }
        }
        val target = elements.lastOrNull { hit(it, x, y) }
        if (target == null) {
            mode = Mode.NONE
            if (selectedId != null) listener?.onSelect(null)
            return
        }
        val now = System.currentTimeMillis()
        if (target.id == lastTapId && now - lastTapTime <= doubleTapTimeout && target.type != ElementType.STICKER) {
            listener?.onEditText(target.id)
        }
        lastTapId = target.id
        lastTapTime = now
        if (target.id != selectedId) listener?.onSelect(target.id)
        val c = centerOf(target)
        dragOffset = PointF(x - c.x, y - c.y)
        mode = Mode.DRAG
    }

    private fun startPinch(event: MotionEvent) {
        val e = selected() ?: return
        mode = Mode.PINCH
        startScale = e.scale
        startRotation = e.rotation
        startDistance = hypot(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0)).coerceAtLeast(1f)
        startAngle = angle(event.getX(0), event.getY(0), event.getX(1), event.getY(1))
    }

    private fun onMove(event: MotionEvent) {
        val e = selected() ?: return
        when (mode) {
            Mode.DRAG -> listener?.onTransform(
                e.id,
                (event.x - dragOffset.x) / width,
                (event.y - dragOffset.y) / height,
                e.scale,
                e.rotation,
            )

            Mode.RESIZE -> {
                val c = centerOf(e)
                val distance = hypot(event.x - c.x, event.y - c.y)
                val rotation = startRotation + angle(c.x, c.y, event.x, event.y) - startAngle
                listener?.onTransform(e.id, e.x, e.y, startScale * distance / startDistance, rotation)
            }

            Mode.PINCH -> if (event.pointerCount >= 2) {
                val distance = hypot(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0))
                val rotation = startRotation + angle(event.getX(0), event.getY(0), event.getX(1), event.getY(1)) - startAngle
                listener?.onTransform(e.id, e.x, e.y, startScale * distance / startDistance, rotation)
            }

            Mode.NONE -> Unit
        }
    }

    private fun angle(x1: Float, y1: Float, x2: Float, y2: Float): Float =
        Math.toDegrees(atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()

    private companion object {
        const val EXPORT_SIZE = 1024
    }
}
