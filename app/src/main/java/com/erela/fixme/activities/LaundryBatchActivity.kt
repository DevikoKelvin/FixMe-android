package com.erela.fixme.activities

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.LaundryBatchLineAdapter
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityLaundryBatchBinding
import com.erela.fixme.helpers.enableEdgeToEdgeOpaqueNav
import com.erela.fixme.viewmodel.LaundryCheckInViewModel
import com.google.android.material.card.MaterialCardView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * One batch, as the COURIER sees it — and the button that collects it.
 *
 * THE COLLECT BUTTON IS GATED ON `ready_count`, NOT ON THE HEADER STATUS. A batch reads `ready`
 * until its last line moves, so a colleague from the same department can have taken everything
 * while the header still says collectable [T-08]. The server counts the lines; this trusts the
 * count rather than re-deriving it from a word.
 *
 * COLLECTING IS A SCAN. The counter QR proves the courier is standing at the counter; their token
 * proves who they are. That split is the same one check-in rests on, and it is why a sticker
 * anybody can photograph authorises nothing on its own.
 *
 * NO GARMENT PICKING. The server derives what this department may take, so there is nothing to
 * tick and nothing the phone could get wrong. A partial pickup still happens [T-07]; it is decided
 * by what is `ready` rather than by anything chosen here.
 */
class LaundryBatchActivity : AppCompatActivity() {
    private val binding: ActivityLaundryBatchBinding by lazy {
        ActivityLaundryBatchBinding.inflate(layoutInflater)
    }
    private val viewModel: LaundryCheckInViewModel by viewModels()
    private lateinit var lineAdapter: LaundryBatchLineAdapter
    private val idTrx: Int by lazy { intent.getIntExtra(EXTRA_ID_TRX, 0) }
    private fun MaterialCardView.enable(on: Boolean) {
        isClickable = on
        isFocusable = on
        alpha = if (on) 1f else 0.4f
    }

    /** The counter sticker: says WHERE, never WHO — the collector comes from the token. */
    private val counterLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.collect(it, idTrx) }
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

            swipeRefreshLayout.setOnRefreshListener {
                viewModel.loadBatch(idTrx)
            }

            // SwipeRefreshLayout ASKS ITS DIRECT CHILD whether the content can scroll up, and its
            // direct child here is a ConstraintLayout - which never can. So every downward drag
            // anywhere on the screen was read as a pull-to-refresh, including one meant to scroll
            // back up a long list of garments. The RecyclerView is the thing that actually
            // scrolls, so it is the thing to ask.
            swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
                rvItems.canScrollVertically(-1)
            }

            lineAdapter = LaundryBatchLineAdapter(this@LaundryBatchActivity)

            rvItems.apply {
                layoutManager = LinearLayoutManager(this@LaundryBatchActivity)
                adapter = lineAdapter
            }

            collectButton.setOnClickListener {
                counterLauncher.launch(
                    ScanOptions().apply {
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setPrompt(getString(R.string.laundry_collect_prompt))
                        setCameraId(0)
                        setBeepEnabled(true)
                        setBarcodeImageEnabled(false)
                        setOrientationLocked(true)
                    }
                )
            }
        }

        setupObservers()
        viewModel.loadBatch(idTrx)
    }

    private fun setupObservers() {
        viewModel.apply {
            binding.apply {
                batchDetail.observe(this@LaundryBatchActivity) { response ->
                    val data = response.data

                    if (!response.isSuccess || data == null) {
                        toast(response.message, warning = true)
                        return@observe
                    }
                    val header = data.transaction

                    tvTrxNo.text = header.trxNo
                    // `ready` MEANS THE WASH IS DONE, NOT THAT ANYBODY HAS COUNTED THE SHELF
                    // [GA, 15 Sep 2026]. The counter checks the bundle back out before a courier
                    // may take it, so a batch that is ready but unstaged gets its own sentence
                    // rather than an invitation to walk over and be refused.
                    tvStatus.text = getString(
                        when (header.status) {
                            "ready" if header.handoverMarkedAt == null ->
                                R.string.laundry_status_awaiting_counter

                            "waiting" -> R.string.laundry_status_waiting
                            "accepted" -> R.string.laundry_status_accepted
                            "ready" -> R.string.laundry_status_ready
                            else -> R.string.laundry_status_completed
                        }
                    )
                    // A CELL EACH, and a dash where a stamp has not happened yet. On one line
                    // an absent stamp was one fewer word and nothing said which - so a batch that
                    // had never started washing looked exactly like one whose start was simply
                    // not interesting enough to print.
                    //
                    // The labels are resources now rather than Indonesian baked into Kotlin,
                    // which is what the rest of this screen already does.
                    tvCheckedIn.text = header.checkedInAt ?: "—"
                    tvWashStarted.text = header.washingStartedAt ?: "—"
                    tvReady.text = header.readyAt ?: "—"
                    tvCompleted.text = header.completedAt ?: "—"

                    lineAdapter.submitList(data.items)

                    rvItems.visibility = if (data.items.isEmpty()) View.GONE else View.VISIBLE
                    tvEmpty.visibility = if (data.items.isEmpty()) View.VISIBLE else View.GONE
                    // `ready_count`, not the status word. See the class docblock.
                    collectButton.visibility =
                        if (data.readyCount > 0) View.VISIBLE else View.GONE
                }

                collectResult.observe(this@LaundryBatchActivity) { response ->
                    // The server's message already names the count, what is left and anything held
                    // back for another department. A second wording here would be one more thing
                    // to keep true.
                    toast(response.message, warning = !response.isSuccess)
                }

                // NOTHING WAS STOPPING THE SPINNER. The refresh listener started a load and
                // the activity observed `batchDetail`, `collectResult`, `isSubmitting` and
                // `error` - but not `isLoading`, so the wheel turned until the screen was closed.
                //
                // Bound to the flag rather than cleared in the success handler, because that
                // handler returns early on a refusal: a failed refresh is exactly when a stuck
                // spinner is most misleading.
                isLoading.observe(this@LaundryBatchActivity) { loading ->
                    swipeRefreshLayout.isRefreshing = loading
                }

                isSubmitting.observe(this@LaundryBatchActivity) { submitting ->
                    collectLoadingBar.visibility = if (submitting) View.VISIBLE else View.GONE
                    collectText.visibility = if (submitting) View.INVISIBLE else View.VISIBLE
                    collectButton.enable(!submitting)
                }

                error.observe(this@LaundryBatchActivity) { message ->
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

    companion object {
        const val EXTRA_ID_TRX = "extra.id.trx"
    }
}
