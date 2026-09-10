package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.LaundryBundleAdapter
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityLaundryCheckInBinding
import com.erela.fixme.dialogs.ConfirmationDialog
import com.erela.fixme.dialogs.LoadingDialog
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.enableEdgeToEdgeOpaqueNav
import com.erela.fixme.viewmodel.LaundryCheckInViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Serah Terima Laundry — a courier hands a bundle of uniforms in at the counter.
 *
 * THIS SCREEN EXISTS BECAUSE THE PHONE HAS A CAMERA. No HID scanner is being procured, and the web
 * host is HTTP by standing decision, so `getUserMedia` has no secure context and a browser cannot
 * scan [D-14, TG-02]. The web counter screen is the typed path; this one is the scanned one, and
 * they post to the same rules on the server.
 *
 * TWO SCANNERS, ONE LAUNCHER EACH, because the two scans mean different things and the code cannot
 * tell them apart from the payload alone: a counter sticker holds `LDY-COUNTER-GA01` and a patch
 * holds six digits. Routing both through one callback would mean guessing from the string shape,
 * and a mis-guess would file a bundle against a garment code.
 *
 * THE COUNTER COMES FIRST AND EVERYTHING ELSE WAITS FOR IT. A bundle scanned against no counter
 * has nowhere to be filed, and learning that at submit — after thirty scans — is the failure this
 * ordering prevents. The counter response also answers whether this courier's department may hand
 * laundry in at all, which is the other thing far better learned before scanning than after.
 *
 * THE BUNDLE LIVES IN THE VIEWMODEL, so a screen rotation does not cost a courier their scans.
 */
class LaundryCheckInActivity : AppCompatActivity() {
    private val binding: ActivityLaundryCheckInBinding by lazy {
        ActivityLaundryCheckInBinding.inflate(layoutInflater)
    }

    private val viewModel: LaundryCheckInViewModel by viewModels()
    private val loadingDialog: LoadingDialog by lazy { LoadingDialog(this) }
    private lateinit var bundleAdapter: LaundryBundleAdapter

    /** The counter sticker: says WHERE, never WHO. */
    private val counterLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.scanCounter(it) }
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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        setupObservers()
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
            toolBar.setNavigationOnClickListener { finish() }

            bundleAdapter = LaundryBundleAdapter(
                context = this@LaundryCheckInActivity,
                // The courier's own department, for the mixed-owner warning. Compared by name
                // because that is all the phone has; the server compares ids [D-17].
                ownDept = UserDataHelper(this@LaundryCheckInActivity).getUserData().dept,
                onRemove = { garment -> viewModel.remove(garment.qrCode) },
                onNoteChanged = { garment, note -> viewModel.setNote(garment.qrCode, note) }
            )

            rvBundle.apply {
                layoutManager = LinearLayoutManager(this@LaundryCheckInActivity)
                adapter = bundleAdapter
            }

            scanCounterButton.setOnClickListener {
                counterLauncher.launch(scanOptions(getString(R.string.laundry_scan_counter_prompt)))
            }

            fabScanPatch.setOnClickListener {
                // Refused here rather than answered by the server, because the courier needs the
                // instruction ("scan the counter first"), not a rejection.
                if (viewModel.counterCode.isNullOrBlank()) {
                    CustomToast.getInstance(this@LaundryCheckInActivity)
                        .setMessage(getString(R.string.laundry_counter_required))
                        .setFontColor(getColor(R.color.custom_toast_font_warning))
                        .setBackgroundColor(getColor(R.color.custom_toast_background_warning))
                        .show()
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
                    this@LaundryCheckInActivity,
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
                viewModel.submit(etBatchNote.text?.toString()?.trim()?.ifBlank { null })
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        viewModel.apply {
            binding.apply {
                items.observe(this@LaundryCheckInActivity) { list ->
                    bundleAdapter.submitList(list)

                    tvBundleCount.text =
                        getString(R.string.laundry_bundle_count, list.size)

                    emptyBundleContainer.visibility =
                        if (list.isEmpty()) View.VISIBLE else View.GONE

                    clearButton.isEnabled = list.isNotEmpty()

                    // Submit needs BOTH a counter and something to hand over. Either alone is a
                    // request the server would refuse.
                    submitButton.isEnabled =
                        list.isNotEmpty() && !viewModel.counterCode.isNullOrBlank()
                }

                counterResult.observe(this@LaundryCheckInActivity) { response ->
                    if (!response.isSuccess) {
                        toast(response.message, warning = true)
                        return@observe
                    }

                    tvCounter.text = viewModel.counterName
                        ?: getString(R.string.laundry_counter_not_scanned)

                    // A real counter, but a department that may not hand in: success and refusal
                    // at the same time, which is why the two are read separately.
                    tvNotRegistered.visibility =
                        if (viewModel.mayCheckIn) View.GONE else View.VISIBLE

                    if (!viewModel.mayCheckIn) {
                        tvNotRegistered.text = response.message
                    }

                    submitButton.isEnabled =
                        viewModel.count() > 0 && viewModel.mayCheckIn

                    fabScanPatch.isEnabled = viewModel.mayCheckIn
                }

                scanResult.observe(this@LaundryCheckInActivity) { response ->
                    // A refusal is the useful case: the server's sentence says what to do about
                    // it — register the garment, or finish the transaction it is already on.
                    if (!response.isSuccess) {
                        toast(response.message, warning = true)
                    }
                }

                checkInResult.observe(this@LaundryCheckInActivity) { response ->
                    if (!response.isSuccess) {
                        toast(response.message, warning = true)
                        return@observe
                    }

                    // The server's message already names the transaction, the count, any garments
                    // belonging to other departments, and any held items that joined the bundle.
                    // Repeating it here in our own words would be a second version to keep in step.
                    toast(response.message, warning = false)

                    etBatchNote.setText("")
                    finish()
                }

                isLoading.observe(this@LaundryCheckInActivity) { loading ->
                    scanCounterButton.isEnabled = !loading
                }

                isSubmitting.observe(this@LaundryCheckInActivity) { submitting ->
                    if (submitting) {
                        loadingDialog.show()
                    } else {
                        loadingDialog.dismiss()
                    }

                    submitButton.isEnabled = !submitting &&
                            viewModel.count() > 0 && !viewModel.counterCode.isNullOrBlank()
                }

                error.observe(this@LaundryCheckInActivity) { message ->
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
