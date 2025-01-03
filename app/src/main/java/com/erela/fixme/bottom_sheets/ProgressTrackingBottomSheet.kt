package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
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
    private var tech = false

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
                if (data.usernUserTeknisi.size == 1)
                    message.append("${data.usernUserTeknisi[i]?.namaUser?.trimEnd()}")
                else {
                    if (data.usernUserTeknisi.size == 2) {
                        if (i == 0)
                            message.append("${data.usernUserTeknisi[i]?.namaUser?.trimEnd()} and ")
                        else
                            message.append("${data.usernUserTeknisi[i]?.namaUser?.trimEnd()}")
                    } else {
                        if (i < data.usernUserTeknisi.size - 1)
                            message.append("${data.usernUserTeknisi[i]?.namaUser?.trimEnd()}, ")
                        else if (i == data.usernUserTeknisi.size - 1)
                            message.append("${data.usernUserTeknisi[i]?.namaUser?.trimEnd()}")
                        else
                            message.append(" and ${data.usernUserTeknisi[i]?.namaUser?.trimEnd()}")
                    }
                }
            }
            progressWorkedBy.text = message.toString()
            progressAdapter = ProgressRvAdapter(context, activity, data, data.progress).also {
                with(it) {
                    setOnItemHoldTapListener(this@ProgressTrackingBottomSheet)
                }
            }
            rvProgress.adapter = progressAdapter
            rvProgress.layoutManager = LinearLayoutManager(context)

            when (data.stsGaprojects) {
                3 -> {
                    if (data.idUser == userData.id) {
                        Log.e("Am I the reporter?", "Yes")
                        for (i in 0 until data.usernUserTeknisi.size) {
                            if (data.usernUserTeknisi[i]?.idUser == userData.id) {
                                Log.e("Am I the Technicians?", "Yes")
                                progressActionButton.visibility = View.VISIBLE
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
                                }
                                tech = true
                                break
                            } else {
                                Log.e("Am I the Technicians?", "No")
                                progressActionButton.visibility = View.GONE
                            }
                        }

                        if (!tech) {
                            for (i in 0 until data.usernUserSpv!!.size) {
                                if (data.usernUserSpv[i]?.idUser == userData.id) {
                                    var progressDone = 0
                                    for (j in 0 until data.progress!!.size) {
                                        if (data.progress[j]?.stsDetail == 1)
                                            progressDone++
                                    }
                                    if (progressDone == data.progress.size && data.progress.isNotEmpty()) {
                                        progressActionButton.visibility = View.VISIBLE
                                        progressActionButton.setCardBackgroundColor(
                                            ContextCompat.getColor(
                                                context, R.color.custom_toast_background_soft_blue
                                            )
                                        )
                                        progressActionText.setTextColor(
                                            ContextCompat.getColor(
                                                context, R.color.custom_toast_font_blue
                                            )
                                        )
                                        progressActionText.text = "Mark Ready for Trial"
                                        progressActionButton.setOnClickListener {
                                            onProgressTrackingListener.readyForTrialClicked()
                                        }
                                    } else {
                                        progressActionButton.visibility = View.GONE
                                    }
                                    break
                                } else {
                                    progressActionButton.visibility = View.GONE
                                }
                            }
                        }
                    } else {
                        Log.e("Am I the reporter?", "No")
                        for (i in 0 until data.usernUserTeknisi.size) {
                            if (data.usernUserTeknisi[i]?.idUser == userData.id) {
                                Log.e("Am I the Technicians?", "Yes")
                                progressActionButton.visibility = View.VISIBLE
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
                                }
                                tech = true
                                break
                            } else {
                                Log.e("Am I the Technicians?", "No")
                                progressActionButton.visibility = View.GONE
                            }
                        }

                        if (!tech) {
                            for (i in 0 until data.usernUserSpv!!.size) {
                                if (data.usernUserSpv[i]?.idUser == userData.id) {
                                    var progressDone = 0
                                    for (j in 0 until data.progress!!.size) {
                                        if (data.progress[j]?.stsDetail == 1)
                                            progressDone++
                                    }
                                    if (progressDone == data.progress.size && data.progress.isNotEmpty()) {
                                        progressActionButton.visibility = View.VISIBLE
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
                                            onProgressTrackingListener.readyForTrialClicked()
                                        }
                                    } else {
                                        progressActionButton.visibility = View.GONE
                                    }
                                    break
                                } else {
                                    progressActionButton.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
                4 -> progressActionButton.visibility = View.GONE
                /*30 -> progressActionButton.visibility = View.GONE*/
                31 -> progressActionButton.visibility = View.GONE
                else -> {
                    if (data.idUser == userData.id) {
                        progressActionButton.visibility = View.VISIBLE
                        progressActionButton.setCardBackgroundColor(
                            ContextCompat.getColor(
                                context, R.color.status_on_trial
                            )
                        )
                        progressActionText.setTextColor(ContextCompat.getColor(context, R.color.white))
                        progressActionText.text = "Start Trial"
                        progressActionButton.setOnClickListener {
                            onProgressTrackingListener.startTrialClicked()
                        }
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
        fun readyForTrialClicked()
        fun startTrialClicked()
    }

    interface OnProgressItemLongTapListener {
        fun onLongTapListener(data: ProgressItem?)
    }

    override fun onItemHoldTap(item: ProgressItem?) {
        onProgressItemLongTapListener.onLongTapListener(item)
    }
}