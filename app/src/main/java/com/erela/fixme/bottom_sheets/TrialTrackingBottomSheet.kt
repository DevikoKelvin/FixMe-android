package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
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
import com.erela.fixme.adapters.recycler_view.TrialRvAdapter
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.BsTrialBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.SubmissionTrialResponse
import com.erela.fixme.objects.TrialDataItem
import com.erela.fixme.objects.UserData
import com.google.android.material.bottomsheet.BottomSheetDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
    private lateinit var trialData: ArrayList<TrialDataItem?>
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
            loadingManager(true)
            try {
                trialData = ArrayList()
                InitAPI.getEndpoint.getTrial(data.idGaprojects!!).enqueue(
                    object : Callback<SubmissionTrialResponse> {
                        override fun onResponse(
                            call: Call<SubmissionTrialResponse?>,
                            response: Response<SubmissionTrialResponse?>
                        ) {
                            if (response.isSuccessful) {
                                val result = response.body()
                                if (result != null) {
                                    if (result.data != null) {
                                        for (i in 0 until result.data.size) {
                                            trialData.add(result.data[i])
                                        }
                                        trialAdapter = TrialRvAdapter(context, data, trialData)
                                        rvTrial.adapter = trialAdapter
                                        rvTrial.setItemViewCacheSize(1000)
                                        rvTrial.layoutManager = LinearLayoutManager(context)
                                        if (data.stsGaprojects == 3 || data.stsGaprojects == 4 || data.stsGaprojects == 30) {
                                            trialActionButton.visibility = View.GONE
                                        } else {
                                            if (data.idUser == userData.id) {
                                                trialActionButton.visibility = View.VISIBLE
                                                for (element in trialData) {
                                                    if (element?.status == 0) {
                                                        readyDone = true
                                                        break
                                                    }
                                                }
                                                if (readyDone) {
                                                    trialActionButton.setCardBackgroundColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.custom_toast_background_soft_blue
                                                        )
                                                    )
                                                    trialActionText.text =
                                                        if (context.getString(R.string.lang) == "in")
                                                            "Tandai Selesai Sekarang!"
                                                        else
                                                            "Mark as Done now!"
                                                    trialActionText.setTextColor(
                                                        ContextCompat.getColor(
                                                            context, R.color.custom_toast_font_blue
                                                        )
                                                    )
                                                    trialActionButton.setOnClickListener {
                                                        onTrialTrackingListener.markIssueDoneClicked(
                                                            this@TrialTrackingBottomSheet
                                                        )
                                                    }
                                                } else {
                                                    trialActionButton.setCardBackgroundColor(
                                                        ContextCompat.getColor(
                                                            context, R.color.status_on_trial
                                                        )
                                                    )
                                                    trialActionText.text =
                                                        if (context.getString(R.string.lang) == "in")
                                                            "Laporkan Uji Coba"
                                                        else
                                                            "Report Trial"
                                                    trialActionText.setTextColor(
                                                        ContextCompat.getColor(
                                                            context, R.color.white
                                                        )
                                                    )
                                                    trialActionButton.setOnClickListener {
                                                        onTrialTrackingListener.reportTrialClicked(
                                                            this@TrialTrackingBottomSheet
                                                        )
                                                    }
                                                }
                                            } else {
                                                trialActionButton.visibility = View.GONE
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
                                    Log.e("ERROR", "Submission Trial Result Null")
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
                                Log.e("ERROR", "Submission Trial Unsuccessful")
                                dismiss()
                            }
                        }

                        override fun onFailure(
                            call: Call<SubmissionTrialResponse?>,
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
                            Log.e("ERROR", "Submission Trial Failure | $throwable")
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
                Log.e("ERROR", "Submission Trial Exception | $exception")
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
                rvTrial.visibility = View.GONE
            } else {
                loadingBar.visibility = View.GONE
                rvTrial.visibility = View.VISIBLE
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