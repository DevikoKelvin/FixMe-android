package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.erela.fixme.R
import com.erela.fixme.databinding.BsSubmissionActionBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignTop

class SubmissionActionBottomSheet(context: Context, val data: SubmissionDetailResponse) :
    BottomSheetDialog(context), UpdateStatusBottomSheet.OnUpdateSuccessListener {
    private val binding: BsSubmissionActionBinding by lazy {
        BsSubmissionActionBinding.inflate(layoutInflater)
    }
    private lateinit var onButtonActionClickedListener: OnButtonActionClickedListener
    private val userData: UserData by lazy {
        UserDataHelper(context).getUserData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    @SuppressLint("SetTextI18n")
    private fun init() {
        binding.apply {
            when (data.stsGaprojects) {
                // Pending
                1 -> {
                    startProgressButton.visibility = View.GONE
                    onTrialDoneButtonContainer.visibility = View.GONE
                    actionsButtonContainer.visibility = View.VISIBLE
                    approveButton.visibility = View.VISIBLE
                    rejectButton.visibility = View.VISIBLE
                    deployTechButton.visibility = View.GONE
                    approveButton.setOnClickListener {
                        val bottomSheet =
                            UpdateStatusBottomSheet(
                                context, data, approve = true, cancel = false, deployTech = false
                            ).also { bottomSheet ->
                                with(bottomSheet) {
                                    setOnUpdateSuccessListener(this@SubmissionActionBottomSheet)
                                }
                            }
                        if (bottomSheet.window != null)
                            bottomSheet.show()
                    }
                    rejectButton.setOnClickListener {
                        val bottomSheet =
                            UpdateStatusBottomSheet(
                                context, data, approve = false, cancel = false, deployTech = false
                            ).also { bottomSheet ->
                                with(bottomSheet) {
                                    setOnUpdateSuccessListener(this@SubmissionActionBottomSheet)
                                }
                            }
                        if (bottomSheet.window != null)
                            bottomSheet.show()
                    }
                }
                // Waiting
                11 -> {
                    startProgressButton.visibility = View.GONE
                    onTrialDoneButtonContainer.visibility = View.GONE
                    actionsButtonContainer.visibility = View.VISIBLE
                    approveButton.visibility = View.VISIBLE
                    rejectButton.visibility = View.VISIBLE
                    deployTechButton.visibility = View.GONE
                    reportManagerApprovalMessageContainer.visibility = View.VISIBLE
                    reportManagerApprovalMessage.text =
                        if (context.getString(R.string.lang) == "in")
                            "Laporan telah disetujui oleh ${data.namaUserPelaporApprove}\nKetuk untuk melihat pesan"
                        else
                            "Report was approved by ${data.namaUserPelaporApprove}\nTap to see message"
                    reportManagerApprovalMessageContainer.setOnClickListener {
                        val balloon = Balloon.Builder(context).also {
                            with(it) {
                                setHeight(BalloonSizeSpec.WRAP)
                                setText(data.keteranganPelaporApprove.toString())
                                setTextColorResource(R.color.custom_toast_font_success)
                                setTextSize(14f)
                                setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                                setArrowSize(10)
                                setPadding(12)
                                setCornerRadius(8f)
                                setBackgroundColorResource(R.color.custom_toast_background_success)
                                setBalloonAnimation(BalloonAnimation.FADE)
                                setLifecycleOwner(lifecycleOwner)
                            }
                        }.build()
                        reportManagerApprovalMessageContainer.showAlignTop(balloon, 0, 0)
                    }
                    approveButton.setOnClickListener {
                        val bottomSheet =
                            UpdateStatusBottomSheet(
                                context, data, approve = true, cancel = false, deployTech = false
                            ).also { bottomSheet ->
                                with(bottomSheet) {
                                    setOnUpdateSuccessListener(this@SubmissionActionBottomSheet)
                                }
                            }
                        if (bottomSheet.window != null)
                            bottomSheet.show()
                    }
                    rejectButton.setOnClickListener {
                        val bottomSheet =
                            UpdateStatusBottomSheet(
                                context, data, approve = false, cancel = false, deployTech = false
                            ).also { bottomSheet ->
                                with(bottomSheet) {
                                    setOnUpdateSuccessListener(this@SubmissionActionBottomSheet)
                                }
                            }
                        if (bottomSheet.window != null)
                            bottomSheet.show()
                    }
                }
                // Approved
                2 -> {
                    for (i in 0 until data.usernUserSpv!!.size) {
                        if (data.usernUserSpv[i]?.idUser == userData.id) {
                            actionsButtonContainer.visibility = View.VISIBLE
                            startProgressButton.visibility = View.GONE
                            approveButton.visibility = View.GONE
                            rejectButton.visibility = View.GONE
                            deployTechButton.visibility = View.VISIBLE
                            deployTechButton.setOnClickListener {
                                val bottomSheet =
                                    UpdateStatusBottomSheet(
                                        context,
                                        data,
                                        approve = false,
                                        cancel = false,
                                        deployTech = true
                                    ).also { bottomSheet ->
                                        with(bottomSheet) {
                                            setOnUpdateSuccessListener(this@SubmissionActionBottomSheet)
                                        }
                                    }
                                if (bottomSheet.window != null)
                                    bottomSheet.show()
                            }
                            break
                        } else {
                            actionsButtonContainer.visibility = View.GONE
                            startProgressButton.visibility = View.VISIBLE
                            startProgressButton.setOnClickListener {
                                onButtonActionClickedListener.onStartProgressNowClicked(this@SubmissionActionBottomSheet)
                            }
                            onTrialDoneButtonContainer.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    fun onUpdateSuccessListener(onButtonActionClickedListener: OnButtonActionClickedListener) {
        this.onButtonActionClickedListener = onButtonActionClickedListener
    }

    interface OnButtonActionClickedListener {
        fun onUpdateSuccess()
        fun onStartProgressNowClicked(bottomSheet: SubmissionActionBottomSheet)
    }

    override fun onApproved() {
        onButtonActionClickedListener.onUpdateSuccess()
        dismiss()
    }

    override fun onRejected() {
        onButtonActionClickedListener.onUpdateSuccess()
        dismiss()
    }

    override fun onCanceled() {
        onButtonActionClickedListener.onUpdateSuccess()
        dismiss()
    }

    override fun onTechniciansDeployed() {
        onButtonActionClickedListener.onUpdateSuccess()
        dismiss()
    }
}