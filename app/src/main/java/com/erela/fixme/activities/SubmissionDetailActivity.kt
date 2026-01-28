package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.erela.fixme.R
import com.erela.fixme.adapters.pager.ImageCarouselPagerAdapter
import com.erela.fixme.bottom_sheets.ActionHoldIssueBottomSheet
import com.erela.fixme.bottom_sheets.ProgressTrackingBottomSheet
import com.erela.fixme.bottom_sheets.ReportTrialBottomSheet
import com.erela.fixme.bottom_sheets.SubmissionActionBottomSheet
import com.erela.fixme.bottom_sheets.TrialTrackingBottomSheet
import com.erela.fixme.bottom_sheets.UpdateStatusBottomSheet
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.custom_views.zoom_helper.ImageZoomHelper
import com.erela.fixme.databinding.ActivitySubmissionDetailBinding
import com.erela.fixme.dialogs.ConfirmationDialog
import com.erela.fixme.dialogs.LoadingDialog
import com.erela.fixme.dialogs.ProgressOptionDialog
import com.erela.fixme.helpers.Base64Helper
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.FotoGaprojectsItem
import com.erela.fixme.objects.GenericSimpleResponse
import com.erela.fixme.objects.ProgressItems
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignTop
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import kotlin.properties.Delegates

class SubmissionDetailActivity : AppCompatActivity(),
    SubmissionActionBottomSheet.OnButtonActionClickedListener,
    ProgressTrackingBottomSheet.OnProgressTrackingListener,
    ProgressTrackingBottomSheet.OnProgressItemLongTapListener,
    ProgressOptionDialog.OnProgressOptionDialogListener,
    TrialTrackingBottomSheet.OnTrialTrackingListener,
    OnMapReadyCallback {
    private val binding: ActivitySubmissionDetailBinding by lazy {
        ActivitySubmissionDetailBinding.inflate(layoutInflater)
    }
    private val userData: UserData by lazy {
        UserDataHelper(applicationContext).getUserData()
    }
    private lateinit var imageData: ArrayList<FotoGaprojectsItem>
    private lateinit var imageCarouselAdapter: ImageCarouselPagerAdapter
    private lateinit var detailId: String
    private var notificationId by Delegates.notNull<Int>()
    private lateinit var detailData: SubmissionDetailResponse
    private lateinit var progressTrackingBottomSheet: ProgressTrackingBottomSheet
    private var message: StringBuilder = StringBuilder()
    private var googleMap: GoogleMap? = null
    private var isFabVisible = false
    private var isUpdated = false
    private val activityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            isUpdated = true
            init()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            if (isFabVisible) {
                val outRect = Rect()
                binding.actionSelfButton.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    val editRect = Rect()
                    binding.editButton.getGlobalVisibleRect(editRect)
                    val cancelRect = Rect()
                    binding.cancelButton.getGlobalVisibleRect(cancelRect)

                    if (!editRect.contains(ev.rawX.toInt(), ev.rawY.toInt()) &&
                        !cancelRect.contains(ev.rawX.toInt(), ev.rawY.toInt())
                    ) {
                        closeFabMenu()
                        return true
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    companion object {
        const val DETAIL_ID = "DETAIL_ID"
        const val NOTIFICATION_ID = "NOTIFICATION_ID"
        fun initiate(context: Context, detailId: String): Intent {
            return Intent(
                context, SubmissionDetailActivity::class.java
            ).also {
                it.putExtra(DETAIL_ID, detailId)
            }
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

            mapPreview.onCreate(savedInstanceState)
            mapPreview.getMapAsync(this@SubmissionDetailActivity)

            swipeRefresh.setOnRefreshListener {
                init()
                swipeRefresh.isRefreshing = false
            }
        }

        init()
    }

    private fun setRoundedBackground(view: View, @androidx.annotation.DrawableRes drawableId: Int) {
        val drawable = ContextCompat.getDrawable(this, drawableId)?.mutate()
        if (drawable is android.graphics.drawable.GradientDrawable) {
            drawable.cornerRadius = resources.getDimension(R.dimen.login_button_corner_radius)
        }
        view.background = drawable
    }

    @SuppressLint("SetTextI18n")
    private fun init() {
        binding.apply {
            isFabVisible = false
            closeFabMenu()

            detailId = intent.getStringExtra(DETAIL_ID) ?: run {
                Log.e("Detail ID", "No DETAIL_ID found in intent")
                finish()
                return@apply
            }

            notificationId = intent.getIntExtra(NOTIFICATION_ID, 0)

            backButton.setOnClickListener {
                if (isUpdated) {
                    onBackPressedDispatcher.onBackPressed()
                    setResult(RESULT_OK)
                } else
                    onBackPressedDispatcher.onBackPressed()
            }

            holdResumeButton.visibility = View.GONE

            loadingManager(true)
            messageProgressAndTrialButtonContainer.visibility = View.GONE
            val handler = Handler(Looper.getMainLooper())
            val runnable = object : Runnable {
                var count = 1
                override fun run() {
                    when (count) {
                        1 -> detailTitle.text =
                            if (getString(R.string.lang) == "in") "Memuat." else "Loading."

                        2 -> detailTitle.text =
                            if (getString(R.string.lang) == "in") "Memuat.." else "Loading.."

                        3 -> detailTitle.text =
                            if (getString(R.string.lang) == "in") "Memuat..." else "Loading..."
                    }
                    count = if (count < 3) count + 1 else 1
                    handler.postDelayed(this, 500)
                }
            }

            handler.post(runnable)

            try {
                (if (notificationId != 0) InitAPI.getEndpoint.getSubmissionDetail(
                    detailId,
                    notificationId
                ) else InitAPI.getEndpoint.getSubmissionDetail(detailId))
                    .enqueue(object : Callback<List<SubmissionDetailResponse>> {
                        @SuppressLint("ClickableViewAccessibility")
                        override fun onResponse(
                            call: Call<List<SubmissionDetailResponse>?>,
                            response: Response<List<SubmissionDetailResponse>?>
                        ) {
                            loadingManager(false)
                            handler.removeCallbacks(runnable)
                            content.visibility = View.VISIBLE
                            if (response.isSuccessful) {
                                if (response.body() != null) {
                                    detailData = response.body()!![0]
                                    showLocationOnMap()
                                    detailTitle.text = detailData.nomorRequest
                                    if (detailData.fotoGaprojects!!.isEmpty()) {
                                        imageContainer.visibility = View.GONE
                                    } else {
                                        imageContainer.visibility = View.VISIBLE
                                        imageData = ArrayList()
                                        if (detailData.fotoGaprojects!!.size > 1) {
                                            imageCarouselHolder.visibility = View.VISIBLE
                                            circleIndicator.visibility = View.VISIBLE
                                            submissionImage.visibility = View.GONE
                                            for (i in 0 until detailData.fotoGaprojects!!.size) {
                                                imageData.add(
                                                    detailData.fotoGaprojects!![i]!!
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
                                            val image = detailData.fotoGaprojects!![0]
                                            if (Base64Helper.isBase64Encoded(
                                                    image?.foto.toString()
                                                )
                                            ) {
                                                val decodedImageURL = Base64Helper.decodeBase64(
                                                    image?.foto.toString()
                                                )
                                                Log.e("ImageURL", decodedImageURL)
                                                Glide.with(applicationContext)
                                                    .load(decodedImageURL)
                                                    .listener(object : RequestListener<Drawable> {
                                                        override fun onLoadFailed(
                                                            e: GlideException?,
                                                            model: Any?,
                                                            target: Target<Drawable?>?,
                                                            isFirstResource: Boolean
                                                        ): Boolean {
                                                            Log.e(
                                                                "PhotoDetail",
                                                                "Glide onLoadFailed: ",
                                                                e
                                                            )
                                                            return false
                                                        }

                                                        override fun onResourceReady(
                                                            resource: Drawable?,
                                                            model: Any?,
                                                            target: Target<Drawable?>?,
                                                            dataSource: DataSource?,
                                                            isFirstResource: Boolean
                                                        ): Boolean {
                                                            return false
                                                        }
                                                    })
                                                    .placeholder(R.drawable.image_placeholder)
                                                    .into(submissionImage)
                                                val imageZoomHelper = ImageZoomHelper(
                                                    this@SubmissionDetailActivity,
                                                    submissionImage
                                                )
                                                submissionImage.setOnTouchListener { _, event ->
                                                    imageZoomHelper.init(event!!)
                                                    true
                                                }
                                            } else {
                                                Glide.with(applicationContext)
                                                    .load(InitAPI.IMAGE_URL + image?.foto)
                                                    .listener(object : RequestListener<Drawable> {
                                                        override fun onLoadFailed(
                                                            e: GlideException?,
                                                            model: Any?,
                                                            target: Target<Drawable?>?,
                                                            isFirstResource: Boolean
                                                        ): Boolean {
                                                            Log.e(
                                                                "PhotoDetail",
                                                                "Glide onLoadFailed: ",
                                                                e
                                                            )
                                                            return false
                                                        }

                                                        override fun onResourceReady(
                                                            resource: Drawable?,
                                                            model: Any?,
                                                            target: Target<Drawable?>?,
                                                            dataSource: DataSource?,
                                                            isFirstResource: Boolean
                                                        ): Boolean {
                                                            return false
                                                        }
                                                    })
                                                    .placeholder(R.drawable.image_placeholder)
                                                    .into(submissionImage)
                                                val imageZoomHelper = ImageZoomHelper(
                                                    this@SubmissionDetailActivity,
                                                    submissionImage
                                                )
                                                submissionImage.setOnTouchListener { _, event ->
                                                    imageZoomHelper.init(event!!)
                                                    true
                                                }
                                            }
                                        }
                                    }
                                    submissionName.text = detailData.judulKasus
                                    inputDate.text = detailData.setTglinput
                                    submissionComplexityText.text =
                                        detailData.difficulty?.replaceFirstChar {
                                            if (it.isLowerCase())
                                                it.titlecase(Locale.getDefault())
                                            else
                                                it.toString()
                                        }

                                    submissionComplexity.visibility =
                                        if (detailData.difficulty == "null" || detailData.difficulty == null)
                                            View.GONE
                                        else
                                            View.VISIBLE

                                    when (detailData.difficulty) {
                                        "low" -> {
                                            setRoundedBackground(
                                                submissionComplexity,
                                                R.drawable.gradient_low_complexity
                                            )
                                            complexityColor.background = null
                                        }

                                        "middle" -> {
                                            setRoundedBackground(
                                                submissionComplexity,
                                                R.drawable.gradient_middle_complexity
                                            )
                                            complexityColor.background = null
                                        }

                                        "high" -> {
                                            setRoundedBackground(
                                                submissionComplexity,
                                                R.drawable.gradient_high_complexity
                                            )
                                            complexityColor.background = null
                                        }
                                    }

                                    setRoundedBackground(
                                        onProgressColor,
                                        R.drawable.gradient_progress_action_color
                                    )
                                    when (detailData.stsGaprojects) {
                                        // Rejected
                                        0 -> {
                                            setRoundedBackground(
                                                submissionStatus,
                                                R.drawable.gradient_rejected_color
                                            )
                                            statusColor.background = null
                                            submissionStatusText.text = "Rejected"
                                            actionButton.visibility = View.GONE
                                            actionSelfButton.visibility = View.GONE
                                            onProgressButton.visibility = View.GONE
                                            messageProgressAndTrialButtonContainer.visibility =
                                                View.VISIBLE
                                            statusMessageContainer.setOnClickListener {
                                                statusMessageContainer.showAlignTop(
                                                    createBalloonOverlay(
                                                        if (getString(R.string.lang) == "in")
                                                            "Alasan: ${detailData.keteranganReject}"
                                                        else "Reason: ${detailData.keteranganReject}",
                                                        R.color.custom_toast_background_normal_dark_gray,
                                                        R.color.custom_toast_font_normal_soft_gray
                                                    ), 0, 0
                                                )
                                            }
                                            statusMessageContainer.visibility = View.VISIBLE
                                            setRoundedBackground(
                                                statusMessageColor,
                                                R.drawable.gradient_rejected_color,
                                            )
                                            statusMessage.text =
                                                if (getString(R.string.lang) == "in")
                                                    "Ditolak oleh ${detailData.nameUserReject?.trimEnd()}\nKetuk untuk melihat alasan"
                                                else
                                                    "Rejected by ${detailData.nameUserReject?.trimEnd()}\nTap to see reason"
                                            statusMessage.setTextColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.white
                                                )
                                            )
                                        }
                                        // Pending
                                        1 -> {
                                            setRoundedBackground(
                                                submissionStatus,
                                                R.drawable.gradient_pending_color
                                            )
                                            statusColor.background = null
                                            submissionStatusText.text = "Pending"
                                            onProgressButton.visibility = View.GONE
                                            messageProgressAndTrialButtonContainer.visibility =
                                                View.GONE
                                        }
                                        // Waiting
                                        11 -> {
                                            setRoundedBackground(
                                                submissionStatus,
                                                R.drawable.gradient_waiting_color
                                            )
                                            statusColor.background = null
                                            submissionStatusText.text = "Waiting"
                                            onProgressButton.visibility = View.GONE
                                            messageProgressAndTrialButtonContainer.visibility =
                                                View.VISIBLE
                                            statusMessageContainer.setOnClickListener {
                                                statusMessageContainer.showAlignTop(
                                                    createBalloonOverlay(
                                                        if (getString(R.string.lang) == "in")
                                                            "Pesan: ${detailData.keteranganPelaporApprove}"
                                                        else "Message: ${detailData.keteranganPelaporApprove}",
                                                        R.color.custom_toast_font_success,
                                                        R.color.custom_toast_background_success
                                                    ), 0, 0
                                                )
                                            }
                                            statusMessageContainer.visibility = View.VISIBLE
                                            setRoundedBackground(
                                                statusMessageColor,
                                                R.drawable.gradient_menu_color,
                                            )
                                            statusMessage.text =
                                                if (getString(R.string.lang) == "in")
                                                    "Disetujui oleh ${detailData.namaUserPelaporApprove?.trimEnd()}\nTunggu manajer tujuan untuk menyetujui\nKetuk untuk melihat pesan"
                                                else
                                                    "Approved by ${detailData.namaUserPelaporApprove?.trimEnd()}\nWait for targeted manager to approve\nTap to see message"
                                            statusMessage.setTextColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.white
                                                )
                                            )
                                        }
                                        // Approved
                                        2 -> {
                                            setRoundedBackground(
                                                submissionStatus,
                                                R.drawable.gradient_approved_color
                                            )
                                            statusColor.background = null
                                            submissionStatusText.text = "Approved"
                                            var tech = false
                                            var spv = false
                                            for (technician in detailData.usernUserTeknisi!!) {
                                                if (technician?.idUser == userData.id) {
                                                    tech = true
                                                    break
                                                }
                                            }

                                            for (supervisor in detailData.usernUserSpv!!) {
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
                                                    statusMessageContainer.setOnClickListener {
                                                        statusMessageContainer.showAlignTop(
                                                            createBalloonOverlay(
                                                                if (getString(R.string.lang) == "in")
                                                                    "Pesan: ${detailData.ketApproved}"
                                                                else "Message: ${detailData.ketApproved}",
                                                                R.color.custom_toast_font_success,
                                                                R.color.custom_toast_background_success
                                                            ), 0, 0
                                                        )
                                                    }
                                                    statusMessageContainer.visibility = View.VISIBLE
                                                    setRoundedBackground(
                                                        statusMessageColor,
                                                        R.drawable.gradient_approved_color,
                                                    )
                                                    message = StringBuilder().append(
                                                        if (getString(R.string.lang) == "in")
                                                            "Disetujui oleh ${detailData.userNamaApprove?.trimEnd()}\nKetuk untuk melihat pesan"
                                                        else
                                                            "Approved by ${detailData.userNamaApprove?.trimEnd()}\nTap to see message"
                                                    )
                                                    statusMessage.text = message.toString()
                                                    statusMessage.setTextColor(
                                                        ContextCompat.getColor(
                                                            this@SubmissionDetailActivity,
                                                            R.color.white
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                        // Hold
                                        22 -> {
                                            for (i in 0 until detailData.usernUserSpv!!.size) {
                                                if (userData.id == detailData.usernUserSpv!![i]
                                                    !!.idUser
                                                ) {
                                                    holdResumeIcon.setImageDrawable(
                                                        ContextCompat.getDrawable(
                                                            applicationContext,
                                                            R.drawable.baseline_play_arrow_24
                                                        )
                                                    )
                                                    holdResumeButton.visibility = View.VISIBLE
                                                    holdResumeColor.background =
                                                        ResourcesCompat.getDrawable(
                                                            resources,
                                                            R.drawable.gradient_approved_color,
                                                            theme
                                                        )
                                                    holdResumeButton.setOnClickListener {
                                                        val confirmationDialog = ConfirmationDialog(
                                                            this@SubmissionDetailActivity,
                                                            if (getString(R.string.lang) == "in")
                                                                "Apakah Anda yakin ingin melanjutkan masalah ini?"
                                                            else "Are you sure want to resume this issue?",
                                                            if (getString(R.string.lang) == "in") "Ya" else "Yes"
                                                        ).also {
                                                            with(it) {
                                                                setConfirmationDialogListener(
                                                                    object :
                                                                        ConfirmationDialog.ConfirmationDialogListener {
                                                                        override fun onConfirm() {
                                                                            val loadingDialog =
                                                                                LoadingDialog(
                                                                                    this@SubmissionDetailActivity
                                                                                )
                                                                            if (loadingDialog.window != null)
                                                                                loadingDialog.show()
                                                                            try {
                                                                                InitAPI.getEndpoint
                                                                                    .resumeIssue(
                                                                                        detailData.idGaprojects!!,
                                                                                        userData.id
                                                                                    ).enqueue(
                                                                                        object :
                                                                                            Callback<GenericSimpleResponse> {
                                                                                            override fun onResponse(
                                                                                                call1: Call<GenericSimpleResponse>,
                                                                                                response1: Response<GenericSimpleResponse>
                                                                                            ) {
                                                                                                loadingDialog.dismiss()
                                                                                                if (response1.isSuccessful) {
                                                                                                    val result =
                                                                                                        response1.body()
                                                                                                    if (result != null) {
                                                                                                        if (result.code == 1) {
                                                                                                            isUpdated =
                                                                                                                true
                                                                                                            CustomToast
                                                                                                                .getInstance(
                                                                                                                    applicationContext
                                                                                                                )
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
                                                                                                                    if (getString(
                                                                                                                            R.string.lang
                                                                                                                        ) == "in"
                                                                                                                    )
                                                                                                                        "Masalah dilanjutkan"
                                                                                                                    else
                                                                                                                        "Issue was resumed!"
                                                                                                                )
                                                                                                                .show()
                                                                                                            init()
                                                                                                        } else {
                                                                                                            CustomToast
                                                                                                                .getInstance(
                                                                                                                    applicationContext
                                                                                                                )
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
                                                                                                                    if (getString(
                                                                                                                            R.string.lang
                                                                                                                        ) == "in"
                                                                                                                    )
                                                                                                                        "Gagal melanjutkan masalah ini"
                                                                                                                    else
                                                                                                                        "Failed to resume this issue"
                                                                                                                )
                                                                                                                .show()
                                                                                                            Log.e(
                                                                                                                "ERROR ${response1.code()}",
                                                                                                                "Resume Issue Response code 0 | ${response1.message()}"
                                                                                                            )
                                                                                                        }
                                                                                                    } else {
                                                                                                        CustomToast
                                                                                                            .getInstance(
                                                                                                                applicationContext
                                                                                                            )
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
                                                                                                                if (getString(
                                                                                                                        R.string.lang
                                                                                                                    ) == "in"
                                                                                                                )
                                                                                                                    "Gagal melanjutkan masalah ini"
                                                                                                                else
                                                                                                                    "Failed to resume this issue"
                                                                                                            )
                                                                                                            .show()
                                                                                                        Log.e(
                                                                                                            "ERROR ${response1.code()}",
                                                                                                            "Resume Issue Response null | ${response1.message()}"
                                                                                                        )
                                                                                                    }
                                                                                                } else {
                                                                                                    CustomToast
                                                                                                        .getInstance(
                                                                                                            applicationContext
                                                                                                        )
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
                                                                                                            if (getString(
                                                                                                                    R.string.lang
                                                                                                                ) == "in"
                                                                                                            )
                                                                                                                "Gagal melanjutkan masalah ini"
                                                                                                            else
                                                                                                                "Failed to resume this issue"
                                                                                                        )
                                                                                                        .show()
                                                                                                    Log.e(
                                                                                                        "ERROR ${response1.code()}",
                                                                                                        "Resume Issue Response Fail | ${response1.message()}"
                                                                                                    )
                                                                                                }
                                                                                                init()
                                                                                            }

                                                                                            override fun onFailure(
                                                                                                call1: Call<GenericSimpleResponse>,
                                                                                                throwable: Throwable
                                                                                            ) {
                                                                                                loadingDialog.dismiss()
                                                                                                CustomToast
                                                                                                    .getInstance(
                                                                                                        applicationContext
                                                                                                    )
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
                                                                                                        if (getString(
                                                                                                                R.string.lang
                                                                                                            ) == "in"
                                                                                                        )
                                                                                                            "Terjadi kesalahan, silakan coba lagi nanti"
                                                                                                        else
                                                                                                            "Something went wrong, please try again later"
                                                                                                    )
                                                                                                    .show()
                                                                                                throwable.printStackTrace()
                                                                                                Log.e(
                                                                                                    "ERROR",
                                                                                                    "Resume Issue Failure | $throwable"
                                                                                                )
                                                                                            }
                                                                                        }
                                                                                    )
                                                                            } catch (jsonException: JSONException) {
                                                                                loadingDialog.dismiss()
                                                                                CustomToast
                                                                                    .getInstance(
                                                                                        applicationContext
                                                                                    )
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
                                                                                        if (getString(
                                                                                                R.string.lang
                                                                                            ) == "in"
                                                                                        )
                                                                                            "Terjadi kesalahan, silakan coba lagi nanti"
                                                                                        else
                                                                                            "Something went wrong, please try again later"
                                                                                    )
                                                                                    .show()
                                                                                jsonException.printStackTrace()
                                                                                Log.e(
                                                                                    "ERROR",
                                                                                    "Resume Issue Exception | $jsonException"
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                )
                                                            }
                                                        }

                                                        if (confirmationDialog.window != null)
                                                            confirmationDialog.show()
                                                    }
                                                    break
                                                }
                                            }
                                            setRoundedBackground(
                                                submissionStatus,
                                                R.drawable.gradient_hold_color
                                            )
                                            statusColor.background = null
                                            submissionStatusText.text = "Hold"
                                            messageProgressAndTrialButtonContainer.visibility =
                                                View.VISIBLE
                                            statusMessageContainer.visibility = View.GONE
                                            onProgressButton.visibility = View.VISIBLE
                                            onProgressText.setCompoundDrawablesWithIntrinsicBounds(
                                                null,
                                                null,
                                                null,
                                                null
                                            )
                                            setRoundedBackground(
                                                onProgressColor,
                                                R.drawable.gradient_hold_color,
                                            )
                                            onProgressButton.setOnClickListener {
                                                progressTrackingBottomSheet =
                                                    ProgressTrackingBottomSheet(
                                                        this@SubmissionDetailActivity,
                                                        this@SubmissionDetailActivity, detailData
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
                                            onProgressText.text =
                                                if (getString(R.string.lang) == "in")
                                                    "Masalah dijeda oleh ${
                                                        detailData.namaUserHold
                                                    }\nKetuk untuk melihat kemajuan"
                                                else
                                                    "Issue on hold by ${
                                                        detailData.namaUserHold
                                                    }\nTap to see progress"
                                            onProgressText.setTextColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.white
                                                )
                                            )
                                        }
                                        // On-Progress
                                        3 -> {
                                            for (i in 0 until detailData.usernUserSpv!!.size) {
                                                if (userData.id == detailData.usernUserSpv!![i]
                                                    !!.idUser
                                                ) {
                                                    holdResumeIcon.setImageDrawable(
                                                        ContextCompat.getDrawable(
                                                            applicationContext,
                                                            R.drawable.baseline_pause_24
                                                        )
                                                    )
                                                    holdResumeButton.visibility = View.VISIBLE
                                                    holdResumeColor.background =
                                                        ResourcesCompat.getDrawable(
                                                            resources,
                                                            R.drawable.gradient_hold_color,
                                                            theme
                                                        )
                                                    holdResumeButton.setOnClickListener {
                                                        val holdBottomSheet =
                                                            ActionHoldIssueBottomSheet(this@SubmissionDetailActivity).also {
                                                                with(it) {
                                                                    setOnButtonClickListener(
                                                                        object :
                                                                            ActionHoldIssueBottomSheet.OnHoldButtonClickListener {
                                                                            override fun onHoldButtonClicked(
                                                                                reason: String
                                                                            ) {
                                                                                val loadingDialog =
                                                                                    LoadingDialog(
                                                                                        this@SubmissionDetailActivity
                                                                                    )
                                                                                if (loadingDialog.window != null)
                                                                                    loadingDialog.show()
                                                                                try {
                                                                                    InitAPI.getEndpoint
                                                                                        .holdIssue(
                                                                                            detailData.idGaprojects!!,
                                                                                            userData.id,
                                                                                            reason
                                                                                        ).enqueue(
                                                                                            object :
                                                                                                Callback<GenericSimpleResponse> {
                                                                                                override fun onResponse(
                                                                                                    call1: Call<GenericSimpleResponse>,
                                                                                                    response1: Response<GenericSimpleResponse>
                                                                                                ) {
                                                                                                    loadingDialog.dismiss()
                                                                                                    if (response1.isSuccessful) {
                                                                                                        val result =
                                                                                                            response1.body()
                                                                                                        if (result != null) {
                                                                                                            if (result.code == 1) {
                                                                                                                isUpdated =
                                                                                                                    true
                                                                                                                CustomToast
                                                                                                                    .getInstance(
                                                                                                                        applicationContext
                                                                                                                    )
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
                                                                                                                        if (getString(
                                                                                                                                R.string.lang
                                                                                                                            ) == "in"
                                                                                                                        )
                                                                                                                            "Masalah telah dijeda!"
                                                                                                                        else
                                                                                                                            "Issue was hold!"
                                                                                                                    )
                                                                                                                    .show()
                                                                                                                init()
                                                                                                            } else {
                                                                                                                CustomToast
                                                                                                                    .getInstance(
                                                                                                                        applicationContext
                                                                                                                    )
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
                                                                                                                        if (getString(
                                                                                                                                R.string.lang
                                                                                                                            ) == "in"
                                                                                                                        )
                                                                                                                            "Gagal menjeda masalah ini"
                                                                                                                        else
                                                                                                                            "Failed to hold this issue"
                                                                                                                    )
                                                                                                                    .show()
                                                                                                                Log.e(
                                                                                                                    "ERROR ${response1.code()}",
                                                                                                                    "Hold Issue Response code 0 | ${response1.message()}"
                                                                                                                )
                                                                                                            }
                                                                                                        } else {
                                                                                                            CustomToast
                                                                                                                .getInstance(
                                                                                                                    applicationContext
                                                                                                                )
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
                                                                                                                    if (getString(
                                                                                                                            R.string.lang
                                                                                                                        ) == "in"
                                                                                                                    )
                                                                                                                        "Gagal menjeda masalah ini"
                                                                                                                    else
                                                                                                                        "Failed to hold this issue"
                                                                                                                )
                                                                                                                .show()
                                                                                                            Log.e(
                                                                                                                "ERROR ${response1.code()}",
                                                                                                                "Hold Issue Response null | ${response1.message()}"
                                                                                                            )
                                                                                                        }
                                                                                                    } else {
                                                                                                        CustomToast
                                                                                                            .getInstance(
                                                                                                                applicationContext
                                                                                                            )
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
                                                                                                                if (getString(
                                                                                                                        R.string.lang
                                                                                                                    ) == "in"
                                                                                                                )
                                                                                                                    "Gagal menjeda masalah ini"
                                                                                                                else
                                                                                                                    "Failed to hold this issue"
                                                                                                            )
                                                                                                            .show()
                                                                                                        Log.e(
                                                                                                            "ERROR ${response1.code()}",
                                                                                                            "Hold Issue Response Fail | ${response1.message()}"
                                                                                                        )
                                                                                                    }
                                                                                                    init()
                                                                                                }

                                                                                                override fun onFailure(
                                                                                                    call1: Call<GenericSimpleResponse>,
                                                                                                    throwable: Throwable
                                                                                                ) {
                                                                                                    loadingDialog.dismiss()
                                                                                                    CustomToast
                                                                                                        .getInstance(
                                                                                                            applicationContext
                                                                                                        )
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
                                                                                                            if (getString(
                                                                                                                    R.string.lang
                                                                                                                ) == "in"
                                                                                                            )
                                                                                                                "Terjadi kesalahan, silakan coba lagi nanti"
                                                                                                            else
                                                                                                                "Something went wrong, please try again later"
                                                                                                        )
                                                                                                        .show()
                                                                                                    throwable.printStackTrace()
                                                                                                    Log.e(
                                                                                                        "ERROR",
                                                                                                        "Hold Issue Failure | $throwable"
                                                                                                    )
                                                                                                }
                                                                                            }
                                                                                        )
                                                                                } catch (jsonException: JSONException) {
                                                                                    loadingDialog.dismiss()
                                                                                    CustomToast
                                                                                        .getInstance(
                                                                                            applicationContext
                                                                                        )
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
                                                                                            if (getString(
                                                                                                    R.string.lang
                                                                                                ) == "in"
                                                                                            )
                                                                                                "Terjadi kesalahan, silakan coba lagi nanti"
                                                                                            else
                                                                                                "Something went wrong, please try again later"
                                                                                        )
                                                                                        .show()
                                                                                    jsonException.printStackTrace()
                                                                                    Log.e(
                                                                                        "ERROR",
                                                                                        "Hold Issue Exception | $jsonException"
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                    )
                                                                }
                                                            }

                                                        if (holdBottomSheet.window != null)
                                                            holdBottomSheet.show()
                                                    }
                                                    break
                                                }
                                            }
                                            setRoundedBackground(
                                                submissionStatus,
                                                R.drawable.gradient_on_progress_color
                                            )
                                            statusColor.background = null
                                            submissionStatusText.text = "On Progress"
                                            actionButton.visibility = View.GONE
                                            actionSelfButton.visibility = View.GONE
                                            messageProgressAndTrialButtonContainer.visibility =
                                                View.VISIBLE
                                            statusMessageContainer.visibility = View.GONE
                                            onProgressButton.visibility = View.VISIBLE
                                            onProgressText.setCompoundDrawablesWithIntrinsicBounds(
                                                ContextCompat.getDrawable(
                                                    this@SubmissionDetailActivity,
                                                    R.drawable.baseline_timelapse_24
                                                ),
                                                null,
                                                null,
                                                null
                                            )
                                            setRoundedBackground(
                                                onProgressColor,
                                                R.drawable.gradient_progress_action_color,
                                            )
                                            message =
                                                StringBuilder().append(
                                                    if (getString(R.string.lang) == "in")
                                                        "Sedang dalam proses\nKetuk untuk melihat kemajuan"
                                                    else
                                                        "On Progress\nTap to see progress"
                                                )
                                            onProgressText.text = message.toString()
                                            onProgressText.setTextColor(
                                                ContextCompat.getColor(
                                                    applicationContext,
                                                    R.color.white
                                                )
                                            )
                                            onProgressButton.setOnClickListener {
                                                progressTrackingBottomSheet =
                                                    ProgressTrackingBottomSheet(
                                                        this@SubmissionDetailActivity,
                                                        this@SubmissionDetailActivity, detailData
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
                                            if (detailData.isAlreadyTrial!!) {
                                                seeTrialContainer.visibility = View.VISIBLE
                                                seeTrialContainer.setOnClickListener {
                                                    val trialBottomSheet = TrialTrackingBottomSheet(
                                                        this@SubmissionDetailActivity, detailData
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
                                            setRoundedBackground(
                                                submissionStatus,
                                                R.drawable.gradient_progress_done_color
                                            )
                                            statusColor.background = null
                                            submissionStatusText.text = "Progress Done"
                                            actionButton.visibility = View.GONE
                                            actionSelfButton.visibility = View.GONE
                                            messageProgressAndTrialButtonContainer.visibility =
                                                View.VISIBLE
                                            onProgressButton.visibility = View.VISIBLE
                                            message =
                                                StringBuilder().append(
                                                    if (getString(R.string.lang) == "in")
                                                        "Kemajuan telah ditandai selesai oleh "
                                                    else
                                                        "Progress marked as done by "
                                                )
                                            message.append(
                                                if (getString(R.string.lang) == "in")
                                                    "${detailData.usernUserSpv!![0]?.namaUser?.trimEnd()}\nKetuk untuk melihat kemajuan"
                                                else
                                                    "${detailData.usernUserSpv!![0]?.namaUser?.trimEnd()}\nTap to see progress"
                                            )
                                            onProgressText.text = message.toString()
                                            onProgressText.setTextColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.white
                                                )
                                            )
                                            onProgressText.setCompoundDrawablesWithIntrinsicBounds(
                                                null,
                                                null,
                                                null,
                                                null
                                            )
                                            setRoundedBackground(
                                                onProgressColor,
                                                R.drawable.gradient_progress_done_color,
                                            )
                                            onProgressButton.setOnClickListener {
                                                progressTrackingBottomSheet =
                                                    ProgressTrackingBottomSheet(
                                                        this@SubmissionDetailActivity,
                                                        this@SubmissionDetailActivity, detailData
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
                                            if (detailData.isAlreadyTrial!!) {
                                                seeTrialContainer.visibility = View.VISIBLE
                                                seeTrialContainer.setOnClickListener {
                                                    val trialBottomSheet = TrialTrackingBottomSheet(
                                                        this@SubmissionDetailActivity, detailData
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
                                            setRoundedBackground(
                                                submissionStatus,
                                                R.drawable.gradient_done_color
                                            )
                                            statusColor.background = null
                                            submissionStatusText.text = "Done"
                                            actionButton.visibility = View.GONE
                                            actionSelfButton.visibility = View.GONE
                                            onProgressButton.visibility = View.GONE
                                            messageProgressAndTrialButtonContainer.visibility =
                                                View.VISIBLE
                                            statusMessageContainer.visibility = View.VISIBLE
                                            setRoundedBackground(
                                                statusMessageColor,
                                                R.drawable.gradient_approved_color,
                                            )
                                            statusMessage.text =
                                                if (getString(R.string.lang) == "in")
                                                    "Masalah telah ditandai selesai oleh ${detailData.nameUserDone?.trimEnd()}\nKetuk untuk melihat kemajuan"
                                                else
                                                    "Done by ${detailData.nameUserDone?.trimEnd()}\nTap to see progress"
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
                                                        this@SubmissionDetailActivity, detailData
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
                                                    this@SubmissionDetailActivity, detailData
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
                                            setRoundedBackground(
                                                submissionStatus,
                                                R.drawable.gradient_canceled_color
                                            )
                                            statusColor.background = null
                                            submissionStatusText.text = "Canceled"
                                            actionButton.visibility = View.GONE
                                            actionSelfButton.visibility = View.GONE
                                            onProgressButton.visibility = View.GONE
                                            messageProgressAndTrialButtonContainer.visibility =
                                                View.VISIBLE
                                            statusMessageContainer.visibility = View.VISIBLE
                                            setRoundedBackground(
                                                statusMessageColor,
                                                R.drawable.gradient_canceled_color,
                                            )
                                            statusMessage.text =
                                                if (userData.id == detailData.idUser) {
                                                    if (getString(R.string.lang) == "in")
                                                        "Dibatalkan oleh Anda"
                                                    else
                                                        "Canceled by You"
                                                } else {
                                                    if (getString(R.string.lang) == "in")
                                                        "Dibatalkan oleh pelapor, ${detailData.namaUserBuat?.trimEnd()}"
                                                    else
                                                        "Canceled by the reporter, ${detailData.namaUserBuat?.trimEnd()}"
                                                }
                                            statusMessage.setTextColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionDetailActivity,
                                                    R.color.white
                                                )
                                            )
                                        }
                                        // On-Trial
                                        31 -> {
                                            setRoundedBackground(
                                                submissionStatus,
                                                R.drawable.gradient_on_trial_color
                                            )
                                            statusColor.background = null
                                            submissionStatusText.text = "On Trial"
                                            actionButton.visibility = View.GONE
                                            actionSelfButton.visibility = View.GONE
                                            onProgressButton.visibility = View.GONE
                                            messageProgressAndTrialButtonContainer.visibility =
                                                View.VISIBLE
                                            statusMessageContainer.visibility = View.VISIBLE
                                            setRoundedBackground(
                                                statusMessageColor,
                                                R.drawable.gradient_accent_color,
                                            )
                                            statusMessage.text =
                                                if (getString(R.string.lang) == "in")
                                                    "Masalah sedang dalam uji coba. Tunggu hingga selesai.\nKetuk untuk melihat kemajuan"
                                                else
                                                    "The fix is under trial. Wait until it done.\nTap to see progress"
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
                                                        this@SubmissionDetailActivity, detailData
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
                                                    this@SubmissionDetailActivity, detailData
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
                                    submissionDescription.text = detailData.keterangan
                                    machineCodeText.text = "${getString(R.string.machine_code)}:"
                                    machineNameText.text = "${getString(R.string.machine_name)}:"
                                    machineCode.text = if (detailData.kodeMesin != null) {
                                        detailData.kodeMesin!!.ifEmpty { "-" }
                                    } else "-"
                                    machineName.text = if (detailData.namaMesin != null) {
                                        detailData.namaMesin!!.ifEmpty { "-" }
                                    } else "-"
                                    user.text =
                                        "${detailData.namaUserBuat?.trimEnd()} " +
                                                "(ID: ${detailData.idUser} | StarConnect ID: ${detailData.idStarconnectUserBuat})" +
                                                if (getString(R.string.lang) == "in") "\nDari Departemen ${detailData.deptUser}"
                                                else "\nFrom ${detailData.deptUser} Department"
                                    actionCondition(detailData)
                                    department.text = detailData.subDeptTujuan
                                    category.text = detailData.namaKategori
                                    inputTime.text = detailData.tglInput
                                    location.text = detailData.lokasi
                                    reportTime.text = if (detailData.tglWaktuLapor != null) {
                                        if (detailData.tglWaktuLapor == "") "-" else detailData.tglWaktuLapor
                                    } else {
                                        "-"
                                    }

                                    startWork.text = if (detailData.tglWaktuKerjaStart != null) {
                                        if (detailData.tglWaktuKerjaStart!!.contains(
                                                "0000-00-00"
                                            ) || detailData.tglWaktuKerjaStart == ""
                                        ) "-" else detailData.tglWaktuKerjaStart
                                    } else {
                                        "-"
                                    }

                                    endWork.text = if (detailData.tglWaktuKerjaEnd != null) {
                                        if (detailData.tglWaktuKerjaEnd!!.contains(
                                                "0000-00-00"
                                            ) || detailData.tglWaktuKerjaEnd == ""
                                        ) "-" else detailData.tglWaktuKerjaEnd
                                    } else {
                                        "-"
                                    }

                                    userApprovedOrRejectedTitle.text = if (detailData
                                            .userNamaApprove
                                        == "" && detailData.nameUserReject == ""
                                    ) {
                                        "-"
                                    } else {
                                        when {
                                            detailData.userNamaApprove?.isNotEmpty() == true -> if (getString(
                                                    R.string.lang
                                                ) == "in"
                                            ) "Disetujui oleh" else "Approved by"

                                            detailData.nameUserReject?.isNotEmpty() == true -> if (getString(
                                                    R.string.lang
                                                ) == "in"
                                            ) "Ditolak oleh" else "Rejected by"

                                            else -> "-"
                                        }
                                    }

                                    userApprovedOrRejected.text =
                                        detailData.userNamaApprove?.takeIf { it.isNotBlank() }
                                            ?.trimEnd()
                                            ?: detailData.nameUserReject?.takeIf { it.isNotBlank() }
                                                ?.trimEnd() ?: "-"

                                    userSupervisors.text = StringBuilder().also {
                                        with(it) {
                                            if (detailData.usernUserSpv!!.isEmpty()) {
                                                append("-")
                                            } else {
                                                for (i in 0 until detailData.usernUserSpv!!.size) {
                                                    if (i == detailData.usernUserSpv!!.size - 1) {
                                                        append(detailData.usernUserSpv!![i]?.namaUser?.trimEnd())
                                                    } else {
                                                        append("${detailData.usernUserSpv!![i]?.namaUser?.trimEnd()}\n")
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    val vendorName = if (detailData.vendorName != "") {
                                        ": ${detailData.vendorName}"
                                    } else ""

                                    userTechnicians.text =
                                        if (detailData.isVendor == "y") "${getString(R.string.work_by_vendors)}${vendorName}" else StringBuilder().also {
                                            with(it) {
                                                if (detailData.usernUserTeknisi!!.isEmpty()) {
                                                    append("-")
                                                } else {
                                                    for (i in 0 until detailData.usernUserTeknisi!!.size) {
                                                        if (i == detailData.usernUserTeknisi!!.size - 1) {
                                                            append(detailData.usernUserTeknisi!![i]?.namaUser?.trimEnd())
                                                        } else {
                                                            append("${detailData.usernUserTeknisi!![i]?.namaUser?.trimEnd()}\n")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                } else {
                                    detailTitle.text = "ERR!!"
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
                                                resources, R.color.custom_toast_font_failed, theme
                                            )
                                        )
                                        .setMessage(
                                            if (getString(R.string.lang) == "in")
                                                "Gagal mendapatkan detail laporan"
                                            else
                                                "Failed to get submission detail"
                                        )
                                        .show()
                                    Log.e(
                                        "ERROR",
                                        "Submission Detail Response null | ${response.message()}"
                                    )
                                    finish()
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
                                    .setMessage(
                                        if (getString(R.string.lang) == "in")
                                            "Gagal mendapatkan detail laporan"
                                        else
                                            "Failed to get submission detail"
                                    )
                                    .show()
                                Log.e(
                                    "ERROR",
                                    "Submission Detail Response fail | ${response.message()}"
                                )
                                finish()
                            }
                        }

                        override fun onFailure(
                            call: Call<List<SubmissionDetailResponse>?>,
                            throwable: Throwable
                        ) {
                            loadingManager(false)
                            handler.removeCallbacks(runnable)
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
                                .setMessage(
                                    if (getString(R.string.lang) == "in")
                                        "Terjadi kesalahan, silakan coba lagi nanti"
                                    else
                                        "Something went wrong, please try again later"
                                )
                                .show()
                            throwable.printStackTrace()
                            Log.e("ERROR", "Submission Detail Failure | $throwable")
                            finish()
                        }
                    })
            } catch (exception: Exception) {
                loadingManager(false)
                handler.removeCallbacks(runnable)
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
                    .setMessage(
                        if (getString(R.string.lang) == "in")
                            "Terjadi kesalahan, silakan coba lagi nanti"
                        else
                            "Something went wrong, please try again later"
                    )
                    .show()
                exception.printStackTrace()
                Log.e("ERROR", "Submission Detail Exception | $exception")
                finish()
            }
        }
    }

    private fun createBalloonOverlay(
        message: String, @ColorRes textColor: Int, @ColorRes backgroundColor: Int
    ): Balloon {
        return Balloon.Builder(this@SubmissionDetailActivity).also {
            with(it) {
                setHeight(BalloonSizeSpec.WRAP)
                setText(message)
                setTextColorResource(textColor)
                setTextSize(14f)
                setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                setArrowSize(10)
                setPadding(12)
                setMarginBottom(4)
                setCornerRadius(8f)
                setBackgroundColorResource(backgroundColor)
                setBalloonAnimation(BalloonAnimation.FADE)
                setLifecycleOwner(lifecycleOwner)
            }
        }.build()
    }

    private fun actionCondition(data: SubmissionDetailResponse) {
        binding.apply {
            when (data.stsGaprojects) {
                // Pending
                1 -> {
                    if (userData.id == data.idUser) { // If logged in user is reporter
                        actionButton.visibility = View.GONE
                        actionSelfButton.visibility = View.VISIBLE
                        onProgressButton.visibility = View.GONE
                    } else {
                        // If logged in user are not the reporter but..
                        if (userData.privilege < 2) { // If logged in user is manager or owner
                            if (userData.dept == data.deptUser
                                || data.deptUser!!.contains(userData.dept, true)
                            ) { // If reporter's department are same as manager's/owner's department
                                actionButton.visibility = View.VISIBLE
                                actionSelfButton.visibility = View.GONE
                                onProgressButton.visibility = View.GONE
                            } else {
                                // If reporter's department are not the same as manager's/owner's department
                                actionButton.visibility = View.GONE
                                actionSelfButton.visibility = View.GONE
                                onProgressButton.visibility = View.GONE
                            }
                        } else {
                            // If logged in user is not a manager or owner
                            actionButton.visibility = View.GONE
                            actionSelfButton.visibility = View.GONE
                            onProgressButton.visibility = View.GONE
                        }
                    }
                    actionSelfButton.setOnClickListener {
                        if (!isFabVisible) {
                            openFabMenu("reporter")

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
                                closeFabMenu()
                                val bottomSheet = UpdateStatusBottomSheet(
                                    this@SubmissionDetailActivity,
                                    data,
                                    approve = false,
                                    cancel = true,
                                    deployTech = false,
                                    isEdit = false
                                ).also { bs ->
                                    with(bs) {
                                        setOnUpdateSuccessListener(object :
                                            UpdateStatusBottomSheet.OnUpdateSuccessListener {
                                            override fun onApproved() {}

                                            override fun onRejected() {}

                                            override fun onCanceled() {
                                                isUpdated = true
                                                init()
                                            }

                                            override fun onTechniciansDeployed() {}
                                        })
                                    }
                                }

                                if (bottomSheet.window != null)
                                    bottomSheet.show()
                            }
                        } else {
                            closeFabMenu()
                        }
                    }
                }
                // Approved
                2 -> {
                    if (userData.id == data.idUserApprove) {
                        actionSelfButton.visibility = View.VISIBLE
                    }

                    actionSelfButton.setOnClickListener {
                        if (!isFabVisible) {
                            openFabMenu("manager")

                            editButton.setOnClickListener {
                                val bottomSheet = UpdateStatusBottomSheet(
                                    this@SubmissionDetailActivity,
                                    data,
                                    approve = true,
                                    cancel = false,
                                    deployTech = false,
                                    isEdit = true
                                ).also { bs ->
                                    with(bs) {
                                        setOnUpdateSuccessListener(object :
                                            UpdateStatusBottomSheet.OnUpdateSuccessListener {
                                            override fun onApproved() {
                                                isUpdated = true
                                                init()
                                            }

                                            override fun onRejected() {}

                                            override fun onCanceled() {}

                                            override fun onTechniciansDeployed() {}
                                        })
                                    }
                                }

                                if (bottomSheet.window != null)
                                    bottomSheet.show()
                            }
                        } else {
                            closeFabMenu()
                        }
                    }
                }
                // Waiting
                11 -> {
                    if (userData.privilege < 2) { // If logged in user is manager or owner
                        if (userData.dept == data.deptTujuan) { // If manager's/owner's department are same as targeted department
                            actionButton.visibility = View.VISIBLE
                            actionSelfButton.visibility = View.GONE
                            onProgressButton.visibility = View.GONE
                            statusMessageContainer.visibility = View.GONE
                            messageProgressAndTrialButtonContainer.visibility = View.GONE
                        } else {
                            // If manager's/owner's department are not the same as targeted department
                            actionButton.visibility = View.GONE
                            actionSelfButton.visibility = View.GONE
                            onProgressButton.visibility = View.GONE
                        }
                    } else {
                        // If logged in user is not a manager or owner
                        actionButton.visibility = View.GONE
                        actionSelfButton.visibility = View.GONE
                        onProgressButton.visibility = View.GONE
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
    }

    private fun loadingManager(isLoading: Boolean) {
        binding.apply {
            if (isLoading) {
                shimmer.apply {
                    visibility = View.VISIBLE
                    startShimmer()
                }
                content.visibility = View.GONE
            } else {
                shimmer.apply {
                    visibility = View.GONE
                    stopShimmer()
                }
                content.visibility = View.VISIBLE
            }
        }
    }

    override fun onUpdateSuccess() {
        isUpdated = true
        init()
    }

    override fun onStartProgressNowClicked(bottomSheet: SubmissionActionBottomSheet) {
        bottomSheet.dismiss()
        activityResultLauncher.launch(
            Intent(
                this@SubmissionDetailActivity,
                ProgressFormActivity::class.java
            ).also {
                with(it) {
                    putExtra("detail", detailData)
                }
            }
        )
    }

    override fun createProgressClicked() {
        progressTrackingBottomSheet.dismiss()
        activityResultLauncher.launch(
            Intent(
                this@SubmissionDetailActivity,
                ProgressFormActivity::class.java
            ).also {
                with(it) {
                    putExtra("detail", detailData)
                }
            }
        )
    }

    override fun readyForTrialClicked() {
        val confirmationDialog =
            ConfirmationDialog(
                this@SubmissionDetailActivity,
                if (getString(R.string.lang) == "in")
                    "Anda yakin ingin menandai masalah ini siap untuk uji coba?"
                else
                    "Are you sure you want to mark this issue ready for trial?",
                if (getString(R.string.lang) == "in") "Ya" else "Yes"
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
                                InitAPI.getEndpoint.markAsReadyForTrial(
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
                                                        isUpdated = true
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
                                                                if (getString(R.string.lang) == "in")
                                                                    "Masalah ditandai siap untuk uji coba!"
                                                                else
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
                                                                if (getString(R.string.lang) == "in")
                                                                    "Gagal menandai sebagai siap untuk uji coba"
                                                                else
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
                                                            if (getString(R.string.lang) == "in")
                                                                "Gagal menandai sebagai siap untuk uji coba"
                                                            else
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
                                                    .setMessage(
                                                        if (getString(R.string.lang) == "in")
                                                            "Gagal menandai sebagai siap untuk uji coba"
                                                        else
                                                            "Failed to mark as ready for trial"
                                                    )
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
                                                    if (getString(R.string.lang) == "in")
                                                        "Terjadi kesalahan, silakan coba lagi nanti"
                                                    else
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
                                    .setMessage(
                                        if (getString(R.string.lang) == "in")
                                            "Terjadi kesalahan, silakan coba lagi nanti"
                                        else
                                            "Something went wrong, please try again later"
                                    )
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
                if (getString(R.string.lang) == "in")
                    "Apakah Anda yakin ingin memulai uji coba?"
                else
                    "Are you sure you want to start trial?",
                if (getString(R.string.lang) == "in") "Ya" else "Yes"
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
                                InitAPI.getEndpoint.startTrial(
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
                                                        isUpdated = true
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
                                                                if (getString(R.string.lang) == "in")
                                                                    "Uji coba dimulai!"
                                                                else
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
                                                            .setMessage(
                                                                if (getString(R.string.lang) == "in")
                                                                    "Gagal memulai uji coba"
                                                                else
                                                                    "Failed to start Trial"
                                                            )
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
                                                        .setMessage(
                                                            if (getString(R.string.lang) == "in")
                                                                "Gagal memulai uji coba"
                                                            else
                                                                "Failed to start trial"
                                                        )
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
                                                    .setMessage(
                                                        if (getString(R.string.lang) == "in")
                                                            "Gagal memulai uji coba"
                                                        else
                                                            "Failed to start trial"
                                                    )
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
                                                    if (getString(R.string.lang) == "in")
                                                        "Terjadi kesalahan, silakan coba lagi nanti"
                                                    else
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
                                    .setMessage(
                                        if (getString(R.string.lang) == "in")
                                            "Terjadi kesalahan, silakan coba lagi nanti"
                                        else
                                            "Something went wrong, please try again later"
                                    )
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

    override fun onLongTapListener(data: ProgressItems?, forSpv: Boolean) {
        val dialog = ProgressOptionDialog(this@SubmissionDetailActivity, data!!, forSpv).also {
            with(it) {
                setOnProgressOptionDialogListener(this@SubmissionDetailActivity)
            }
        }

        if (dialog.window != null)
            dialog.show()
    }

    override fun onProgressDeleted(data: ProgressItems) {
        val confirmationDialog =
            ConfirmationDialog(
                this@SubmissionDetailActivity,
                if (getString(R.string.lang) == "in")
                    "Apakah Anda yakin ingin menghapus kemajuan ini?"
                else
                    "Are you sure you want to delete this progress?",
                if (getString(R.string.lang) == "in") "Ya" else "Yes"
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
                                InitAPI.getEndpoint.deleteProgress(
                                    data.progress?.idGaprojectsDetail!!, userData.id
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
                                                        isUpdated = true
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
                                                                if (getString(R.string.lang) == "in")
                                                                    "Kemajuan berhasil dihapus!"
                                                                else
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
                                                            .setMessage(
                                                                if (getString(R.string.lang) == "in")
                                                                    "Gagal menghapus kemajuan"
                                                                else
                                                                    "Failed to delete progress"
                                                            )
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
                                                        .setMessage(
                                                            if (getString(R.string.lang) == "in")
                                                                "Gagal menghapus kemajuan"
                                                            else
                                                                "Failed to delete progress"
                                                        )
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
                                                    .setMessage(
                                                        if (getString(R.string.lang) == "in")
                                                            "Gagal menghapus kemajuan"
                                                        else
                                                            "Failed to delete progress"
                                                    )
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
                                                    if (getString(R.string.lang) == "in")
                                                        "Terjadi kesalahan, silakan coba lagi nanti"
                                                    else
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
                                    .setMessage(
                                        if (getString(R.string.lang) == "in")
                                            "Terjadi kesalahan, silakan coba lagi nanti"
                                        else
                                            "Something went wrong, please try again later"
                                    )
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

    override fun onProgressEdited(data: ProgressItems) {
        progressTrackingBottomSheet.dismiss()
        activityResultLauncher.launch(
            Intent(
                this@SubmissionDetailActivity,
                ProgressFormActivity::class.java
            ).also {
                with(it) {
                    putExtra("data", data)
                    putExtra("detail", detailData)
                }
            }
        )
    }

    override fun onProgressSetDone(data: ProgressItems) {
        progressTrackingBottomSheet.dismiss()
        activityResultLauncher.launch(
            Intent(
                this@SubmissionDetailActivity,
                ProgressDoneFormActivity::class.java
            ).also {
                with(it) {
                    putExtra("data", data)
                }
            }
        )
    }

    override fun onMaterialEdited(data: ProgressItems) {
        progressTrackingBottomSheet.dismiss()
        activityResultLauncher.launch(
            Intent(
                this@SubmissionDetailActivity,
                ProgressFormActivity::class.java
            ).also {
                with(it) {
                    putExtra("data", data)
                    putExtra("detail", detailData)
                    putExtra("edit_material", true)
                }
            }
        )
    }

    override fun onMaterialApproved(data: ProgressItems) {
        val confirmationDialog =
            ConfirmationDialog(
                this@SubmissionDetailActivity,
                if (getString(R.string.lang) == "in")
                    "Apakah Anda yakin ingin menyetujui penambahan bahan kemajuan ini?\n\nPastikan " +
                            "tindakan Anda benar-benar matang sebelum menyetujuinya!"
                else
                    "Are you sure you want to approve this progress materials?\n\nMake sure " +
                            "your action are totally final before approving it!",
                if (getString(R.string.lang) == "in") "Ya" else "Yes"
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
                                InitAPI.getEndpoint.approveMaterialRequest(
                                    data.progress?.idGaprojectsDetail!!,
                                    userData.id
                                ).enqueue(object : Callback<GenericSimpleResponse> {
                                    override fun onResponse(
                                        call: Call<GenericSimpleResponse>,
                                        response: Response<GenericSimpleResponse>
                                    ) {
                                        loadingDialog.dismiss()
                                        if (response.isSuccessful) {
                                            val result = response.body()
                                            if (result != null) {
                                                if (result.code == 1) {
                                                    isUpdated = true
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
                                                            if (getString(R.string.lang) == "in")
                                                                "Bahan kemajuan berhasil disetujui!"
                                                            else
                                                                "Progress material successfully approved!"
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
                                                            if (getString(R.string.lang) == "in")
                                                                "Gagal menyetujui bahan kemajuan"
                                                            else
                                                                "Failed to approve progress materials"
                                                        )
                                                        .show()
                                                    Log.e(
                                                        "ERROR ${response.code()}",
                                                        "Approve Progress Materials Response code 0 | ${response.message()}"
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
                                                        if (getString(R.string.lang) == "in")
                                                            "Gagal menyetujui bahan kemajuan"
                                                        else
                                                            "Failed to approve progress materials"
                                                    )
                                                    .show()
                                                Log.e(
                                                    "ERROR ${response.code()}",
                                                    "Approve Progress Materials Response null | ${response.message()}"
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
                                                .setMessage(
                                                    if (getString(R.string.lang) == "in")
                                                        "Gagal menyetujui bahan kemajuan"
                                                    else
                                                        "Failed to approve progress materials"
                                                )
                                                .show()
                                            Log.e(
                                                "ERROR ${response.code()}",
                                                "Approve Progress Materials Response Fail | ${response.message()}"
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
                                                if (getString(R.string.lang) == "in")
                                                    "Terjadi kesalahan, silakan coba lagi nanti"
                                                else
                                                    "Something went wrong, please try again later"
                                            ).show()
                                        throwable.printStackTrace()
                                        Log.e(
                                            "ERROR",
                                            "Approve Progress Materials Failure | $throwable"
                                        )
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
                                    .setMessage(
                                        if (getString(R.string.lang) == "in")
                                            "Terjadi kesalahan, silakan coba lagi nanti"
                                        else
                                            "Something went wrong, please try again later"
                                    )
                                    .show()
                                jsonException.printStackTrace()
                                Log.e(
                                    "ERROR",
                                    "Approve Progress Materials Exception | $jsonException"
                                )
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
                                isUpdated = true
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
                if (getString(R.string.lang) == "in")
                    "Apakah Anda yakin ingin menandai ini sebagai selesai?\n\nPastikan isu Anda " +
                            "bekerja dengan baik"
                else
                    "Are you sure you want to mark this as done?\n\nMake sure your issue are working properly",
                if (getString(R.string.lang) == "in") "Ya" else "Yes"
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
                                InitAPI.getEndpoint.markIssueDone(
                                    detailData.idGaprojects!!,
                                    userData.id
                                )
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
                                                        isUpdated = true
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
                                                                if (getString(R.string.lang) == "in")
                                                                    "Berhasil menandai sebagai selesai!"
                                                                else
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
                                                            .setMessage(
                                                                if (getString(R.string.lang) == "in")
                                                                    "Gagal menandai sebagai selesai"
                                                                else
                                                                    "Failed to mark as done"
                                                            )
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
                                                        .setMessage(
                                                            if (getString(R.string.lang) == "in")
                                                                "Gagal menandai sebagai selesai"
                                                            else
                                                                "Failed to mark as done"
                                                        )
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
                                                    .setMessage(
                                                        if (getString(R.string.lang) == "in")
                                                            "Gagal menandai sebagai selesai"
                                                        else
                                                            "Failed to mark as done"
                                                    )
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
                                                    if (getString(R.string.lang) == "in")
                                                        "Terjadi kesalahan, silakan coba lagi nanti"
                                                    else
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
                                    .setMessage(
                                        if (getString(R.string.lang) == "in")
                                            "Terjadi kesalahan, silakan coba lagi nanti"
                                        else
                                            "Something went wrong, please try again later"
                                    )
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

    private fun openFabMenu(role: String) {
        binding.apply {
            isFabVisible = true
            TransitionManager.beginDelayedTransition(buttonContainer, AutoTransition())
            actionSelfText.visibility = View.GONE

            if (role == "reporter") {
                animShow(editButton)
                animShow(cancelButton)
            } else {
                animShow(editButton)
            }
        }
    }

    private fun closeFabMenu() {
        binding.apply {
            isFabVisible = false
            TransitionManager.beginDelayedTransition(buttonContainer, AutoTransition())
            actionSelfText.visibility = View.VISIBLE

            animHide(editButton)
            animHide(cancelButton)
        }
    }

    private fun animShow(view: View) {
        view.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0f
            scaleY = 0f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(150)
                .start()
        }
    }

    private fun animHide(view: View) {
        view.animate()
            .alpha(0f)
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(150)
            .withEndAction {
                view.visibility = View.GONE
            }
            .start()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.apply {
            setAllGesturesEnabled(false)
            isMapToolbarEnabled = false
        }
        showLocationOnMap()
    }

    private fun showLocationOnMap() {
        if (googleMap == null || !::detailData.isInitialized) return
        try {
            val position =
                LatLng(detailData.latitude!!.toDouble(), detailData.longitude!!.toDouble())
            googleMap?.apply {
                clear()
                addMarker(MarkerOptions().position(position).title(detailData.lokasi))
                moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.mapPreview.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapPreview.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapPreview.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapPreview.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapPreview.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapPreview.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapPreview.onLowMemory()
    }
}

