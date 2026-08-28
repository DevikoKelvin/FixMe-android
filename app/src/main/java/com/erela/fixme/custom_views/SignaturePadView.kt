package com.erela.fixme.custom_views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.createBitmap

/**
 * Finger-drawn signature capture.
 *
 * Plain View + Path rather than a signature library: the whole feature is three touch events and a
 * bitmap export, and a dependency for that would be carried by every build forever.
 *
 * Strokes are kept as Paths and redrawn each frame instead of being painted into a backing bitmap.
 * A signature is a handful of paths, so the cost is nothing, and it means clear() and the PNG export
 * need no separate bitmap to keep in sync.
 */
class SignaturePadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val strokes = mutableListOf<Path>()
    private var active: Path? = null
    private var lastX = 0f
    private var lastY = 0f

    /** Whether the finger actually travelled. A tap leaves a Path that draws nothing. */
    private var moved = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val isEmpty: Boolean
        get() = strokes.isEmpty() && active == null

    fun clear() {
        strokes.clear()
        active = null
        moved = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        strokes.forEach { canvas.drawPath(it, paint) }
        active?.let { canvas.drawPath(it, paint) }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // THE reason this view needs its own touch handling. It sits inside the ScrollView
                // on the check-out screen, which claims the gesture after a few pixels of vertical
                // travel — without this the page scrolls and the stroke dies mid-signature.
                parent?.requestDisallowInterceptTouchEvent(true)
                active = Path().apply { moveTo(event.x, event.y) }
                lastX = event.x
                lastY = event.y
                moved = false
            }

            MotionEvent.ACTION_MOVE -> {
                val path = active ?: return true
                // Curve through the midpoint of each pair of samples. Joining raw touch samples
                // with lineTo renders as visible straight segments, which does not read as
                // handwriting on anything but a very fast device.
                path.quadTo(lastX, lastY, (lastX + event.x) / 2f, (lastY + event.y) / 2f)
                lastX = event.x
                lastY = event.y
                moved = true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // A stationary tap is discarded, so isEmpty cannot report "signed" for a stray
                // touch that left no visible mark.
                if (moved) {
                    active?.let { strokes.add(it) }
                }
                active = null
                parent?.requestDisallowInterceptTouchEvent(false)
            }

            else -> return false
        }

        invalidate()
        return true
    }

    /**
     * Write the signature to [target] as a PNG, on white.
     *
     * Opaque white rather than transparent: a transparent signature is invisible anywhere that
     * renders it on a dark background, and the file is small either way.
     *
     * @return false when nothing has been drawn or the view has not been laid out yet.
     */
    fun saveAsPng(target: File): Boolean {
        if (isEmpty || width <= 0 || height <= 0) {
            return false
        }
        val bitmap = createBitmap(width, height)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            strokes.forEach { drawPath(it, paint) }
        }

        return try {
            FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            true
        } catch (e: Exception) {
            false
        } finally {
            bitmap.recycle()
        }
    }
}
