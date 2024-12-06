package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import com.erela.fixme.databinding.BsReportTrialBinding
import com.erela.fixme.objects.SubmissionDetailResponse
import com.google.android.material.bottomsheet.BottomSheetDialog

class ReportTrialBottomSheet(context: Context, private val detail: SubmissionDetailResponse) :
    BottomSheetDialog(context) {
    private val binding: BsReportTrialBinding by lazy {
        BsReportTrialBinding.inflate(layoutInflater)
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

        }
    }
}