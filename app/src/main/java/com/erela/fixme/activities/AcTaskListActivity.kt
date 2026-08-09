package com.erela.fixme.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.erela.fixme.helpers.enableEdgeToEdgeOpaqueNav
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.AcTaskAdapter
import com.erela.fixme.bottom_sheets.AcCheckInBottomSheet
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityAcTaskListBinding
import com.erela.fixme.dialogs.LoadingDialog
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.viewmodel.AcMaintenanceViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class AcTaskListActivity : AppCompatActivity(), AcCheckInBottomSheet.OnCheckInListener {
    private val binding: ActivityAcTaskListBinding by lazy {
        ActivityAcTaskListBinding.inflate(
            layoutInflater
        )
    }
    private val viewModel: AcMaintenanceViewModel by viewModels()
    private lateinit var taskAdapter: AcTaskAdapter
    private val loadingDialog: LoadingDialog by lazy { LoadingDialog(this) }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val userId = UserDataHelper(this).getUserData().id
            viewModel.onQrScanned(result.contents, userId)
        }
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

        val userId = UserDataHelper(this).getUserData().id
        viewModel.getTaskList(userId)
    }

    private fun setupUI() {
        binding.apply {
            toolBar.setNavigationOnClickListener { finish() }

            taskAdapter = AcTaskAdapter(this@AcTaskListActivity) { task ->
                if (task.itemStatus == "in_progress" && task.logId != null) {
                    // Assist tech joining an already checked-in session
                    navigateToSession(logId = task.logId, itemId = null)
                } else {
                    showCheckInBottomSheet(
                        task.itemId,
                        task.acCode,
                        task.location,
                        task.detail,
                        task.area,
                        task.floor
                    )
                }
            }

            rvTasks.apply {
                layoutManager = LinearLayoutManager(this@AcTaskListActivity)
                adapter = taskAdapter
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        val firstVisible = (recyclerView.layoutManager as LinearLayoutManager)
                            .findFirstVisibleItemPosition()
                        scrollToTopButton.fade(show = firstVisible > 0)
                    }
                })
            }

            scrollToTopButton.setOnClickListener { rvTasks.smoothScrollToPosition(0) }

            swipeRefresh.setOnRefreshListener {
                taskAdapter.submitList(emptyList())
                // Clearing the list won't fire onScrolled, so the button would stay up
                // over a list that is back at the top.
                scrollToTopButton.fade(show = false)
                val userId = UserDataHelper(this@AcTaskListActivity).getUserData().id
                viewModel.getTaskList(userId)
                swipeRefresh.isRefreshing = false
            }

            fabScan.setOnClickListener {
                val options = ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt("Scan AC QR Code")
                    setCameraId(0)
                    setBeepEnabled(true)
                    setBarcodeImageEnabled(false)
                    setOrientationLocked(true)
                }
                barcodeLauncher.launch(options)
            }
        }
    }

    private fun setupObservers() {
        viewModel.apply {
            binding.apply {
                isLoading.observe(this@AcTaskListActivity) { isLoading ->
                    if (isLoading) {
                        shimmerLayout.visibility = View.VISIBLE
                        shimmerLayout.startShimmer()
                        rvTasks.visibility = View.GONE
                        emptyListContainer.visibility = View.GONE
                    } else {
                        shimmerLayout.visibility = View.GONE
                        shimmerLayout.stopShimmer()
                    }
                }

                taskListResult.observe(this@AcTaskListActivity) { response ->
                    if (response.isSuccess && !response.data.isNullOrEmpty()) {
                        rvTasks.visibility = View.VISIBLE
                        emptyListContainer.visibility = View.GONE
                        emptyListAnimation.pauseAnimation()
                        taskAdapter.submitList(response.data)
                    } else {
                        rvTasks.visibility = View.GONE
                        emptyListContainer.visibility = View.VISIBLE
                        emptyListAnimation.playAnimation()
                    }
                }

                scanResult.observe(this@AcTaskListActivity) { response ->
                    if (response.isSuccess) {
                        val item = response.item ?: return@observe
                        when (item.itemStatus) {
                            "in_progress" if item.logId != null -> {
                                // Session already active — join it directly
                                navigateToSession(logId = item.logId, itemId = null)
                            }

                            "in_progress" -> {
                                // Fallback: let check-in handle the already-in-progress case
                                navigateToSession(logId = null, itemId = item.itemId)
                            }

                            else -> {
                                showCheckInBottomSheet(
                                    itemId = item.itemId,
                                    acCode = response.unit?.acCode,
                                    location = response.unit?.location,
                                    detail = response.unit?.detail,
                                    area = response.unit?.area,
                                    floor = response.unit?.floor
                                )
                            }
                        }
                    } else {
                        CustomToast.getInstance(this@AcTaskListActivity)
                            .setMessage(response.message)
                            .setBackgroundColor(
                                ResourcesCompat.getColor(
                                    resources, R.color.custom_toast_background_failed, theme
                                )
                            )
                            .setFontColor(
                                ResourcesCompat.getColor(
                                    resources, R.color.custom_toast_font_failed, theme
                                )
                            )
                            .show()
                    }
                }

                checkInResult.observe(this@AcTaskListActivity) { response ->
                    loadingDialog.dismiss()
                    if (response.isSuccess || response.isAlreadyIn) {
                        navigateToSession(logId = response.logId, itemId = null)
                    } else {
                        CustomToast.getInstance(this@AcTaskListActivity)
                            .setMessage(response.message)
                            .setBackgroundColor(
                                ResourcesCompat.getColor(
                                    resources, R.color.custom_toast_background_failed, theme
                                )
                            )
                            .setFontColor(
                                ResourcesCompat.getColor(
                                    resources, R.color.custom_toast_font_failed, theme
                                )
                            )
                            .show()
                    }
                }

                error.observe(this@AcTaskListActivity) { errorMsg ->
                    loadingDialog.dismiss()
                    swipeRefresh.isRefreshing = false
                    CustomToast.getInstance(this@AcTaskListActivity)
                        .setMessage(errorMsg)
                        .setBackgroundColor(
                            ResourcesCompat.getColor(
                                resources, R.color.custom_toast_background_failed, theme
                            )
                        )
                        .setFontColor(
                            ResourcesCompat.getColor(
                                resources, R.color.custom_toast_font_failed, theme
                            )
                        )
                        .show()
                }
            }
        }
    }

    // ── AcCheckInBottomSheet.OnCheckInListener ───────────────────────────────
    override fun onCheckInRequested(itemId: Int) {
        val userId = UserDataHelper(this).getUserData().id
        loadingDialog.show()
        viewModel.checkIn(itemId, userId, null, null)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private fun showCheckInBottomSheet(
        itemId: Int,
        acCode: String?,
        location: String?,
        detail: String?,
        area: String?,
        floor: String?
    ) {
        AcCheckInBottomSheet(this, itemId, acCode, location, detail, area, floor)
            .apply { setOnCheckInListener(this@AcTaskListActivity) }
            .show()
    }

    private fun navigateToSession(logId: Int?, itemId: Int?) {
        val intent = Intent(this, AcSessionActivity::class.java).apply {
            logId?.let { putExtra("LOG_ID", it) }
            itemId?.let { putExtra("ITEM_ID", it) }
        }
        startActivity(intent)
    }

    /**
     * Same 300ms fade as the scroll-to-top button on the submission list. Kept short by
     * letting withEndAction carry the final visibility instead of tracking an
     * "is animating" flag: animate().cancel() drops the pending end action, so a fade
     * reversed mid-flight can't leave the button stuck in the wrong state.
     */
    private fun View.fade(show: Boolean) {
        if (show == isVisible) return
        animate().cancel()
        if (show) {
            alpha = 0f
            isVisible = true
        }
        animate()
            .alpha(if (show) 1f else 0f)
            .setDuration(300)
            .withEndAction { isVisible = show }
            .start()
    }
}
