package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.SelectedMaterialsRvAdapters
import com.erela.fixme.bottom_sheets.MaterialQuantityBottomSheet
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityProgressFormBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.networking.InitAPI
import com.erela.fixme.objects.CreationResponse
import com.erela.fixme.objects.GenericSimpleResponse
import com.erela.fixme.objects.MaterialListResponse
import com.erela.fixme.objects.ProgressItem
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProgressFormActivity : AppCompatActivity() {
    private val binding: ActivityProgressFormBinding by lazy {
        ActivityProgressFormBinding.inflate(layoutInflater)
    }
    private val userData: UserData by lazy {
        UserDataHelper(applicationContext).getUserData()
    }
    private var detail: SubmissionDetailResponse? = null
    private var progressData: ProgressItem? = null
    private var selectedMaterialsArrayList: ArrayList<MaterialListResponse> = ArrayList()
    private lateinit var materialAdapter: SelectedMaterialsRvAdapters
    private var materialQuantityList: ArrayList<Int> = ArrayList()

    /*private val imageArrayUri = ArrayList<Uri>()
    private val oldImageArray = ArrayList<FotoItem>()
    private var deletedOldImageArray: ArrayList<Int> = ArrayList()
    private var cameraCaptureFileName: String = ""
    private lateinit var imageUri: Uri
    private val photoFiles: ArrayList<MultipartBody.Part?> = ArrayList()*/
    private val requestBodyMap: MutableMap<String, RequestBody> = mutableMapOf()
    private var isFormEmpty = arrayOf(
        false,
        false
    )

    /*private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        with(it) {
            if (resultCode == RESULT_OK) {
                if (imageArrayUri.isEmpty()) {
                    imageArrayUri.add(imageUri)
                } else {
                    if (!imageArrayUri.contains(imageUri))
                        imageArrayUri.add(imageUri)
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
                        if (imageArrayUri.isEmpty())
                            imageArrayUri.add(imageUrl)
                        else {
                            if (!imageArrayUri.contains(imageUrl))
                                imageArrayUri.add(imageUrl)
                        }
                    }
                } else {
                    val imageUrl: Uri? = data?.data
                    if (imageUrl != null) {
                        if (imageArrayUri.isEmpty())
                            imageArrayUri.add(imageUrl)
                        else {
                            if (!imageArrayUri.contains(imageUrl))
                                imageArrayUri.add(imageUrl)
                        }
                    }
                }
                setManageAttachment()
            }
        }
    }*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()
    }

    @SuppressLint("SetTextI18n")
    private fun init() {
        binding.apply {
            backButton.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            detail = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("detail", SubmissionDetailResponse::class.java)!!
                } else {
                    intent.getParcelableExtra("detail")!!
                }
            } catch (nullPointerException: NullPointerException) {
                null
            }
            progressData = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("data", ProgressItem::class.java)!!
                } else {
                    intent.getParcelableExtra("data")
                }
            } catch (nullPointerException: NullPointerException) {
                null
            }

            if (progressData != null) {
                progressActionText.text = "Save Edited Progress"
                repairAnalysisField.setText(progressData?.analisa)
                if (!progressData?.analisa.isNullOrEmpty())
                    isFormEmpty[0] = true
                descriptionField.setText(progressData?.keterangan)
                if (!progressData?.keterangan.isNullOrEmpty())
                    isFormEmpty[1] = true
                if (progressData?.material!!.isNotEmpty()) {
                    for (i in 0 until progressData?.material!!.size) {
                        selectedMaterialsArrayList.add(
                            MaterialListResponse(
                                progressData?.material!![i]?.stsAktif,
                                progressData?.material!![i]?.harga,
                                progressData?.material!![i]?.namaMaterial,
                                progressData?.material!![i]?.satuan,
                                progressData?.material!![i]?.idMaterial,
                                progressData?.material!![i]?.kodeMaterial,
                                progressData?.material!![i]?.idKategori
                            )
                        )
                        materialQuantityList.add(progressData?.material!![i]?.qtyMaterial!!)
                    }
                }
                /*if (progressData?.foto!!.isNotEmpty()) {
                    manageAttachmentText.text = getString(R.string.manage_new_photo)
                    for (photo in progressData?.foto!!) {
                        if (photo != null) {
                            oldImageArray.add(photo)
                        }
                    }
                }
                if (oldImageArray.isNotEmpty()) {
                    manageOldAttachmentButton.visibility = View.VISIBLE
                    manageOldAttachmentButton.setOnClickListener {
                        val bottomSheet = ManageOldPhotoBottomSheet(
                            this@ProgressFormActivity, oldImageArray
                        ).also {
                            with(it) {
                                setOnProgressAttachmentActionListener(object :
                                    ManageOldPhotoBottomSheet.OnProgressAttachmentActionListener {
                                    override fun onDeleteOldPhoto(photo: FotoItem) {
                                        deletedOldImageArray.add(photo.idFoto!!)
                                        oldImageArray.remove(photo)
                                        if (oldImageArray.isEmpty())
                                            manageOldAttachmentButton.visibility = View.GONE
                                    }
                                })
                            }
                        }

                        if (bottomSheet.window != null)
                            bottomSheet.show()
                    }
                }*/
            }

            prepareMaterials()

            repairAnalysisField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s!!.isEmpty()) {
                        repairAnalysisFieldLayout.error = "Analysis can't be empty!"
                        isFormEmpty[0] = false
                    } else {
                        repairAnalysisFieldLayout.error = null
                        isFormEmpty[0] = true
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
            descriptionField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s!!.isEmpty()) {
                        descriptionFieldLayout.error = "Description can't be empty!"
                        isFormEmpty[1] = false
                    } else {
                        descriptionFieldLayout.error = null
                        isFormEmpty[1] = true
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
            /*chooseFileButton.setOnClickListener {
                val bottomSheet = ChooseFileBottomSheet(this@ProgressFormActivity).also {
                    with(it) {
                        setOnChooseFileListener(object :
                            ChooseFileBottomSheet.OnChooseFileListener {
                            override fun onOpenCameraClicked() {
                                if (PermissionHelper.isPermissionGranted(
                                        this@ProgressFormActivity,
                                        PermissionHelper.CAMERA
                                    )
                                ) {
                                    openCamera()
                                } else {
                                    PermissionHelper.requestPermission(
                                        this@ProgressFormActivity,
                                        arrayOf(PermissionHelper.CAMERA),
                                        PermissionHelper.REQUEST_CODE_CAMERA
                                    )
                                }
                                dismiss()
                            }

                            override fun onOpenGalleryClicked() {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (PermissionHelper.isPermissionGranted(
                                            this@ProgressFormActivity,
                                            PermissionHelper.READ_MEDIA_IMAGES
                                        ) || PermissionHelper.isPermissionGranted(
                                            this@ProgressFormActivity,
                                            PermissionHelper.READ_MEDIA_VIDEO
                                        )
                                    ) {
                                        openGallery()
                                    } else {
                                        PermissionHelper.requestPermission(
                                            this@ProgressFormActivity,
                                            arrayOf(
                                                PermissionHelper.READ_MEDIA_IMAGES,
                                                PermissionHelper.READ_MEDIA_VIDEO
                                            ),
                                            PermissionHelper.REQUEST_CODE_GALLERY
                                        )
                                    }
                                } else {
                                    if (PermissionHelper.isPermissionGranted(
                                            this@ProgressFormActivity,
                                            PermissionHelper.READ_EXTERNAL_STORAGE
                                        )
                                    ) {
                                        openGallery()
                                    } else {
                                        PermissionHelper.requestPermission(
                                            this@ProgressFormActivity,
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
            }*/

            progressActionButton.setOnClickListener {
                loadingBar.visibility = View.VISIBLE
                val inputMethodManager =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                if (progressData != null) {
                    if (!formCheck()) {
                        CustomToast.getInstance(applicationContext)
                            .setMessage("Please make sure all fields in the form are filled in.")
                            .setBackgroundColor(
                                ContextCompat.getColor(
                                    this@ProgressFormActivity,
                                    R.color.custom_toast_background_failed
                                )
                            )
                            .setFontColor(
                                ContextCompat.getColor(
                                    this@ProgressFormActivity,
                                    R.color.custom_toast_font_failed
                                )
                            ).show()
                    } else {
                        if (prepareEditForm()) {
                            try {
                                InitAPI.getAPI.editProgress(requestBodyMap)
                                    .enqueue(object : Callback<GenericSimpleResponse> {
                                        override fun onResponse(
                                            call: Call<GenericSimpleResponse>,
                                            response: Response<GenericSimpleResponse>
                                        ) {
                                            loadingBar.visibility = View.GONE
                                            if (response.isSuccessful) {
                                                if (response.body() != null) {
                                                    val result = response.body()
                                                    if (result?.code == 1) {
                                                        CustomToast.getInstance(applicationContext)
                                                            .setMessage(
                                                                "Progress edited successfully."
                                                            )
                                                            .setBackgroundColor(
                                                                ContextCompat.getColor(
                                                                    this@ProgressFormActivity,
                                                                    R.color.custom_toast_background_success
                                                                )
                                                            )
                                                            .setFontColor(
                                                                ContextCompat.getColor(
                                                                    this@ProgressFormActivity,
                                                                    R.color.custom_toast_font_success
                                                                )
                                                            ).show()
                                                        setResult(RESULT_OK)
                                                        finish()
                                                    } else {
                                                        CustomToast.getInstance(applicationContext)
                                                            .setMessage("Failed to create progress!")
                                                            .setFontColor(
                                                                ContextCompat.getColor(
                                                                    this@ProgressFormActivity,
                                                                    R.color.custom_toast_font_failed
                                                                )
                                                            )
                                                            .setBackgroundColor(
                                                                ContextCompat.getColor(
                                                                    this@ProgressFormActivity,
                                                                    R.color.custom_toast_background_failed
                                                                )
                                                            ).show()
                                                        Log.e(
                                                            "ERROR ${response.code()}",
                                                            "Edit Progress Response Code 0 | ${result?.message}"
                                                        )
                                                    }
                                                } else {
                                                    CustomToast.getInstance(applicationContext)
                                                        .setMessage("Failed to create progress!")
                                                        .setFontColor(
                                                            ContextCompat.getColor(
                                                                this@ProgressFormActivity,
                                                                R.color.custom_toast_font_failed
                                                            )
                                                        )
                                                        .setBackgroundColor(
                                                            ContextCompat.getColor(
                                                                this@ProgressFormActivity,
                                                                R.color.custom_toast_background_failed
                                                            )
                                                        ).show()
                                                    Log.e(
                                                        "ERROR ${response.code()}",
                                                        "Edit Progress Response null | ${response.message()}"
                                                    )
                                                }
                                            } else {
                                                CustomToast.getInstance(applicationContext)
                                                    .setMessage("Failed to create progress!")
                                                    .setFontColor(
                                                        ContextCompat.getColor(
                                                            this@ProgressFormActivity,
                                                            R.color.custom_toast_font_failed
                                                        )
                                                    )
                                                    .setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            this@ProgressFormActivity,
                                                            R.color.custom_toast_background_failed
                                                        )
                                                    ).show()
                                                Log.e(
                                                    "ERROR ${response.code()}",
                                                    "Edit Progress Response Fail | ${response.message()}"
                                                )
                                            }
                                        }

                                        override fun onFailure(
                                            call: Call<GenericSimpleResponse>,
                                            throwable: Throwable
                                        ) {
                                            loadingBar.visibility = View.GONE
                                            CustomToast.getInstance(applicationContext)
                                                .setMessage("Something went wrong, please try again.")
                                                .setFontColor(
                                                    ContextCompat.getColor(
                                                        this@ProgressFormActivity,
                                                        R.color.custom_toast_font_failed
                                                    )
                                                )
                                                .setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        this@ProgressFormActivity,
                                                        R.color.custom_toast_background_failed
                                                    )
                                                ).show()
                                            Log.e("ERROR", "Edit Progress Failure | $throwable")
                                            throwable.printStackTrace()
                                        }
                                    })
                            } catch (jsonException: JSONException) {
                                loadingBar.visibility = View.GONE
                                CustomToast.getInstance(applicationContext)
                                    .setMessage("Something went wrong, please try again.")
                                    .setFontColor(
                                        ContextCompat.getColor(
                                            this@ProgressFormActivity,
                                            R.color.custom_toast_font_failed
                                        )
                                    )
                                    .setBackgroundColor(
                                        ContextCompat.getColor(
                                            this@ProgressFormActivity,
                                            R.color.custom_toast_background_failed
                                        )
                                    ).show()
                                Log.e("ERROR", "Edit Progress JSON Exception | $jsonException")
                                jsonException.printStackTrace()
                            }
                        } else {
                            loadingBar.visibility = View.GONE
                            CustomToast.getInstance(applicationContext)
                                .setMessage("Something went wrong, please try again.")
                                .setFontColor(
                                    ContextCompat.getColor(
                                        this@ProgressFormActivity,
                                        R.color.custom_toast_font_failed
                                    )
                                )
                                .setBackgroundColor(
                                    ContextCompat.getColor(
                                        this@ProgressFormActivity,
                                        R.color.custom_toast_background_failed
                                    )
                                ).show()
                            Log.e("ERROR", "Edit form not prepared")
                        }
                    }
                } else {
                    if (!formCheck()) {
                        loadingBar.visibility = View.GONE
                        CustomToast.getInstance(applicationContext)
                            .setMessage("Please make sure all fields in the form are filled in.")
                            .setBackgroundColor(
                                ContextCompat.getColor(
                                    this@ProgressFormActivity,
                                    R.color.custom_toast_background_failed
                                )
                            )
                            .setFontColor(
                                ContextCompat.getColor(
                                    this@ProgressFormActivity,
                                    R.color.custom_toast_font_failed
                                )
                            ).show()
                    } else {
                        if (prepareSubmitForm()) {
                            try {
                                InitAPI.getAPI.createProgress(requestBodyMap)
                                    .enqueue(object : Callback<CreationResponse> {
                                        override fun onResponse(
                                            call: Call<CreationResponse>,
                                            response: Response<CreationResponse>
                                        ) {
                                            loadingBar.visibility = View.GONE
                                            if (response.isSuccessful) {
                                                if (response.body() != null) {
                                                    val result = response.body()
                                                    if (result?.code == 1) {
                                                        CustomToast.getInstance(applicationContext)
                                                            .setMessage(
                                                                "Progress created successfully."
                                                            )
                                                            .setBackgroundColor(
                                                                ContextCompat.getColor(
                                                                    this@ProgressFormActivity,
                                                                    R.color.custom_toast_background_success
                                                                )
                                                            )
                                                            .setFontColor(
                                                                ContextCompat.getColor(
                                                                    this@ProgressFormActivity,
                                                                    R.color.custom_toast_font_success
                                                                )
                                                            ).show()
                                                        setResult(RESULT_OK)
                                                        finish()
                                                    } else {
                                                        CustomToast.getInstance(applicationContext)
                                                            .setMessage("Failed to create progress!")
                                                            .setFontColor(
                                                                ContextCompat.getColor(
                                                                    this@ProgressFormActivity,
                                                                    R.color.custom_toast_font_failed
                                                                )
                                                            )
                                                            .setBackgroundColor(
                                                                ContextCompat.getColor(
                                                                    this@ProgressFormActivity,
                                                                    R.color.custom_toast_background_failed
                                                                )
                                                            ).show()
                                                        Log.e(
                                                            "ERROR ${response.code()}",
                                                            "Create Progress Response Code 0 | ${result?.message}"
                                                        )
                                                    }
                                                } else {
                                                    CustomToast.getInstance(applicationContext)
                                                        .setMessage("Failed to create progress!")
                                                        .setFontColor(
                                                            ContextCompat.getColor(
                                                                this@ProgressFormActivity,
                                                                R.color.custom_toast_font_failed
                                                            )
                                                        )
                                                        .setBackgroundColor(
                                                            ContextCompat.getColor(
                                                                this@ProgressFormActivity,
                                                                R.color.custom_toast_background_failed
                                                            )
                                                        ).show()
                                                    Log.e(
                                                        "ERROR ${response.code()}",
                                                        "Create Progress Response null | ${response.message()}"
                                                    )
                                                }
                                            } else {
                                                CustomToast.getInstance(applicationContext)
                                                    .setMessage("Failed to create progress!")
                                                    .setFontColor(
                                                        ContextCompat.getColor(
                                                            this@ProgressFormActivity,
                                                            R.color.custom_toast_font_failed
                                                        )
                                                    )
                                                    .setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            this@ProgressFormActivity,
                                                            R.color.custom_toast_background_failed
                                                        )
                                                    ).show()
                                                Log.e(
                                                    "ERROR ${response.code()}",
                                                    "Create Progress Response Fail | ${response.message()}"
                                                )
                                            }
                                        }

                                        override fun onFailure(
                                            call: Call<CreationResponse>,
                                            throwable: Throwable
                                        ) {
                                            loadingBar.visibility = View.GONE
                                            CustomToast.getInstance(applicationContext)
                                                .setMessage("Something went wrong, please try again.")
                                                .setFontColor(
                                                    ContextCompat.getColor(
                                                        this@ProgressFormActivity,
                                                        R.color.custom_toast_font_failed
                                                    )
                                                )
                                                .setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        this@ProgressFormActivity,
                                                        R.color.custom_toast_background_failed
                                                    )
                                                ).show()
                                            Log.e("ERROR", "Create Progress Failure | $throwable")
                                            throwable.printStackTrace()
                                        }
                                    })
                            } catch (jsonException: JSONException) {
                                loadingBar.visibility = View.GONE
                                CustomToast.getInstance(applicationContext)
                                    .setMessage("Something went wrong, please try again.")
                                    .setFontColor(
                                        ContextCompat.getColor(
                                            this@ProgressFormActivity,
                                            R.color.custom_toast_font_failed
                                        )
                                    )
                                    .setBackgroundColor(
                                        ContextCompat.getColor(
                                            this@ProgressFormActivity,
                                            R.color.custom_toast_background_failed
                                        )
                                    ).show()
                                Log.e("ERROR", "Create Progress JSON Exception | $jsonException")
                                jsonException.printStackTrace()
                            }
                        } else {
                            loadingBar.visibility = View.GONE
                            CustomToast.getInstance(applicationContext)
                                .setMessage("Something went wrong, please try again.")
                                .setFontColor(
                                    ContextCompat.getColor(
                                        this@ProgressFormActivity,
                                        R.color.custom_toast_font_failed
                                    )
                                )
                                .setBackgroundColor(
                                    ContextCompat.getColor(
                                        this@ProgressFormActivity,
                                        R.color.custom_toast_background_failed
                                    )
                                ).show()
                            Log.e("ERROR", "Submit form not prepared")
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun prepareMaterials() {
        binding.apply {
            selectedMaterialsArrayList.add(
                MaterialListResponse(
                    null, null, "+", null, null, null
                )
            )
            materialQuantityList.add(0)

            materialAdapter = SelectedMaterialsRvAdapters(
                this@ProgressFormActivity,
                selectedMaterialsArrayList,
                materialQuantityList,
                detail!!
            ).also {
                with(it) {
                    setOnMaterialsSetListener(object :
                        SelectedMaterialsRvAdapters.OnMaterialsSetListener {
                        override fun onMaterialsSelected(
                            data: MaterialListResponse, checkBox: CheckBox, isChecked: Boolean
                        ) {
                            val bottomSheet =
                                MaterialQuantityBottomSheet(
                                    this@ProgressFormActivity,
                                    data.namaMaterial!!
                                ).also { qtyBottomSheet ->
                                    with(qtyBottomSheet) {
                                        setOnQuantityConfirmListener(object :
                                            MaterialQuantityBottomSheet.OnQuantityConfirmListener {
                                            override fun onQuantityConfirm(quantity: Int) {
                                                materialQuantityList.removeAt(
                                                    materialQuantityList.size - 1
                                                )
                                                selectedMaterialsArrayList.remove(
                                                    MaterialListResponse(
                                                        null, null, "+", null, null, null
                                                    )
                                                )
                                                materialQuantityList.add(quantity)
                                                selectedMaterialsArrayList.add(data)
                                                materialQuantityList.add(0)
                                                selectedMaterialsArrayList.add(
                                                    MaterialListResponse(
                                                        null, null, "+", null, null, null
                                                    )
                                                )
                                                materialAdapter.notifyDataSetChanged()
                                            }

                                            override fun onBottomSheetDismissed(quantity: Int) {
                                                checkBox.isChecked = false
                                            }
                                        })
                                    }
                                }

                            if (bottomSheet.window != null)
                                bottomSheet.show()
                        }

                        override fun onMaterialsUnselected(
                            data: MaterialListResponse, position: Int
                        ) {
                            materialQuantityList.removeAt(
                                materialQuantityList.size - 1
                            )
                            selectedMaterialsArrayList.remove(
                                MaterialListResponse(
                                    null, null, "+", null, null, null
                                )
                            )
                            materialQuantityList.removeAt(position)
                            selectedMaterialsArrayList.remove(data)
                            materialQuantityList.add(0)
                            selectedMaterialsArrayList.add(
                                MaterialListResponse(
                                    null, null, "+", null, null, null
                                )
                            )
                            materialAdapter.notifyDataSetChanged()
                        }

                        override fun onMaterialsQuantityEdited(quantity: Int, position: Int) {
                            materialQuantityList[position] = quantity
                            materialAdapter.notifyDataSetChanged()
                        }
                    })
                }
            }
            rvMaterials.adapter = materialAdapter
            rvMaterials.layoutManager = FlexboxLayoutManager(
                this@ProgressFormActivity, FlexDirection.ROW, FlexWrap.WRAP
            )
            materialAdapter.notifyDataSetChanged()
        }
    }

    /*private fun setManageAttachment() {
        binding.apply {
            manageAttachmentButton.setOnClickListener {
                val bottomSheet = ManagePhotoBottomSheet(
                    this@ProgressFormActivity, imageArrayUri
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

    private fun openGallery() {
        galleryLauncher.launch(
            Intent().also {
                it.type = "image*//*"
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
    }*/
    private fun formCheck(): Boolean {
        var validated = 0
        binding.apply {
            for (element in isFormEmpty) {
                if (element)
                    validated++
            }
            if (repairAnalysisField.text!!.isEmpty())
                repairAnalysisFieldLayout.error = "Analysis can't be empty!"
            if (descriptionField.text!!.isEmpty())
                descriptionFieldLayout.error = "Description can't be empty!"
        }

        return validated == isFormEmpty.size
    }

    private fun prepareSubmitForm(): Boolean {
        binding.apply {
            /*if (imageArrayUri.isNotEmpty()) {
                for (element in imageArrayUri) {
                    photoFiles.add(
                        createMultipartBody(element)
                    )
                }
            }*/
            with(requestBodyMap) {
                put("id_user", createPartFromString(userData.id.toString())!!)
                put("id_gaprojects", createPartFromString(detail?.idGaprojects.toString())!!)
                put(
                    "analisa_perbaikan", createPartFromString(repairAnalysisField.text.toString())!!
                )
                put(
                    "keterangan_perbaikan", createPartFromString(descriptionField.text.toString())!!
                )
                if (selectedMaterialsArrayList.size > 1 && materialQuantityList.size > 1) {
                    for (i in 0 until selectedMaterialsArrayList.size - 1) {
                        put(
                            "material[]",
                            createPartFromString(selectedMaterialsArrayList[i].idMaterial.toString())!!
                        )
                        put(
                            "qty_material[]",
                            createPartFromString(materialQuantityList[i].toString())!!
                        )
                    }
                }
            }
        }

        return requestBodyMap.isNotEmpty()
        /*return if (requestBodyMap.isNotEmpty()) {
            if (photoFiles.isNotEmpty())
                true
            else
                true
        } else
            false*/
    }

    private fun prepareEditForm(): Boolean {
        binding.apply {
            /*if (imageArrayUri.isNotEmpty()) {
                for (element in imageArrayUri) {
                    photoFiles.add(
                        createMultipartBody(element)
                    )
                }
            }*/
            with(requestBodyMap) {
                put("id_user", createPartFromString(userData.id.toString())!!)
                put("id_gaprojects", createPartFromString(progressData?.idGaprojects.toString())!!)
                put(
                    "id_gaprojects_detail",
                    createPartFromString(progressData?.idGaprojectsDetail.toString())!!
                )
                put(
                    "analisa_perbaikan", createPartFromString(repairAnalysisField.text.toString())!!
                )
                put(
                    "keterangan_perbaikan", createPartFromString(descriptionField.text.toString())!!
                )
                if (selectedMaterialsArrayList.size > 1 && materialQuantityList.size > 1) {
                    for (i in 0 until selectedMaterialsArrayList.size - 1) {
                        put(
                            "material[]",
                            createPartFromString(selectedMaterialsArrayList[i].idMaterial.toString())!!
                        )
                        put(
                            "qty_material[]",
                            createPartFromString(materialQuantityList[i].toString())!!
                        )
                    }
                }
                /*if (deletedOldImageArray.isNotEmpty()) {
                    for (element in deletedOldImageArray) {
                        put(
                            "foto_old[]",
                            createPartFromString(
                                element.toString()
                            )!!
                        )
                    }
                }*/
            }
        }

        return requestBodyMap.isNotEmpty()
        /*return if (requestBodyMap.isNotEmpty()) {
            if (photoFiles.isNotEmpty())
                true
            else
                true
        } else
            false*/
    }

    /*private fun createMultipartBody(uri: Uri): MultipartBody.Part? {
        return try {
            val file = File(getRealPathFromURI(uri)!!)
            val requestBody = file.asRequestBody("image*//*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("foto[]", file.name, requestBody)
        } catch (e: Exception) {
            Log.e("createMultipartBody", "Error creating MultipartBody.Part", e)
            null
        }
    }*/
    /*private fun getRealPathFromURI(uri: Uri): String? {
        val contentResolver = contentResolver
        val fileName = getFileName(contentResolver, uri)

        if (fileName != null) {
            val file = File(cacheDir, fileName)
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(file)
                val buffer = ByteArray(4 * 1024)
                var read: Int

                while (inputStream!!.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                return file.absolutePath
            } catch (e: IOException) {
                Log.e("getRealPathFromURI", "Error: ${e.message}")
            }
        }

        return null
    }*/
    /*private fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (displayNameIndex != -1) {
                val fileName = cursor.getString(displayNameIndex)
                cursor.close()
                return fileName
            }
        }
        cursor?.close()
        return null
    }*/
    private fun createPartFromString(stringData: String?): RequestBody? {
        return stringData?.toRequestBody("text/plain".toMediaTypeOrNull())
    }
}