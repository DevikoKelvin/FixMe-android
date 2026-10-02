package com.erela.fixme.helpers

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
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
 * Switching restarts every activity, which on its own is an abrupt cut. [switchTo] photographs
 * the screen first; [reveal] lays that photo over the new screen and opens a growing circle
 * in it from the switch, so the new mode spreads out from where the finger was. Animator duration
 * scale 0 (the system's "remove animations") ends it at once, which is the reduced-motion
 * behaviour for free.
 */
object ThemeHelper {
    const val PREFS = "fixme_theme_prefs"
    const val KEY_DARK = "dark_mode"
    private const val KEY_VIEWS = "theme_switch_views"

    private var snapshot: Bitmap? = null
    private var originX = 0f
    private var originY = 0f

    fun isDark(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK, false)

    /**
     * From Application.onCreate, before any activity inflates, and on every switch.
     *
     * Android 12+ is told the mode itself (`setApplicationNightMode`, persisted by the system), and
     * that is what makes the SPLASH follow it: the splash is drawn before any code of ours runs, so
     * it can only read a mode the system already knows. AppCompat then just follows the system -
     * setting both would restart every screen twice per switch and cut the reveal short. Below 12
     * there is no such API, so there the splash follows the phone's dark setting.
     */
    fun apply(context: Context) {
        val dark = isDark(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(UiModeManager::class.java).setApplicationNightMode(
                if (dark) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
            )
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } else {
            AppCompatDelegate.setDefaultNightMode(
                if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    /** [origin] is where the reveal grows from: the switch the user touched. */
    fun switchTo(activity: Activity, dark: Boolean, origin: View) {
        // Finishing: a second tap landed on the screen that is already being replaced.
        if (dark == isDark(activity) || activity.isFinishing) return
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_DARK, dark) }
        val decor = activity.window.decorView
        if (decor.isLaidOut) {
            snapshot = decor.drawToBitmap()
            val at = IntArray(2).also { origin.getLocationInWindow(it) }
            originX = at[0] + origin.width / 2f
            originY = at[1] + origin.height / 2f
        }
        apply(activity)
        // REPLACED, NOT RECREATED [2 Oct 2026]. A recreate removes this window before the new one
        // has drawn, and for that gap - four frames - the phone shows no window at all: the black
        // flash before the reveal. Opening a new instance is an ordinary activity start, which keeps
        // this window up until the new one's first frame, the photo, is ready. It only works because
        // the activity declares configChanges="uiMode"; without it, apply() recreates it first.
        activity.startActivity(
            Intent(activity, activity.javaClass)
                .putExtras(activity.intent)
                .putExtra(KEY_VIEWS, activity.window.saveHierarchyState())
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        )
        activity.finish()
    }

    /**
     * Call after setContentView in an activity that can call [switchTo]: one that declares
     * configChanges="uiMode", and whose scrolling view has an id - that is what carries the scroll
     * position over, without which the circle opens onto a different part of the page than the
     * photo shows.
     */
    fun reveal(activity: Activity) {
        val shot = snapshot ?: return
        snapshot = null
        activity.intent.getBundleExtra(KEY_VIEWS)?.let { activity.window.restoreHierarchyState(it) }
        val decor = activity.window.decorView as ViewGroup
        val cover = RevealCover(activity, shot, originX, originY)
        decor.addView(cover, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val end = hypot(max(originX, shot.width - originX), max(originY, shot.height - originY))
        val grow = ValueAnimator.ofFloat(0f, end).apply {
            duration = 550
            interpolator = DecelerateInterpolator()
            addUpdateListener { cover.radius = it.animatedValue as Float }
            doOnEnd {
                decor.removeView(cover)
                shot.recycle()
            }
        }
        // FROM WHEN IT IS ON SCREEN, NOT FROM HERE. The window is shown once it has drawn, ~190 ms
        // after onCreate on the emulator, and a clock started here had the circle half open on the
        // first frame anyone saw. On Android 12+ focus comes once the window is visible (earlier
        // versions may give it sooner, which is no worse than before). The timer is for a window
        // that never gets focus, e.g. another app focused in split screen.
        var started = false
        val start = { if (!started) { started = true; grow.start() } }
        decor.viewTreeObserver.addOnWindowFocusChangeListener { if (it) start() }
        decor.postDelayed(start, 600)
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
