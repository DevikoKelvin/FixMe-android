package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.erela.fixme.databinding.BsSubmissionActionBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData
import com.google.android.material.bottomsheet.BottomSheetDialog

class SubmissionActionBottomSheet(context: Context, val data: SubmissionDetailResponse) :
    BottomSheetDialog(context), UpdateStatusBottomSheet.OnUpdateSuccessListener {
    private val binding: BsSubmissionActionBinding by lazy {
        BsSubmissionActionBinding.inflate(layoutInflater)
    }
    private lateinit var onUpdateSuccessListener: OnUpdateSuccessListener
    private val userData: UserData by lazy {
        UserDataHelper(context).getUserData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

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
                            onTrialDoneButtonContainer.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    fun onUpdateSuccessListener(onUpdateSuccessListener: OnUpdateSuccessListener) {
        this.onUpdateSuccessListener = onUpdateSuccessListener
    }

    interface OnUpdateSuccessListener {
        fun onUpdateSuccess()
    }

    override fun onApproved() {
        onUpdateSuccessListener.onUpdateSuccess()
        dismiss()
    }

    override fun onRejected() {
        onUpdateSuccessListener.onUpdateSuccess()
        dismiss()
    }

    override fun onCanceled() {
        onUpdateSuccessListener.onUpdateSuccess()
        dismiss()
    }

    override fun onTechniciansDeployed() {
        onUpdateSuccessListener.onUpdateSuccess()
        dismiss()
    }
}