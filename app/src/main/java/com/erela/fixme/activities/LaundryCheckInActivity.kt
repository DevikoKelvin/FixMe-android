package com.erela.fixme.activities

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erela.fixme.R
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityLaundryCheckInBinding
import com.erela.fixme.helpers.enableEdgeToEdgeOpaqueNav
import com.erela.fixme.viewmodel.LaundryCheckInViewModel
import com.google.android.material.card.MaterialCardView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Kedatangan Laundry — the COURIER says they have arrived, and that is all they do.
 *
 * THIS SCREEN USED TO BE THE WHOLE FLOW. It scanned the counter, then each uniform, then saved the
 * bundle. GA corrected that on 12 Sep 2026 [via Rosita Secoadi]: the courier scans only the counter
 * QR, and the counter operator scans the uniforms — on a phone too, in [LaundryCounterActivity].
 * The garment scanning moved there wholesale; what remains here is one button.
 *
 * THE SCREEN EXISTS BECAUSE THE PHONE HAS A CAMERA. No HID scanner is being procured, and the web
 * host is HTTP by standing decision, so `getUserMedia` has no secure context and a browser cannot
 * scan [D-14, TG-02].
 *
 * ONE ROUND TRIP, NOT TWO. `laundryArrive` checks the sticker and the department itself, so there
 * is no `laundryCounter` lookup first: a courier from a department not on the laundry list is told
 * by the same call that would have registered them.
 *
 * THE CONFIRMATION STAYS ON SCREEN rather than finishing the activity. The transaction number is
 * what the operator calls out, and a courier who has already walked away from it has nothing to
 * show when asked.
 */
class LaundryCheckInActivity : AppCompatActivity() {
    private val binding: ActivityLaundryCheckInBinding by lazy {
        ActivityLaundryCheckInBinding.inflate(layoutInflater)
    }
    private val viewModel: LaundryCheckInViewModel by viewModels()

    /**
     * Enable or disable a card acting as a button.
     *
     * Both halves are needed. `isClickable` is what actually stops the tap; the alpha is what
     * tells the courier it is stopped. A MaterialCardView's own `isEnabled` does neither.
     */
    private fun MaterialCardView.enable(on: Boolean) {
        isClickable = on
        isFocusable = on
        alpha = if (on) 1f else 0.4f
    }

    /** The counter sticker: says WHERE, never WHO — the courier comes from the token. */
    private val counterLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.arrive(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdgeOpaqueNav()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // No IME padding here, unlike the counter screen: nothing on this screen is typed, so the
        // keyboard never opens over it.

        binding.apply {
            toolBar.setNavigationOnClickListener { finish() }

            scanCounterButton.setOnClickListener {
                counterLauncher.launch(scanOptions())
            }
        }

        setupObservers()
    }

    private fun scanOptions() = ScanOptions().apply {
        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        setPrompt(getString(R.string.laundry_scan_counter_prompt))
        setCameraId(0)
        setBeepEnabled(true)
        setBarcodeImageEnabled(false)
        setOrientationLocked(true)
    }

    private fun setupObservers() {
        viewModel.apply {
            binding.apply {
                arrivalResult.observe(this@LaundryCheckInActivity) { response ->
                    if (!response.isSuccess) {
                        toast(response.message, warning = true)
                        return@observe
                    }

                    // The server's sentence already distinguishes a fresh arrival from one that
                    // was already open, and names the transaction in both. Repeating it here in
                    // our own words would be a second version to keep in step.
                    toast(response.message, warning = false)

                    tvTrxNo.text = response.data?.trxNo ?: "-"
                    tvArrivalMessage.text = response.message

                    instructionContainer.visibility = View.GONE
                    instructionAnimation.pauseAnimation()
                    arrivalCard.visibility = View.VISIBLE
                }

                isSubmitting.observe(this@LaundryCheckInActivity) { submitting ->
                    // Progress INSIDE the button, the way the login button does it.
                    scanLoadingBar.visibility = if (submitting) View.VISIBLE else View.GONE
                    scanCounterText.visibility = if (submitting) View.INVISIBLE else View.VISIBLE
                    scanCounterButton.enable(!submitting)
                }

                error.observe(this@LaundryCheckInActivity) { message ->
                    toast(message, warning = true)
                }
            }
        }
    }

    private fun toast(message: String, warning: Boolean) {
        CustomToast.getInstance(this)
            .setMessage(message)
            .setFontColor(
                getColor(
                    if (warning) R.color.custom_toast_font_warning
                    else R.color.custom_toast_font_success
                )
            )
            .setBackgroundColor(
                getColor(
                    if (warning) R.color.custom_toast_background_warning
                    else R.color.custom_toast_background_success
                )
            )
            .show()
    }
}
