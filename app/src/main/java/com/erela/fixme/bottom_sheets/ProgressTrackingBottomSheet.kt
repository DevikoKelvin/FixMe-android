package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.ProgressRvAdapter
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.BsProgressTrackingBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.ProgressItems
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.SubmissionProgressResponse
import com.erela.fixme.objects.UserData
import com.google.android.material.bottomsheet.BottomSheetDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
            loadingManager(true)
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

            try {
                progressItem = ArrayList()
                InitAPI.getEndpoint.getSubmissionProgress(data.idGaprojects!!).enqueue(
                    object : Callback<SubmissionProgressResponse> {
                        override fun onResponse(
                            call: Call<SubmissionProgressResponse?>,
                            response: Response<SubmissionProgressResponse?>
                        ) {
                            if (response.isSuccessful) {
                                val result = response.body()
                                if (result != null) {
                                    if (result.data != null) {
                                        for (i in 0 until result.data.size) {
                                            progressItem.add(
                                                ProgressItems(
                                                    false,
                                                    result.data[i]
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
                                                                for (j in 0 until result.data.size) {
                                                                    if (result.data[j]?.stsDetail == 1)
                                                                        progressDone++
                                                                }
                                                                if (progressDone == result.data.size && result.data.isNotEmpty()) {
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
                                                                for (j in 0 until result.data.size) {
                                                                    if (result.data[j]?.stsDetail == 1)
                                                                        progressDone++
                                                                }
                                                                if (progressDone == result.data.size && result.data.isNotEmpty()) {
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
                                } else {
                                    CustomToast.getInstance(context)
                                        .setBackgroundColor(
                                            ResourcesCompat.getColor(
                                                context.resources,
                                                R.color.custom_toast_background_failed,
                                                context.theme
                                            )
                                        )
                                        .setFontColor(
                                            ResourcesCompat.getColor(
                                                context.resources,
                                                R.color.custom_toast_font_failed,
                                                context.theme
                                            )
                                        )
                                        .setMessage(
                                            if (context.getString(R.string.lang) == "in")
                                                "Terjadi kesalahan, silakan coba lagi nanti"
                                            else
                                                "Something went wrong, please try again later"
                                        ).show()
                                    Log.e("ERROR", "Submission Progress Result Null")
                                    dismiss()
                                }
                            } else {
                                CustomToast.getInstance(context)
                                    .setBackgroundColor(
                                        ResourcesCompat.getColor(
                                            context.resources,
                                            R.color.custom_toast_background_failed,
                                            context.theme
                                        )
                                    )
                                    .setFontColor(
                                        ResourcesCompat.getColor(
                                            context.resources,
                                            R.color.custom_toast_font_failed,
                                            context.theme
                                        )
                                    )
                                    .setMessage(
                                        if (context.getString(R.string.lang) == "in")
                                            "Terjadi kesalahan, silakan coba lagi nanti"
                                        else
                                            "Something went wrong, please try again later"
                                    ).show()
                                Log.e("ERROR", "Submission Progress Unsuccessful")
                                dismiss()
                            }
                        }

                        override fun onFailure(
                            call: Call<SubmissionProgressResponse?>,
                            throwable: Throwable
                        ) {
                            CustomToast.getInstance(context)
                                .setBackgroundColor(
                                    ResourcesCompat.getColor(
                                        context.resources,
                                        R.color.custom_toast_background_failed,
                                        context.theme
                                    )
                                )
                                .setFontColor(
                                    ResourcesCompat.getColor(
                                        context.resources,
                                        R.color.custom_toast_font_failed,
                                        context.theme
                                    )
                                )
                                .setMessage(
                                    if (context.getString(R.string.lang) == "in")
                                        "Terjadi kesalahan, silakan coba lagi nanti"
                                    else
                                        "Something went wrong, please try again later"
                                ).show()
                            throwable.printStackTrace()
                            Log.e("ERROR", "Submission Progress Failure | $throwable")
                            dismiss()
                        }
                    }
                )
            } catch (exception: Exception) {
                CustomToast.getInstance(context)
                    .setBackgroundColor(
                        ResourcesCompat.getColor(
                            context.resources, R.color.custom_toast_background_failed, context.theme
                        )
                    )
                    .setFontColor(
                        ResourcesCompat.getColor(
                            context.resources, R.color.custom_toast_font_failed, context.theme
                        )
                    )
                    .setMessage(
                        if (context.getString(R.string.lang) == "in")
                            "Terjadi kesalahan, silakan coba lagi nanti"
                        else
                            "Something went wrong, please try again later"
                    )
                    .show()
                exception.printStackTrace()
                Log.e("ERROR", "Submission Progress Exception | $exception")
                dismiss()
            } finally {
                loadingManager(false)
            }
        }
    }

    private fun loadingManager(isLoading: Boolean) {
        binding.apply {
            if (isLoading) {
                loadingBar.visibility = View.VISIBLE
                rvProgress.visibility = View.GONE
            } else {
                loadingBar.visibility = View.GONE
                rvProgress.visibility = View.VISIBLE
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
        fun onLongTapListener(data: ProgressItems?, forSpv: Boolean)
    }

    override fun onItemHoldTap(item: ProgressItems?, forSpv: Boolean) {
        onProgressItemLongTapListener.onLongTapListener(item, forSpv)
    }
}