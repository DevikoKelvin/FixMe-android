package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.app.DatePickerDialog
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
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.erela.fixme.R
import androidx.core.widget.doAfterTextChanged
import com.erela.fixme.adapters.recycler_view.LaundryBundleAdapter
import com.erela.fixme.adapters.recycler_view.LaundryHistoryAdapter
import com.erela.fixme.adapters.recycler_view.LaundryOutScanAdapter
import com.erela.fixme.adapters.recycler_view.LaundryQueueAdapter
import com.erela.fixme.adapters.recycler_view.LaundryReadyAdapter
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityLaundryCounterBinding
import com.erela.fixme.dialogs.ConfirmationDialog
import com.erela.fixme.helpers.enableEdgeToEdgeOpaqueNav
import com.erela.fixme.objects.laundry.LaundryReadyBatch
import com.erela.fixme.objects.laundry.LaundryWaitingCourier
import com.erela.fixme.viewmodel.LaundryCheckInViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
 *
 * TWO FLOWS, ONE SCREEN — GA, 15 September 2026. Collection was split the same way check-in was:
 * the operator scans the bundle back OUT and marks it ready, and only then may a courier collect.
 * That is this screen again with four decisions changed — which queue loads, which adapter the
 * list gets, what the scanner feeds, and what the save button calls — so it is [Flow] on this
 * activity rather than three hundred lines of near-identical second activity. Launched at itself
 * with [EXTRA_FLOW], so Back walks out of the outgoing flow into the incoming one.
 *
 * The OUT flow has no note field: a note belongs to a bundle being received, and the garments
 * going out already carry the condition the laundry recorded for each of them.
 */
class LaundryCounterActivity : AppCompatActivity() {
    private val binding: ActivityLaundryCounterBinding by lazy {
        ActivityLaundryCounterBinding.inflate(layoutInflater)
    }
    private val viewModel: LaundryCheckInViewModel by viewModels()
    private lateinit var bundleAdapter: LaundryBundleAdapter
    private lateinit var queueAdapter: LaundryQueueAdapter
    private lateinit var outScanAdapter: LaundryOutScanAdapter
    private lateinit var readyAdapter: LaundryReadyAdapter
    private lateinit var historyAdapter: LaundryHistoryAdapter

    /** True while a list is on screen, false while a bundle is being scanned. */
    private var queueMode = true

    /**
     * The counter's four lists, in the order the tabs show them and the Compose app shows them.
     *
     * A TAB, NOT FOUR ACTIVITIES [GA, 18 Sep 2026]. Two of these used to relaunch this activity
     * with a flag and two opened a second activity; all four are the same rows read for
     * different reasons, and the operator's job is comparing them. Leaving the screen to answer
     * "is it washed yet" was the cost of that arrangement.
     */
    private enum class Flow { INCOMING, ACTIVE, OUTGOING, HISTORY }

    private var flow = Flow.INCOMING

    /** Which end of the day this is: garments arriving, or garments leaving. */
    private val outgoing: Boolean get() = flow == Flow.OUTGOING

    /** Whether this tab's rows act on anything, or are a record being read. */
    private val readOnly: Boolean get() = flow == Flow.ACTIVE || flow == Flow.HISTORY

    // WHAT THE SERVER SENT, kept beside what is on screen: the search box filters a list rather
    // than re-fetching it, so the unfiltered rows have to survive somewhere. Four fields rather
    // than one of a common type - two of these lists are a different shape.
    private var rawArrivals = emptyList<LaundryWaitingCourier>()
    private var rawActive = emptyList<LaundryWaitingCourier>()
    private var rawReady = emptyList<LaundryReadyBatch>()
    private var rawHistory = emptyList<LaundryWaitingCourier>()
    private var query = ""

    /** `YYYY-MM-DD`, or null for "any date" - the default, and it has to stay reachable. */
    private var from: String? = null
    private var to: String? = null
    private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private fun MaterialCardView.enable(on: Boolean) {
        isClickable = on
        isFocusable = on
        alpha = if (on) 1f else 0.4f
    }

    /**
     * A uniform patch.
     *
     * ON THE WAY IN the server resolves it before it joins the bundle, because the operator cannot
     * tell a wrong garment from a right one by looking at six digits. ON THE WAY OUT there is
     * nothing to resolve: the question is whether the code belongs to THIS batch, which the master
     * register cannot answer and the server checks on submit.
     */
    private val patchLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let {
            if (outgoing) viewModel.onOutScanned(it) else viewModel.onQrScanned(it)
        }
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

        renderRange()
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
            loadQueue()
        }
    }

    /** The list this tab works from. */
    private fun loadQueue() {
        when (flow) {
            Flow.INCOMING -> viewModel.loadArrivals()
            Flow.ACTIVE -> viewModel.loadActiveQueue()
            Flow.OUTGOING -> viewModel.loadHandoverQueue()
            Flow.HISTORY -> viewModel.loadHistory(from, to)
        }
    }

    /**
     * Whether one row survives the search box.
     *
     * EVERY TERM MUST HIT, ANY FIELD MAY ANSWER IT. "produksi 0003" is how somebody actually
     * narrows fifty rows, and a single `contains` of the whole phrase finds nothing - the
     * department and the number live in different fields. The Compose app splits it the same way.
     */
    private fun matches(vararg fields: String?): Boolean {
        val terms = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

        return terms.all { term -> fields.any { it?.contains(term, ignoreCase = true) == true } }
    }

    /** The current tab's rows, filtered, into the adapter that draws them. */
    private fun renderList() {
        if (!queueMode) {
            return
        }
        val count = when (flow) {
            Flow.INCOMING -> rawArrivals
                .filter { matches(it.trxNo, it.pengantar, it.namaDept, it.subDept) }
                .also { queueAdapter.submitList(it) }.size

            Flow.ACTIVE -> rawActive
                .filter { matches(it.trxNo, it.pengantar, it.namaDept, it.subDept) }
                .also { historyAdapter.submitList(it) }.size

            Flow.OUTGOING -> rawReady
                .filter { matches(it.trxNo, it.pengantar, it.namaDept, it.subDept) }
                .also { readyAdapter.submitList(it) }.size

            Flow.HISTORY -> rawHistory
                .filter { matches(it.trxNo, it.pengantar, it.namaDept, it.subDept) }
                .also { historyAdapter.submitList(it) }.size
        }

        renderEmptyState(count)
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
            // The title stays put and the subtitle carries the tab, which is how the Compose
            // header reads: four one-word tabs cannot say whether "Masuk" means couriers waiting
            // or garments arriving.
            title.text = getString(R.string.laundry_counter_title)

            listOf(
                R.string.laundry_tab_incoming,
                R.string.laundry_in_progress,
                R.string.laundry_handover_action,
                R.string.laundry_history_title
            ).forEach { label ->
                tabLayout.addTab(tabLayout.newTab().setText(getString(label)))
            }

            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    flow = Flow.entries[tab.position]
                    // Out of a half-scanned bundle, because the tab is a different list: staying
                    // in bundle mode would leave the save button acting on the batch behind it.
                    showQueue()
                    loadQueue()
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit

                // Re-tapping the tab you are on is how somebody asks for the list again.
                override fun onTabReselected(tab: TabLayout.Tab) {
                    if (!queueMode) back() else loadQueue()
                }
            })
            // AN ICON UNTIL IT IS WANTED, like the Compose header: a field across the top of a
            // phone leaves no room for the title, and the operator searches on the day the queue
            // is long - not on the twenty days it holds two rows.
            searchButton.setOnClickListener {
                val opening = searchField.visibility != View.VISIBLE

                // The field fades, and the tabs and the list slide to meet it. `AutoTransition`
                // is Fade + ChangeBounds, which is both halves of that in one line and the same
                // call the splash screen and the submission footer already use.
                //
                // ONLY ON THE TAP. showQueue/showBundle set the same visibility without this:
                // those are a whole screen changing mode, and animating one field in the middle
                // of it draws the eye to the wrong thing.
                TransitionManager.beginDelayedTransition(main, AutoTransition())

                searchField.visibility = if (opening) View.VISIBLE else View.GONE
                // Closing clears: a hidden filter silently holding rows back is the bug report
                // "the batch is not in the list".
                if (!opening) {
                    etSearch.setText("")
                }
            }

            etSearch.doAfterTextChanged { text ->
                query = text?.toString().orEmpty()
                renderList()
            }

            refreshButton.setOnClickListener { loadQueue() }
            // The courier's door, as a floating button in the bottom-left corner - where the
            // Compose app puts it. Counter staff wear uniforms and hand them in like anybody
            // else, so this screen is not a dead end for them.
            fabHandIn.setOnClickListener {
                startActivity(
                    Intent(this@LaundryCounterActivity, LaundryCheckInActivity::class.java)
                )
            }

            fromButton.setOnClickListener { pickDate(isFrom = true) }
            toButton.setOnClickListener { pickDate(isFrom = false) }

            rangeClearButton.setOnClickListener {
                from = null
                to = null
                renderRange()
                loadQueue()
            }
            // THE TWO READ-ONLY TABS share one adapter and one destination. On a finished batch
            // the process screen has no action buttons - the `when` over the status has no branch
            // for `completed` - so what is left is what somebody opening a record came for: what
            // was in it, and the button that reprints its slip.
            historyAdapter = LaundryHistoryAdapter(
                context = this@LaundryCounterActivity,
                showCompletedAt = false
            ) { batch ->
                startActivity(
                    Intent(this@LaundryCounterActivity, LaundryProcessActivity::class.java)
                        .putExtra(LaundryProcessActivity.EXTRA_ID_TRX, batch.id)
                )
            }

            queueAdapter = LaundryQueueAdapter(
                context = this@LaundryCounterActivity,
                onPick = { arrival ->
                    viewModel.workOn(arrival)
                    showBundle()
                }
            )

            readyAdapter = LaundryReadyAdapter(
                context = this@LaundryCounterActivity,
                onPick = { batch ->
                    viewModel.beginHandover(batch)
                    showBundle()
                }
            )

            outScanAdapter = LaundryOutScanAdapter(
                onRemove = { code -> viewModel.removeOutScan(code) }
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
                    toast(
                        getString(
                            if (outgoing) R.string.laundry_pick_batch_required
                            else R.string.laundry_pick_courier_required
                        ),
                        warning = true
                    )
                    return@setOnClickListener
                }

                patchLauncher.launch(
                    scanOptions(
                        getString(
                            if (outgoing) R.string.laundry_scan_out_prompt
                            else R.string.laundry_scan_patch_prompt
                        )
                    )
                )
            }

            clearButton.setOnClickListener {
                val count = if (outgoing) viewModel.outCount() else viewModel.count()

                if (count == 0) {
                    return@setOnClickListener
                }

                ConfirmationDialog(
                    this@LaundryCounterActivity,
                    getString(
                        if (outgoing) R.string.laundry_clear_out_confirm
                        else R.string.laundry_clear_confirm,
                        count
                    ),
                    if (getString(R.string.lang) == "in") "Ya" else "Yes"
                ).also { dialog ->
                    dialog.setConfirmationDialogListener(
                        object : ConfirmationDialog.ConfirmationDialogListener {
                            override fun onConfirm() {
                                if (outgoing) viewModel.clearOutScan() else viewModel.clearBundle()
                            }
                        }
                    )

                    if (dialog.window != null) {
                        dialog.show()
                    }
                }
            }

            submitButton.setOnClickListener {
                if (outgoing) {
                    viewModel.markHandover()
                } else {
                    viewModel.saveItems(etBatchNote.text?.toString()?.trim()?.ifBlank { null })
                }
            }
        }
    }

    /** Out of a bundle and back to the queue, refreshed — couriers arrive while one is served. */
    private fun back() {
        showQueue()
        loadQueue()
    }

    /** List mode: whichever tab is open, and nothing that acts on a bundle. */
    private fun showQueue() {
        queueMode = true

        binding.apply {
            historyAdapter.showCompletedAt = flow == Flow.HISTORY

            rvList.adapter = when (flow) {
                Flow.INCOMING -> queueAdapter
                Flow.OUTGOING -> readyAdapter
                Flow.ACTIVE, Flow.HISTORY -> historyAdapter
            }

            subtitle.setText(
                when (flow) {
                    Flow.INCOMING -> R.string.laundry_counter_subtitle
                    Flow.ACTIVE -> R.string.laundry_in_progress_subtitle
                    Flow.OUTGOING -> R.string.laundry_handover_subtitle
                    Flow.HISTORY -> R.string.laundry_history_subtitle
                }
            )

            tvServing.text = getString(R.string.laundry_no_courier_selected)
            tvServingTrxNo.visibility = View.GONE
            // Today's work carries no date filter: it could only hide something still to do.
            rangeRow.visibility = if (flow == Flow.HISTORY) View.VISIBLE else View.GONE
            // The tabs and the search belong to the lists, where the operator is between tasks.
            // Mid-bundle they are navigation offered at the worst moment.
            tabLayout.visibility = View.VISIBLE
            searchButton.visibility = View.VISIBLE
            refreshButton.visibility = View.VISIBLE
            searchField.visibility = if (query.isBlank()) View.GONE else View.VISIBLE
            fabHandIn.visibility = View.VISIBLE

            arrivalCard.visibility = View.GONE
            tvBundleCount.visibility = View.GONE
            clearButton.visibility = View.GONE
            noteField.visibility = View.GONE
            submitButton.visibility = View.GONE
            fabScanPatch.visibility = View.GONE

            renderList()
        }
    }

    /** Bundle mode: the garments scanned onto the picked courier's arrival. */
    @SuppressLint("SetTextI18n")
    private fun showBundle() {
        queueMode = false

        binding.apply {
            bundleAdapter.ownDept = viewModel.workingDept
            rvList.adapter = if (outgoing) outScanAdapter else bundleAdapter

            tvServing.text = listOfNotNull(
                viewModel.workingCourier,
                viewModel.workingDept
            ).joinToString(" · ").ifBlank { "-" }

            tvEmptyMessage.text = getString(
                if (outgoing) R.string.laundry_handover_bundle_empty
                else R.string.laundry_bundle_empty
            )

            submitText.text = getString(
                if (outgoing) R.string.laundry_handover_submit else R.string.laundry_submit
            )

            tabLayout.visibility = View.GONE
            searchButton.visibility = View.GONE
            refreshButton.visibility = View.GONE
            searchField.visibility = View.GONE
            rangeRow.visibility = View.GONE
            fabHandIn.visibility = View.GONE

            arrivalCard.visibility = View.VISIBLE
            tvBundleCount.visibility = View.VISIBLE
            clearButton.visibility = View.VISIBLE
            // No note going out: a note belongs to a bundle being RECEIVED, and every garment
            // leaving already carries the condition the laundry recorded for it.
            noteField.visibility = if (outgoing) View.GONE else View.VISIBLE
            submitButton.visibility = View.VISIBLE
            fabScanPatch.visibility = View.VISIBLE

            etBatchNote.setText("")
            // Set here as well as in the observer, because the observer returns early while the
            // queue is on screen - so entering bundle mode with an empty bundle would otherwise
            // leave both buttons looking live. Neither would do anything, which is worse than
            // being refused: it reads as a broken screen rather than a step not yet taken.
            val count = if (outgoing) viewModel.outCount() else viewModel.count()

            clearButton.enable(count > 0)
            submitButton.enable(count > 0)

            renderEmptyState(count)
        }
    }

    /** The list or the animation, never both. */
    private fun renderEmptyState(count: Int) {
        binding.apply {
            if (queueMode) {
                // Before the list's own sentence: "nobody is waiting" is a lie when three
                // couriers are waiting and the filter hid them.
                tvEmptyMessage.setText(
                    when {
                        query.isNotBlank() -> R.string.laundry_search_empty
                        flow == Flow.ACTIVE -> R.string.laundry_in_progress_empty
                        flow == Flow.OUTGOING -> R.string.laundry_handover_queue_empty
                        flow == Flow.HISTORY -> R.string.laundry_history_empty
                        else -> R.string.laundry_queue_empty
                    }
                )
            }

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

                    if (queueMode || outgoing) {
                        return@observe
                    }

                    tvBundleCount.text = getString(R.string.laundry_bundle_count, list.size)
                    renderEmptyState(list.size)

                    clearButton.enable(list.isNotEmpty())
                    submitButton.enable(list.isNotEmpty())
                }

                outItems.observe(this@LaundryCounterActivity) { list ->
                    outScanAdapter.submitList(list)

                    if (queueMode || !outgoing) {
                        return@observe
                    }

                    tvBundleCount.text = getString(R.string.laundry_bundle_count, list.size)
                    renderEmptyState(list.size)

                    clearButton.enable(list.isNotEmpty())
                    submitButton.enable(list.isNotEmpty())
                }

                handoverQueue.observe(this@LaundryCounterActivity) { response ->
                    if (!response.isSuccess) {
                        toast(response.message, warning = true)
                        return@observe
                    }

                    rawReady = response.data.orEmpty()

                    if (flow == Flow.OUTGOING) {
                        renderList()
                    }
                }

                markResult.observe(this@LaundryCounterActivity) { response ->
                    if (!response.isSuccess) {
                        toast(response.message, warning = true)
                        return@observe
                    }
                    // The server's message names the count and the transaction. Back to the queue
                    // rather than staying on a batch that is now finished: the operator's next act
                    // is the next batch, and leaving them on a cleared list reads as a failed save.
                    toast(response.message, warning = false)
                    back()
                }

                arrivals.observe(this@LaundryCounterActivity) { response ->
                    if (!response.isSuccess) {
                        toast(response.message, warning = true)
                        return@observe
                    }

                    rawArrivals = response.data.orEmpty()

                    if (flow == Flow.INCOMING) {
                        renderList()
                    }
                }
                // THE TWO READ-ONLY TABS. Two fields rather than one, because only one of them
                // is ever asked for and the other stays silent - a shared field would redraw the
                // open tab with the other tab's rows.
                active.observe(this@LaundryCounterActivity) { response ->
                    if (!response.isSuccess) {
                        toast(response.message, warning = true)
                        return@observe
                    }

                    rawActive = response.data.orEmpty()

                    if (flow == Flow.ACTIVE) {
                        renderList()
                    }
                }

                history.observe(this@LaundryCounterActivity) { response ->
                    if (!response.isSuccess) {
                        toast(response.message, warning = true)
                        return@observe
                    }

                    rawHistory = response.data.orEmpty()

                    if (flow == Flow.HISTORY) {
                        renderList()
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
                    val count = if (outgoing) viewModel.outCount() else viewModel.count()

                    submitButton.enable(!submitting && count > 0)
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

    /**
     * The platform's own picker.
     *
     * Opens on the date already chosen, or today. Typing `2026-09-18` on a phone is how a range
     * ends up empty for a reason nobody can see.
     */
    private fun pickDate(isFrom: Boolean) {
        val calendar = Calendar.getInstance()

        (if (isFrom) from else to)?.let { existing ->
            runCatching { formatter.parse(existing) }.getOrNull()?.let { calendar.time = it }
        }

        DatePickerDialog(
            this,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0) }

                if (isFrom) {
                    from = formatter.format(picked.time)
                } else {
                    to = formatter.format(picked.time)
                }

                renderRange()
                loadQueue()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun renderRange() {
        binding.fromText.text = from ?: getString(R.string.laundry_date_from)
        binding.toText.text = to ?: getString(R.string.laundry_date_to)
        binding.rangeClearButton.visibility =
            if (from == null && to == null) View.GONE else View.VISIBLE
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
