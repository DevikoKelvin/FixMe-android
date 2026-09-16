package com.erela.fixme.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erela.fixme.R
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.adapters.recycler_view.LaundryBatchAdapter
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
 *
 * A LIST, NOT A BARE SCAN BUTTON [GA, 15 Sep 2026]. Until today this answered "how do I hand this
 * in" and nothing else: a courier could not see whether last week's bundle was washed, ready, or
 * already collected by a colleague. The list is their DEPARTMENT's [T-08], because any active
 * account of it may collect - a list of their own hand-overs would hide the batch they were sent
 * to fetch.
 */
class LaundryCheckInActivity : AppCompatActivity() {
    private val binding: ActivityLaundryCheckInBinding by lazy {
        ActivityLaundryCheckInBinding.inflate(layoutInflater)
    }
    private val viewModel: LaundryCheckInViewModel by viewModels()
    private lateinit var batchAdapter: LaundryBatchAdapter

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

    override fun onResume() {
        super.onResume()
        // Reloaded on every return, not only on create: a colleague from the same department may
        // have collected while this screen was in the background, and the batch detail hands back
        // here after a collection.
        viewModel.loadBatches()
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

            batchAdapter = LaundryBatchAdapter(this@LaundryCheckInActivity) { batch ->
                startActivity(
                    Intent(this@LaundryCheckInActivity, LaundryBatchActivity::class.java)
                        .putExtra(LaundryBatchActivity.EXTRA_ID_TRX, batch.id)
                )
            }

            rvBatches.apply {
                layoutManager = LinearLayoutManager(this@LaundryCheckInActivity)
                adapter = batchAdapter
            }

            // The answer to "are my uniforms back yet" changes while the courier is looking at it,
            // and `onResume` only helps if they leave the screen and come back.
            swipeRefreshLayout.setOnRefreshListener { viewModel.loadBatches() }

            scanCounterButton.setOnClickListener {
                counterLauncher.launch(scanOptions())
            }

            // The banner has no timeout on purpose - it holds the transaction number - so it needs
            // a way out. Closing it can uncover the empty state, hence the re-render.
            arrivalDismiss.setOnClickListener {
                arrivalCard.visibility = View.GONE
                renderEmptyState()
            }
        }

        setupObservers()
    }

    /**
     * The instruction doubles as the empty state, and it is allowed to say so only once.
     *
     * THREE THINGS HAVE TO BE TRUE: the load has finished, the list really is empty, and no
     * arrival is on screen. It used to be decided in the batches observer alone, which fires only
     * when a response ARRIVES - so before the first one, and during every refresh after it, the
     * screen asserted "you have nothing at the laundry" on no evidence.
     *
     * A FAILED LOAD LANDS HERE TOO, by way of `isLoading`: the batches observer returns early on a
     * refusal, and without this the courier would be left with a toast and a blank screen.
     *
     * The animation is started and stopped with it, as the task list does: a looping Lottie behind
     * a hidden container still renders every frame.
     */
    private fun renderEmptyState() {
        binding.apply {
            val show = viewModel.isLoading.value != true &&
                batchAdapter.itemCount == 0 &&
                arrivalCard.visibility != View.VISIBLE

            instructionContainer.visibility = if (show) View.VISIBLE else View.GONE

            if (show) {
                instructionAnimation.playAnimation()
            } else {
                instructionAnimation.pauseAnimation()
            }
        }
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
                batches.observe(this@LaundryCheckInActivity) { response ->
                    if (!response.isSuccess) {
                        toast(response.message, warning = true)
                        return@observe
                    }

                    val rows = response.data.orEmpty()
                    batchAdapter.submitList(rows)

                    rvBatches.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
                    renderEmptyState()
                }

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

                    arrivalCard.visibility = View.VISIBLE
                    renderEmptyState()

                    // The new batch belongs on the list straight away.
                    viewModel.loadBatches()
                }

                // `isLoading` belongs to the LIST on this screen - `arrive()` reports through
                // `isSubmitting` instead - so binding the spinner to it cannot leave it turning
                // through a counter scan. Bound to the flag rather than cleared in the success
                // handler, because that handler returns early on a refusal, and a failed refresh
                // is when a stuck spinner misleads most.
                isLoading.observe(this@LaundryCheckInActivity) { loading ->
                    swipeRefreshLayout.isRefreshing = loading
                    renderEmptyState()
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
