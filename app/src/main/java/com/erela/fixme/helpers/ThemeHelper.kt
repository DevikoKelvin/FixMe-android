package com.erela.fixme.helpers

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.animation.doOnEnd
import androidx.core.content.edit
import androidx.core.view.drawToBitmap
import kotlin.math.hypot
import kotlin.math.max

/**
 * Light / dark mode, chosen per device [25 Sep 2026].
 *
 * The prefs file and key are shared with the Compose app on purpose: both install as the same
 * applicationId, so a phone moving from this app to that one keeps its choice.
 *
 * Switching recreates every activity, which on its own is an abrupt cut. [switchTo] photographs
 * the screen first; [reveal] lays that photo over the recreated screen and opens a growing circle
 * in it from the switch, so the new mode spreads out from where the finger was. Animator duration
 * scale 0 (the system's "remove animations") ends it at once, which is the reduced-motion
 * behaviour for free.
 */
object ThemeHelper {
    const val PREFS = "fixme_theme_prefs"
    const val KEY_DARK = "dark_mode"

    private var snapshot: Bitmap? = null
    private var originX = 0f
    private var originY = 0f

    fun isDark(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK, false)

    /** From Application.onCreate, before any activity inflates. */
    fun apply(context: Context) = AppCompatDelegate.setDefaultNightMode(
        if (isDark(context)) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
    )

    /** [origin] is where the reveal grows from: the switch the user touched. */
    fun switchTo(activity: Activity, dark: Boolean, origin: View) {
        if (dark == isDark(activity)) return
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_DARK, dark) }
        val decor = activity.window.decorView
        if (decor.isLaidOut) {
            snapshot = decor.drawToBitmap()
            val at = IntArray(2).also { origin.getLocationInWindow(it) }
            originX = at[0] + origin.width / 2f
            originY = at[1] + origin.height / 2f
        }
        apply(activity)
    }

    /**
     * Call after setContentView in an activity that can call [switchTo]. The activity must keep its
     * scroll position across the recreate (a ScrollView needs an id for that), or the circle opens
     * onto a different part of the page than the photo shows.
     */
    fun reveal(activity: Activity) {
        val shot = snapshot ?: return
        snapshot = null
        val decor = activity.window.decorView as ViewGroup
        val cover = RevealCover(activity, shot, originX, originY)
        decor.addView(cover, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val end = hypot(max(originX, shot.width - originX), max(originY, shot.height - originY))
        ValueAnimator.ofFloat(0f, end).apply {
            duration = 550
            interpolator = DecelerateInterpolator()
            addUpdateListener { cover.radius = it.animatedValue as Float }
            doOnEnd {
                decor.removeView(cover)
                shot.recycle()
            }
            start()
        }
    }

    /**
     * The old screen with a hole in it. The hole is drawn, not clipped: a clip path has hard,
     * stair-stepped edges, a CLEAR circle on a layer is antialiased.
     */
    @SuppressLint("ViewConstructor")
    private class RevealCover(
        context: Context,
        private val shot: Bitmap,
        private val cx: Float,
        private val cy: Float
    ) : View(context) {
        var radius = 0f
            set(value) {
                field = value
                invalidate()
            }

        private val hole = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        init {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawBitmap(shot, 0f, 0f, null)
            canvas.drawCircle(cx, cy, radius, hole)
        }
    }
}
