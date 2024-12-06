package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.adapters.recycler_view.TrialRvAdapter
import com.erela.fixme.databinding.BsTrialBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData
import com.google.android.material.bottomsheet.BottomSheetDialog

class TrialTrackingBottomSheet(
    context: Context, val data: SubmissionDetailResponse
) : BottomSheetDialog(context) {
    private val binding: BsTrialBinding by lazy {
        BsTrialBinding.inflate(layoutInflater)
    }
    private lateinit var trialAdapter: TrialRvAdapter
    private lateinit var onTrialTrackingListener: OnTrialTrackingListener

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
            trialAdapter = TrialRvAdapter(context, data, data.trial)
            rvTrial.adapter = trialAdapter
            rvTrial.layoutManager = LinearLayoutManager(context)
            if (data.stsGaprojects == 4) {
                trialActionButton.visibility = View.GONE
            }
            trialActionButton.setOnClickListener {
                onTrialTrackingListener.reportTrialClicked()
            }
        }
    }

    fun setOnTrialTrackingListener(onTrialTrackingListener: OnTrialTrackingListener) {
        this.onTrialTrackingListener = onTrialTrackingListener
    }

    interface OnTrialTrackingListener {
        fun reportTrialClicked()
    }
}