package com.erela.fixme.bottom_sheets

import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import com.erela.fixme.R
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.BsReportTrialBinding
import com.erela.fixme.dialogs.ConfirmationDialog
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.CreationResponse
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
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

        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            val view: View? = currentFocus
            if (view is TextInputEditText || view is EditText) {
                val rect = Rect()
                view.getGlobalVisibleRect(rect)
                if (!rect.contains(motionEvent.rawX.toInt(), motionEvent.rawY.toInt())) {
                    view.clearFocus()
                    val inputMethodManager =
                        context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(motionEvent)
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
            val confirmationDialog = ConfirmationDialog(
                context,
                if (pass) {
                    if (context.getString(R.string.lang) == "in")
                        "Apakah Anda yakin ingin melaporkan percobaan ini lolos uji?"
                    else
                        "Are you sure you want to report this trial as passed?"
                } else {
                    if (context.getString(R.string.lang) == "in")
                        "Apakah Anda yakin ingin melaporkan percobaan ini sebagai gagal?"
                    else
                        "Are you sure you want to report this trial as failed?"
                },
                "Yes"
            ).also {
                with(it) {
                    setConfirmationDialogListener(object :
                        ConfirmationDialog.ConfirmationDialogListener {
                        override fun onConfirm() {
                            if (pass) {
                                passLoading.visibility = View.VISIBLE
                                notPassLoading.visibility = View.GONE
                            } else {
                                passLoading.visibility = View.GONE
                                notPassLoading.visibility = View.VISIBLE
                            }
                            try {
                                InitAPI.getEndpoint.reportTrial(
                                    userData.id,
                                    detail.idGaprojects!!,
                                    descriptionField.text.toString(),
                                    if (pass) 0 else 1
                                ).enqueue(object : Callback<CreationResponse> {
                                    override fun onResponse(
                                        call: Call<CreationResponse>,
                                        response: Response<CreationResponse>
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
                                                        .setMessage(
                                                            if (context.getString(R.string.lang) == "in")
                                                                "Laporan uji coba berhasil dikirim!"
                                                            else
                                                                "Trial report successfully submitted!"
                                                        )
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
                                                        .setMessage(
                                                            if (context.getString(R.string.lang) == "in")
                                                                "Gagal mengirim laporan uji coba"
                                                            else
                                                                "Failed to report the trial"
                                                        )
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
                                                    .setMessage(
                                                        if (context.getString(R.string.lang) == "in")
                                                            "Gagal mengirim laporan uji coba"
                                                        else
                                                            "Failed to report the trial"
                                                    )
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
                                                .setMessage(
                                                    if (context.getString(R.string.lang) == "in")
                                                        "Gagal mengirim laporan uji coba"
                                                    else
                                                        "Failed to report the trial"
                                                )
                                                .show()
                                            Log.e(
                                                "ERROR ${response.code()}",
                                                "Report Trial Fail | ${response.message()}"
                                            )
                                        }
                                    }

                                    override fun onFailure(
                                        call: Call<CreationResponse>,
                                        throwable: Throwable
                                    ) {
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
                                            .setMessage(
                                                if (context.getString(R.string.lang) == "in")
                                                    "Terjadi kesalahan, silakan coba lagi nanti"
                                                else
                                                    "Something went wrong, please try again later"
                                            )
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
                                    )
                                    .show()
                                jsonException.printStackTrace()
                                Log.e("ERROR", "Report Trial Exception | $jsonException")
                            }
                        }
                    })
                }
            }

            if (confirmationDialog.window != null)
                confirmationDialog.show()
        }
    }

    fun setOnReportTrialSuccessListener(listener: OnReportTrialSuccessListener) {
        onReportTrialSuccessListener = listener
    }

    interface OnReportTrialSuccessListener {
        fun reportTrialSuccess()
    }
}