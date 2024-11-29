package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.SelectedMaterialsRvAdapters
import com.erela.fixme.bottom_sheets.ChooseFileBottomSheet
import com.erela.fixme.bottom_sheets.DepartmentListBottomSheet
import com.erela.fixme.bottom_sheets.ManageOldPhotoBottomSheet
import com.erela.fixme.bottom_sheets.ManagePhotoBottomSheet
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivitySubmissionFormBinding
import com.erela.fixme.helpers.InitAPI
import com.erela.fixme.helpers.PermissionHelper
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.CategoryListResponse
import com.erela.fixme.objects.DepartmentListResponse
import com.erela.fixme.objects.FotoGaprojectsItem
import com.erela.fixme.objects.MaterialListResponse
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.SubmitSubmissionResponse
import com.erela.fixme.objects.UpdateSubmissionResponse
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SubmissionFormActivity : AppCompatActivity() {
    private val binding: ActivitySubmissionFormBinding by lazy {
        ActivitySubmissionFormBinding.inflate(layoutInflater)
    }
    private var detail: SubmissionDetailResponse? = null
    private var selectedDept: DepartmentListResponse? = null
    private var selectedCategory: Int = 0
    private var selectedDepartment: Int = 0
    private var selectedMaterialsArrayList: ArrayList<MaterialListResponse> = ArrayList()
    private lateinit var materialAdapter: SelectedMaterialsRvAdapters
    private val imageArrayUri = ArrayList<Uri>()
    private val oldImageArray = ArrayList<FotoGaprojectsItem>()
    private var deletedOldImageArray: ArrayList<Int> = ArrayList()
    private var cameraCaptureFileName: String = ""
    private lateinit var imageUri: Uri
    private val photoFiles: ArrayList<MultipartBody.Part> = ArrayList()
    private val requestBodyMap: MutableMap<String, RequestBody> = mutableMapOf()
    private var isFormEmpty = arrayOf(
        false,
        false,
        false,
        false,
        false,
        false,
        false
    )
    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        with(it) {
            if (resultCode == RESULT_OK) {
                val imageUrl: Uri? = data?.data
                if (imageUrl != null) {
                    for (element in imageArrayUri) {
                        if (element == imageUrl)
                            return@registerForActivityResult
                        else
                            imageArrayUri.add(imageUrl)
                    }
                }
                setManageAttachment()
            }
        }
    }

    private val galleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        with(it) {
            if (resultCode == RESULT_OK) {
                if (data?.clipData != null) {
                    val mClipData: ClipData = data!!.clipData!!
                    for (i in 0 until mClipData.itemCount) {
                        val imageUrl: Uri = mClipData.getItemAt(i).uri
                        for (element in imageArrayUri) {
                            if (element == imageUrl)
                                return@registerForActivityResult
                            else
                                imageArrayUri.add(imageUrl)
                        }
                    }
                } else {
                    val imageUrl: Uri? = data?.data
                    if (imageUrl != null) {
                        for (element in imageArrayUri) {
                            if (element == imageUrl)
                                return@registerForActivityResult
                            else
                                imageArrayUri.add(imageUrl)
                        }
                    }
                }
                setManageAttachment()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()
        formInput()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun init() {
        binding.apply {
            backButton.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            machineCodeFieldLayout.visibility = View.GONE
            machineNameFieldLayout.visibility = View.GONE

            detail = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("data", SubmissionDetailResponse::class.java)!!
                } else {
                    intent.getParcelableExtra("data")!!
                }
            } catch (nullPointerException: NullPointerException) {
                null
            }

            if (detail != null) {
                Log.e("Detail Data", detail.toString())
                pageTitle.text = getString(R.string.edit_submission_title)
                caseTitleField.setText("${detail?.judulKasus}")
                isFormEmpty[0] = true
                locationField.setText("${detail?.lokasi}")
                isFormEmpty[1] = true
                selectedDepartment = detail?.idDeptTujuan!!.toInt()
                isFormEmpty[2] = true
                getDepartmentList()
                selectedCategory = detail?.idKategori!!.toInt()
                isFormEmpty[3] = true
                getCategoryList()
                isFormEmpty[4] = detail?.kodeMesin!!.isNotEmpty()
                machineCodeField.setText("${detail?.kodeMesin}".ifEmpty { "" })
                isFormEmpty[5] = detail?.namaMesin!!.isNotEmpty()
                machineNameField.setText("${detail?.namaMesin}".ifEmpty { "" })
                descriptionField.setText("${detail?.keterangan}")
                isFormEmpty[6] = true
                if (detail?.material!!.isNotEmpty()) {
                    InitAPI.getAPI.getMaterialList().enqueue(
                        object : Callback<List<MaterialListResponse>> {
                            override fun onResponse(
                                call: Call<List<MaterialListResponse>>,
                                response: Response<List<MaterialListResponse>>
                            ) {
                                if (response.isSuccessful) {
                                    if (response.body() != null) {
                                        for (i in 0 until detail?.material!!.size) {
                                            for (j in 0 until response.body()!!.size) {
                                                if (detail?.material!![i]?.idMaterial == response.body()!![j].idMaterial?.toInt()) {
                                                    selectedMaterialsArrayList.add(
                                                        response.body()!![j]
                                                    )
                                                }
                                            }
                                        }
                                        prepareMaterials()
                                    }
                                } else {
                                    Log.e("ERROR", response.message())
                                    prepareMaterials()
                                }
                            }

                            override fun onFailure(
                                call: Call<List<MaterialListResponse>>, throwable: Throwable
                            ) {
                                Log.e("ERROR", throwable.message.toString())
                                throwable.printStackTrace()
                                prepareMaterials()
                            }
                        })
                } else {
                    prepareMaterials()
                }
                if (detail?.fotoGaprojects!!.isNotEmpty()) {
                    for (element in detail?.fotoGaprojects!!) {
                        if (element != null) {
                            oldImageArray.add(
                                element
                            )
                        }
                    }
                }
                if (oldImageArray.isNotEmpty()) {
                    manageOldAttachmentButton.visibility = View.VISIBLE
                    manageOldAttachmentButton.setOnClickListener {
                        val bottomSheet = ManageOldPhotoBottomSheet(
                            this@SubmissionFormActivity, oldImageArray
                        ).also {
                            with(it) {
                                setOnAttachmentActionListener(object :
                                    ManageOldPhotoBottomSheet.OnAttachmentActionListener {
                                    override fun onDeleteOldPhoto(
                                        fotoGaProjectsItem: FotoGaprojectsItem
                                    ) {
                                        deletedOldImageArray.add(
                                            fotoGaProjectsItem.idFotoGaprojects!!
                                        )
                                        oldImageArray.remove(fotoGaProjectsItem)
                                        if (oldImageArray.isEmpty()) {
                                            manageOldAttachmentButton.visibility = View.GONE
                                        }
                                    }
                                })
                            }
                        }

                        if (bottomSheet.window != null)
                            bottomSheet.show()
                    }
                }
                submitButton.setOnClickListener {
                    submitButton.visibility = View.GONE
                    loadingBar.visibility = View.VISIBLE
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                    if (!formCheck()) {
                        submitButton.visibility = View.VISIBLE
                        loadingBar.visibility = View.GONE
                        CustomToast.getInstance(applicationContext)
                            .setMessage(
                                "Please make sure all fields in the form are filled in."
                            )
                            .setBackgroundColor(
                                ContextCompat.getColor(
                                    this@SubmissionFormActivity,
                                    R.color.custom_toast_background_failed
                                )
                            )
                            .setFontColor(
                                ContextCompat.getColor(
                                    this@SubmissionFormActivity,
                                    R.color.custom_toast_font_failed
                                )
                            ).show()
                    } else {
                        if (prepareUpdateForm()) {
                            Log.e("Photo Files", photoFiles.toString())
                            try {
                                (if (photoFiles.isNotEmpty()) {
                                    InitAPI.getAPI.updateSubmission(requestBodyMap, photoFiles)
                                } else {
                                    InitAPI.getAPI.updateSubmissionNoAttachment(requestBodyMap)
                                }).enqueue(object : Callback<UpdateSubmissionResponse> {
                                    override fun onResponse(
                                        call: Call<UpdateSubmissionResponse>,
                                        response: Response<UpdateSubmissionResponse>
                                    ) {
                                        submitButton.visibility = View.VISIBLE
                                        loadingBar.visibility = View.GONE
                                        Log.e("Response Update", response.body().toString())
                                        if (response.isSuccessful) {
                                            if (response.body() != null) {
                                                if (response.body()?.code == 1) {
                                                    CustomToast.getInstance(applicationContext)
                                                        .setMessage(
                                                            "${response.body()?.message.toString()}."
                                                        )
                                                        .setBackgroundColor(
                                                            ContextCompat.getColor(
                                                                this@SubmissionFormActivity,
                                                                R.color.custom_toast_background_success
                                                            )
                                                        )
                                                        .setFontColor(
                                                            ContextCompat.getColor(
                                                                this@SubmissionFormActivity,
                                                                R.color.custom_toast_font_success
                                                            )
                                                        ).show()
                                                    setResult(RESULT_OK)
                                                    finish()
                                                } else {
                                                    CustomToast.getInstance(applicationContext)
                                                        .setMessage(
                                                            "${response.body()?.message.toString()}!"
                                                        )
                                                        .setFontColor(
                                                            ContextCompat.getColor(
                                                                this@SubmissionFormActivity,
                                                                R.color.custom_toast_font_failed
                                                            )
                                                        )
                                                        .setBackgroundColor(
                                                            ContextCompat.getColor(
                                                                this@SubmissionFormActivity,
                                                                R.color.custom_toast_background_failed
                                                            )
                                                        ).show()
                                                }
                                            }
                                        } else {
                                            CustomToast.getInstance(applicationContext)
                                                .setMessage("Update submission failed.")
                                                .setFontColor(
                                                    ContextCompat.getColor(
                                                        this@SubmissionFormActivity,
                                                        R.color.custom_toast_font_failed
                                                    )
                                                )
                                                .setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        this@SubmissionFormActivity,
                                                        R.color.custom_toast_background_failed
                                                    )
                                                ).show()
                                            Log.e("ERROR", response.message())
                                        }
                                    }

                                    override fun onFailure(
                                        call: Call<UpdateSubmissionResponse>, throwable: Throwable
                                    ) {
                                        submitButton.visibility = View.VISIBLE
                                        loadingBar.visibility = View.GONE
                                        CustomToast.getInstance(applicationContext)
                                            .setMessage("Something went wrong, please try again.")
                                            .setFontColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionFormActivity,
                                                    R.color.custom_toast_font_failed
                                                )
                                            )
                                            .setBackgroundColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionFormActivity,
                                                    R.color.custom_toast_background_failed
                                                )
                                            ).show()
                                        Log.e("ERROR", throwable.toString())
                                        throwable.printStackTrace()
                                    }
                                })
                            } catch (jsonException: JSONException) {
                                submitButton.visibility = View.VISIBLE
                                loadingBar.visibility = View.GONE
                                CustomToast.getInstance(applicationContext)
                                    .setMessage("Something went wrong, please try again.")
                                    .setFontColor(
                                        ContextCompat.getColor(
                                            this@SubmissionFormActivity,
                                            R.color.custom_toast_font_failed
                                        )
                                    )
                                    .setBackgroundColor(
                                        ContextCompat.getColor(
                                            this@SubmissionFormActivity,
                                            R.color.custom_toast_background_failed
                                        )
                                    ).show()
                                Log.e("ERROR", jsonException.toString())
                                jsonException.printStackTrace()
                            }
                        }
                    }
                }
            } else {
                pageTitle.text = getString(R.string.make_submission_title)
                getDepartmentList()
                getCategoryList()
                prepareMaterials()

                submitButton.setOnClickListener {
                    submitButton.visibility = View.GONE
                    loadingBar.visibility = View.VISIBLE
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                    if (!formCheck()) {
                        submitButton.visibility = View.VISIBLE
                        loadingBar.visibility = View.GONE
                        CustomToast.getInstance(applicationContext)
                            .setMessage("Please make sure all fields in the form are filled in.")
                            .setBackgroundColor(
                                ContextCompat.getColor(
                                    this@SubmissionFormActivity,
                                    R.color.custom_toast_background_failed
                                )
                            )
                            .setFontColor(
                                ContextCompat.getColor(
                                    this@SubmissionFormActivity,
                                    R.color.custom_toast_font_failed
                                )
                            ).show()
                    } else {
                        if (prepareSubmitForm()) {
                            Log.e("Photo Files", photoFiles.toString())
                            try {
                                (if (photoFiles.isNotEmpty()) {
                                    InitAPI.getAPI.submitSubmission(requestBodyMap, photoFiles)
                                } else {
                                    InitAPI.getAPI.submitSubmissionNoAttachment(requestBodyMap)
                                }).enqueue(object : Callback<SubmitSubmissionResponse> {
                                    override fun onResponse(
                                        call: Call<SubmitSubmissionResponse?>,
                                        response: Response<SubmitSubmissionResponse?>
                                    ) {
                                        submitButton.visibility = View.VISIBLE
                                        loadingBar.visibility = View.GONE
                                        if (response.isSuccessful) {
                                            if (response.body() != null) {
                                                if (response.body()?.code == 1) {
                                                    CustomToast.getInstance(applicationContext)
                                                        .setMessage(
                                                            "${response.body()?.message.toString()}. Form submitted successfully."
                                                        )
                                                        .setBackgroundColor(
                                                            ContextCompat.getColor(
                                                                this@SubmissionFormActivity,
                                                                R.color.custom_toast_background_success
                                                            )
                                                        )
                                                        .setFontColor(
                                                            ContextCompat.getColor(
                                                                this@SubmissionFormActivity,
                                                                R.color.custom_toast_font_success
                                                            )
                                                        ).show()
                                                    finish()
                                                } else {
                                                    CustomToast.getInstance(applicationContext)
                                                        .setMessage(
                                                            "${response.body()?.message.toString()}!"
                                                        )
                                                        .setFontColor(
                                                            ContextCompat.getColor(
                                                                this@SubmissionFormActivity,
                                                                R.color.custom_toast_font_failed
                                                            )
                                                        )
                                                        .setBackgroundColor(
                                                            ContextCompat.getColor(
                                                                this@SubmissionFormActivity,
                                                                R.color.custom_toast_background_failed
                                                            )
                                                        ).show()
                                                }
                                            }
                                        } else {
                                            CustomToast.getInstance(applicationContext)
                                                .setMessage("Form submission failed.")
                                                .setFontColor(
                                                    ContextCompat.getColor(
                                                        this@SubmissionFormActivity,
                                                        R.color.custom_toast_font_failed
                                                    )
                                                )
                                                .setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        this@SubmissionFormActivity,
                                                        R.color.custom_toast_background_failed
                                                    )
                                                ).show()
                                            Log.e("ERROR", response.message())
                                        }
                                    }

                                    override fun onFailure(
                                        call: Call<SubmitSubmissionResponse?>,
                                        throwable: Throwable
                                    ) {
                                        submitButton.visibility = View.VISIBLE
                                        loadingBar.visibility = View.GONE
                                        CustomToast.getInstance(applicationContext)
                                            .setMessage("Something went wrong, please try again.")
                                            .setFontColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionFormActivity,
                                                    R.color.custom_toast_font_failed
                                                )
                                            )
                                            .setBackgroundColor(
                                                ContextCompat.getColor(
                                                    this@SubmissionFormActivity,
                                                    R.color.custom_toast_background_failed
                                                )
                                            ).show()
                                        Log.e("ERROR", throwable.toString())
                                        throwable.printStackTrace()
                                    }
                                })
                            } catch (jsonException: JSONException) {
                                submitButton.visibility = View.VISIBLE
                                loadingBar.visibility = View.GONE
                                CustomToast.getInstance(applicationContext)
                                    .setMessage("Something went wrong, please try again.")
                                    .setFontColor(
                                        ContextCompat.getColor(
                                            this@SubmissionFormActivity,
                                            R.color.custom_toast_font_failed
                                        )
                                    )
                                    .setBackgroundColor(
                                        ContextCompat.getColor(
                                            this@SubmissionFormActivity,
                                            R.color.custom_toast_background_failed
                                        )
                                    ).show()
                                Log.e("ERROR", jsonException.toString())
                                jsonException.printStackTrace()
                            }
                        }
                    }
                }
            }

            chooseFileButton.setOnClickListener {
                val bottomSheet = ChooseFileBottomSheet(this@SubmissionFormActivity).also {
                    with(it) {
                        setOnChooseFileListener(object :
                            ChooseFileBottomSheet.OnChooseFileListener {
                            override fun onOpenCameraClicked() {
                                if (PermissionHelper.isPermissionGranted(
                                        this@SubmissionFormActivity,
                                        PermissionHelper.CAMERA
                                    )
                                ) {
                                    openCamera()
                                } else {
                                    PermissionHelper.requestPermission(
                                        this@SubmissionFormActivity,
                                        arrayOf(PermissionHelper.CAMERA),
                                        PermissionHelper.REQUEST_CODE_CAMERA
                                    )
                                }
                                dismiss()
                            }

                            override fun onOpenGalleryClicked() {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (PermissionHelper.isPermissionGranted(
                                            this@SubmissionFormActivity,
                                            PermissionHelper.READ_MEDIA_IMAGES
                                        ) || PermissionHelper.isPermissionGranted(
                                            this@SubmissionFormActivity,
                                            PermissionHelper.READ_MEDIA_VIDEO
                                        )
                                    ) {
                                        openGallery()
                                    } else {
                                        PermissionHelper.requestPermission(
                                            this@SubmissionFormActivity,
                                            arrayOf(
                                                PermissionHelper.READ_MEDIA_IMAGES,
                                                PermissionHelper.READ_MEDIA_VIDEO
                                            ),
                                            PermissionHelper.REQUEST_CODE_GALLERY
                                        )
                                    }
                                } else {
                                    if (PermissionHelper.isPermissionGranted(
                                            this@SubmissionFormActivity,
                                            PermissionHelper.READ_EXTERNAL_STORAGE
                                        )
                                    ) {
                                        openGallery()
                                    } else {
                                        PermissionHelper.requestPermission(
                                            this@SubmissionFormActivity,
                                            arrayOf(PermissionHelper.READ_EXTERNAL_STORAGE),
                                            PermissionHelper.REQUEST_CODE_GALLERY
                                        )
                                    }
                                }
                                dismiss()
                            }
                        })
                    }
                }

                if (bottomSheet.window != null)
                    bottomSheet.show()
            }
        }
    }

    private fun prepareUpdateForm(): Boolean {
        binding.apply {
            if (imageArrayUri.isNotEmpty()) {
                for (i in 0 until imageArrayUri.size) {
                    photoFiles.add(
                        createMultipartBody(imageArrayUri[i], "foto[]")
                    )
                }
            }
            with(requestBodyMap) {
                put(
                    "id_user", createPartFromString(
                        UserDataHelper(
                            applicationContext
                        ).getUserData().id.toString()
                    )!!
                )
                put(
                    "id_gaprojects",
                    createPartFromString(detail?.idGaprojects.toString())!!
                )
                put(
                    "judul_kasus",
                    createPartFromString(caseTitleField.text.toString())!!
                )
                put("lokasi", createPartFromString(locationField.text.toString())!!)
                put(
                    "departemen",
                    createPartFromString(selectedDepartment.toString())!!
                )
                put("kategori", createPartFromString(selectedCategory.toString())!!)
                put(
                    "kode_mesin",
                    createPartFromString(machineCodeField.text.toString())!!
                )
                put(
                    "nama_mesin",
                    createPartFromString(machineNameField.text.toString())!!
                )
                put(
                    "keterangan",
                    createPartFromString(descriptionField.text.toString())!!
                )
                if (selectedMaterialsArrayList.size > 1) {
                    for (i in 0 until selectedMaterialsArrayList.size - 1) {
                        put(
                            "material[$i]",
                            createPartFromString(
                                selectedMaterialsArrayList[i].idMaterial
                            )!!
                        )
                    }
                }
                if (deletedOldImageArray.isNotEmpty()) {
                    for (element in deletedOldImageArray) {
                        put(
                            "foto_old[]",
                            createPartFromString(
                                element.toString()
                            )!!
                        )
                    }
                }
            }
        }

        return if (requestBodyMap.isNotEmpty()) {
            if (photoFiles.isNotEmpty())
                true
            else
                true
        } else
            false
    }

    private fun prepareSubmitForm(): Boolean {
        binding.apply {
            if (imageArrayUri.isNotEmpty()) {
                for (i in 0 until imageArrayUri.size) {
                    photoFiles.add(
                        createMultipartBody(imageArrayUri[i], "foto[]")
                    )
                }
            }
            with(requestBodyMap) {
                put(
                    "id_user", createPartFromString(
                        UserDataHelper(applicationContext).getUserData().id.toString()
                    )!!
                )
                put(
                    "judul_kasus",
                    createPartFromString(caseTitleField.text.toString())!!
                )
                put("lokasi", createPartFromString(locationField.text.toString())!!)
                put("departemen", createPartFromString(selectedDepartment.toString())!!)
                put("kategori", createPartFromString(selectedCategory.toString())!!)
                put(
                    "kode_mesin",
                    createPartFromString(machineCodeField.text.toString())!!
                )
                put(
                    "nama_mesin",
                    createPartFromString(machineNameField.text.toString())!!
                )
                put(
                    "keterangan",
                    createPartFromString(descriptionField.text.toString())!!
                )
                if (selectedMaterialsArrayList.size > 1) {
                    for (i in 0 until selectedMaterialsArrayList.size - 1) {
                        put(
                            "material[$i]",
                            createPartFromString(
                                selectedMaterialsArrayList[i].idMaterial
                            )!!
                        )
                    }
                }
            }
        }

        return if (requestBodyMap.isNotEmpty()) {
            if (photoFiles.isNotEmpty())
                true
            else
                true
        } else
            false
    }

    private fun formInput() {
        binding.apply {
            caseTitleField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int,
                    count: Int
                ) {
                    if (s!!.isEmpty()) {
                        caseTitleFieldLayout.error = "Title can't be empty!"
                        isFormEmpty[0] = false
                    } else {
                        caseTitleFieldLayout.error = null
                        isFormEmpty[0] = true
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
            locationField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int,
                    count: Int
                ) {
                    if (s!!.isEmpty()) {
                        locationFieldLayout.error = "Location can't be empty!"
                        isFormEmpty[1] = false
                    } else {
                        locationFieldLayout.error = null
                        isFormEmpty[1] = true
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
            descriptionField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int,
                    count: Int
                ) {
                    if (s!!.isEmpty()) {
                        descriptionFieldLayout.error = "Description"
                        isFormEmpty[4] = false
                    } else {
                        descriptionFieldLayout.error = null
                        isFormEmpty[4] = true
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
            if (machineCodeFieldLayout.visibility == View.VISIBLE) {
                machineCodeField.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int,
                        count: Int
                    ) {
                        if (s!!.isEmpty()) {
                            machineCodeFieldLayout.error = "Enter the Machine Code!"
                            isFormEmpty[5] = false
                        } else {
                            machineCodeFieldLayout.error = null
                            isFormEmpty[5] = true
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })
            }
            if (machineNameFieldLayout.visibility == View.VISIBLE) {
                machineNameField.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int,
                        count: Int
                    ) {
                        if (s!!.isEmpty()) {
                            machineNameFieldLayout.error = "Enter the Machine Name!"
                            isFormEmpty[6] = false
                        } else {
                            machineNameFieldLayout.error = null
                            isFormEmpty[6] = true
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })
            }
        }
    }

    private fun formCheck(): Boolean {
        var validated = 0
        var isFormValid = false
        binding.apply {
            for (element in isFormEmpty) {
                if (element)
                    validated++
            }
            if (caseTitleField.text!!.isEmpty())
                caseTitleFieldLayout.error = "Title can't be empty!"

            if (locationField.text!!.isEmpty())
                locationFieldLayout.error = "Location can't be empty!"

            if (descriptionField.text!!.isEmpty())
                descriptionFieldLayout.error = "Description can't be empty!"

            if (selectedCategory == 0)
                categoryDropdownLayout.strokeColor = ContextCompat.getColor(
                    this@SubmissionFormActivity,
                    R.color.custom_toast_font_failed
                )

            if (selectedDepartment == 0)
                departmentDropdownLayout.strokeColor = ContextCompat.getColor(
                    this@SubmissionFormActivity,
                    R.color.custom_toast_font_failed
                )
            else {
                if (selectedDepartmentText.text.contains("Engineering")) {
                    if (machineCodeField.text!!.isEmpty())
                        machineCodeFieldLayout.error = "Enter the Machine Code!"

                    if (machineNameField.text!!.isEmpty())
                        machineNameFieldLayout.error = "Enter the Machine Name!"

                    isFormValid = validated == 7
                } else
                    isFormValid = validated == 5
            }
        }
        Log.e("Form Valid | Validated", "$isFormValid | $validated")
        return isFormValid
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionHelper.REQUEST_CODE_GALLERY) {
            if (grantResults.isNotEmpty()) {
                if (grantResults[0] == PERMISSION_GRANTED) {
                    openGallery()
                }
            }
        }
        if (requestCode == PermissionHelper.REQUEST_CODE_CAMERA) {
            if (grantResults.isNotEmpty()) {
                if (grantResults[0] == PERMISSION_GRANTED) {
                    openCamera()
                }
            }
        }
    }

    private fun openGallery() {
        galleryLauncher.launch(
            Intent().also {
                it.type = "image/*"
                it.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                it.action = Intent.ACTION_GET_CONTENT
            }
        )
    }

    private fun openCamera() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        cameraCaptureFileName = "FixMe_Capture_$timeStamp.jpg"
        imageUri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().also {
                with(it) {
                    put(MediaStore.Images.Media.TITLE, cameraCaptureFileName)
                    put(MediaStore.Images.Media.DESCRIPTION, "Image capture by camera")
                }
            }
        )!!

        cameraLauncher.launch(
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also {
                with(it) {
                    putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                }
            }
        )
    }

    private fun setManageAttachment() {
        binding.apply {
            manageAttachmentButton.setOnClickListener {
                val bottomSheet = ManagePhotoBottomSheet(
                    this@SubmissionFormActivity, imageArrayUri
                ).also {
                    with(it) {
                        setOnAttachmentActionListener(object :
                            ManagePhotoBottomSheet.OnAttachmentActionListener {
                            override fun onDeletePhoto(uri: Uri) {
                                imageArrayUri.remove(uri)
                                if (imageArrayUri.isEmpty()) {
                                    manageAttachmentButton.visibility = View.GONE
                                }
                            }
                        })
                    }
                }

                if (bottomSheet.window != null)
                    bottomSheet.show()
            }
            if (imageArrayUri.isNotEmpty()) {
                manageAttachmentButton.visibility = View.VISIBLE
            } else {
                manageAttachmentButton.visibility = View.GONE
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getDepartmentList() {
        binding.apply {
            try {
                InitAPI.getAPI.getDepartmentList()
                    .enqueue(object : Callback<List<DepartmentListResponse>> {
                        override fun onResponse(
                            call: Call<List<DepartmentListResponse>>,
                            response: Response<List<DepartmentListResponse>>
                        ) {
                            if (response.isSuccessful) {
                                if (response.body() != null) {
                                    if (selectedDepartment != 0) {
                                        for (i in 0 until response.body()!!.size) {
                                            if (response.body()!![i].idDept!!.toInt() == selectedDepartment) {
                                                selectedDepartmentText.text =
                                                    "${response.body()!![i].namaDept}\n\"${response.body()!![i].subDept}\""
                                                this@SubmissionFormActivity.selectedDept =
                                                    response.body()!![i]
                                            }
                                        }
                                        if (selectedDepartmentText.text.contains(
                                                "Engineering"
                                            )
                                        ) {
                                            machineCodeFieldLayout.visibility =
                                                View.VISIBLE
                                            machineNameFieldLayout.visibility =
                                                View.VISIBLE
                                        } else {
                                            machineCodeFieldLayout.visibility =
                                                View.GONE
                                            machineNameFieldLayout.visibility =
                                                View.GONE
                                        }
                                    }
                                    departmentDropdownLayout.setOnClickListener {
                                        val departmentBottomSheet = DepartmentListBottomSheet(
                                            this@SubmissionFormActivity,
                                            response.body()!!,
                                            selectedDept
                                        ).also {
                                            with(it) {
                                                onDepartmentClickListener(object :
                                                    DepartmentListBottomSheet.OnDepartmentClickListener {
                                                    override fun onDepartmentClick(
                                                        data: DepartmentListResponse
                                                    ) {
                                                        selectedDepartment =
                                                            data.idDept!!.toInt()
                                                        this@SubmissionFormActivity.selectedDept =
                                                            data
                                                        selectedDepartmentText.text =
                                                            "${data.namaDept}\n\"${data.subDept}\""
                                                        isFormEmpty[2] = selectedDepartment != 0
                                                        if (selectedDepartment != 0) {
                                                            departmentDropdownLayout.strokeColor =
                                                                ContextCompat.getColor(
                                                                    this@SubmissionFormActivity,
                                                                    R.color.form_field_stroke
                                                                )
                                                        }
                                                        if (selectedDepartmentText.text.contains(
                                                                "Engineering"
                                                            )
                                                        ) {
                                                            machineCodeFieldLayout.visibility =
                                                                View.VISIBLE
                                                            machineNameFieldLayout.visibility =
                                                                View.VISIBLE
                                                        } else {
                                                            machineCodeFieldLayout.visibility =
                                                                View.GONE
                                                            machineNameFieldLayout.visibility =
                                                                View.GONE
                                                        }
                                                        dismiss()
                                                    }
                                                })
                                            }
                                        }

                                        if (departmentBottomSheet.window != null)
                                            departmentBottomSheet.show()
                                    }
                                }
                            } else {
                                Log.e("ERROR", response.message())
                                CustomToast.getInstance(applicationContext)
                                    .setMessage("Something went wrong, please try again.")
                                    .setFontColor(
                                        ContextCompat.getColor(
                                            this@SubmissionFormActivity,
                                            R.color.custom_toast_font_failed
                                        )
                                    )
                                    .setBackgroundColor(
                                        ContextCompat.getColor(
                                            this@SubmissionFormActivity,
                                            R.color.custom_toast_background_failed
                                        )
                                    ).show()
                                finish()
                            }
                        }

                        override fun onFailure(
                            call: Call<List<DepartmentListResponse>>,
                            throwable: Throwable
                        ) {
                            throwable.printStackTrace()
                            CustomToast.getInstance(applicationContext)
                                .setMessage("Something went wrong, please try again.")
                                .setFontColor(
                                    ContextCompat.getColor(
                                        this@SubmissionFormActivity,
                                        R.color.custom_toast_font_failed
                                    )
                                )
                                .setBackgroundColor(
                                    ContextCompat.getColor(
                                        this@SubmissionFormActivity,
                                        R.color.custom_toast_background_failed
                                    )
                                ).show()
                            finish()
                        }
                    })
            } catch (exception: Exception) {
                exception.printStackTrace()
                CustomToast.getInstance(applicationContext)
                    .setMessage("Something went wrong, please try again.")
                    .setFontColor(
                        ContextCompat.getColor(
                            this@SubmissionFormActivity,
                            R.color.custom_toast_font_failed
                        )
                    )
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            this@SubmissionFormActivity,
                            R.color.custom_toast_background_failed
                        )
                    ).show()
                finish()
            }
        }
    }

    private fun getCategoryList() {
        binding.apply {
            try {
                InitAPI.getAPI.getCategoryList()
                    .enqueue(object : Callback<List<CategoryListResponse>> {
                        override fun onResponse(
                            call: Call<List<CategoryListResponse>?>,
                            response: Response<List<CategoryListResponse>?>
                        ) {
                            if (response.isSuccessful) {
                                if (response.body() != null) {
                                    val data: ArrayList<String> = ArrayList()
                                    data.add("Select Category")
                                    val categoryList = response.body()
                                    for (i in 0 until categoryList!!.size) {
                                        data.add(
                                            categoryList[i].namaKategori!!
                                        )
                                    }
                                    val dropdownAdapter = ArrayAdapter(
                                        this@SubmissionFormActivity,
                                        R.layout.general_dropdown_item,
                                        R.id.dropdownItemText,
                                        data.distinct()
                                    )
                                    categoryDropdown.adapter = dropdownAdapter
                                    if (selectedCategory != 0) {
                                        for (i in 0 until response.body()!!.size) {
                                            if (response.body()!![i].idKategori!!.toInt() == selectedCategory) {
                                                categoryDropdown.setSelection(
                                                    dropdownAdapter.getPosition(
                                                        response.body()!![i].namaKategori
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    categoryDropdown.onItemSelectedListener =
                                        object : AdapterView.OnItemSelectedListener {
                                            override fun onItemSelected(
                                                parent: AdapterView<*>?,
                                                view: View?, position: Int,
                                                id: Long
                                            ) {
                                                selectedCategory = if (position == 0)
                                                    0
                                                else
                                                    categoryList[position - 1].idKategori!!.toInt()
                                                isFormEmpty[3] = selectedCategory != 0
                                                if (selectedCategory != 0) {
                                                    categoryDropdownLayout.strokeColor =
                                                        ContextCompat.getColor(
                                                            this@SubmissionFormActivity,
                                                            R.color.form_field_stroke
                                                        )
                                                }
                                            }

                                            override fun onNothingSelected(
                                                parent: AdapterView<*>?
                                            ) {
                                            }
                                        }
                                }
                            } else {
                                Log.e("ERROR", response.message())
                                CustomToast.getInstance(applicationContext)
                                    .setMessage("Something went wrong, please try again.")
                                    .setFontColor(
                                        ContextCompat.getColor(
                                            this@SubmissionFormActivity,
                                            R.color.custom_toast_font_failed
                                        )
                                    )
                                    .setBackgroundColor(
                                        ContextCompat.getColor(
                                            this@SubmissionFormActivity,
                                            R.color.custom_toast_background_failed
                                        )
                                    ).show()
                                finish()
                            }
                        }

                        override fun onFailure(
                            call: Call<List<CategoryListResponse>?>,
                            throwable: Throwable
                        ) {
                        }
                    })
            } catch (exception: Exception) {
                exception.printStackTrace()
                CustomToast.getInstance(applicationContext)
                    .setMessage("Something went wrong, please try again.")
                    .setFontColor(
                        ContextCompat.getColor(
                            this@SubmissionFormActivity,
                            R.color.custom_toast_font_failed
                        )
                    )
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            this@SubmissionFormActivity,
                            R.color.custom_toast_background_failed
                        )
                    ).show()
                finish()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun prepareMaterials() {
        binding.apply {
            selectedMaterialsArrayList.add(
                MaterialListResponse(
                    null,
                    null,
                    "+",
                    null,
                    null,
                    null
                )
            )

            materialAdapter = SelectedMaterialsRvAdapters(
                this@SubmissionFormActivity, selectedMaterialsArrayList
            ).also {
                with(it) {
                    setOnMaterialsSetListener(object :
                        SelectedMaterialsRvAdapters.OnMaterialsSetListener {
                        override fun onMaterialsSelected(
                            data: MaterialListResponse
                        ) {
                            selectedMaterialsArrayList.remove(
                                MaterialListResponse(
                                    null,
                                    null,
                                    "+",
                                    null,
                                    null,
                                    null
                                )
                            )
                            selectedMaterialsArrayList.add(data)
                            selectedMaterialsArrayList.add(
                                MaterialListResponse(
                                    null,
                                    null,
                                    "+",
                                    null,
                                    null,
                                    null
                                )
                            )
                            materialAdapter.notifyDataSetChanged()
                        }

                        override fun onMaterialsUnselected(
                            data: MaterialListResponse
                        ) {
                            selectedMaterialsArrayList.remove(
                                MaterialListResponse(
                                    null,
                                    null,
                                    "+",
                                    null,
                                    null,
                                    null
                                )
                            )
                            selectedMaterialsArrayList.remove(data)
                            selectedMaterialsArrayList.add(
                                MaterialListResponse(
                                    null,
                                    null,
                                    "+",
                                    null,
                                    null,
                                    null
                                )
                            )
                            materialAdapter.notifyDataSetChanged()
                        }
                    })
                }
            }
            rvMaterials.adapter = materialAdapter
            rvMaterials.layoutManager = FlexboxLayoutManager(
                this@SubmissionFormActivity, FlexDirection.ROW, FlexWrap.WRAP
            )
            materialAdapter.notifyDataSetChanged()
        }
    }

    private fun createMultipartBody(uri: Uri, multipartName: String): MultipartBody.Part {
        val documentImage =
            BitmapFactory.decodeFile(getRealPathFromURI(uri))
        val file = File(getRealPathFromURI(uri))
        val os: OutputStream = BufferedOutputStream(FileOutputStream(file))
        documentImage.compress(Bitmap.CompressFormat.JPEG, 100, os)
        os.close()
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(multipartName, file.name, requestBody)
    }

    private fun getRealPathFromURI(uri: Uri): String {
        val returnCursor = contentResolver.query(uri, null, null, null, null)
        val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        val file = File(filesDir, name)
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            var read = 0
            val maxBufferSize = 1 * 1024 * 1024
            val bytesAvailable: Int = inputStream?.available() ?: 0
            //int bufferSize = 1024;
            val bufferSize = bytesAvailable.coerceAtMost(maxBufferSize)
            val buffers = ByteArray(bufferSize)
            while (inputStream?.read(buffers).also {
                    if (it != null) {
                        read = it
                    }
                } != -1) {
                outputStream.write(buffers, 0, read)
            }
            inputStream?.close()
            outputStream.close()
        } catch (e: java.lang.Exception) {
            Log.e("Exception", e.message!!)
        }
        return file.path
    }

    private fun createPartFromString(stringData: String?): RequestBody? {
        return stringData?.toRequestBody("text/plain".toMediaTypeOrNull())
    }
}