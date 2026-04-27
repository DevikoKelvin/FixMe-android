package com.erela.fixme.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
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
    private lateinit var binding: ActivityAcTaskListBinding
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
        binding = ActivityAcTaskListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
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
                showCheckInBottomSheet(task.itemId, task.acCode)
            }

            rvTasks.apply {
                layoutManager = LinearLayoutManager(this@AcTaskListActivity)
                adapter = taskAdapter
            }

            swipeRefresh.setOnRefreshListener {
                taskAdapter.submitList(emptyList())
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
                        if (response.item?.itemStatus == "in_progress") {
                            // Already claimed — go straight to session
                            navigateToSession(logId = null, itemId = response.item.itemId)
                        } else {
                            showCheckInBottomSheet(
                                itemId = response.item?.itemId ?: return@observe,
                                acCode = response.unit?.acCode
                            )
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
    private fun showCheckInBottomSheet(itemId: Int, acCode: String?) {
        AcCheckInBottomSheet(this, itemId, acCode)
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
}
