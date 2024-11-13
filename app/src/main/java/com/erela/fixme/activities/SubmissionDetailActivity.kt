package com.erela.fixme.activities

import com.erela.fixme.R
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.erela.fixme.bottom_sheets.SubmissionActionBottomSheet
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivitySubmissionDetailBinding
import com.erela.fixme.helpers.Base64Helper
import com.erela.fixme.helpers.InitAPI
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.UsernameFormatHelper
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData
import com.erela.fixme.objects.UserListResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SubmissionDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySubmissionDetailBinding
    private lateinit var detailId: String

    companion object {
        const val DETAIL_ID = "DETAIL_ID"

        fun initiate(context: Context, detailId: String) {
            context.startActivity(
                Intent(
                    context, SubmissionDetailActivity::class.java
                ).also {
                    it.putExtra(DETAIL_ID, detailId)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubmissionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
    }

    private fun init() {
        binding.apply {
            detailId = intent.getStringExtra(DETAIL_ID).toString()

            backButton.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            loadingBar.visibility = View.VISIBLE
            contentScrollContainer.visibility = View.GONE
            buttonContainer.visibility = View.GONE

            try {
                InitAPI.getAPI.getSubmissionDetail(detailId)
                    .enqueue(object : Callback<List<SubmissionDetailResponse>> {
                        override fun onResponse(
                            call: Call<List<SubmissionDetailResponse>?>,
                            response: Response<List<SubmissionDetailResponse>?>
                        ) {
                            loadingBar.visibility = View.GONE
                            contentScrollContainer.visibility = View.VISIBLE
                            buttonContainer.visibility = View.VISIBLE
                            if (response.isSuccessful) {
                                if (response.body() != null) {
                                    val data = response.body()!![0]
                                    if (data.fotoGaprojects!!.isEmpty()) {
                                        imageContainer.visibility = View.GONE
                                    } else {
                                        imageContainer.visibility = View.VISIBLE
                                        val imageData = data.fotoGaprojects[0]
                                        if (Base64Helper.isBase64Encoded(
                                                imageData?.foto.toString()
                                            )
                                        ) {
                                            val decodedImageURL = Base64Helper.decodeBase64(
                                                imageData?.foto.toString()
                                            )
                                            Glide.with(applicationContext)
                                                .load(decodedImageURL)
                                                .placeholder(R.drawable.image_placeholder)
                                                .into(submissionImage)
                                        } else {
                                            Glide.with(applicationContext)
                                                .load(InitAPI.IMAGE_URL + imageData?.foto)
                                                .placeholder(R.drawable.image_placeholder)
                                                .into(submissionImage)
                                        }
                                    }
                                    submissionName.text = data.judulKasus
                                    inputDate.text = data.setTglinput
                                    when (data.stsGaprojects) {
                                        // Rejected
                                        0.toString() -> {
                                            submissionStatus.setCardBackgroundColor(
                                                ResourcesCompat.getColor(
                                                    resources,
                                                    R.color.status_rejected,
                                                    theme
                                                )
                                            )
                                            submissionStatusText.text = "Rejected"
                                            buttonContainer.visibility = View.GONE
                                            actionButton.visibility = View.VISIBLE
                                            editButton.visibility = View.VISIBLE
                                            onProgressButton.visibility = View.GONE
                                        }
                                        // Pending
                                        1.toString() -> {
                                            submissionStatus.setCardBackgroundColor(
                                                ResourcesCompat.getColor(
                                                    resources,
                                                    R.color.status_pending,
                                                    theme
                                                )
                                            )
                                            submissionStatusText.text = "Pending"
                                            buttonContainer.visibility = View.VISIBLE
                                            actionButton.visibility = View.VISIBLE
                                            editButton.visibility = View.VISIBLE
                                            onProgressButton.visibility = View.GONE
                                        }
                                        // Approved
                                        2.toString() -> {
                                            submissionStatus.setCardBackgroundColor(
                                                ResourcesCompat.getColor(
                                                    resources,
                                                    R.color.status_approved,
                                                    theme
                                                )
                                            )
                                            submissionStatusText.text = "Approved"
                                        }
                                        // On-Progress
                                        3.toString() -> {
                                            submissionStatus.setCardBackgroundColor(
                                                ResourcesCompat.getColor(
                                                    resources,
                                                    R.color.status_on_progress,
                                                    theme
                                                )
                                            )
                                            submissionStatusText.text = "On Progress"
                                            buttonContainer.visibility = View.VISIBLE
                                            actionButton.visibility = View.GONE
                                            editButton.visibility = View.GONE
                                            onProgressButton.visibility = View.VISIBLE
                                        }
                                        // Done
                                        4.toString() -> {
                                            submissionStatus.setCardBackgroundColor(
                                                ResourcesCompat.getColor(
                                                    resources,
                                                    R.color.status_done,
                                                    theme
                                                )
                                            )
                                            submissionStatusText.text = "Done"
                                            buttonContainer.visibility = View.GONE
                                            actionButton.visibility = View.VISIBLE
                                            editButton.visibility = View.VISIBLE
                                            onProgressButton.visibility = View.GONE
                                        }
                                        // On-Trial
                                        31.toString() -> {
                                            submissionStatus.setCardBackgroundColor(
                                                ResourcesCompat.getColor(
                                                    resources,
                                                    R.color.status_on_trial,
                                                    theme
                                                )
                                            )
                                            submissionStatusText.text = "On Trial"
                                        }
                                    }
                                    submissionDescription.text = data.keterangan
                                    machineCode.text = data.kodeMesin
                                    machineName.text = data.namaMesin
                                    try {
                                        InitAPI.getAPI.getUserList()
                                            .enqueue(object : Callback<List<UserListResponse>> {
                                                override fun onResponse(
                                                    call: Call<List<UserListResponse>?>,
                                                    response: Response<List<UserListResponse>?>
                                                ) {
                                                    if (response.isSuccessful) {
                                                        if (response.body() != null) {
                                                            for (i in 0 until response.body()!!.size) {
                                                                if (data.idUser == response.body()
                                                                        ?.get(i)?.idUser
                                                                ) {
                                                                    user.text =
                                                                        UsernameFormatHelper.getRealUsername(
                                                                            response.body()?.get(
                                                                                i
                                                                            )?.usern.toString()
                                                                        )
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Log.e("ERROR", response.message())
                                                    }
                                                }

                                                override fun onFailure(
                                                    call: Call<List<UserListResponse>?>,
                                                    throwable: Throwable
                                                ) {
                                                    throwable.printStackTrace()
                                                }
                                            })
                                    } catch (exception: Exception) {
                                        exception.printStackTrace()
                                    }
                                    department.text = data.dept
                                    inputTime.text = data.tglInput
                                    location.text = data.lokasi
                                    reportTime.text = data.tglWaktuStart
                                    actualTime.text = if (data.tglWaktuActual != ""
                                        || data.tglWaktuActual != "0000-00-00 00:00:00"
                                        || data.tglWaktuActual != null
                                    ) {
                                        data.tglWaktuActual
                                    } else
                                        "-"
                                    workingTime.text = if (data.tglWaktuPengerjaan != ""
                                        || data.tglWaktuPengerjaan != "0000-00-00 00:00:00"
                                        || data.tglWaktuPengerjaan != null
                                    ) {
                                        data.tglWaktuPengerjaan
                                    } else
                                        "-"
                                    actionButton.setOnClickListener {
                                        val bottomSheet = SubmissionActionBottomSheet(
                                            this@SubmissionDetailActivity, data
                                        )
                                        if (bottomSheet.window != null) {
                                            bottomSheet.show()
                                        }
                                    }
                                }
                            } else {
                                CustomToast.getInstance(applicationContext)
                                    .setBackgroundColor(
                                        ResourcesCompat.getColor(
                                            resources, R.color.custom_toast_background_failed, theme
                                        )
                                    )
                                    .setFontColor(
                                        ResourcesCompat.getColor(
                                            resources, R.color.custom_toast_font_failed, theme
                                        )
                                    )
                                    .setMessage("Failed to get submission detail")
                                    .show()
                                finish()
                                Log.e("Submission Detail", response.message())
                            }
                        }

                        override fun onFailure(
                            call: Call<List<SubmissionDetailResponse>?>,
                            throwable: Throwable
                        ) {
                            throwable.printStackTrace()
                            CustomToast.getInstance(applicationContext)
                                .setBackgroundColor(
                                    ResourcesCompat.getColor(
                                        resources, R.color.custom_toast_background_failed, theme
                                    )
                                )
                                .setFontColor(
                                    ResourcesCompat.getColor(
                                        resources, R.color.custom_toast_font_failed, theme
                                    )
                                )
                                .setMessage("Something went wrong, please try again later")
                                .show()
                            finish()
                        }
                    })
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }
}