package com.erela.fixme.activities

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.LaundryProcessLineAdapter
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityLaundryProcessBinding
import com.erela.fixme.dialogs.ConfirmationDialog
import com.erela.fixme.dialogs.DialogOption
import com.erela.fixme.dialogs.InputDialog
import com.erela.fixme.dialogs.OptionListDialog
import com.erela.fixme.helpers.ThermalPrinter
import com.erela.fixme.helpers.enableEdgeToEdgeOpaqueNav
import com.erela.fixme.objects.laundry.LaundryBatchHeader
import com.erela.fixme.objects.laundry.LaundryCollector
import com.erela.fixme.objects.laundry.LaundrySlipRow
import com.erela.fixme.viewmodel.LaundryCheckInViewModel
import com.google.android.material.card.MaterialCardView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

/**
 * One batch, and everything the counter does to it — ported from Compose, 17 September 2026.
 *
 * THE WEB IS A DESK AND THE OPERATOR IS AT THE MACHINES. Verifying a bundle, starting the wash,
 * recording what came out and signing the batch off all existed only on a PC, so the wash start —
 * the moment every department lead time is measured from — got typed in whenever somebody next
 * walked past a keyboard.
 *
 * ONE STAGE, ONE PAIR OF BUTTONS. The actions are stages of the same batch and only ever one of
 * them is legal at a time, so the screen offers the stage the batch is actually at. A column of
 * five buttons, four of them refusals waiting to happen, is the same information arranged to be
 * got wrong.
 *
 * IT DECIDES NOTHING. Every rule — who may accept, when a note is compulsory, whether a batch may
 * be signed off — is in `LaundryProcess` on the server, which the web screens and the Compose app
 * call too. What this knows is which button to show.
 */
class LaundryProcessActivity : AppCompatActivity() {
    private val binding: ActivityLaundryProcessBinding by lazy {
        ActivityLaundryProcessBinding.inflate(layoutInflater)
    }
    private val viewModel: LaundryCheckInViewModel by viewModels()
    private val idTrx: Int by lazy { intent.getIntExtra(EXTRA_ID_TRX, 0) }
    private lateinit var lineAdapter: LaundryProcessLineAdapter

    /** Lines ticked for an exit condition or a handover. Cleared whenever the batch reloads. */
    private var selected = mutableSetOf<Int>()
    private var header: LaundryBatchHeader? = null
    private var lineIds = emptyList<Int>()

    /** The garment just scanned, waiting for the operator to say what condition it arrived in. */
    private val patchLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { code ->
            askCondition { condition, note ->
                viewModel.verifyGarment(idTrx, code, condition, note)
            }
        }
    }
    /**
     * Bluetooth, asked for the first time somebody prints.
     *
     * NOT AT LAUNCH. An operator who never prints is never asked, and a permission prompt makes
     * sense at the moment it is obviously for something - which is the tap on the printer icon.
     *
     * A REFUSAL HAS TO SAY SO. `if (granted)` with no else left a button that looked broken: the
     * system dialog does not reappear once somebody has refused twice, so the second tap did
     * nothing at all and there was nothing on screen to explain it.
     */
    private val askBluetooth = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        when {
            granted -> viewModel.loadSlip(idTrx)

            // Android stops showing the dialog after a second refusal, and says so by returning
            // false here. Settings is then the only way back, so offer to open it.
            !shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT) ->
                offerAppSettings()

            else -> toast(getString(R.string.laundry_bluetooth_needed), warning = true)
        }
    }

    /** The one route back once the prompt has been refused for good. */
    private fun offerAppSettings() {
        ConfirmationDialog(
            this,
            getString(R.string.laundry_bluetooth_blocked),
            getString(R.string.laundry_open_settings)
        ).also { dialog ->
            dialog.setConfirmationDialogListener(
                object : ConfirmationDialog.ConfirmationDialogListener {
                    override fun onConfirm() {
                        startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", packageName, null)
                            )
                        )
                    }
                }
            )
        }.show()
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

        binding.apply {
            toolBar.setNavigationOnClickListener { finish() }
            // The RecyclerView is the thing that actually scrolls, so it is the thing the refresh
            // layout asks - its direct child is a ConstraintLayout, which never can.
            swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
                rvItems.canScrollVertically(-1)
            }
            swipeRefreshLayout.setOnRefreshListener { viewModel.loadCounterBatch(idTrx) }

            lineAdapter = LaundryProcessLineAdapter(this@LaundryProcessActivity) { line ->
                if (line.id in selected) selected.remove(line.id) else selected.add(line.id)
                lineAdapter.setSelected(selected)
                renderSelection()
            }

            rvItems.apply {
                layoutManager = LinearLayoutManager(this@LaundryProcessActivity)
                adapter = lineAdapter
            }

            printButton.setOnClickListener {
                val permission = ThermalPrinter.permission()

                if (permission != null &&
                    !ThermalPrinter.hasPermission(this@LaundryProcessActivity)
                ) {
                    askBluetooth.launch(permission)
                } else {
                    // The lines come down first; the picker opens when they arrive.
                    viewModel.loadSlip(idTrx)
                }
            }
        }

        setupObservers()
        viewModel.loadCounterBatch(idTrx)
    }

    private fun setupObservers() {
        viewModel.apply {
            counterBatch.observe(this@LaundryProcessActivity) { response ->
                val data = response.data

                if (!response.isSuccess || data == null) {
                    toast(response.message, warning = true)
                    return@observe
                }

                header = data.transaction
                lineIds = data.items.map { it.id }
                selected = mutableSetOf()
                val head = data.transaction

                binding.tvTrxNo.text = head.trxNo
                binding.tvStatus.text = getString(
                    when (head.status) {
                        "ready" if head.handoverMarkedAt == null ->
                            R.string.laundry_status_awaiting_counter

                        "waiting" -> R.string.laundry_status_waiting
                        "accepted" -> R.string.laundry_status_accepted
                        "ready" -> R.string.laundry_status_ready
                        else -> R.string.laundry_status_completed
                    }
                )
                // Only while verifying: the count the Accept button exists for.
                binding.tvProgress.visibility =
                    if (head.status == "waiting") View.VISIBLE else View.GONE
                binding.tvProgress.text =
                    getString(R.string.laundry_verify_progress, data.verified, data.claimed)
                // A slip only means something once there is a handover or a total to print.
                binding.printButton.visibility =
                    if (head.status == "waiting") View.GONE else View.VISIBLE

                lineAdapter.submitList(data.items, canTick = tickable(head))
                lineAdapter.setSelected(selected)

                binding.rvItems.visibility = if (data.items.isEmpty()) View.GONE else View.VISIBLE
                binding.emptyContainer.visibility =
                    if (data.items.isEmpty()) View.VISIBLE else View.GONE

                // The house empty state is an animation, and one nobody can see should not be
                // running - every other list in this app pauses it the same way.
                if (data.items.isEmpty()) {
                    binding.emptyAnimation.playAnimation()
                } else {
                    binding.emptyAnimation.pauseAnimation()
                }

                renderActions(
                    head,
                    data.items.none { it.source == "scan" && it.conditionOut == null })
                renderSelection()
            }

            actionResult.observe(this@LaundryProcessActivity) { response ->
                // The server's sentence, not ours: it names the garment, the count or the role
                // that was wrong, and a second wording here would be one to keep in step with
                // five refusal paths.
                toast(response.message, warning = !response.isSuccess)
            }

            slip.observe(this@LaundryProcessActivity) { rows ->
                if (rows.isNotEmpty()) askPrinter(rows)
            }

            slipNeedsReason.observe(this@LaundryProcessActivity) { message ->
                askReprintReason(message)
            }

            isLoading.observe(this@LaundryProcessActivity) { loading ->
                binding.swipeRefreshLayout.isRefreshing = loading
            }

            // The spinner rides inside the primary card, the way Collect and Submit already do.
            isSubmitting.observe(this@LaundryProcessActivity) { busy ->
                binding.primaryLoadingBar.visibility = if (busy) View.VISIBLE else View.GONE
                binding.primaryText.visibility = if (busy) View.INVISIBLE else View.VISIBLE
                binding.primaryButton.enable(!busy)
                binding.secondaryButton.enable(!busy)
            }

            error.observe(this@LaundryProcessActivity) { message ->
                toast(message, warning = true)
            }
        }
    }

    /** Ticks are only meaningful where a bulk action exists to use them. */
    private fun tickable(head: LaundryBatchHeader): Boolean =
        (head.status == "accepted" && head.washingStartedAt != null) || head.status == "ready"

    /**
     * Which two buttons this stage offers.
     *
     * `washing_started_at` IS A TIMESTAMP, NOT A STATUS [3e], so "accepted" covers both the bundle
     * waiting for a machine and the one turning in it - and those two offer different actions.
     */
    private fun renderActions(head: LaundryBatchHeader, allJudged: Boolean) {
        val secondary = binding.secondaryButton
        val primary = binding.primaryButton
        // NOT `readyToCollectText`. The card is the batch screen's, but its label here is
        // Terima, Mulai cuci, Siap diambil or Serahkan depending on the stage - a name that only
        // describes one of the four is a name that reads as a bug in the other three.
        val primaryText = binding.primaryText

        secondary.visibility = View.GONE
        primary.visibility = View.GONE
        primary.enable(true)

        when (head.status) {
            "waiting" -> {
                secondary.visibility = View.VISIBLE
                binding.secondaryText.text = getString(R.string.laundry_verify)
                secondary.setOnClickListener { scanPatch() }

                primary.visibility = View.VISIBLE
                primaryText.text = getString(R.string.laundry_accept)
                primary.setOnClickListener { viewModel.acceptBatch(idTrx) }
            }

            "accepted" if head.washingStartedAt == null -> {
                primary.visibility = View.VISIBLE
                primaryText.text = getString(R.string.laundry_wash_start)
                primary.setOnClickListener { viewModel.startWash(idTrx) }
            }

            "accepted" -> {
                secondary.visibility = View.VISIBLE
                binding.secondaryText.text = getString(R.string.laundry_condition_out)
                secondary.setOnClickListener {
                    if (selected.isEmpty()) {
                        toast(getString(R.string.laundry_nothing_selected), warning = true)
                    } else {
                        askCondition { condition, note ->
                            viewModel.recordConditionOut(
                                idTrx, selected.toList(), condition, note
                            )
                        }
                    }
                }

                primary.visibility = View.VISIBLE
                primaryText.text = getString(R.string.laundry_mark_ready)
                // DEAD UNTIL EVERY GARMENT HAS BEEN JUDGED, which is the server's own rule for
                // `ready`. A live button whose only outcome is a refusal, on the screen that has
                // just asked for thirty conditions, is worse than no button.
                primary.enable(allJudged)
                primary.setOnClickListener { viewModel.markReady(idTrx) }
            }

            "ready" -> {
                primary.visibility = View.VISIBLE
                primaryText.text = getString(R.string.laundry_hand_over)
                primary.setOnClickListener {
                    if (selected.isEmpty()) {
                        toast(getString(R.string.laundry_nothing_selected), warning = true)
                    } else {
                        viewModel.loadCollectors(idTrx)
                        askCollector()
                    }
                }
            }
        }
    }

    /** A card button, enabled or not, the way the collect button on the batch screen does it. */
    private fun MaterialCardView.enable(on: Boolean) {
        isClickable = on
        isFocusable = on
        alpha = if (on) 1f else 0.4f
    }

    private fun renderSelection() {
        binding.tvSelected.visibility = if (selected.isEmpty()) View.GONE else View.VISIBLE
        binding.tvSelected.text = getString(R.string.laundry_selected_count, selected.size)
    }

    private fun scanPatch() {
        patchLauncher.launch(
            ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt(getString(R.string.laundry_scan_patch_prompt))
                setCameraId(0)
                setBeepEnabled(true)
                setBarcodeImageEnabled(false)
                setOrientationLocked(true)
            }
        )
    }

    /**
     * The four conditions, and the note the last three oblige.
     *
     * TWO DIALOGS, NOT ONE WITH A FIELD IN IT. A note is compulsory exactly when the condition is
     * NOT "Baik", so asking for it up front puts an empty box in front of the operator on the
     * common path - thirty garments in a row that are simply fine.
     *
     * THE SERVER REFUSES A MISSING NOTE ANYWAY. Asking here saves a round trip for somebody
     * holding a torn shirt, who should not have to wonder why the app went quiet.
     */
    private fun askCondition(onPick: (condition: String, note: String?) -> Unit) {
        val codes = listOf("good", "needs_repair", "damaged", "lost")

        val options = listOf(
            R.string.laundry_condition_good,
            R.string.laundry_condition_needs_repair,
            R.string.laundry_condition_damaged,
            R.string.laundry_condition_lost
        ).map { DialogOption(getString(it)) }

        OptionListDialog(this, getString(R.string.laundry_condition_title), options).also { dialog ->
            dialog.setOptionListDialogListener { index ->
                val code = codes[index]

                if (code == "good") {
                    onPick(code, null)
                    return@setOptionListDialogListener
                }

                askNote(options[index].label) { note -> onPick(code, note) }
            }
        }.show()
    }

    /** Why this garment is not simply fine. Compulsory, which is why the field cannot be empty. */
    private fun askNote(condition: String, onNote: (String) -> Unit) {
        InputDialog(
            this,
            condition,
            getString(R.string.laundry_note_hint),
            getString(R.string.ok),
            getString(R.string.laundry_note_required)
        ).also { dialog ->
            dialog.setInputDialogListener { value -> onNote(value) }
        }.show()
    }

    /**
     * Who signs for the batch.
     *
     * PICKED, NEVER ASSUMED. The operator running this screen is not the person taking the
     * garments away, and the record has to say so [GAP-05].
     */
    private fun askCollector() {
        viewModel.collectors.observe(this) { people ->
            if (people.isEmpty()) return@observe
            val options = people.map { person ->
                DialogOption(
                    person.fullName ?: person.usern ?: "-",
                    listOfNotNull(person.namaDept, person.subDept).joinToString(" · ")
                        .ifBlank { null }
                )
            }

            OptionListDialog(this, getString(R.string.laundry_pick_collector), options)
                .also { dialog ->
                    dialog.setOptionListDialogListener { index -> pickCollector(people[index]) }
                }
                .show()
        }
    }

    private fun pickCollector(person: LaundryCollector) {
        viewModel.handOverTo(idTrx, person.idUser, selected.toList())
    }

    /**
     * Why this slip is being printed again.
     *
     * THE REASON IS THE POINT, NOT A FORMALITY [3e]. Two identical slips in circulation cannot be
     * told apart by counter staff, so the second one carries a CETAK ULANG banner naming who
     * reprinted it and why — and the server refuses to produce one without that sentence.
     *
     * The server's own refusal is shown as the dialog's message rather than restated here: it
     * already says what happened, in the caller's language.
     */
    private fun askReprintReason(message: String) {
        InputDialog(
            this,
            getString(R.string.laundry_reprint_title),
            getString(R.string.laundry_reprint_reason),
            getString(R.string.laundry_print_slip),
            message
        ).also { dialog ->
            dialog.setInputDialogListener { value -> viewModel.loadSlip(idTrx, reason = value) }
        }.show()
    }

    /**
     * Which of the paired printers this slip goes to.
     *
     * PAIRED ONLY. Discovery needs location permission on every version we ship to, and pairing a
     * printer is a thing somebody does once in the system settings.
     */
    private fun askPrinter(rows: List<LaundrySlipRow>) {
        val printers = ThermalPrinter.paired(this)

        if (printers.isEmpty()) {
            toast(getString(R.string.laundry_print_none), warning = true)
            viewModel.clearSlip()
            return
        }

        var picked = false

        OptionListDialog(
            this,
            getString(R.string.laundry_print_pick),
            printers.map { DialogOption(it.name, it.address) }
        ).also { dialog ->
            dialog.setOptionListDialogListener { index ->
                picked = true

                lifecycleScope.launch {
                    val failure = ThermalPrinter.print(
                        this@LaundryProcessActivity,
                        printers[index].address,
                        rows
                    )

                    toast(failure ?: getString(R.string.laundry_print_done), failure != null)
                    // Kept on a failure so a retry prints the SAME lines without asking the server
                    // again - a second fetch would be logged as an unexplained reprint.
                    if (failure == null) viewModel.clearSlip()
                }
            }

            // Backing out holds no slip. The lines were fetched - and therefore logged as a print
            // - but nothing reached paper, so keeping them would offer a stale reprint later.
            dialog.setOnDismissListener { if (!picked) viewModel.clearSlip() }
        }.show()
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

    companion object {
        const val EXTRA_ID_TRX = "extra.id.trx"
    }
}
