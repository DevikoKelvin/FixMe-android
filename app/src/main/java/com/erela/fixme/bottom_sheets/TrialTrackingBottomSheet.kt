package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
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
    private val userData: UserData by lazy {
        UserDataHelper(context).getUserData()
    }
    private lateinit var trialAdapter: TrialRvAdapter
    private lateinit var onTrialTrackingListener: OnTrialTrackingListener
    private var readyDone: Boolean = false

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
            trialAdapter = TrialRvAdapter(context, data, data.trial)
            rvTrial.adapter = trialAdapter
            rvTrial.layoutManager = LinearLayoutManager(context)
            if (data.stsGaprojects == 3 || data.stsGaprojects == 4 || data.stsGaprojects == 30) {
                trialActionButton.visibility = View.GONE
            } else {
                if (data.idUser == userData.id) {
                    trialActionButton.visibility = View.VISIBLE
                    for (element in data.trial!!) {
                        if (element?.status == 0) {
                            readyDone = true
                            break
                        }
                    }
                    if (readyDone) {
                        trialActionButton.setCardBackgroundColor(
                            ContextCompat.getColor(
                                context, R.color.custom_toast_background_soft_blue
                            )
                        )
                        trialActionText.text = if (context.getString(R.string.lang) == "in")
                            "Tandai Selesai Sekarang!"
                        else
                            "Mark as Done now!"
                        trialActionText.setTextColor(
                            ContextCompat.getColor(
                                context, R.color.custom_toast_font_blue
                            )
                        )
                        trialActionButton.setOnClickListener {
                            onTrialTrackingListener.markIssueDoneClicked(this@TrialTrackingBottomSheet)
                        }
                    } else {
                        trialActionButton.setCardBackgroundColor(
                            ContextCompat.getColor(
                                context, R.color.status_on_trial
                            )
                        )
                        trialActionText.text = if (context.getString(R.string.lang) == "in")
                            "Laporkan Uji Coba"
                        else
                            "Report Trial"
                        trialActionText.setTextColor(
                            ContextCompat.getColor(
                                context, R.color.white
                            )
                        )
                        trialActionButton.setOnClickListener {
                            onTrialTrackingListener.reportTrialClicked(this@TrialTrackingBottomSheet)
                        }
                    }
                } else {
                    trialActionButton.visibility = View.GONE
                }
            }
        }
    }

    fun setOnTrialTrackingListener(onTrialTrackingListener: OnTrialTrackingListener) {
        this.onTrialTrackingListener = onTrialTrackingListener
    }

    interface OnTrialTrackingListener {
        fun reportTrialClicked(bottomSheet: TrialTrackingBottomSheet)
        fun markIssueDoneClicked(bottomSheet: TrialTrackingBottomSheet)
    }
}