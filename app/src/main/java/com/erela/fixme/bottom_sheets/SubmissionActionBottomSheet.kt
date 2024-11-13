package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.erela.fixme.databinding.BsSubmissionActionBinding
import com.erela.fixme.objects.SubmissionDetailResponse
import com.google.android.material.bottomsheet.BottomSheetDialog

class SubmissionActionBottomSheet(context: Context, val data: SubmissionDetailResponse) :
    BottomSheetDialog(context) {
    private lateinit var binding: BsSubmissionActionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BsSubmissionActionBinding.inflate(layoutInflater)
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
                1.toString() -> {
                    onTrialDoneButtonContainer.visibility = View.GONE
                    approveRejectButtonContainer.visibility = View.VISIBLE
                }
                // Approved
                2.toString() -> {
                    onTrialDoneButtonContainer.visibility = View.VISIBLE
                    approveRejectButtonContainer.visibility = View.GONE
                }
            }
        }
    }
}