package com.erela.fixme.activities

import android.annotation.SuppressLint
import com.erela.fixme.R
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.erela.fixme.adapters.pager.ImageCarouselPagerAdapter
import com.erela.fixme.bottom_sheets.ProgressTrackingBottomSheet
import com.erela.fixme.bottom_sheets.ReportTrialBottomSheet
import com.erela.fixme.bottom_sheets.SubmissionActionBottomSheet
import com.erela.fixme.bottom_sheets.TrialTrackingBottomSheet
import com.erela.fixme.bottom_sheets.UpdateStatusBottomSheet
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivitySubmissionDetailBinding
import com.erela.fixme.dialogs.ConfirmationDialog
import com.erela.fixme.dialogs.LoadingDialog
import com.erela.fixme.dialogs.ProgressOptionDialog
import com.erela.fixme.helpers.Base64Helper
import com.erela.fixme.helpers.networking.InitAPI
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.GenericSimpleResponse
import com.erela.fixme.objects.FotoGaprojectsItem
import com.erela.fixme.objects.ProgressItem
import com.erela.fixme.objects.StarconnectUserResponse
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData
import com.erela.fixme.objects.UserDetailResponse
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SubmissionDetailActivity : AppCompatActivity(),
                                 SubmissionActionBottomSheet.OnUpdateSuccessListener,
                                 ProgressTrackingBottomSheet.OnProgressTrackingListener,
                                 ProgressTrackingBottomSheet.OnProgressItemLongTapListener,
                                 ProgressOptionDialog.OnProgressOptionDialogListener,
                                 TrialTrackingBottomSheet.OnTrialTrackingListener {
    private val binding: ActivitySubmissionDetailBinding by lazy {
        ActivitySubmissionDetailBinding.inflate(layoutInflater)
    }
    private lateinit var imageData: ArrayList<FotoGaprojectsItem>
    private lateinit var imageCarouselAdapter: ImageCarouselPagerAdapter
    private lateinit var detailId: String
    private lateinit var userData: UserData
    private lateinit var detailData: SubmissionDetailResponse
    private lateinit var progressTrackingBottomSheet: ProgressTrackingBottomSheet
    private var message: StringBuilder = StringBuilder()
    private var isFabVisible = false
    val activityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            init()
        }
    }

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
        setContentView(binding.root)

        binding.apply {
            enableEdgeToEdge()
            ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            userData = UserDataHelper(applicationContext).getUserData()

            swipeRefresh.setOnRefreshListener {
                init()
                swipeRefresh.isRefreshing = false
            }
        }

        init()
    }

    @SuppressLint("SetTextI18n")
    private fun init() {
        binding.apply {
            isFabVisible = false
            actionSelfButton.extend()
            editButton.hide()
            editButton.shrink()
            cancelButton.hide()
            cancelButton.shrink()
            detailId = intent.getStringExtra(DETAIL_ID).toString()

            backButton.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            loadingBar.visibility = View.VISIBLE
            contentScrollContainer.visibility = View.GONE

            try {
                InitAPI.getAPI.getSubmissionDetail(detailId)
                    .enqueue(object : Callback<List<SubmissionDetailResponse>> {
                        override fun onResponse(
                            call: Call<List<SubmissionDetailResponse>?>,
                            response: Response<List<SubmissionDetailResponse>?>
                        ) {
                            loadingBar.visibility = View.GONE
                            contentScrollContainer.visibility = View.VISIBLE
                            if (response.isSuccessful) {
                                if (response.body() != null) {
                                    val data = response.body()!![0]
                                    detailData = data
                                    Log.e("DATA", data.toString())
                                    detailTitle.text = data.nomorRequest
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
                                            actionSelfButton.visibility = View.GONE
                                            onProgressButton.visibility = View.GONE
                                            messageProgressAndTrialButtonContainer.visibility =
                                                View.VISIBLE
                                            statusMessageContainer.visibility = View.VISIBLE
                                            statusMessageContainer.setCardBackgroundColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.custom_toast_font_failed
                                                )
                                            )
                                            statusMessage.text =
                                                "Rejected by ${data.nameUserReject?.trimEnd()}"
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
                                            onProgressButton.visibility = View.GONE
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
                                            var tech = false
                                            var spv = false
                                            for (technician in data.usernUserTeknisi!!) {
                                                if (technician?.idUser == userData.id) {
                                                    tech = true
                                                    break
                                                }
                                            }

                                            for (supervisor in data.usernUserSpv!!) {
                                                if (supervisor?.idUser == userData.id) {
                                                    spv = true
                                                    break
                                                }
                                            }

                                            if (tech) {
                                                actionButton.visibility = View.VISIBLE
                                                actionSelfButton.visibility = View.GONE
                                                onProgressButton.visibility = View.GONE
                                                messageProgressAndTrialButtonContainer.visibility =
                                                    View.GONE
                                                statusMessageContainer.visibility = View.GONE
                                            } else {
                                                if (spv) {
                                                    actionButton.visibility = View.VISIBLE
                                                    actionSelfButton.visibility = View.GONE
                                                    onProgressButton.visibility = View.GONE
                                                    messageProgressAndTrialButtonContainer.visibility =
                                                        View.GONE
                                                    statusMessageContainer.visibility = View.GONE
                                                } else {
                                                    actionButton.visibility = View.GONE
                                                    actionSelfButton.visibility = View.GONE
                                                    onProgressButton.visibility = View.GONE
                                                    messageProgressAndTrialButtonContainer.visibility =
                                                        View.VISIBLE
                                                    statusMessageContainer.visibility = View.VISIBLE
                                                    statusMessageContainer.setCardBackgroundColor(
                                                        ContextCompat.getColor(
                                                            this@SubmissionDetailActivity,
                                                            R.color.custom_toast_background_soft_blue
                                                        )
                                                    )
                                                    message = StringBuilder().append(
                                                        "Approved by ${data.userNamaApprove?.trimEnd()}\nWaiting for action from "
                                                    )
                                                    if (data.usernUserTeknisi.isNotEmpty()) {
                                                        for (i in 0 until data.usernUserTeknisi.size) {
                                                            if (data.usernUserTeknisi.size > 1) {
                                                                if (i < data.usernUserTeknisi.size - 1) {
                                                                    message.append(
                                                                        "${data.usernUserTeknisi[i]?.namaUser?.trimEnd()} or "
                                                                    )
                                                                } else {
                                                                    message.append(
                                                                        "${data.usernUserTeknisi[i]?.namaUser?.trimEnd()}"
                                                                    )
                                                                }
                                                            } else
                                                                message.append(
                                                                    "${data.usernUserTeknisi[i]?.namaUser?.trimEnd()}"
                                                                )
                                                        }
                                                        statusMessage.text = message.toString()
                                                        statusMessage.setTextColor(
                                                            ContextCompat.getColor(
                                                                this@SubmissionDetailActivity,
                                                                R.color.black
                                                            )
                                                        )
                                                    } else {
                                                        InitAPI.getAPI.getUserDetail(
                                                            data.usernUserSpv[0]?.idUser!!.toInt()
                                                        ).enqueue(object :
                                                            Callback<UserDetailResponse> {
                                                            override fun onResponse(
                                                                call1: Call<UserDetailResponse>,
                                                                response1: Response<UserDetailResponse>
                                                            ) {
                                                                if (response1.isSuccessful) {
                                                                    if (response1.body() != null) {
                                                                        val result =
                                                                            response1.body()!!
                                                                        message.append(
                                                                            "${result.nama?.trimEnd()} to assign technicians"
                                                                        )
                                                                        statusMessage.text =
                                                                            message.toString()
                                                                        statusMessage.setTextColor(
                                                                            ContextCompat.getColor(
                                                                                this@SubmissionDetailActivity,
                                                                                R.color.black
                                                                            )
                                                                        )
                                                                    }
                                                                } else {
                                                                    Log.e(
                                                                        "ERROR ${response1.code()}",
                                                                        "User Detail SPV Response null | ${response1.message()}"
                                                                    )
                                                                }
                                                            }

                                                            override fun onFailure(
                                                                call1: Call<UserDetailResponse>,
                                                                throwable: Throwable
                                                            ) {
                                                                Log.e(
                                                                    "ERROR",
                                                                    "User Detail SPV Failure | $throwable"
                                                                )
                                                                throwable.printStackTrace()
                                                            }
                                                        })
                                                    }
                                                }
                                            }
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
                                            actionSelfButton.visibility = View.GONE
                                            messageProgressAndTrialButtonContainer.visibility =
                                                View.VISIBLE
                                            statusMessageContainer.visibility = View.GONE
                                            onProgressButton.visibility = View.VISIBLE
                                            onProgressText.setCompoundDrawables(
                                                ContextCompat.getDrawable(
                                                    this@SubmissionDetailActivity,
                                                    R.drawable.baseline_timelapse_24
                                                ),
                                                null,
                                                null,
                                                null
                                            )
                                            onProgressButton.setCardBackgroundColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.custom_toast_font_normal_gray
                                                )
                                            )
                                            message = StringBuilder().append("On Progress by ")
                                            for (i in 0 until data.usernUserTeknisi!!.size) {
                                                if (data.usernUserTeknisi.size > 1) {
                                                    if (i < data.usernUserTeknisi.size - 1)
                                                        message.append(
                                                            "${data.usernUserTeknisi[i]?.namaUser?.trimEnd()}, "
                                                        )
                                                    else
                                                        message.append(
                                                            "${data.usernUserTeknisi[i]?.namaUser?.trimEnd()}\nClick to see progress"
                                                        )
                                                } else
                                                    message.append(
                                                        "${data.usernUserTeknisi[i]?.namaUser?.trimEnd()}\nClick to see progress"
                                                    )
                                            }
                                            onProgressText.text = message.toString()
                                            onProgressButton.setOnClickListener {
                                                progressTrackingBottomSheet =
                                                    ProgressTrackingBottomSheet(
                                                        this@SubmissionDetailActivity,
                                                        this@SubmissionDetailActivity, data
                                                    ).also {
                                                        with(it) {
                                                            setOnProgressTrackingListener(
                                                                this@SubmissionDetailActivity
                                                            )
                                                            setOnProgressItemLongTapListener(
                                                                this@SubmissionDetailActivity
                                                            )
                                                        }
                                                    }

                                                if (progressTrackingBottomSheet.window != null)
                                                    progressTrackingBottomSheet.show()
                                            }
                                            if (data.trial!!.isNotEmpty()) {
                                                seeTrialContainer.visibility = View.VISIBLE
                                                seeTrialContainer.setOnClickListener {
                                                    val trialBottomSheet = TrialTrackingBottomSheet(
                                                        this@SubmissionDetailActivity, data
                                                    ).also {
                                                        with(it) {
                                                            setOnTrialTrackingListener(
                                                                this@SubmissionDetailActivity
                                                            )
                                                        }
                                                    }

                                                    if (trialBottomSheet.window != null)
                                                        trialBottomSheet.show()
                                                }
                                            }
                                        }
                                        // Progress Done
                                        30 -> {
                                            submissionStatus.setCardBackgroundColor(
                                                ResourcesCompat.getColor(
                                                    resources,
                                                    R.color.status_progress_done,
                                                    theme
                                                )
                                            )
                                            submissionStatusText.text = "Progress Done"
                                            actionButton.visibility = View.GONE
                                            actionSelfButton.visibility = View.GONE
                                            onProgressButton.visibility = View.VISIBLE
                                            message =
                                                StringBuilder().append(
                                                    "Progress marked as done by "
                                                )
                                            message.append(
                                                "${data.usernUserSpv!![0]?.namaUser?.trimEnd()}\nClick to see progress"
                                            )
                                            onProgressText.text = message.toString()
                                            onProgressText.setTextColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.black
                                                )
                                            )
                                            onProgressText.setCompoundDrawables(
                                                null,
                                                null,
                                                null,
                                                null
                                            )
                                            onProgressButton.setCardBackgroundColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.status_progress_done
                                                )
                                            )
                                            onProgressButton.setOnClickListener {
                                                progressTrackingBottomSheet =
                                                    ProgressTrackingBottomSheet(
                                                        this@SubmissionDetailActivity,
                                                        this@SubmissionDetailActivity, data
                                                    ).also {
                                                        with(it) {
                                                            setOnProgressTrackingListener(
                                                                this@SubmissionDetailActivity
                                                            )
                                                            setOnProgressItemLongTapListener(
                                                                this@SubmissionDetailActivity
                                                            )
                                                        }
                                                    }

                                                if (progressTrackingBottomSheet.window != null)
                                                    progressTrackingBottomSheet.show()
                                            }
                                            if (data.trial!!.isNotEmpty()) {
                                                seeTrialContainer.visibility = View.VISIBLE
                                                seeTrialContainer.setOnClickListener {
                                                    val trialBottomSheet = TrialTrackingBottomSheet(
                                                        this@SubmissionDetailActivity, data
                                                    ).also {
                                                        with(it) {
                                                            setOnTrialTrackingListener(
                                                                this@SubmissionDetailActivity
                                                            )
                                                        }
                                                    }

                                                    if (trialBottomSheet.window != null)
                                                        trialBottomSheet.show()
                                                }
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
                                            actionSelfButton.visibility = View.GONE
                                            onProgressButton.visibility = View.GONE
                                            messageProgressAndTrialButtonContainer.visibility =
                                                View.VISIBLE
                                            statusMessageContainer.visibility = View.VISIBLE
                                            statusMessageContainer.setCardBackgroundColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.custom_toast_font_success
                                                )
                                            )
                                            statusMessage.text =
                                                "Done by ${data.nameUserDone?.trimEnd()}\nClick to see progress"
                                            statusMessage.setTextColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.white
                                                )
                                            )
                                            statusMessageContainer.setOnClickListener {
                                                progressTrackingBottomSheet =
                                                    ProgressTrackingBottomSheet(
                                                        this@SubmissionDetailActivity,
                                                        this@SubmissionDetailActivity, data
                                                    ).also {
                                                        with(it) {
                                                            setOnProgressTrackingListener(
                                                                this@SubmissionDetailActivity
                                                            )
                                                            setOnProgressItemLongTapListener(
                                                                this@SubmissionDetailActivity
                                                            )
                                                        }
                                                    }

                                                if (progressTrackingBottomSheet.window != null)
                                                    progressTrackingBottomSheet.show()
                                            }
                                            seeTrialContainer.visibility = View.VISIBLE
                                            seeTrialContainer.setOnClickListener {
                                                val trialBottomSheet = TrialTrackingBottomSheet(
                                                    this@SubmissionDetailActivity, data
                                                ).also {
                                                    with(it) {
                                                        setOnTrialTrackingListener(
                                                            this@SubmissionDetailActivity
                                                        )
                                                    }
                                                }

                                                if (trialBottomSheet.window != null)
                                                    trialBottomSheet.show()
                                            }
                                        }
                                        // Canceled
                                        5 -> {
                                            submissionStatus.setCardBackgroundColor(
                                                ResourcesCompat.getColor(
                                                    resources,
                                                    R.color.custom_toast_background_failed,
                                                    theme
                                                )
                                            )
                                            submissionStatusText.text = "Canceled"
                                            submissionStatusText.setTextColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.black
                                                )
                                            )
                                            actionButton.visibility = View.GONE
                                            actionSelfButton.visibility = View.GONE
                                            onProgressButton.visibility = View.GONE
                                            messageProgressAndTrialButtonContainer.visibility =
                                                View.VISIBLE
                                            statusMessageContainer.visibility = View.VISIBLE
                                            statusMessageContainer.setCardBackgroundColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.custom_toast_background_failed
                                                )
                                            )
                                            statusMessage.text = if (userData.id == data.idUser)
                                                "Canceled by You"
                                            else
                                                "Canceled by the reporter, ${data.namaUserBuat?.trimEnd()}"
                                            statusMessage.setTextColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.black
                                                )
                                            )
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
                                            actionSelfButton.visibility = View.GONE
                                            onProgressButton.visibility = View.GONE
                                            messageProgressAndTrialButtonContainer.visibility =
                                                View.VISIBLE
                                            statusMessageContainer.visibility = View.VISIBLE
                                            statusMessageContainer.setCardBackgroundColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.custom_toast_background_warning
                                                )
                                            )
                                            statusMessage.text =
                                                "The fix is under trial. Wait until it done.\nClick to see progress"
                                            statusMessage.setTextColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.black
                                                )
                                            )
                                            statusMessageContainer.setOnClickListener {
                                                progressTrackingBottomSheet =
                                                    ProgressTrackingBottomSheet(
                                                        this@SubmissionDetailActivity,
                                                        this@SubmissionDetailActivity, data
                                                    ).also {
                                                        with(it) {
                                                            setOnProgressTrackingListener(
                                                                this@SubmissionDetailActivity
                                                            )
                                                            setOnProgressItemLongTapListener(
                                                                this@SubmissionDetailActivity
                                                            )
                                                        }
                                                    }

                                                if (progressTrackingBottomSheet.window != null)
                                                    progressTrackingBottomSheet.show()
                                            }
                                            seeTrialContainer.visibility = View.VISIBLE
                                            seeTrialContainer.setOnClickListener {
                                                val trialBottomSheet = TrialTrackingBottomSheet(
                                                    this@SubmissionDetailActivity, data
                                                ).also {
                                                    with(it) {
                                                        setOnTrialTrackingListener(
                                                            this@SubmissionDetailActivity
                                                        )
                                                    }
                                                }

                                                if (trialBottomSheet.window != null)
                                                    trialBottomSheet.show()
                                            }
                                        }
                                    }
                                    submissionDescription.text = data.keterangan
                                    machineCodeText.text = "${getString(R.string.machine_code)}:"
                                    machineNameText.text = "${getString(R.string.machine_name)}:"
                                    machineCode.text = if (data.kodeMesin != null) {
                                        data.kodeMesin.ifEmpty { "-" }
                                    } else "-"
                                    machineName.text = if (data.namaMesin != null) {
                                        data.namaMesin.ifEmpty { "-" }
                                    } else "-"
                                    user.text = data.namaUserBuat?.trimEnd()
                                    actionCondition(data)
                                    /*try {
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
                                                            user.text = data.namaUserBuat?.trimEnd()
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
                                    }*/
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
                                Log.e(
                                    "ERROR",
                                    "Submission Detail Response null | ${response.message()}"
                                )
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
                            Log.e("ERROR", "Submission Detail Failure | $throwable")
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
                Log.e("ERROR", "Submission Detail Exception | $exception")
                finish()
            }
        }
    }

    private fun actionCondition(data: SubmissionDetailResponse) {
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
                                                    actionSelfButton.visibility = View.VISIBLE
                                                    actionSelfButton.extend()
                                                    onProgressButton.visibility = View.GONE
                                                } else {
                                                    if (userData.privilege < 2) {
                                                        actionButton.visibility = View.VISIBLE
                                                        actionSelfButton.visibility = View.GONE
                                                        onProgressButton.visibility = View.GONE
                                                    }
                                                }
                                            } else {
                                                if (data.idUser.toInt() == userData.id) {
                                                    actionButton.visibility = View.GONE
                                                    actionSelfButton.visibility = View.VISIBLE
                                                    actionSelfButton.extend()
                                                    onProgressButton.visibility = View.GONE
                                                } else {
                                                    if (userData.privilege < 2) {
                                                        actionButton.visibility = View.GONE
                                                        actionSelfButton.visibility = View.GONE
                                                        onProgressButton.visibility = View.GONE
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    actionSelfButton.setOnClickListener {
                                        if (!isFabVisible) {
                                            actionSelfButton.shrink()
                                            editButton.show()
                                            editButton.extend()
                                            cancelButton.show()
                                            cancelButton.extend()

                                            editButton.setOnClickListener {
                                                activityResultLauncher.launch(
                                                    Intent(
                                                        this@SubmissionDetailActivity,
                                                        SubmissionFormActivity::class.java
                                                    ).also {
                                                        with(it) {
                                                            putExtra("data", data)
                                                        }
                                                    }
                                                )
                                            }

                                            cancelButton.setOnClickListener {
                                                actionSelfButton.extend()
                                                editButton.hide()
                                                editButton.shrink()
                                                cancelButton.hide()
                                                cancelButton.shrink()
                                                isFabVisible = false
                                                val bottomSheet = UpdateStatusBottomSheet(
                                                    this@SubmissionDetailActivity,
                                                    data,
                                                    approve = false,
                                                    cancel = true,
                                                    deployTech = false
                                                ).also { bs ->
                                                    with(bs) {
                                                        setOnUpdateSuccessListener(object : UpdateStatusBottomSheet.OnUpdateSuccessListener {
                                                            override fun onApproved() {}

                                                            override fun onRejected() {}

                                                            override fun onCanceled() {
                                                                init()
                                                            }

                                                            override fun onTechniciansDeployed() {}
                                                        })
                                                    }
                                                }

                                                if (bottomSheet.window != null)
                                                    bottomSheet.show()
                                            }

                                            isFabVisible = true
                                        } else {
                                            actionSelfButton.extend()
                                            editButton.hide()
                                            editButton.shrink()
                                            cancelButton.hide()
                                            cancelButton.shrink()
                                            isFabVisible = false
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
                                Log.e(
                                    "ERROR",
                                    "Starconnect User Response null | ${response.message()}"
                                )
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
                            Log.e("ERROR", "Starconnect User Exception | $jsonException")
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
                        Log.e("ERROR", "Starconnect User Failure | $throwable")
                    }
                })
        }
    }

    override fun onUpdateSuccess() {
        init()
    }

    override fun createProgressClicked() {
        progressTrackingBottomSheet.dismiss()
        activityResultLauncher.launch(
            Intent(
                this@SubmissionDetailActivity,
                ProgressFormActivity::class.java
            ).also {
                with(it) {
                    putExtra("data", detailData)
                }
            }
        )
    }

    override fun readyForTrialClicked() {
        val confirmationDialog =
            ConfirmationDialog(
                this@SubmissionDetailActivity,
                "Are you sure you want to mark this issue ready for trial?",
                "Yes"
            ).also {
                with(it) {
                    setConfirmationDialogListener(object :
                        ConfirmationDialog.ConfirmationDialogListener {
                        override fun onConfirm() {
                            progressTrackingBottomSheet.dismiss()
                            val loadingDialog = LoadingDialog(this@SubmissionDetailActivity)
                            if (loadingDialog.window != null)
                                loadingDialog.show()
                            try {
                                InitAPI.getAPI.markAsReadyForTrial(
                                    detailData.idGaprojects!!,
                                    userData.id
                                )
                                    .enqueue(object :
                                        Callback<GenericSimpleResponse> {
                                        override fun onResponse(
                                            call: Call<GenericSimpleResponse>,
                                            response: Response<GenericSimpleResponse>
                                        ) {
                                            loadingDialog.dismiss()
                                            if (response.isSuccessful) {
                                                if (response.body() != null) {
                                                    val result = response.body()
                                                    if (result?.code == 1) {
                                                        CustomToast.getInstance(applicationContext)
                                                            .setBackgroundColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_background_success,
                                                                    theme
                                                                )
                                                            )
                                                            .setFontColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_font_success,
                                                                    theme
                                                                )
                                                            )
                                                            .setMessage(
                                                                "Issue marked as ready for Trial!"
                                                            ).show()
                                                        init()
                                                    } else {
                                                        CustomToast.getInstance(applicationContext)
                                                            .setBackgroundColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_background_failed,
                                                                    theme
                                                                )
                                                            )
                                                            .setFontColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_font_failed,
                                                                    theme
                                                                )
                                                            )
                                                            .setMessage(
                                                                "Failed to mark as ready for Trial"
                                                            )
                                                            .show()
                                                        Log.e(
                                                            "ERROR ${response.code()}",
                                                            "Mark Ready Trial Response code 0 | ${response.message()}"
                                                        )
                                                    }
                                                } else {
                                                    CustomToast.getInstance(applicationContext)
                                                        .setBackgroundColor(
                                                            ResourcesCompat.getColor(
                                                                resources,
                                                                R.color.custom_toast_background_failed,
                                                                theme
                                                            )
                                                        )
                                                        .setFontColor(
                                                            ResourcesCompat.getColor(
                                                                resources,
                                                                R.color.custom_toast_font_failed,
                                                                theme
                                                            )
                                                        )
                                                        .setMessage(
                                                            "Failed to mark as ready for trial"
                                                        )
                                                        .show()
                                                    Log.e(
                                                        "ERROR ${response.code()}",
                                                        "Mark Ready Trial Response null | ${response.message()}"
                                                    )
                                                }
                                            } else {
                                                CustomToast.getInstance(applicationContext)
                                                    .setBackgroundColor(
                                                        ResourcesCompat.getColor(
                                                            resources,
                                                            R.color.custom_toast_background_failed,
                                                            theme
                                                        )
                                                    )
                                                    .setFontColor(
                                                        ResourcesCompat.getColor(
                                                            resources,
                                                            R.color.custom_toast_font_failed, theme
                                                        )
                                                    )
                                                    .setMessage("Failed to mark as ready for trial")
                                                    .show()
                                                Log.e(
                                                    "ERROR ${response.code()}",
                                                    "Mark Ready Trial Response Fail | ${response.message()}"
                                                )
                                            }
                                        }

                                        override fun onFailure(
                                            call: Call<GenericSimpleResponse>,
                                            throwable: Throwable
                                        ) {
                                            loadingDialog.dismiss()
                                            CustomToast.getInstance(applicationContext)
                                                .setBackgroundColor(
                                                    ResourcesCompat.getColor(
                                                        resources,
                                                        R.color.custom_toast_background_failed,
                                                        theme
                                                    )
                                                )
                                                .setFontColor(
                                                    ResourcesCompat.getColor(
                                                        resources, R.color.custom_toast_font_failed,
                                                        theme
                                                    )
                                                )
                                                .setMessage(
                                                    "Something went wrong, please try again later"
                                                )
                                                .show()
                                            throwable.printStackTrace()
                                            Log.e("ERROR", "Mark Ready Trial Failure | $throwable")
                                        }
                                    })
                            } catch (jsonException: JSONException) {
                                loadingDialog.dismiss()
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
                                Log.e("ERROR", "Mark Ready Trial Exception | $jsonException")
                            }
                        }
                    })
                }
            }
        if (confirmationDialog.window != null)
            confirmationDialog.show()
    }

    override fun startTrialClicked() {
        val confirmationDialog =
            ConfirmationDialog(
                this@SubmissionDetailActivity,
                "Are you sure you want to start trial?",
                "Yes"
            ).also {
                with(it) {
                    setConfirmationDialogListener(object :
                        ConfirmationDialog.ConfirmationDialogListener {
                        override fun onConfirm() {
                            progressTrackingBottomSheet.dismiss()
                            val loadingDialog = LoadingDialog(this@SubmissionDetailActivity)
                            if (loadingDialog.window != null)
                                loadingDialog.show()
                            try {
                                InitAPI.getAPI.startTrial(detailData.idGaprojects!!, userData.id)
                                    .enqueue(object :
                                        Callback<GenericSimpleResponse> {
                                        override fun onResponse(
                                            call: Call<GenericSimpleResponse>,
                                            response: Response<GenericSimpleResponse>
                                        ) {
                                            loadingDialog.dismiss()
                                            if (response.isSuccessful) {
                                                if (response.body() != null) {
                                                    val result = response.body()
                                                    if (result?.code == 1) {
                                                        CustomToast.getInstance(applicationContext)
                                                            .setBackgroundColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_background_success,
                                                                    theme
                                                                )
                                                            )
                                                            .setFontColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_font_success,
                                                                    theme
                                                                )
                                                            )
                                                            .setMessage(
                                                                "Trial started!"
                                                            ).show()
                                                        init()
                                                    } else {
                                                        CustomToast.getInstance(applicationContext)
                                                            .setBackgroundColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_background_failed,
                                                                    theme
                                                                )
                                                            )
                                                            .setFontColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_font_failed,
                                                                    theme
                                                                )
                                                            )
                                                            .setMessage("Failed to start Trial")
                                                            .show()
                                                        Log.e(
                                                            "ERROR ${response.code()}",
                                                            "Start Trial Response code 0 | ${response.message()}"
                                                        )
                                                    }
                                                } else {
                                                    CustomToast.getInstance(applicationContext)
                                                        .setBackgroundColor(
                                                            ResourcesCompat.getColor(
                                                                resources,
                                                                R.color.custom_toast_background_failed,
                                                                theme
                                                            )
                                                        )
                                                        .setFontColor(
                                                            ResourcesCompat.getColor(
                                                                resources,
                                                                R.color.custom_toast_font_failed,
                                                                theme
                                                            )
                                                        )
                                                        .setMessage("Failed to start trial")
                                                        .show()
                                                    Log.e(
                                                        "ERROR ${response.code()}",
                                                        "Start Trial Response null | ${response.message()}"
                                                    )
                                                }
                                            } else {
                                                CustomToast.getInstance(applicationContext)
                                                    .setBackgroundColor(
                                                        ResourcesCompat.getColor(
                                                            resources,
                                                            R.color.custom_toast_background_failed,
                                                            theme
                                                        )
                                                    )
                                                    .setFontColor(
                                                        ResourcesCompat.getColor(
                                                            resources,
                                                            R.color.custom_toast_font_failed, theme
                                                        )
                                                    )
                                                    .setMessage("Failed to start trial")
                                                    .show()
                                                Log.e(
                                                    "ERROR ${response.code()}",
                                                    "Start Trial Response Fail | ${response.message()}"
                                                )
                                            }
                                        }

                                        override fun onFailure(
                                            call: Call<GenericSimpleResponse>,
                                            throwable: Throwable
                                        ) {
                                            loadingDialog.dismiss()
                                            CustomToast.getInstance(applicationContext)
                                                .setBackgroundColor(
                                                    ResourcesCompat.getColor(
                                                        resources,
                                                        R.color.custom_toast_background_failed,
                                                        theme
                                                    )
                                                )
                                                .setFontColor(
                                                    ResourcesCompat.getColor(
                                                        resources, R.color.custom_toast_font_failed,
                                                        theme
                                                    )
                                                )
                                                .setMessage(
                                                    "Something went wrong, please try again later"
                                                )
                                                .show()
                                            throwable.printStackTrace()
                                            Log.e("ERROR", "Start Trial Failure | $throwable")
                                        }
                                    })
                            } catch (jsonException: JSONException) {
                                loadingDialog.dismiss()
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
                                Log.e("ERROR", "Start Trial Exception | $jsonException")
                            }
                        }
                    })
                }
            }
        if (confirmationDialog.window != null)
            confirmationDialog.show()
    }

    override fun onLongTapListener(data: ProgressItem?) {
        val dialog = ProgressOptionDialog(this@SubmissionDetailActivity, data!!).also {
            with(it) {
                setOnProgressOptionDialogListener(this@SubmissionDetailActivity)
            }
        }

        if (dialog.window != null)
            dialog.show()
    }

    override fun onProgressDeleted(data: ProgressItem) {
        val confirmationDialog =
            ConfirmationDialog(
                this@SubmissionDetailActivity,
                "Are you sure you want to delete this progress?",
                "Yes"
            ).also {
                with(it) {
                    setConfirmationDialogListener(object :
                        ConfirmationDialog.ConfirmationDialogListener {
                        override fun onConfirm() {
                            progressTrackingBottomSheet.dismiss()
                            val loadingDialog = LoadingDialog(this@SubmissionDetailActivity)
                            if (loadingDialog.window != null)
                                loadingDialog.show()
                            try {
                                InitAPI.getAPI.deleteProgress(
                                    data.idGaprojectsDetail!!, userData.id
                                )
                                    .enqueue(object :
                                        Callback<GenericSimpleResponse> {
                                        override fun onResponse(
                                            call: Call<GenericSimpleResponse>,
                                            response: Response<GenericSimpleResponse>
                                        ) {
                                            loadingDialog.dismiss()
                                            if (response.isSuccessful) {
                                                if (response.body() != null) {
                                                    val result = response.body()
                                                    if (result?.code == 1) {
                                                        CustomToast.getInstance(applicationContext)
                                                            .setBackgroundColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_background_success,
                                                                    theme
                                                                )
                                                            )
                                                            .setFontColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_font_success,
                                                                    theme
                                                                )
                                                            )
                                                            .setMessage(
                                                                "Progress deleted successfully!"
                                                            ).show()
                                                        init()
                                                    } else {
                                                        CustomToast.getInstance(applicationContext)
                                                            .setBackgroundColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_background_failed,
                                                                    theme
                                                                )
                                                            )
                                                            .setFontColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_font_failed,
                                                                    theme
                                                                )
                                                            )
                                                            .setMessage("Failed to delete progress")
                                                            .show()
                                                        Log.e(
                                                            "ERROR ${response.code()}",
                                                            "Delete Progress Response code 0 | ${response.message()}"
                                                        )
                                                    }
                                                } else {
                                                    CustomToast.getInstance(applicationContext)
                                                        .setBackgroundColor(
                                                            ResourcesCompat.getColor(
                                                                resources,
                                                                R.color.custom_toast_background_failed,
                                                                theme
                                                            )
                                                        )
                                                        .setFontColor(
                                                            ResourcesCompat.getColor(
                                                                resources,
                                                                R.color.custom_toast_font_failed,
                                                                theme
                                                            )
                                                        )
                                                        .setMessage("Failed to delete progress")
                                                        .show()
                                                    Log.e(
                                                        "ERROR ${response.code()}",
                                                        "Delete Progress Response null | ${response.message()}"
                                                    )
                                                }
                                            } else {
                                                CustomToast.getInstance(applicationContext)
                                                    .setBackgroundColor(
                                                        ResourcesCompat.getColor(
                                                            resources,
                                                            R.color.custom_toast_background_failed,
                                                            theme
                                                        )
                                                    )
                                                    .setFontColor(
                                                        ResourcesCompat.getColor(
                                                            resources,
                                                            R.color.custom_toast_font_failed, theme
                                                        )
                                                    )
                                                    .setMessage("Failed to delete progress")
                                                    .show()
                                                Log.e(
                                                    "ERROR ${response.code()}",
                                                    "Delete Progress Response Fail | ${response.message()}"
                                                )
                                            }
                                        }

                                        override fun onFailure(
                                            call: Call<GenericSimpleResponse>,
                                            throwable: Throwable
                                        ) {
                                            loadingDialog.dismiss()
                                            CustomToast.getInstance(applicationContext)
                                                .setBackgroundColor(
                                                    ResourcesCompat.getColor(
                                                        resources,
                                                        R.color.custom_toast_background_failed,
                                                        theme
                                                    )
                                                )
                                                .setFontColor(
                                                    ResourcesCompat.getColor(
                                                        resources, R.color.custom_toast_font_failed,
                                                        theme
                                                    )
                                                )
                                                .setMessage(
                                                    "Something went wrong, please try again later"
                                                )
                                                .show()
                                            throwable.printStackTrace()
                                            Log.e("ERROR", "Delete Progress Failure | $throwable")
                                        }
                                    })
                            } catch (jsonException: JSONException) {
                                loadingDialog.dismiss()
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
                                Log.e("ERROR", "Delete Progress Exception | $jsonException")
                            }
                        }
                    })
                }
            }

        if (confirmationDialog.window != null)
            confirmationDialog.show()
    }

    override fun onProgressEdited(data: ProgressItem) {
        progressTrackingBottomSheet.dismiss()
        activityResultLauncher.launch(
            Intent(
                this@SubmissionDetailActivity,
                ProgressFormActivity::class.java
            ).also {
                with(it) {
                    putExtra("data", data)
                }
            }
        )
    }

    override fun onProgressSetDone(data: ProgressItem) {
        val confirmationDialog =
            ConfirmationDialog(
                this@SubmissionDetailActivity,
                "Are you sure you want to mark this progress as done?\n\nMake sure your progress are totally done before marking it as done",
                "Yes"
            ).also {
                with(it) {
                    setConfirmationDialogListener(object :
                        ConfirmationDialog.ConfirmationDialogListener {
                        override fun onConfirm() {
                            progressTrackingBottomSheet.dismiss()
                            val loadingDialog = LoadingDialog(this@SubmissionDetailActivity)
                            if (loadingDialog.window != null)
                                loadingDialog.show()
                            try {
                                InitAPI.getAPI.markProgressDone(
                                    data.idGaprojectsDetail!!, userData.id
                                ).enqueue(object :
                                    Callback<GenericSimpleResponse> {
                                    override fun onResponse(
                                        call: Call<GenericSimpleResponse>,
                                        response: Response<GenericSimpleResponse>
                                    ) {
                                        loadingDialog.dismiss()
                                        if (response.isSuccessful) {
                                            if (response.body() != null) {
                                                val result = response.body()
                                                if (result?.code == 1) {
                                                    CustomToast.getInstance(applicationContext)
                                                        .setBackgroundColor(
                                                            ResourcesCompat.getColor(
                                                                resources,
                                                                R.color.custom_toast_background_success,
                                                                theme
                                                            )
                                                        )
                                                        .setFontColor(
                                                            ResourcesCompat.getColor(
                                                                resources,
                                                                R.color.custom_toast_font_success,
                                                                theme
                                                            )
                                                        )
                                                        .setMessage(
                                                            "Progress marked as done successfully!"
                                                        ).show()
                                                    init()
                                                } else {
                                                    CustomToast.getInstance(applicationContext)
                                                        .setBackgroundColor(
                                                            ResourcesCompat.getColor(
                                                                resources,
                                                                R.color.custom_toast_background_failed,
                                                                theme
                                                            )
                                                        )
                                                        .setFontColor(
                                                            ResourcesCompat.getColor(
                                                                resources,
                                                                R.color.custom_toast_font_failed,
                                                                theme
                                                            )
                                                        )
                                                        .setMessage(
                                                            "Failed to mark progress as done"
                                                        )
                                                        .show()
                                                    Log.e(
                                                        "ERROR ${response.code()}",
                                                        "Mark Progress Done Response code 0 | ${response.message()}"
                                                    )
                                                }
                                            } else {
                                                CustomToast.getInstance(applicationContext)
                                                    .setBackgroundColor(
                                                        ResourcesCompat.getColor(
                                                            resources,
                                                            R.color.custom_toast_background_failed,
                                                            theme
                                                        )
                                                    )
                                                    .setFontColor(
                                                        ResourcesCompat.getColor(
                                                            resources,
                                                            R.color.custom_toast_font_failed,
                                                            theme
                                                        )
                                                    )
                                                    .setMessage("Failed to mark progress as done")
                                                    .show()
                                                Log.e(
                                                    "ERROR ${response.code()}",
                                                    "Mark Progress Done Response null | ${response.message()}"
                                                )
                                            }
                                        } else {
                                            CustomToast.getInstance(applicationContext)
                                                .setBackgroundColor(
                                                    ResourcesCompat.getColor(
                                                        resources,
                                                        R.color.custom_toast_background_failed,
                                                        theme
                                                    )
                                                )
                                                .setFontColor(
                                                    ResourcesCompat.getColor(
                                                        resources,
                                                        R.color.custom_toast_font_failed, theme
                                                    )
                                                )
                                                .setMessage("Failed to mark progress as done")
                                                .show()
                                            Log.e(
                                                "ERROR ${response.code()}",
                                                "Mark Progress Done Response Fail | ${response.message()}"
                                            )
                                        }
                                    }

                                    override fun onFailure(
                                        call: Call<GenericSimpleResponse>,
                                        throwable: Throwable
                                    ) {
                                        loadingDialog.dismiss()
                                        CustomToast.getInstance(applicationContext)
                                            .setBackgroundColor(
                                                ResourcesCompat.getColor(
                                                    resources,
                                                    R.color.custom_toast_background_failed,
                                                    theme
                                                )
                                            )
                                            .setFontColor(
                                                ResourcesCompat.getColor(
                                                    resources, R.color.custom_toast_font_failed,
                                                    theme
                                                )
                                            )
                                            .setMessage(
                                                "Something went wrong, please try again later"
                                            )
                                            .show()
                                        throwable.printStackTrace()
                                        Log.e("ERROR", "Mark Progress Done Failure | $throwable")
                                    }
                                })
                            } catch (jsonException: JSONException) {
                                loadingDialog.dismiss()
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
                                Log.e("ERROR", "Mark Progress Done Exception | $jsonException")
                            }
                        }
                    })
                }
            }
        if (confirmationDialog.window != null)
            confirmationDialog.show()
    }

    override fun reportTrialClicked(bottomSheet: TrialTrackingBottomSheet) {
        val reportTrialBs =
            ReportTrialBottomSheet(
                this@SubmissionDetailActivity,
                detailData
            ).also { reportTrial ->
                with(reportTrial) {
                    setOnReportTrialSuccessListener(
                        object :
                            ReportTrialBottomSheet.OnReportTrialSuccessListener {
                            override fun reportTrialSuccess() {
                                dismiss()
                                bottomSheet.dismiss()
                                init()
                            }
                        }
                    )
                }
            }

        if (reportTrialBs.window != null)
            reportTrialBs.show()
    }

    override fun markIssueDoneClicked(bottomSheet: TrialTrackingBottomSheet) {
        val confirmationDialog =
            ConfirmationDialog(
                this@SubmissionDetailActivity,
                "Are you sure you want to mark this as done?\n\nMake sure your issue are working properly",
                "Yes"
            ).also {
                with(it) {
                    setConfirmationDialogListener(object :
                        ConfirmationDialog.ConfirmationDialogListener {
                        override fun onConfirm() {
                            dismiss()
                            bottomSheet.dismiss()
                            val loadingDialog = LoadingDialog(this@SubmissionDetailActivity)
                            if (loadingDialog.window != null)
                                loadingDialog.show()
                            try {
                                InitAPI.getAPI.markIssueDone(detailData.idGaprojects!!, userData.id)
                                    .enqueue(object : Callback<GenericSimpleResponse> {
                                        override fun onResponse(
                                            call: Call<GenericSimpleResponse>,
                                            response: Response<GenericSimpleResponse>
                                        ) {
                                            loadingDialog.dismiss()
                                            if (response.isSuccessful) {
                                                if (response.body() != null) {
                                                    val result = response.body()
                                                    if (result?.code == 1) {
                                                        CustomToast.getInstance(applicationContext)
                                                            .setBackgroundColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_background_success,
                                                                    theme
                                                                )
                                                            )
                                                            .setFontColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_font_success,
                                                                    theme
                                                                )
                                                            )
                                                            .setMessage(
                                                                "Marked as done successfully!"
                                                            ).show()
                                                        init()
                                                    } else {
                                                        CustomToast.getInstance(applicationContext)
                                                            .setBackgroundColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_background_failed,
                                                                    theme
                                                                )
                                                            )
                                                            .setFontColor(
                                                                ResourcesCompat.getColor(
                                                                    resources,
                                                                    R.color.custom_toast_font_failed,
                                                                    theme
                                                                )
                                                            )
                                                            .setMessage("Failed to mark as done")
                                                            .show()
                                                        Log.e(
                                                            "ERROR ${response.code()}",
                                                            "Mark Done Response code 0 | ${response.message()}"
                                                        )
                                                    }
                                                } else {
                                                    CustomToast.getInstance(applicationContext)
                                                        .setBackgroundColor(
                                                            ResourcesCompat.getColor(
                                                                resources,
                                                                R.color.custom_toast_background_failed,
                                                                theme
                                                            )
                                                        )
                                                        .setFontColor(
                                                            ResourcesCompat.getColor(
                                                                resources,
                                                                R.color.custom_toast_font_failed,
                                                                theme
                                                            )
                                                        )
                                                        .setMessage("Failed to mark as done")
                                                        .show()
                                                    Log.e(
                                                        "ERROR ${response.code()}",
                                                        "Mark Done Response null | ${response.message()}"
                                                    )
                                                }
                                            } else {
                                                CustomToast.getInstance(applicationContext)
                                                    .setBackgroundColor(
                                                        ResourcesCompat.getColor(
                                                            resources,
                                                            R.color.custom_toast_background_failed,
                                                            theme
                                                        )
                                                    )
                                                    .setFontColor(
                                                        ResourcesCompat.getColor(
                                                            resources,
                                                            R.color.custom_toast_font_failed, theme
                                                        )
                                                    )
                                                    .setMessage("Failed to mark as done")
                                                    .show()
                                                Log.e(
                                                    "ERROR ${response.code()}",
                                                    "Mark Done Fail | ${response.message()}"
                                                )
                                            }
                                        }

                                        override fun onFailure(
                                            call: Call<GenericSimpleResponse>, throwable: Throwable
                                        ) {
                                            loadingDialog.dismiss()
                                            CustomToast.getInstance(applicationContext)
                                                .setBackgroundColor(
                                                    ResourcesCompat.getColor(
                                                        resources,
                                                        R.color.custom_toast_background_failed,
                                                        theme
                                                    )
                                                )
                                                .setFontColor(
                                                    ResourcesCompat.getColor(
                                                        resources, R.color.custom_toast_font_failed,
                                                        theme
                                                    )
                                                )
                                                .setMessage(
                                                    "Something went wrong, please try again later"
                                                )
                                                .show()
                                            throwable.printStackTrace()
                                            Log.e("ERROR", "Mark Done Failure | $throwable")
                                        }
                                    })
                            } catch (jsonException: JSONException) {
                                loadingDialog.dismiss()
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
                                Log.e("ERROR", "Mark Done Exception | $jsonException")
                            }
                        }
                    })
                }
            }
        if (confirmationDialog.window != null)
            confirmationDialog.show()
    }
}