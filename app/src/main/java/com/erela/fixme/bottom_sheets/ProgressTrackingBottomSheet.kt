package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.ProgressRvAdapter
import com.erela.fixme.databinding.BsProgressTrackingBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.ProgressItem
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData
import com.google.android.material.bottomsheet.BottomSheetDialog

class ProgressTrackingBottomSheet(
    context: Context, val activity: Activity, val data: SubmissionDetailResponse
) : BottomSheetDialog(context), ProgressRvAdapter.OnItemHoldTapListener {
    private val binding: BsProgressTrackingBinding by lazy {
        BsProgressTrackingBinding.inflate(layoutInflater)
    }
    private val userData: UserData by lazy {
        UserDataHelper(context).getUserData()
    }
    private lateinit var progressAdapter: ProgressRvAdapter
    private lateinit var onProgressTrackingListener: OnProgressTrackingListener
    private lateinit var onProgressItemLongTapListener: OnProgressItemLongTapListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    @SuppressLint("SetTextI18n")
    private fun init() {
        binding.apply {
            val message = StringBuilder().append("Progress is being worked on by ")
            for (i in 0 until data.usernUserTeknisi!!.size) {
                if (data.usernUserTeknisi.size == 1) {
                    message.append("${data.usernUserTeknisi[i]?.namaUser}")
                } else {
                    if (i < data.usernUserTeknisi.size - 1)
                        message.append("${data.usernUserTeknisi[i]?.namaUser}, ")
                    else
                        message.append(" and ${data.usernUserTeknisi[i]?.namaUser}")
                }
            }
            progressWorkedBy.text = message.toString()
            progressAdapter = ProgressRvAdapter(context, activity, data.progress).also {
                with(it) {
                    setOnItemHoldTapListener(this@ProgressTrackingBottomSheet)
                }
            }
            progressAdapter.itemCount
            rvProgress.adapter = progressAdapter
            rvProgress.layoutManager = LinearLayoutManager(context)
            if (data.idUser == userData.id) {
                progressActionButton.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context, R.color.status_on_trial
                    )
                )
                progressActionText.setTextColor(ContextCompat.getColor(context, R.color.white))
                progressActionText.text = "Start Trial"
                progressActionButton.setOnClickListener {
                }
            } else if (userData.id == data.usernUserSpv!![0]?.idUser) {
                /*for (i in 0 until data.progress!!.size) {
                    if (data.progress[i])
                }*/
                progressActionButton.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context, R.color.custom_toast_background_soft_blue
                    )
                )
                progressActionText.setTextColor(
                    ContextCompat.getColor(context, R.color.custom_toast_font_blue)
                )
                progressActionText.text = "Mark Ready for Trial"
                progressActionButton.setOnClickListener {
                }
            } else {
                for (i in 0 until data.usernUserTeknisi.size) {
                    if (data.usernUserTeknisi[i]?.idUser == userData.id) {
                        progressActionButton.setCardBackgroundColor(
                            ContextCompat.getColor(
                                context, R.color.button_color
                            )
                        )
                        progressActionText.setTextColor(
                            ContextCompat.getColor(context, R.color.white)
                        )
                        progressActionText.text =
                            if (progressAdapter.itemCount == 0) context.getString(
                                R.string.action_on_progress
                            ) else context.getString(R.string.create_new_progress)
                        progressActionButton.setOnClickListener {
                            onProgressTrackingListener.createProgressClicked()
                            dismiss()
                        }
                    } else {
                        progressActionButton.visibility = View.GONE
                    }
                }
            }
        }
    }

    fun setOnProgressTrackingListener(onProgressTrackingListener: OnProgressTrackingListener) {
        this.onProgressTrackingListener = onProgressTrackingListener
    }

    fun setOnProgressItemLongTapListener(onProgressItemLongTapListener: OnProgressItemLongTapListener) {
        this.onProgressItemLongTapListener = onProgressItemLongTapListener
    }

    interface OnProgressTrackingListener {
        fun createProgressClicked()
    }

    interface OnProgressItemLongTapListener {
        fun onLongTapListener(data: ProgressItem?)
    }

    override fun onItemHoldTap(item: ProgressItem?) {
        onProgressItemLongTapListener.onLongTapListener(item)
    }
}