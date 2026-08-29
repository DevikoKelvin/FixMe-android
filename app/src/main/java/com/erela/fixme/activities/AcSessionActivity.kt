package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setPadding
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.SelectedSupervisorTechniciansRvAdapter
import com.erela.fixme.bottom_sheets.AcSelectTechnicianBottomSheet
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityAcSessionBinding
import com.erela.fixme.dialogs.SignaturePadDialog
import com.erela.fixme.helpers.PermissionHelper
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.enableEdgeToEdgeOpaqueNav
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.SupervisorTechnician
import com.erela.fixme.objects.UserData
import com.erela.fixme.viewmodel.AcMaintenanceViewModel
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AcSessionActivity : AppCompatActivity(),
    AcSelectTechnicianBottomSheet.OnTechnicianSelectedListener {
    private val binding: ActivityAcSessionBinding by lazy {
        ActivityAcSessionBinding.inflate(
            layoutInflater
        )
    }
    private val userData: UserData by lazy {
        UserDataHelper(this@AcSessionActivity).getUserData()
    }
    private val viewModel: AcMaintenanceViewModel by viewModels()
    private val selectedTechniciansArrayList: ArrayList<SupervisorTechnician> = ArrayList()
    private lateinit var techniciansRvAdapter: SelectedSupervisorTechniciansRvAdapter
    private var logId: Int = -1
    private var photoDuringFile: File? = null

    /** The committed witness signature. Written only by SignaturePadDialog's Save. */
    private var witnessSignatureFile: File? = null
    private var currentPhotoUri: Uri? = null
    private var currentPhotoFile: File? = null
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            binding.apply {
                if (success) {
                    val bitmap = BitmapFactory.decodeFile(currentPhotoFile!!.absolutePath)
                    photoDuringFile = compressImage(currentPhotoFile!!)
                    imgPhotoDuring.setImageBitmap(bitmap)
                    imgPhotoDuring.setPadding(0)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdgeOpaqueNav()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        logId = intent.getIntExtra("LOG_ID", -1)

        setupTechnician()
        setupUI()
        setupObservers()

        if (logId != -1) {
            val userId = userData.id
            viewModel.loadSessionParticipants(logId, userId)
        }
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
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(motionEvent)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupTechnician() {
        binding.apply {
            selectedTechniciansArrayList.clear()
            // "+" placeholder is always the last chip — tapping it opens the user picker
            selectedTechniciansArrayList.add(plusPlaceholder())

            techniciansRvAdapter = SelectedSupervisorTechniciansRvAdapter(
                this@AcSessionActivity,
                SubmissionDetailResponse(),
                selectedTechniciansArrayList,
                false,
                onPlusClicked = { showTechnicianPicker() }
            ) { null }.also { adapter ->
                adapter.setOnTechniciansSetListener(
                    object : SelectedSupervisorTechniciansRvAdapter.OnTechniciansSetListener {
                        override fun onTechniciansSelected(data: SupervisorTechnician) {
                            // handled via AcSelectTechnicianBottomSheet callback
                        }

                        override fun onTechniciansUnselected(data: SupervisorTechnician) {
                            selectedTechniciansArrayList.remove(plusPlaceholder())
                            selectedTechniciansArrayList.remove(data)
                            selectedTechniciansArrayList.add(plusPlaceholder())
                            techniciansRvAdapter.notifyDataSetChanged()
                            val userId = data.userId ?: return
                            viewModel.removeTechnician(logId, userId)
                        }
                    }
                )
            }

            rvTechnicians.adapter = techniciansRvAdapter
            rvTechnicians.layoutManager = FlexboxLayoutManager(
                this@AcSessionActivity, FlexDirection.ROW, FlexWrap.WRAP
            )
            techniciansRvAdapter.notifyDataSetChanged()
        }
    }

    private fun setupUI() {
        binding.apply {
            toolBar.setNavigationOnClickListener { finish() }

            ivPhotoDuring.setOnClickListener { requestCameraThenOpen() }

            signaturePreviewContainer.setOnClickListener { showSignaturePad() }
            btnClearSignature.setOnClickListener {
                witnessSignatureFile = null
                showSignature(null)
            }

            btnCheckOut.setOnClickListener {
                val condition = when (rgCondition.checkedRadioButtonId) {
                    rbGood.id -> "good"
                    rbRepair.id -> "needs_repair"
                    rbBroken.id -> "broken"
                    else -> ""
                }

                if (condition.isEmpty()) {
                    showFailure("Please select an AC condition", "Pilih kondisi AC")
                    return@setOnClickListener
                }

                if (photoDuringFile == null) {
                    showFailure(
                        "Please capture a photo of the maintenance in progress",
                        "Mohon ambil foto saat sedang melakukan perawatan."
                    )
                    return@setOnClickListener
                }
                val witnessName = etWitnessName.text.toString().trim()
                if (witnessName.isEmpty()) {
                    showFailure(
                        "Please enter the name of the room PIC witnessing the work",
                        "Mohon isi nama PIC ruangan sebagai saksi."
                    )
                    return@setOnClickListener
                }

                // Already a written PNG by this point: SignaturePadDialog exports on Save, so
                // there is nothing left to rasterise or to fail here.
                val signatureFile = witnessSignatureFile
                if (signatureFile == null) {
                    showFailure(
                        "Please ask the room PIC to sign",
                        "Mohon minta PIC ruangan untuk menandatangani."
                    )
                    return@setOnClickListener
                }
                val userId = UserDataHelper(this@AcSessionActivity).getUserData().id
                viewModel.checkOut(
                    logId = logId,
                    userId = userId,
                    acCondition = condition,
                    photo = photoDuringFile!!,
                    findings = etFindings.text.toString(),
                    actionsTaken = etActions.text.toString(),
                    lat = null,
                    lng = null,
                    witnessName = witnessName,
                    witnessSignature = signatureFile
                )
            }
        }
    }

    private fun setupObservers() {
        viewModel.apply {
            binding.apply {
                isLoading.observe(this@AcSessionActivity) { isLoading ->
                    progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    btnCheckOut.isEnabled = !isLoading
                }

                @Suppress("NotifyDataSetChanged")
                sessionParticipants.observe(this@AcSessionActivity) { response ->
                    // Backend orders lead first (orderByDesc is_lead), assists follow
                    val participants = response.data?.filterNotNull() ?: return@observe
                    val leadEntry = participants.firstOrNull()
                    val isCurrentUserLead = leadEntry?.userId == userData.id

                    if (isCurrentUserLead) {
                        sectionLeadTechnicians.visibility = View.VISIBLE
                        sectionAssistInfo.visibility = View.GONE
                        selectedTechniciansArrayList.clear()
                        participants.drop(1).forEach { selectedTechniciansArrayList.add(it) }
                        selectedTechniciansArrayList.add(plusPlaceholder())
                        techniciansRvAdapter.notifyDataSetChanged()
                    } else {
                        sectionLeadTechnicians.visibility = View.GONE
                        sectionAssistInfo.visibility = View.VISIBLE
                        tvLeadName.text = leadEntry?.fullName ?: "-"
                        tvAssistNames.text = participants.drop(1)
                            .mapNotNull { it.fullName?.trim() }
                            .joinToString("\n")
                            .ifEmpty { "-" }
                    }
                }

                actionResult.observe(this@AcSessionActivity) { response ->
                    val bgColor = if (response.isSuccess) R.color.custom_toast_background_success
                    else R.color.custom_toast_background_failed
                    val fontColor = if (response.isSuccess) R.color.custom_toast_font_success
                    else R.color.custom_toast_font_failed
                    CustomToast.getInstance(this@AcSessionActivity)
                        .setMessage(response.message)
                        .setBackgroundColor(ResourcesCompat.getColor(resources, bgColor, theme))
                        .setFontColor(ResourcesCompat.getColor(resources, fontColor, theme))
                        .show()
                    if (response.isSuccess) {
                        when {
                            response.message.contains("Check-out", ignoreCase = true) -> finish()
                            // add/remove results: chip list already updated optimistically in the
                            // listener; no further action needed here
                        }
                    }
                }

                // 403 is MobileAcController.validateCaller refusing the account — it is inactive
                // or gone, so nothing on this screen can succeed and the form is dead. Leave the
                // way the back button would, rather than sitting on it.
                //
                // Matched on the status rather than the message: "HTTP 403 Forbidden" is Retrofit's
                // wording and would silently stop matching if that ever changed.
                errorCode.observe(this@AcSessionActivity) { code ->
                    if (code == 403) {
                        // Delayed so the message is readable first; finishing immediately closes
                        // the screen with no explanation, which reads as a crash.
                        binding.root.postDelayed({ finish() }, ERROR_EXIT_DELAY_MS)
                    }
                }

                error.observe(this@AcSessionActivity) { errorMsg ->
                    CustomToast.getInstance(this@AcSessionActivity)
                        .setMessage(errorMsg)
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
                        .show()
                }
            }
        }
    }
    // ── Helpers ──────────────────────────────────────────────────────────────
    /**
     * A failed-validation toast. Was copied out in full per check; there are five checks now, and
     * the colour lookups were identical in every copy.
     */
    private fun showFailure(english: String, indonesian: String) {
        CustomToast.getInstance(this@AcSessionActivity)
            .setMessage(if (getString(R.string.lang) == "en") english else indonesian)
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
            .show()
    }

    private fun plusPlaceholder() = SupervisorTechnician(
        null, null, null, null, null, null, "+", null
    )

    /** Opens the signing dialog. Only its Save path writes back to witnessSignatureFile. */
    private fun showSignaturePad() {
        SignaturePadDialog(this@AcSessionActivity).apply {
            setOnSignatureSavedListener(object : SignaturePadDialog.OnSignatureSavedListener {
                override fun onSignatureSaved(file: File) {
                    witnessSignatureFile = file
                    showSignature(file)
                }
            })
        }.show()
    }

    /** Preview when signed, prompt when not. Null also covers the clear button. */
    private fun showSignature(file: File?) {
        binding.apply {
            val signed = file != null
            imgSignaturePreview.visibility = if (signed) View.VISIBLE else View.GONE
            tvSignaturePlaceholder.visibility = if (signed) View.GONE else View.VISIBLE
            btnClearSignature.visibility = if (signed) View.VISIBLE else View.GONE
            /*tvSignatureHint.visibility = if (signed) View.GONE else View.VISIBLE*/

            if (file != null) {
                imgSignaturePreview.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
            } else {
                imgSignaturePreview.setImageDrawable(null)
            }
        }
    }

    /**
     * The manifest declares CAMERA — zxing-android-embedded's manifest contributes it too, for QR
     * scanning — and once it is declared the system enforces it on ACTION_IMAGE_CAPTURE as well,
     * even though the camera app is the one actually taking the photo. Without the grant the
     * launch is refused with "Permission Denial ... with revoked permission android.permission
     * .CAMERA" and nothing happens on screen.
     *
     * Same check ProgressDoneFormActivity already does before its own openCamera().
     */
    private fun requestCameraThenOpen() {
        if (PermissionHelper.isPermissionGranted(this, PermissionHelper.CAMERA)) {
            openCamera()
        } else {
            PermissionHelper.requestPermission(
                this, arrayOf(PermissionHelper.CAMERA), PermissionHelper.REQUEST_CODE_CAMERA
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionHelper.REQUEST_CODE_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                showFailure(
                    "Camera permission is required to take the maintenance photo",
                    "Izin kamera diperlukan untuk mengambil foto perawatan."
                )
            }
        }
    }

    private fun openCamera() {
        val filename = "AC_during_${UUID.randomUUID()}.jpg"
        currentPhotoFile = File(externalCacheDir, filename)
        currentPhotoUri =
            FileProvider.getUriForFile(this, "${packageName}.provider", currentPhotoFile!!)
        takePictureLauncher.launch(currentPhotoUri!!)
    }

    private fun showTechnicianPicker() {
        val userId = userData.id
        AcSelectTechnicianBottomSheet(this, userId, selectedTechniciansArrayList)
            .apply { setOnTechnicianSelectedListener(this@AcSessionActivity) }
            .show()
    }

    // ── AcSelectTechnicianBottomSheet.OnTechnicianSelectedListener ────────────
    @SuppressLint("NotifyDataSetChanged")
    override fun onTechnicianSelected(data: SupervisorTechnician) {
        selectedTechniciansArrayList.remove(plusPlaceholder())
        selectedTechniciansArrayList.add(data)
        selectedTechniciansArrayList.add(plusPlaceholder())
        techniciansRvAdapter.notifyDataSetChanged()
        val userId = data.userId ?: return
        viewModel.addTechnician(logId, userId)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onTechnicianUnselected(data: SupervisorTechnician) {
        selectedTechniciansArrayList.remove(plusPlaceholder())
        selectedTechniciansArrayList.remove(data)
        selectedTechniciansArrayList.add(plusPlaceholder())
        techniciansRvAdapter.notifyDataSetChanged()
        val userId = data.userId ?: return
        viewModel.removeTechnician(logId, userId)
    }

    private companion object {
        /** Long enough to read the refusal before the screen closes. */
        const val ERROR_EXIT_DELAY_MS = 1500L
    }

    private fun compressImage(file: File): File {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        out.flush()
        out.close()
        return file
    }
}
