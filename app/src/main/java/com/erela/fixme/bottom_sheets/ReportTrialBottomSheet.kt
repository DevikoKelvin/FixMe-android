package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import com.erela.fixme.R
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.BsReportTrialBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.networking.InitAPI
import com.erela.fixme.objects.CreationResponse
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReportTrialBottomSheet(context: Context, private val detail: SubmissionDetailResponse) :
    BottomSheetDialog(context) {
    private val binding: BsReportTrialBinding by lazy {
        BsReportTrialBinding.inflate(layoutInflater)
    }
    private val userData: UserData by lazy {
        UserDataHelper(context).getUserData()
    }
    private lateinit var onReportTrialSuccessListener: OnReportTrialSuccessListener

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
            descriptionField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s.toString() != "" || s!!.isNotEmpty()) {
                        actionsButtonContainer.visibility = View.VISIBLE
                    } else {
                        actionsButtonContainer.visibility = View.GONE
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            passButton.setOnClickListener {
                executeReportTrial(true)
            }

            notPassButton.setOnClickListener {
                executeReportTrial(false)
            }
        }
    }

    private fun executeReportTrial(pass: Boolean) {
        binding.apply {
            if (pass) {
                passLoading.visibility = View.VISIBLE
                notPassLoading.visibility = View.GONE
            } else {
                passLoading.visibility = View.GONE
                notPassLoading.visibility = View.VISIBLE
            }
            try {
                InitAPI.getAPI.reportTrial(
                    userData.id,
                    detail.idGaprojects!!,
                    descriptionField.text.toString(),
                    if (pass) 0 else 1
                ).enqueue(object : Callback<CreationResponse> {
                    override fun onResponse(
                        call: Call<CreationResponse>, response: Response<CreationResponse>
                    ) {
                        passLoading.visibility = View.GONE
                        notPassLoading.visibility = View.GONE
                        if (response.isSuccessful) {
                            if (response.body() != null) {
                                val result = response.body()
                                if (result?.code == 1) {
                                    CustomToast.getInstance(context)
                                        .setBackgroundColor(
                                            ResourcesCompat.getColor(
                                                context.resources,
                                                R.color.custom_toast_background_success,
                                                context.theme
                                            )
                                        )
                                        .setFontColor(
                                            ResourcesCompat.getColor(
                                                context.resources,
                                                R.color.custom_toast_font_success,
                                                context.theme
                                            )
                                        )
                                        .setMessage("Trial report successfully submitted!")
                                        .show()
                                    onReportTrialSuccessListener.reportTrialSuccess()
                                    dismiss()
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
                                        .setMessage("Failed to report the trial")
                                        .show()
                                    Log.e(
                                        "ERROR ${response.code()}",
                                        "Report Trial Response code 0 | ${response.message()}"
                                    )
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
                                    .setMessage("Failed to report the trial")
                                    .show()
                                Log.e(
                                    "ERROR ${response.code()}",
                                    "Report Trial Response null | ${response.message()}"
                                )
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
                                .setMessage("Failed to report the trial")
                                .show()
                            Log.e(
                                "ERROR ${response.code()}",
                                "Report Trial Fail | ${response.message()}"
                            )
                        }
                    }

                    override fun onFailure(call: Call<CreationResponse>, throwable: Throwable) {
                        passLoading.visibility = View.GONE
                        notPassLoading.visibility = View.GONE
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
                            .setMessage("Something went wrong, please try again later")
                            .show()
                        throwable.printStackTrace()
                        Log.e("ERROR", "Report Trial Failure | $throwable")
                    }
                })
            } catch (jsonException: JSONException) {
                passLoading.visibility = View.GONE
                notPassLoading.visibility = View.GONE
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
                    .setMessage("Something went wrong, please try again later")
                    .show()
                jsonException.printStackTrace()
                Log.e("ERROR", "Report Trial Exception | $jsonException")
            }
        }
    }

    fun setOnReportTrialSuccessListener(listener: OnReportTrialSuccessListener) {
        onReportTrialSuccessListener = listener
    }

    interface OnReportTrialSuccessListener {
        fun reportTrialSuccess()
    }
}