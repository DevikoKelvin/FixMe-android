package com.erela.fixme

import android.graphics.BitmapFactory
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View.MeasureSpec
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.erela.fixme.custom_views.SignaturePadView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Covers SignaturePadView, because its failure modes are quiet ones: a pad that reports "signed"
 * for a stray tap, or a PNG export that silently writes nothing, both let a check-out through with
 * no real witness signature — which is the entire point of the field.
 *
 * Instrumented rather than a plain unit test: Path, Bitmap and MotionEvent are framework classes,
 * and pulling in Robolectric to fake them would be a new dependency for 40 lines of view code.
 *
 * Needs a connected device or emulator:  ./gradlew connectedStableDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class SignaturePadViewTest {
    private lateinit var pad: SignaturePadView
    private lateinit var cacheDir: File

    private companion object {
        const val W = 600
        const val H = 300
    }

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        cacheDir = context.cacheDir

        pad = SignaturePadView(context)
        // Laid out by hand: saveAsPng refuses a zero-size view, so without this every export
        // assertion would pass for the wrong reason.
        pad.measure(
            MeasureSpec.makeMeasureSpec(W, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(H, MeasureSpec.EXACTLY)
        )
        pad.layout(0, 0, W, H)
    }

    private fun touch(action: Int, x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, action, x, y, 0)
        try {
            pad.onTouchEvent(event)
        } finally {
            event.recycle()
        }
    }

    /** A stroke: down, several moves, up. */
    private fun drawStroke() {
        touch(MotionEvent.ACTION_DOWN, 20f, 20f)
        for (i in 1..10) {
            touch(MotionEvent.ACTION_MOVE, 20f + i * 15f, 20f + i * 8f)
        }
        touch(MotionEvent.ACTION_UP, 170f, 100f)
    }

    @Test
    fun starts_empty() {
        assertTrue(pad.isEmpty)
    }

    @Test
    fun a_stationary_tap_does_not_count_as_signed() {
        // The one that matters. If a tap registered, a technician could satisfy a required
        // signature by brushing the pad, and the log would carry a blank image.
        touch(MotionEvent.ACTION_DOWN, 40f, 40f)
        touch(MotionEvent.ACTION_UP, 40f, 40f)

        assertTrue("a tap with no movement must leave the pad empty", pad.isEmpty)
    }

    @Test
    fun a_drag_counts_as_signed() {
        drawStroke()
        assertFalse(pad.isEmpty)
    }

    @Test
    fun a_cancelled_gesture_still_keeps_what_was_drawn() {
        touch(MotionEvent.ACTION_DOWN, 20f, 20f)
        touch(MotionEvent.ACTION_MOVE, 80f, 60f)
        touch(MotionEvent.ACTION_CANCEL, 80f, 60f)
        // The stroke was real; losing it because the system cancelled the gesture would make the
        // pad feel broken mid-signature.
        assertFalse(pad.isEmpty)
    }

    @Test
    fun clear_resets_to_empty() {
        drawStroke()
        assertFalse(pad.isEmpty)

        pad.clear()
        assertTrue(pad.isEmpty)
    }

    @Test
    fun export_refuses_when_nothing_was_drawn() {
        val target = File(cacheDir, "sig_empty_test.png")
        target.delete()

        assertFalse("an empty pad must not produce a file", pad.saveAsPng(target))
        assertFalse("nothing should have been written", target.exists())
    }

    @Test
    fun export_writes_a_decodable_png_at_the_view_size() {
        val target = File(cacheDir, "sig_export_test.png")
        target.delete()
        drawStroke()

        assertTrue(pad.saveAsPng(target))
        assertTrue("file should exist", target.exists())
        assertTrue("file should not be empty", target.length() > 0)
        // Decoding proves it is a real PNG rather than a zero-byte or truncated file, which is how
        // a broken export would otherwise reach the server unnoticed.
        val bitmap = BitmapFactory.decodeFile(target.absolutePath)
        assertTrue("should decode as an image", bitmap != null)
        assertEquals(W, bitmap.width)
        assertEquals(H, bitmap.height)

        bitmap.recycle()
        target.delete()
    }

    @Test
    fun export_is_opaque_so_the_signature_is_visible_on_any_background() {
        val target = File(cacheDir, "sig_opaque_test.png")
        target.delete()
        drawStroke()
        assertTrue(pad.saveAsPng(target))
        val bitmap = BitmapFactory.decodeFile(target.absolutePath)
        // A corner the stroke never reaches: transparent here means the signature would be
        // invisible wherever it is rendered on a dark background.
        assertEquals(255, android.graphics.Color.alpha(bitmap.getPixel(W - 2, H - 2)))

        bitmap.recycle()
        target.delete()
    }
}
