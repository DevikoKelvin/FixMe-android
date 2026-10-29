package com.erela.fixme.activities

import android.content.Intent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erela.fixme.R
import com.erela.fixme.helpers.normalizeManualCode
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView

/**
 * The scanner every Smart Wash screen opens: the camera, plus a field to type the code when the
 * camera will not read the sticker.
 *
 * A TYPED CODE IS HANDED BACK AS A SCAN. It leaves through the same result a read QR does, so the
 * calling screen runs one code path and the server applies the same checks (the counter whitelist,
 * the six-digit pattern, whether the garment is in this batch) to both. The Compose app's scanner
 * sheet works the same way.
 */
class ManualEntryCaptureActivity : CaptureActivity() {

    override fun initializeContent(): DecoratedBarcodeView {
        setContentView(R.layout.activity_scan_manual)

        // Edge-to-edge is enforced on this target, so the field sits under the navigation bar and
        // the keyboard unless the root pads for both.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scanRoot)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val field = findViewById<TextInputEditText>(R.id.etManualCode)

        fun send() {
            val code = normalizeManualCode(field.text) ?: return

            setResult(
                RESULT_OK,
                Intent()
                    .putExtra(Intents.Scan.RESULT, code)
                    .putExtra(Intents.Scan.RESULT_FORMAT, "QR_CODE")
            )
            finish()
        }

        findViewById<View>(R.id.btnSendCode).setOnClickListener { send() }
        field.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send()
                true
            } else {
                false
            }
        }

        return findViewById(R.id.zxing_barcode_scanner)
    }
}
