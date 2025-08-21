package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.ProgressRvAdapter
import com.erela.fixme.databinding.BsProgressTrackingBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.ProgressItem
import com.erela.fixme.objects.ProgressItems
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
    private lateinit var progressItem: ArrayList<ProgressItems>
    private lateinit var onProgressTrackingListener: OnProgressTrackingListener
    private lateinit var onProgressItemLongTapListener: OnProgressItemLongTapListener
    private var tech = false

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
            val message = StringBuilder().append(
                if (context.getString(R.string.lang) == "in")
                    "Kemajuan sedang dikerjakan."
                else
                    "Progress is being worked on."
            )
            progressWorkedBy.text = message.toString()
            if (data.stsGaprojects == 22) {
                holdMessage.visibility = View.VISIBLE
                holdMessage.text = if (context.getString(R.string.lang) == "in")
                    "Proyek ini sedang dalam status Jeda.\nAlasan: ${data.keteranganHold}"
                else
                    "Issue on Hold status.\nReason: ${data.keteranganHold}"
            } else
                holdMessage.visibility = View.GONE
            progressItem = ArrayList()
            for (i in 0 until data.progress!!.size) {
                progressItem.add(
                    ProgressItems(
                        false,
                        data.progress[i]
                    )
                )
            }
            progressAdapter = ProgressRvAdapter(context, activity, data, progressItem).also {
                with(it) {
                    setOnItemHoldTapListener(this@ProgressTrackingBottomSheet)
                }
            }
            rvProgress.adapter = progressAdapter
            rvProgress.setItemViewCacheSize(1000)
            rvProgress.layoutManager = LinearLayoutManager(context)

            when (data.stsGaprojects) {
                22 -> progressActionButton.visibility = View.GONE
                3 -> {
                    if (data.idUser == userData.id) {
                        for (i in 0 until data.usernUserTeknisi!!.size) {
                            if (data.usernUserTeknisi[i]?.idUser == userData.id) {
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
                                progressActionButton.visibility = View.GONE
                            }
                        }

                        if (!tech) {
                            for (i in 0 until data.usernUserSpv!!.size) {
                                if (data.usernUserSpv[i]?.idUser == userData.id) {
                                    var progressDone = 0
                                    for (j in 0 until data.progress.size) {
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
                                        progressActionText.text =
                                            if (context.getString(R.string.lang) == "in")
                                                "Tandai Siap Uji Coba"
                                            else "Mark Ready for Trial"
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
                        for (i in 0 until data.usernUserTeknisi!!.size) {
                            if (data.usernUserTeknisi[i]?.idUser == userData.id) {
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
                                progressActionButton.visibility = View.GONE
                            }
                        }

                        if (!tech) {
                            for (i in 0 until data.usernUserSpv!!.size) {
                                if (data.usernUserSpv[i]?.idUser == userData.id) {
                                    var progressDone = 0
                                    for (j in 0 until data.progress.size) {
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
                                                context,
                                                R.color.custom_toast_font_blue
                                            )
                                        )
                                        progressActionText.text =
                                            if (context.getString(R.string.lang) == "in")
                                                "Tandai Siap Uji Coba"
                                            else
                                                "Mark Ready for Trial"
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
                31 -> progressActionButton.visibility = View.GONE
                else -> {
                    if (data.idUser == userData.id) {
                        progressActionButton.visibility = View.VISIBLE
                        progressActionButton.setCardBackgroundColor(
                            ContextCompat.getColor(
                                context, R.color.status_on_trial
                            )
                        )
                        progressActionText.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.white
                            )
                        )
                        progressActionText.text = if (context.getString(R.string.lang) == "in")
                            "Mulai Uji Coba"
                        else
                            "Start Trial"
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
        fun onLongTapListener(data: ProgressItem?, forSpv: Boolean)
    }

    override fun onItemHoldTap(item: ProgressItem?, forSpv: Boolean) {
        onProgressItemLongTapListener.onLongTapListener(item, forSpv)
    }
}