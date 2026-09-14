package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.LaundryBundleAdapter
import com.erela.fixme.adapters.recycler_view.LaundryQueueAdapter
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityLaundryCounterBinding
import com.erela.fixme.dialogs.ConfirmationDialog
import com.erela.fixme.helpers.enableEdgeToEdgeOpaqueNav
import com.erela.fixme.viewmodel.LaundryCheckInViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Counter Laundry — the OPERATOR scans the bundle a courier has brought.
 *
 * WHY THIS SCREEN EXISTS AT ALL. Until 12 Sep 2026 the courier scanned everything on their own
 * phone. GA corrected that [via Rosita Secoadi]: the courier scans the counter sticker and stops,
 * and the counter operator scans the uniforms. The garment scanning that used to live in
 * [LaundryCheckInActivity] is here now, and that screen is down to one button.
 *
 * ONE LIST, TWO MODES. The queue and the bundle share `rvList` and the adapter is swapped, because
 * the operator is never reading both: they pick a courier, then scan. Two RecyclerViews would be
 * two empty states and two scroll positions for one thing at a time.
 *
 * NOTHING IS SCANNED UNTIL A COURIER IS PICKED. A garment scanned against no arrival has no
 * transaction to land on, and the server would refuse it — but the operator needs the instruction,
 * not the refusal.
 *
 * SAVE IS NOT THE END. `laundryAddItems` may be called repeatedly for the same arrival, so a long
 * bundle can be banked in stages; the bundle clears on success and the same courier stays selected.
 *
 * THE BUTTONS ARE CARDS, which is this app's convention and has one consequence worth stating:
 * `isEnabled` on a MaterialCardView neither blocks the click nor changes how it looks. Every
 * enable/disable here goes through [enable] instead, which sets clickability AND dims — miss that
 * and a disabled-looking button still fires, or a live one looks dead.
 */
class LaundryCounterActivity : AppCompatActivity() {
    private val binding: ActivityLaundryCounterBinding by lazy {
        ActivityLaundryCounterBinding.inflate(layoutInflater)
    }
    private val viewModel: LaundryCheckInViewModel by viewModels()
    private lateinit var bundleAdapter: LaundryBundleAdapter
    private lateinit var queueAdapter: LaundryQueueAdapter

    /** True while the queue is on screen, false while a bundle is being scanned. */
    private var queueMode = true

    private fun MaterialCardView.enable(on: Boolean) {
        isClickable = on
        isFocusable = on
        alpha = if (on) 1f else 0.4f
    }

    /** A uniform patch. Resolved by the server before it joins the bundle. */
    private val patchLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.onQrScanned(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdgeOpaqueNav()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            // THE KEYBOARD IS PADDING, NOT A WINDOW RESIZE. `adjustResize` in the manifest does
            // nothing on its own here: edge-to-edge sets decorFitsSystemWindows=false, so the
            // window no longer shrinks for the IME and the note field and save button would sit
            // underneath it.
            //
            // The larger of the two, never the sum: the navigation bar is BEHIND the keyboard
            // when it is open, so adding them would leave a gap the height of the nav bar.
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }

        setupUI()
        setupObservers()

        showQueue()

        // A half-scanned bundle is worth a step back rather than losing the screen: Back returns
        // to the queue, and only leaves from there. Through the dispatcher rather than an
        // onBackPressed() override, which is deprecated and inert once a predictive-back
        // manifest flag is set.
        onBackPressedDispatcher.addCallback(this) {
            if (queueMode) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                return@addCallback
            }

            back()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refreshed on every return, not only on create: couriers join the queue while the
        // operator is mid-bundle, and the camera activity comes back through here too.
        if (queueMode) {
            viewModel.loadArrivals()
        }
    }

    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            val view: View? = currentFocus
            if (view is TextInputEditText || view is EditText) {
                val rect = Rect()
                view.getGlobalVisibleRect(rect)
                if (!rect.contains(motionEvent.rawX.toInt(), motionEvent.rawY.toInt())) {
                    view.clearFocus()
                    val inputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(motionEvent)
    }

    private fun scanOptions(prompt: String) = ScanOptions().apply {
        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        setPrompt(prompt)
        setCameraId(0)
        setBeepEnabled(true)
        setBarcodeImageEnabled(false)
        setOrientationLocked(true)
    }

    private fun setupUI() {
        binding.apply {
            toolBar.setNavigationOnClickListener {
                if (queueMode) finish() else back()
            }

            // The menu opens ONE of the two screens; it does not say which role a person may
            // play. Counter staff wear uniforms and hand them in like anybody else, so the
            // operator screen carries a door to the courier one rather than being a dead end.
            handInButton.setOnClickListener {
                startActivity(
                    Intent(this@LaundryCounterActivity, LaundryCheckInActivity::class.java)
                )
            }

            queueAdapter = LaundryQueueAdapter(
                context = this@LaundryCounterActivity,
                onPick = { arrival ->
                    viewModel.workOn(arrival)
                    showBundle()
                }
            )

            bundleAdapter = LaundryBundleAdapter(
                context = this@LaundryCounterActivity,
                // Filled in when a courier is picked. Null here on purpose: with no arrival there
                // is no department to compare a garment against, and the operator's own would
                // flag every garment in every bundle as mixed.
                ownDept = null,
                onRemove = { garment -> viewModel.remove(garment.qrCode) },
                onNoteChanged = { garment, note -> viewModel.setNote(garment.qrCode, note) }
            )

            rvList.layoutManager = LinearLayoutManager(this@LaundryCounterActivity)

            pickCourierButton.setOnClickListener { back() }

            fabScanPatch.setOnClickListener {
                // Refused here rather than by the server, because the operator needs the
                // instruction ("pick a courier first"), not a rejection.
                if (viewModel.workingIdTrx == null) {
                    toast(getString(R.string.laundry_pick_courier_required), warning = true)
                    return@setOnClickListener
                }

                patchLauncher.launch(scanOptions(getString(R.string.laundry_scan_patch_prompt)))
            }

            clearButton.setOnClickListener {
                val count = viewModel.count()

                if (count == 0) {
                    return@setOnClickListener
                }

                ConfirmationDialog(
                    this@LaundryCounterActivity,
                    getString(R.string.laundry_clear_confirm, count),
                    if (getString(R.string.lang) == "in") "Ya" else "Yes"
                ).also { dialog ->
                    dialog.setConfirmationDialogListener(
                        object : ConfirmationDialog.ConfirmationDialogListener {
                            override fun onConfirm() {
                                viewModel.clearBundle()
                            }
                        }
                    )

                    if (dialog.window != null) {
                        dialog.show()
                    }
                }
            }

            submitButton.setOnClickListener {
                viewModel.saveItems(etBatchNote.text?.toString()?.trim()?.ifBlank { null })
            }
        }
    }

    /** Out of a bundle and back to the queue, refreshed — couriers arrive while one is served. */
    private fun back() {
        showQueue()
        viewModel.loadArrivals()
    }

    /** Queue mode: the waiting couriers, and nothing that acts on a bundle. */
    private fun showQueue() {
        queueMode = true

        binding.apply {
            rvList.adapter = queueAdapter

            tvServing.text = getString(R.string.laundry_no_courier_selected)
            tvServingTrxNo.visibility = View.GONE
            tvEmptyMessage.text = getString(R.string.laundry_queue_empty)

            tvBundleCount.visibility = View.GONE
            clearButton.visibility = View.GONE
            noteField.visibility = View.GONE
            submitButton.visibility = View.GONE
            fabScanPatch.visibility = View.GONE

            renderEmptyState(queueAdapter.itemCount)
        }
    }

    /** Bundle mode: the garments scanned onto the picked courier's arrival. */
    @SuppressLint("SetTextI18n")
    private fun showBundle() {
        queueMode = false

        binding.apply {
            bundleAdapter.ownDept = viewModel.workingDept
            rvList.adapter = bundleAdapter

            tvServing.text = listOfNotNull(
                viewModel.workingCourier,
                viewModel.workingDept
            ).joinToString(" · ").ifBlank { "-" }

            tvEmptyMessage.text = getString(R.string.laundry_bundle_empty)

            tvBundleCount.visibility = View.VISIBLE
            clearButton.visibility = View.VISIBLE
            noteField.visibility = View.VISIBLE
            submitButton.visibility = View.VISIBLE
            fabScanPatch.visibility = View.VISIBLE

            etBatchNote.setText("")

            // Set here as well as in the observer, because the observer returns early while the
            // queue is on screen - so entering bundle mode with an empty bundle would otherwise
            // leave both buttons looking live. Neither would do anything, which is worse than
            // being refused: it reads as a broken screen rather than a step not yet taken.
            clearButton.enable(viewModel.count() > 0)
            submitButton.enable(viewModel.count() > 0)

            renderEmptyState(viewModel.count())
        }
    }

    /** The list or the animation, never both. */
    private fun renderEmptyState(count: Int) {
        binding.apply {
            rvList.visibility = if (count == 0) View.GONE else View.VISIBLE
            emptyContainer.visibility = if (count == 0) View.VISIBLE else View.GONE

            // Paused when hidden, as the task list does it: a Lottie left running behind a full
            // list still renders every frame.
            if (count == 0) {
                emptyAnimation.playAnimation()
            } else {
                emptyAnimation.pauseAnimation()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        viewModel.apply {
            binding.apply {
                items.observe(this@LaundryCounterActivity) { list ->
                    bundleAdapter.submitList(list)

                    if (queueMode) {
                        return@observe
                    }

                    tvBundleCount.text = getString(R.string.laundry_bundle_count, list.size)
                    renderEmptyState(list.size)

                    clearButton.enable(list.isNotEmpty())
                    submitButton.enable(list.isNotEmpty())
                }

                arrivals.observe(this@LaundryCounterActivity) { response ->
                    if (!response.isSuccess) {
                        toast(response.message, warning = true)
                        return@observe
                    }

                    val waiting = response.data.orEmpty()
                    queueAdapter.submitList(waiting)

                    if (queueMode) {
                        renderEmptyState(waiting.size)
                    }
                }

                scanResult.observe(this@LaundryCounterActivity) { response ->
                    // A refusal is the useful case: the server's sentence says what to do about
                    // it — register the garment, or finish the transaction it is already on.
                    if (!response.isSuccess) {
                        toast(response.message, warning = true)
                    }
                }

                checkInResult.observe(this@LaundryCounterActivity) { response ->
                    if (!response.isSuccess) {
                        toast(response.message, warning = true)
                        return@observe
                    }
                    // The server's message already names the transaction, the count, any garments
                    // belonging to other departments, and any held items that joined the bundle.
                    // Repeating it here in our own words would be a second version to keep in step.
                    toast(response.message, warning = false)

                    // The courier stays selected: the same bundle may be saved in stages, and an
                    // operator who has more garments in hand should not have to pick them again.
                    etBatchNote.setText("")
                    tvServingTrxNo.text = response.data?.trxNo ?: ""
                    tvServingTrxNo.visibility =
                        if (response.data?.trxNo.isNullOrBlank()) View.GONE else View.VISIBLE
                }

                isLoading.observe(this@LaundryCounterActivity) { loading ->
                    pickCourierButton.enable(!loading)
                }

                isSubmitting.observe(this@LaundryCounterActivity) { submitting ->
                    // Progress INSIDE the button, the way the login button does it, rather than a
                    // dialog over the screen: the operator can still see the bundle while it saves.
                    submitLoadingBar.visibility = if (submitting) View.VISIBLE else View.GONE
                    submitText.visibility = if (submitting) View.INVISIBLE else View.VISIBLE

                    submitButton.enable(!submitting && viewModel.count() > 0)
                }

                error.observe(this@LaundryCounterActivity) { message ->
                    // A double scan is an ordinary slip, not an error worth alarming language.
                    if (message.startsWith(LaundryCheckInViewModel.DUPLICATE_PREFIX)) {
                        val code = message.removePrefix(LaundryCheckInViewModel.DUPLICATE_PREFIX)
                        toast(getString(R.string.laundry_duplicate_code, code), warning = true)
                        return@observe
                    }

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
