package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.erela.fixme.databinding.BsSubmissionActionBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData
import com.google.android.material.bottomsheet.BottomSheetDialog

class SubmissionActionBottomSheet(context: Context, val data: SubmissionDetailResponse) :
    BottomSheetDialog(context), UpdateStatusBottomSheet.OnUpdateSuccessListener {
    private lateinit var binding: BsSubmissionActionBinding
    private lateinit var onUpdateSuccessListener: OnUpdateSuccessListener
    private lateinit var userData: UserData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BsSubmissionActionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        userData = UserDataHelper(context).getUserData()

        init()
    }

    private fun init() {
        binding.apply {
            when (data.stsGaprojects) {
                // Pending
                1.toString() -> {
                    startProgressButton.visibility = View.GONE
                    onTrialDoneButtonContainer.visibility = View.GONE
                    approveRejectButtonContainer.visibility = View.VISIBLE
                    approveButton.setOnClickListener {
                        val bottomSheet =
                            UpdateStatusBottomSheet(context, data, true).also { bottomSheet ->
                                with(bottomSheet) {
                                    setOnUpdateSuccessListener(this@SubmissionActionBottomSheet)
                                }
                            }
                        if (bottomSheet.window != null)
                            bottomSheet.show()
                    }
                    rejectButton.setOnClickListener {
                        val bottomSheet =
                            UpdateStatusBottomSheet(context, data, false).also { bottomSheet ->
                                with(bottomSheet) {
                                    setOnUpdateSuccessListener(this@SubmissionActionBottomSheet)
                                }
                            }
                        if (bottomSheet.window != null)
                            bottomSheet.show()
                    }
                }
                // Approved
                2.toString() -> {
                    startProgressButton.visibility = View.VISIBLE
                    onTrialDoneButtonContainer.visibility = View.GONE
                    approveRejectButtonContainer.visibility = View.GONE
                }
            }
        }
    }

    fun onUpdateSuccessListener(onUpdateSuccessListener: OnUpdateSuccessListener) {
        this.onUpdateSuccessListener = onUpdateSuccessListener
    }

    override fun onUpdateSuccess() {
        onUpdateSuccessListener.onUpdateSuccess()
        dismiss()
    }

    interface OnUpdateSuccessListener {
        fun onUpdateSuccess()
    }
}