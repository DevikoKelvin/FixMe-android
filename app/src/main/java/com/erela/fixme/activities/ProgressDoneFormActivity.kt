package com.erela.fixme.activities

import android.content.ClipData
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erela.fixme.R
import com.erela.fixme.bottom_sheets.ChooseFileBottomSheet
import com.erela.fixme.bottom_sheets.ManagePhotoBottomSheet
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityProgressDoneFormBinding
import com.erela.fixme.dialogs.ConfirmationDialog
import com.erela.fixme.dialogs.LoadingDialog
import com.erela.fixme.helpers.PermissionHelper
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.networking.InitAPI
import com.erela.fixme.objects.GenericSimpleResponse
import com.erela.fixme.objects.ProgressItem
import com.erela.fixme.objects.UserData
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressDoneFormActivity : AppCompatActivity() {
    private val binding: ActivityProgressDoneFormBinding by lazy {
        ActivityProgressDoneFormBinding.inflate(layoutInflater)
    }
    private val userData: UserData by lazy {
        UserDataHelper(applicationContext).getUserData()
    }
    private var progressData: ProgressItem? = null
    private val imageArrayUri = ArrayList<Uri>()
    private var cameraCaptureFileName: String = ""
    private lateinit var imageUri: Uri
    private val photoFiles: ArrayList<MultipartBody.Part?> = ArrayList()
    private val requestBodyMap: MutableMap<String, RequestBody> = mutableMapOf()
    private var isFormEmpty = arrayOf(
        false,
        false
    )
    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.apply {
            enableEdgeToEdge()
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            init()
        }
    }

    private fun init() {
        binding.apply {
            progressData = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("data", ProgressItem::class.java)!!
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra("data")
                }
            } catch (nullPointerException: NullPointerException) {
                null
            }

            backButton.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            descriptionField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s!!.isEmpty()) {
                        descriptionFieldLayout.error = "Description can't be empty!"
                        isFormEmpty[0] = false
                    } else {
                        descriptionFieldLayout.error = null
                        isFormEmpty[0] = true
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            chooseFileButton.setOnClickListener {
                val bottomSheet = ChooseFileBottomSheet(this@ProgressDoneFormActivity).also {
                    with(it) {
                        setOnChooseFileListener(object :
                            ChooseFileBottomSheet.OnChooseFileListener {
                            override fun onOpenCameraClicked() {
                                if (PermissionHelper.isPermissionGranted(
                                        this@ProgressDoneFormActivity,
                                        PermissionHelper.CAMERA
                                    )
                                ) {
                                    openCamera()
                                } else {
                                    PermissionHelper.requestPermission(
                                        this@ProgressDoneFormActivity,
                                        arrayOf(PermissionHelper.CAMERA),
                                        PermissionHelper.REQUEST_CODE_CAMERA
                                    )
                                }
                                dismiss()
                            }

                            override fun onOpenGalleryClicked() {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (PermissionHelper.isPermissionGranted(
                                            this@ProgressDoneFormActivity,
                                            PermissionHelper.READ_MEDIA_IMAGES
                                        ) || PermissionHelper.isPermissionGranted(
                                            this@ProgressDoneFormActivity,
                                            PermissionHelper.READ_MEDIA_VIDEO
                                        )
                                    ) {
                                        openGallery()
                                    } else {
                                        PermissionHelper.requestPermission(
                                            this@ProgressDoneFormActivity,
                                            arrayOf(
                                                PermissionHelper.READ_MEDIA_IMAGES,
                                                PermissionHelper.READ_MEDIA_VIDEO
                                            ),
                                            PermissionHelper.REQUEST_CODE_GALLERY
                                        )
                                    }
                                } else {
                                    if (PermissionHelper.isPermissionGranted(
                                            this@ProgressDoneFormActivity,
                                            PermissionHelper.READ_EXTERNAL_STORAGE
                                        )
                                    ) {
                                        openGallery()
                                    } else {
                                        PermissionHelper.requestPermission(
                                            this@ProgressDoneFormActivity,
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

            doneActionButton.setOnClickListener {
                val confirmationDialog =
                    ConfirmationDialog(
                        this@ProgressDoneFormActivity,
                        "Are you sure you want to mark this progress as done?\n\nMake sure your progress are totally done before marking it as done",
                        "Yes"
                    ).also {
                        with(it) {
                            setConfirmationDialogListener(object :
                                ConfirmationDialog.ConfirmationDialogListener {
                                override fun onConfirm() {
                                    dismiss()
                                    val loadingDialog = LoadingDialog(this@ProgressDoneFormActivity)
                                    if (loadingDialog.window != null)
                                        loadingDialog.show()
                                    if (formCheck()) {
                                        if (prepareSubmitForm()) {
                                            try {
                                                InitAPI.getAPI
                                                    .markProgressDone(requestBodyMap, photoFiles)
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
                                                                                "Progress marked as done successfully!"
                                                                            ).show()
                                                                        setResult(RESULT_OK)
                                                                        finish()
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
                                                                                "Failed to mark progress as done"
                                                                            ).show()
                                                                        Log.e(
                                                                            "ERROR ${response.code()}",
                                                                            "Mark Progress Done Response code 0 | ${response.message()}"
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
                                                                        .setMessage("Failed to mark progress as done")
                                                                        .show()
                                                                    Log.e(
                                                                        "ERROR ${response.code()}",
                                                                        "Mark Progress Done Response null | ${response.message()}"
                                                                    )
                                                                }
                                                            } else {
                                                                CustomToast
                                                                    .getInstance(applicationContext)
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
                                                                    "Mark Progress Done Response Fail | ${response.message()}"
                                                                )
                                                            }
                                                        }

                                                        override fun onFailure(
                                                            call: Call<GenericSimpleResponse>,
                                                            throwable: Throwable
                                                        ) {
                                                            loadingDialog.dismiss()
                                                            CustomToast
                                                                .getInstance(applicationContext)
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
                                                                    "Something went wrong, please try again later"
                                                                )
                                                                .show()
                                                            throwable.printStackTrace()
                                                            Log.e(
                                                                "ERROR",
                                                                "Mark Progress Done Failure | $throwable"
                                                            )
                                                        }
                                                    })
                                            } catch (jsonException: JSONException) {
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
                                                            resources,
                                                            R.color.custom_toast_font_failed,
                                                            theme
                                                        )
                                                    )
                                                    .setMessage("Something went wrong, please try again later")
                                                    .show()
                                                jsonException.printStackTrace()
                                                Log.e(
                                                    "ERROR",
                                                    "Mark Progress Done Exception | $jsonException"
                                                )
                                            }
                                        } else {
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
                                                        resources,
                                                        R.color.custom_toast_font_failed,
                                                        theme
                                                    )
                                                )
                                                .setMessage("Form are not prepared. Try to check again!")
                                                .show()
                                        }
                                    } else {
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
                                                    resources,
                                                    R.color.custom_toast_font_failed,
                                                    theme
                                                )
                                            )
                                            .setMessage("Please check your input. Make sure all required fields are filled")
                                            .show()
                                    }
                                }
                            })
                        }
                    }

                if (confirmationDialog.window != null)
                    confirmationDialog.show()
            }
        }
    }

    private fun setManageAttachment() {
        binding.apply {
            manageAttachmentButton.setOnClickListener {
                val bottomSheet = ManagePhotoBottomSheet(
                    this@ProgressDoneFormActivity, imageArrayUri
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

    private fun formCheck(): Boolean {
        var validated = 0
        binding.apply {
            if (photoFiles.isEmpty())
                isFormEmpty[1] = false
            for (element in isFormEmpty) {
                if (element)
                    validated++
            }
            if (descriptionField.text.toString().isEmpty())
                descriptionFieldLayout.error = "Description can't be empty!"
        }

        return validated == isFormEmpty.size
    }

    private fun prepareSubmitForm(): Boolean {
        binding.apply {
            if (imageArrayUri.isNotEmpty()) {
                for (element in imageArrayUri) {
                    photoFiles.add(
                        createMultipartBody(element)
                    )
                }
            }

            with(requestBodyMap) {
                put("id", createPartFromString(progressData?.idGaprojectsDetail.toString())!!)
                put("id_user", createPartFromString(userData.id.toString())!!)
                put("keterangan", createPartFromString(descriptionField.text.toString())!!)
            }
        }

        return requestBodyMap.isNotEmpty()
    }

    private fun createMultipartBody(uri: Uri): MultipartBody.Part? {
        return try {
            val file = File(getRealPathFromURI(uri)!!)
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("foto[]", file.name, requestBody)
        } catch (e: Exception) {
            Log.e("createMultipartBody", "Error creating MultipartBody.Part", e)
            null
        }
    }

    private fun getRealPathFromURI(uri: Uri): String? {
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
            } catch (ioException: IOException) {
                Log.e("getRealPathFromURI", "Error: ${ioException.message}")
            }
        }

        return null
    }

    private fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
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
    }

    private fun createPartFromString(stringData: String?): RequestBody? {
        return stringData?.toRequestBody("text/plain".toMediaTypeOrNull())
    }
}