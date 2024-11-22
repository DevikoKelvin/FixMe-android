package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import com.erela.fixme.R
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.erela.fixme.databinding.BsProgressTrackingBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData
import com.google.android.material.bottomsheet.BottomSheetDialog

class ProgressTrackingBottomSheet(context: Context, val data: SubmissionDetailResponse) :
    BottomSheetDialog(context) {
    private lateinit var binding: BsProgressTrackingBinding
    private lateinit var userData: UserData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BsProgressTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        userData = UserDataHelper(context).getUserData()

        init()
    }

    @SuppressLint("SetTextI18n")
    private fun init() {
        binding.apply {
            if (data.usernApprove == userData.username.lowercase()) {
                progressActionButton.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context, R.color.status_on_trial
                    )
                )
                progressActionText.setTextColor(ContextCompat.getColor(context, R.color.white))
                progressActionText.text = "Start Trial"
                progressActionButton.setOnClickListener {
                }
            } else {
                if (data.usernUserEnd == userData.username.lowercase()) {
                    progressActionButton.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context, R.color.button_color
                        )
                    )
                    progressActionText.setTextColor(ContextCompat.getColor(context, R.color.white))
                    progressActionText.text = context.getString(R.string.create_new_progress)
                    progressActionButton.setOnClickListener {
                    }
                } else {
                    progressActionButton.visibility = View.GONE
                }
            }
        }
    }
}