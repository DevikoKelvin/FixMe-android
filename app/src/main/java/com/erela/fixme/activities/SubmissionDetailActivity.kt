package com.erela.fixme.activities

import android.annotation.SuppressLint
import com.erela.fixme.R
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.erela.fixme.adapters.pager.ImageCarouselPagerAdapter
import com.erela.fixme.bottom_sheets.ProgressTrackingBottomSheet
import com.erela.fixme.bottom_sheets.SubmissionActionBottomSheet
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivitySubmissionDetailBinding
import com.erela.fixme.helpers.Base64Helper
import com.erela.fixme.helpers.InitAPI
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.UsernameFormatHelper
import com.erela.fixme.objects.FotoGaprojectsItem
import com.erela.fixme.objects.StarconnectUserResponse
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData
import com.erela.fixme.objects.UserDetailResponse
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.PI

class SubmissionDetailActivity : AppCompatActivity(),
    SubmissionActionBottomSheet.OnUpdateSuccessListener {
    private lateinit var binding: ActivitySubmissionDetailBinding
    private lateinit var imageData: ArrayList<FotoGaprojectsItem>
    private lateinit var imageCarouselAdapter: ImageCarouselPagerAdapter
    private lateinit var detailId: String
    private lateinit var userData: UserData
    private lateinit var userDetail: UserDetailResponse

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

        userData = UserDataHelper(applicationContext).getUserData()

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

            try {
                InitAPI.getAPI.getSubmissionDetail(detailId)
                    .enqueue(object : Callback<List<SubmissionDetailResponse>> {
                        @SuppressLint("SetTextI18n")
                        override fun onResponse(
                            call: Call<List<SubmissionDetailResponse>?>,
                            response: Response<List<SubmissionDetailResponse>?>
                        ) {
                            loadingBar.visibility = View.GONE
                            contentScrollContainer.visibility = View.VISIBLE
                            if (response.isSuccessful) {
                                if (response.body() != null) {
                                    val data = response.body()!![0]
                                    Log.e("DATA", data.toString())
                                    if (data.fotoGaprojects!!.isEmpty()) {
                                        imageContainer.visibility = View.GONE
                                    } else {
                                        imageContainer.visibility = View.VISIBLE
                                        imageData = ArrayList()
                                        if (data.fotoGaprojects.size > 1) {
                                            imageCarouselHolder.visibility = View.VISIBLE
                                            circleIndicator.visibility = View.VISIBLE
                                            submissionImage.visibility = View.GONE
                                            for (i in 0 until data.fotoGaprojects.size) {
                                                imageData.add(
                                                    data.fotoGaprojects[i]!!
                                                )
                                            }
                                            imageCarouselAdapter = ImageCarouselPagerAdapter(
                                                this@SubmissionDetailActivity, imageData,
                                                this@SubmissionDetailActivity
                                            )
                                            imageCarouselHolder.adapter = imageCarouselAdapter
                                            circleIndicator.setViewPager(imageCarouselHolder)
                                            imageCarouselAdapter.registerDataSetObserver(
                                                circleIndicator.dataSetObserver
                                            )
                                        } else {
                                            imageCarouselHolder.visibility = View.GONE
                                            circleIndicator.visibility = View.GONE
                                            submissionImage.visibility = View.VISIBLE
                                            val image = data.fotoGaprojects[0]
                                            if (Base64Helper.isBase64Encoded(
                                                    image?.foto.toString()
                                                )
                                            ) {
                                                val decodedImageURL = Base64Helper.decodeBase64(
                                                    image?.foto.toString()
                                                )
                                                Glide.with(applicationContext)
                                                    .load(decodedImageURL)
                                                    .placeholder(R.drawable.image_placeholder)
                                                    .into(submissionImage)
                                            } else {
                                                Glide.with(applicationContext)
                                                    .load(InitAPI.IMAGE_URL + image?.foto)
                                                    .placeholder(R.drawable.image_placeholder)
                                                    .into(submissionImage)
                                            }
                                        }
                                    }
                                    submissionName.text = data.judulKasus
                                    inputDate.text = data.setTglinput
                                    when (data.stsGaprojects) {
                                        // Rejected
                                        0 -> {
                                            submissionStatus.setCardBackgroundColor(
                                                ResourcesCompat.getColor(
                                                    resources,
                                                    R.color.status_rejected,
                                                    theme
                                                )
                                            )
                                            submissionStatusText.text = "Rejected"
                                            actionButton.visibility = View.GONE
                                            editButton.visibility = View.GONE
                                            seeProgressButton.visibility = View.GONE
                                            onProgressButton.visibility = View.GONE
                                            statusMessageContainer.visibility = View.VISIBLE
                                            statusMessageContainer.setCardBackgroundColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.custom_toast_font_failed
                                                )
                                            )
                                            statusMessage.text = "Rejected by ${data.usernApprove}"
                                            statusMessage.setTextColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.white
                                                )
                                            )
                                        }
                                        // Pending
                                        1 -> {
                                            submissionStatus.setCardBackgroundColor(
                                                ResourcesCompat.getColor(
                                                    resources,
                                                    R.color.status_pending,
                                                    theme
                                                )
                                            )
                                            submissionStatusText.text = "Pending"
                                            seeProgressButton.visibility = View.GONE
                                        }
                                        // Approved
                                        2 -> {
                                            submissionStatus.setCardBackgroundColor(
                                                ResourcesCompat.getColor(
                                                    resources,
                                                    R.color.status_approved,
                                                    theme
                                                )
                                            )
                                            submissionStatusText.text = "Approved"
                                            /*if (userData.id == data.idUserEnd?.toInt()) {
                                                actionButton.visibility = View.VISIBLE
                                                editButton.visibility = View.GONE
                                                seeProgressButton.visibility = View.GONE
                                                onProgressButton.visibility = View.GONE
                                                statusMessageContainer.visibility = View.GONE
                                            } else {
                                                actionButton.visibility = View.GONE
                                                editButton.visibility = View.GONE
                                                seeProgressButton.visibility = View.GONE
                                                onProgressButton.visibility = View.GONE
                                                statusMessageContainer.visibility = View.VISIBLE
                                                statusMessageContainer.setCardBackgroundColor(
                                                    ContextCompat.getColor(
                                                        this@SubmissionDetailActivity,
                                                        R.color.custom_toast_background_soft_blue
                                                    )
                                                )
                                                statusMessage.text =
                                                    "Approved by ${data.usernApprove}\nWaiting for action from ${data.usernUserEnd}"
                                                statusMessage.setTextColor(
                                                    ContextCompat.getColor(
                                                        this@SubmissionDetailActivity,
                                                        R.color.black
                                                    )
                                                )
                                            }*/
                                        }
                                        // On-Progress
                                        3 -> {
                                            submissionStatus.setCardBackgroundColor(
                                                ResourcesCompat.getColor(
                                                    resources,
                                                    R.color.status_on_progress,
                                                    theme
                                                )
                                            )
                                            submissionStatusText.text = "On Progress"
                                            actionButton.visibility = View.GONE
                                            editButton.visibility = View.GONE
                                            seeProgressButton.visibility = View.GONE
                                            onProgressButton.visibility = View.VISIBLE
                                            onProgressText.text =
                                                "On Progress by ${data.usernUserEnd}.\nClick to see progress."
                                            onProgressButton.setOnClickListener {
                                                val bottomSheet = ProgressTrackingBottomSheet(
                                                    this@SubmissionDetailActivity, data
                                                )

                                                if (bottomSheet.window != null)
                                                    bottomSheet.show()
                                            }
                                        }
                                        // Done
                                        4 -> {
                                            submissionStatus.setCardBackgroundColor(
                                                ResourcesCompat.getColor(
                                                    resources,
                                                    R.color.status_done,
                                                    theme
                                                )
                                            )
                                            submissionStatusText.text = "Done"
                                            actionButton.visibility = View.GONE
                                            editButton.visibility = View.GONE
                                            seeProgressButton.visibility = View.GONE
                                            onProgressButton.visibility = View.GONE
                                            statusMessageContainer.visibility = View.VISIBLE
                                            statusMessageContainer.setCardBackgroundColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.custom_toast_font_success
                                                )
                                            )
                                            statusMessage.text =
                                                "This issue is done by ${data.usernUserDone}.\nClick to see progress"
                                            statusMessage.setTextColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.white
                                                )
                                            )
                                            statusMessageContainer.setOnClickListener {
                                                val bottomSheet = ProgressTrackingBottomSheet(
                                                    this@SubmissionDetailActivity, data
                                                )

                                                if (bottomSheet.window != null)
                                                    bottomSheet.show()
                                            }
                                        }
                                        // On-Trial
                                        31 -> {
                                            submissionStatus.setCardBackgroundColor(
                                                ResourcesCompat.getColor(
                                                    resources,
                                                    R.color.status_on_trial,
                                                    theme
                                                )
                                            )
                                            submissionStatusText.text = "On Trial"
                                            actionButton.visibility = View.GONE
                                            editButton.visibility = View.GONE
                                            seeProgressButton.visibility = View.GONE
                                            onProgressButton.visibility = View.GONE
                                            statusMessageContainer.visibility = View.VISIBLE
                                            statusMessageContainer.setCardBackgroundColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.custom_toast_background_warning
                                                )
                                            )
                                            statusMessage.text =
                                                "The fix is under trial. Wait until it done.\nClick to see progress."
                                            statusMessage.setTextColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.black
                                                )
                                            )
                                            statusMessageContainer.setOnClickListener {
                                                val bottomSheet = ProgressTrackingBottomSheet(
                                                    this@SubmissionDetailActivity, data
                                                )

                                                if (bottomSheet.window != null)
                                                    bottomSheet.show()
                                            }
                                        }
                                    }
                                    submissionDescription.text = data.keterangan
                                    machineCodeText.text = "${getString(R.string.machine_code)}:"
                                    machineNameText.text = "${getString(R.string.machine_name)}:"
                                    machineCode.text = if (data.kodeMesin != null) {
                                        if (data.kodeMesin.isNotEmpty()) data.kodeMesin else "-"
                                    } else "-"
                                    machineName.text = if (data.namaMesin != null) {
                                        if (data.namaMesin.isNotEmpty()) data.namaMesin else "-"
                                    } else "-"
                                    try {
                                        InitAPI.getAPI.getUserDetail(data.idUser!!.toInt())
                                            .enqueue(object : Callback<UserDetailResponse> {
                                                override fun onResponse(
                                                    call: Call<UserDetailResponse?>,
                                                    response: Response<UserDetailResponse?>
                                                ) {
                                                    if (response.isSuccessful) {
                                                        if (response.body() != null) {
                                                            userDetail = UserDetailResponse(
                                                                response.body()!!.stsAktif,
                                                                response.body()!!.nama,
                                                                response.body()!!.usern,
                                                                response.body()!!.idDept,
                                                                response.body()!!.hakAkses,
                                                                response.body()!!.idUser,
                                                                response.body()!!.idUserStarconnect
                                                            )
                                                            user.text =
                                                                userDetail.nama
                                                            actionCondition(data, userDetail)
                                                        }
                                                    } else {
                                                        user.text = "Can't retrieve Reporter's name"
                                                        Log.e("ERROR", response.message())
                                                    }
                                                }

                                                override fun onFailure(
                                                    call: Call<UserDetailResponse?>,
                                                    throwable: Throwable
                                                ) {
                                                    user.text = "Can't retrieve Reporter's name"
                                                    Log.e("ERROR", throwable.toString())
                                                    throwable.printStackTrace()
                                                }
                                            })
                                    } catch (exception: Exception) {
                                        user.text = "Can't retrieve Reporter's name"
                                        Log.e("ERROR", exception.toString())
                                        exception.printStackTrace()
                                    }
                                    department.text = data.deptTujuan
                                    inputTime.text = data.tglInput
                                    location.text = data.lokasi
                                    reportTime.text = if (data.tglWaktuLapor != null) {
                                        if (data.tglWaktuLapor == "") "-" else data.tglWaktuLapor
                                    } else {
                                        "-"
                                    }

                                    startWork.text = if (data.tglWaktuKerjaStart != null) {
                                        if (data.tglWaktuKerjaStart.contains(
                                                "0000-00-00"
                                            ) || data.tglWaktuKerjaStart == ""
                                        ) "-" else data.tglWaktuKerjaStart
                                    } else {
                                        "-"
                                    }

                                    endWork.text = if (data.tglWaktuKerjaEnd != null) {
                                        if (data.tglWaktuKerjaEnd.contains(
                                                "0000-00-00"
                                            ) || data.tglWaktuKerjaEnd == ""
                                        ) "-" else data.tglWaktuKerjaEnd
                                    } else {
                                        "-"
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
                                Log.e("ERROR", response.message())
                                finish()
                            }
                        }

                        override fun onFailure(
                            call: Call<List<SubmissionDetailResponse>?>,
                            throwable: Throwable
                        ) {
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
                            throwable.printStackTrace()
                            Log.e("ERROR", throwable.toString())
                            finish()
                        }
                    })
            } catch (exception: Exception) {
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
                exception.printStackTrace()
                Log.e("ERROR", exception.toString())
                finish()
            }
        }
    }

    private fun actionCondition(data: SubmissionDetailResponse, userDetail: UserDetailResponse) {
        binding.apply {
            InitAPI.getAPI.getUserFromStarConnect(data.idUser!!.toInt())
                .enqueue(object : Callback<StarconnectUserResponse> {
                    override fun onResponse(
                        call: Call<StarconnectUserResponse?>,
                        response: Response<StarconnectUserResponse?>
                    ) {
                        try {
                            if (response.isSuccessful) {
                                if (response.body() != null) {
                                    when (data.stsGaprojects) {
                                        1 -> {
                                            if (response.body()?.mEMORG!!.contains(
                                                    data.deptTujuan.toString()
                                                )
                                            ) {
                                                if (data.idUser.toInt() == userData.id) {
                                                    actionButton.visibility = View.GONE
                                                    editButton.visibility = View.VISIBLE
                                                    onProgressButton.visibility = View.GONE
                                                } else {
                                                    if (userDetail.hakAkses!!.toInt() < 2) {
                                                        actionButton.visibility = View.VISIBLE
                                                        editButton.visibility = View.GONE
                                                        onProgressButton.visibility = View.GONE
                                                    }
                                                }
                                            } else {
                                                if (data.idUser.toInt() == userData.id) {
                                                    actionButton.visibility = View.GONE
                                                    editButton.visibility = View.VISIBLE
                                                    onProgressButton.visibility = View.GONE
                                                } else {
                                                    if (userDetail.hakAkses!!.toInt() < 2) {
                                                        actionButton.visibility = View.GONE
                                                        editButton.visibility = View.GONE
                                                        onProgressButton.visibility = View.GONE
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    actionButton.setOnClickListener {
                                        val bottomSheet = SubmissionActionBottomSheet(
                                            this@SubmissionDetailActivity, data
                                        ).also { bottomSheet ->
                                            with(bottomSheet) {
                                                onUpdateSuccessListener(
                                                    this@SubmissionDetailActivity
                                                )
                                            }
                                        }
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
                                    .setMessage("Something went wrong, please try again later")
                                    .show()
                                Log.e("ERROR", response.message())
                            }
                        } catch (jsonException: JSONException) {
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
                            jsonException.printStackTrace()
                            Log.e("ERROR", jsonException.toString())
                        }
                    }

                    override fun onFailure(
                        call: Call<StarconnectUserResponse?>,
                        throwable: Throwable
                    ) {
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
                        throwable.printStackTrace()
                        Log.e("ERROR", throwable.toString())
                    }
                })
        }
    }

    override fun onUpdateSuccess() {
        init()
    }
}