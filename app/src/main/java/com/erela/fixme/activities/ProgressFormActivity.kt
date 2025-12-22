package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
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
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.CreationResponse
import com.erela.fixme.objects.GenericSimpleResponse
import com.erela.fixme.objects.MaterialListResponse
import com.erela.fixme.objects.ProgressItem
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.textfield.TextInputEditText
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
    private var editMaterial: Boolean = false
    private var selectedMaterialsArrayList: ArrayList<MaterialListResponse> = ArrayList()
    private lateinit var materialAdapter: SelectedMaterialsRvAdapters
    private var materialQuantityList: ArrayList<Int> = ArrayList()
    private var oldMaterialCount: Int = 0
    private val requestBodyMap: MutableMap<String, RequestBody> = mutableMapOf()
    private val requestBodyMapMaterial: MutableMap<String, RequestBody> = mutableMapOf()
    private var isFormEmpty = arrayOf(
        false,
        false
    )

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
                    @Suppress("DEPRECATION") intent.getParcelableExtra("detail")!!
                }
            } catch (_: NullPointerException) {
                null
            }
            progressData = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("data", ProgressItem::class.java)!!
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra("data")
                }
            } catch (_: NullPointerException) {
                null
            }

            if (progressData != null) {
                editMaterial = try {
                    intent.getBooleanExtra("edit_material", false)
                } catch (_: NullPointerException) {
                    false
                }
                if (editMaterial) {
                    repairAnalysisField.isEnabled = false
                    descriptionField.isEnabled = false
                } else {
                    repairAnalysisField.isEnabled = true
                    descriptionField.isEnabled = true
                }
                progressActionText.text =
                    if (getString(R.string.lang) == "in")
                        "Simpan Suntingan Kemajuan"
                    else
                        "Save Edited Progress"
                repairAnalysisField.setText(progressData?.analisa)
                if (!progressData?.analisa.isNullOrEmpty())
                    isFormEmpty[0] = true
                descriptionField.setText(progressData?.keterangan)
                if (!progressData?.keterangan.isNullOrEmpty())
                    isFormEmpty[1] = true
                if (progressData?.material!!.isNotEmpty()) {
                    oldMaterialCount = progressData?.material!!.size
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
            }

            prepareMaterials()

            repairAnalysisField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s!!.isEmpty()) {
                        repairAnalysisFieldLayout.error = if (getString(R.string.lang) == "in")
                            "Analisa perbaikan tidak boleh kosong!"
                        else
                            "Analysis can't be empty!"
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
                        descriptionFieldLayout.error = if (getString(R.string.lang) == "in")
                            "Keterangan tidak boleh kosong!"
                        else
                            "Description can't be empty!"
                        isFormEmpty[1] = false
                    } else {
                        descriptionFieldLayout.error = null
                        isFormEmpty[1] = true
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            progressActionButton.setOnClickListener {
                loadingBar.visibility = View.VISIBLE
                val inputMethodManager =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                if (progressData != null) {
                    if (!formCheck()) {
                        CustomToast.getInstance(applicationContext)
                            .setMessage(
                                if (getString(R.string.lang) == "in")
                                    "Pastikan semua kolom di formulir terisi."
                                else
                                    "Please make sure all fields in the form are filled in."
                            )
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
                                (if (editMaterial)
                                    InitAPI.getEndpoint.editMaterialProgress(requestBodyMap)
                                else
                                    InitAPI.getEndpoint.editProgress(requestBodyMap))
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
                                                        if (selectedMaterialsArrayList.size - 1 > oldMaterialCount) {
                                                            if (prepareRequestMaterialForm(
                                                                    progressData!!
                                                                        .idGaprojectsDetail!!
                                                                )
                                                            ) {
                                                                InitAPI.getEndpoint.requestMaterialAdd(
                                                                    requestBodyMapMaterial
                                                                ).enqueue(
                                                                    object :
                                                                        Callback<GenericSimpleResponse> {
                                                                        override fun onResponse(
                                                                            call1: Call<GenericSimpleResponse>,
                                                                            response1: Response<GenericSimpleResponse>
                                                                        ) {
                                                                            loadingBar.visibility =
                                                                                View.GONE
                                                                            val result1 =
                                                                                response1.body()
                                                                            if (
                                                                                result1?.code == 1
                                                                            ) {
                                                                                CustomToast
                                                                                    .getInstance(
                                                                                        applicationContext
                                                                                    )
                                                                                    .setMessage(
                                                                                        if (getString(
                                                                                                R.string.lang
                                                                                            ) == "in"
                                                                                        )
                                                                                            "Kemajuan suntingan berhasil disimpan."
                                                                                        else
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
                                                                                CustomToast
                                                                                    .getInstance(
                                                                                        applicationContext
                                                                                    ).setMessage(
                                                                                        if (getString(
                                                                                                R.string.lang
                                                                                            ) == "in"
                                                                                        )
                                                                                            "Gagal meminta material!"
                                                                                        else
                                                                                            "Failed to request material!"
                                                                                    ).setFontColor(
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
                                                                                    "ERROR " +
                                                                                            "${response1.code()}",
                                                                                    "Request Material Response Code 0 | ${result1?.message}"
                                                                                )
                                                                            }
                                                                        }

                                                                        override fun onFailure(
                                                                            call1: Call<GenericSimpleResponse>,
                                                                            throwable: Throwable
                                                                        ) {
                                                                            loadingBar.visibility =
                                                                                View.GONE
                                                                            CustomToast.getInstance(
                                                                                applicationContext
                                                                            )
                                                                                .setMessage(
                                                                                    if (getString(R.string.lang) == "in")
                                                                                        "Terjadi kesalahan, silakan coba lagi."
                                                                                    else
                                                                                        "Something went wrong, please try again."
                                                                                )
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
                                                                                "ERROR",
                                                                                "Request Material Failure | $throwable"
                                                                            )
                                                                            throwable.printStackTrace()
                                                                        }
                                                                    }
                                                                )
                                                            } else {
                                                                loadingBar.visibility = View.GONE
                                                                CustomToast
                                                                    .getInstance(applicationContext)
                                                                    .setMessage(
                                                                        if (getString(R.string.lang) == "in")
                                                                            "Terjadi kesalahan, silakan coba lagi."
                                                                        else
                                                                            "Something went wrong, please try again."
                                                                    )
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
                                                                    "ERROR",
                                                                    "Submit form not prepared"
                                                                )
                                                            }
                                                        } else {
                                                            CustomToast
                                                                .getInstance(applicationContext)
                                                                .setMessage(
                                                                    if (getString(R.string.lang) == "in")
                                                                        "Kemajuan berhasil disunting."
                                                                    else
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
                                                        }
                                                    } else {
                                                        CustomToast.getInstance(applicationContext)
                                                            .setMessage(
                                                                if (getString(R.string.lang) == "in")
                                                                    "Gagal menyunting kemajuan!"
                                                                else
                                                                    "Failed to edit progress!"
                                                            )
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
                                                        .setMessage(
                                                            if (getString(R.string.lang) == "in")
                                                                "Gagal menyunting kemajuan!"
                                                            else
                                                                "Failed to edit progress!"
                                                        )
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
                                                    .setMessage(
                                                        if (getString(R.string.lang) == "in")
                                                            "Gagal menyunting kemajuan!"
                                                        else
                                                            "Failed to edit progress!"
                                                    )
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
                                                .setMessage(
                                                    if (getString(R.string.lang) == "in")
                                                        "Terjadi kesalahan, silakan coba lagi."
                                                    else
                                                        "Something went wrong, please try again."
                                                )
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
                                    .setMessage(
                                        if (getString(R.string.lang) == "in")
                                            "Terjadi kesalahan, silakan coba lagi."
                                        else
                                            "Something went wrong, please try again."
                                    )
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
                                .setMessage(
                                    if (getString(R.string.lang) == "in")
                                        "Terjadi kesalahan, silakan coba lagi."
                                    else
                                        "Something went wrong, please try again."
                                )
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
                            .setMessage(
                                if (getString(R.string.lang) == "in")
                                    "Pastikan semua kolom di formulir terisi."
                                else
                                    "Please make sure all fields in the form are filled in."
                            )
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
                                InitAPI.getEndpoint.createProgress(requestBodyMap)
                                    .enqueue(object : Callback<CreationResponse> {
                                        override fun onResponse(
                                            call: Call<CreationResponse>,
                                            response: Response<CreationResponse>
                                        ) {
                                            if (response.isSuccessful) {
                                                if (response.body() != null) {
                                                    val result = response.body()
                                                    if (result?.code == 1) {
                                                        if (selectedMaterialsArrayList.size > 1) {
                                                            if (prepareRequestMaterialForm(
                                                                    result.lastId!!
                                                                )
                                                            ) {
                                                                InitAPI.getEndpoint.requestMaterialAdd(
                                                                    requestBodyMapMaterial
                                                                ).enqueue(
                                                                    object :
                                                                        Callback<GenericSimpleResponse> {
                                                                        override fun onResponse(
                                                                            call1: Call<GenericSimpleResponse>,
                                                                            response1: Response<GenericSimpleResponse>
                                                                        ) {
                                                                            loadingBar.visibility =
                                                                                View.GONE
                                                                            val result1 =
                                                                                response1.body()
                                                                            if (
                                                                                result1?.code == 1
                                                                            ) {
                                                                                CustomToast
                                                                                    .getInstance(
                                                                                        applicationContext
                                                                                    )
                                                                                    .setMessage(
                                                                                        if (getString(
                                                                                                R.string.lang
                                                                                            ) == "in"
                                                                                        )
                                                                                            "Material berhasil diminta."
                                                                                        else
                                                                                            "Materials requested successfully."
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
                                                                                setResult(
                                                                                    RESULT_OK
                                                                                )
                                                                                finish()
                                                                            } else {
                                                                                CustomToast
                                                                                    .getInstance(
                                                                                        applicationContext
                                                                                    ).setMessage(
                                                                                        if (getString(
                                                                                                R.string.lang
                                                                                            ) == "in"
                                                                                        )
                                                                                            "Gagal meminta material!"
                                                                                        else
                                                                                            "Failed to request material!"
                                                                                    ).setFontColor(
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
                                                                                    "ERROR " +
                                                                                            "${response1.code()}",
                                                                                    "Request Material Response Code 0 | ${result1?.message}"
                                                                                )
                                                                            }
                                                                        }

                                                                        override fun onFailure(
                                                                            call1: Call<GenericSimpleResponse>,
                                                                            throwable: Throwable
                                                                        ) {
                                                                            loadingBar.visibility =
                                                                                View.GONE
                                                                            CustomToast.getInstance(
                                                                                applicationContext
                                                                            )
                                                                                .setMessage(
                                                                                    if (getString(R.string.lang) == "in")
                                                                                        "Terjadi kesalahan, silakan coba lagi."
                                                                                    else
                                                                                        "Something went wrong, please try again."
                                                                                )
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
                                                                                "ERROR",
                                                                                "Request Material Failure | $throwable"
                                                                            )
                                                                            throwable.printStackTrace()
                                                                        }
                                                                    }
                                                                )
                                                            } else {
                                                                loadingBar.visibility = View.GONE
                                                                CustomToast
                                                                    .getInstance(applicationContext)
                                                                    .setMessage(
                                                                        if (getString(R.string.lang) == "in")
                                                                            "Terjadi kesalahan, silakan coba lagi."
                                                                        else
                                                                            "Something went wrong, please try again."
                                                                    )
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
                                                                    "ERROR",
                                                                    "Submit form not prepared"
                                                                )
                                                            }
                                                        } else {
                                                            loadingBar.visibility = View.GONE
                                                            CustomToast
                                                                .getInstance(
                                                                    applicationContext
                                                                )
                                                                .setMessage(
                                                                    if (getString(R.string.lang) == "in")
                                                                        "Kemajuan berhasil dibuat."
                                                                    else
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
                                                        }
                                                    } else {
                                                        CustomToast.getInstance(applicationContext)
                                                            .setMessage(
                                                                if (getString(R.string.lang) == "in")
                                                                    "Gagal membuat kemajuan!"
                                                                else
                                                                    "Failed to create progress!"
                                                            )
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
                                                        .setMessage(
                                                            if (getString(R.string.lang) == "in")
                                                                "Gagal membuat kemajuan!"
                                                            else
                                                                "Failed to create progress!"
                                                        )
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
                                                    .setMessage(
                                                        if (getString(R.string.lang) == "in")
                                                            "Gagal membuat kemajuan!"
                                                        else
                                                            "Failed to create progress!"
                                                    )
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
                                                .setMessage(
                                                    if (getString(R.string.lang) == "in")
                                                        "Terjadi kesalahan, silakan coba lagi."
                                                    else
                                                        "Something went wrong, please try again."
                                                )
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
                                    .setMessage(
                                        if (getString(R.string.lang) == "in")
                                            "Terjadi kesalahan, silakan coba lagi."
                                        else
                                            "Something went wrong, please try again."
                                    )
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
                                .setMessage(
                                    if (getString(R.string.lang) == "in")
                                        "Terjadi kesalahan, silakan coba lagi."
                                    else
                                        "Something went wrong, please try again."
                                )
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

    private fun formCheck(): Boolean {
        var validated = 0
        binding.apply {
            for (element in isFormEmpty) {
                if (element)
                    validated++
            }
            if (repairAnalysisField.text!!.toString().isEmpty())
                repairAnalysisFieldLayout.error = if (getString(R.string.lang) == "in")
                    "Analisa perbaikan tidak boleh kosong!"
                else
                    "Analysis can't be empty!"
            if (descriptionField.text!!.toString().isEmpty())
                descriptionFieldLayout.error = if (getString(R.string.lang) == "in")
                    "Keterangan perbaikan tidak boleh kosong!"
                else
                    "Description can't be empty!"
        }

        return validated == isFormEmpty.size
    }

    private fun prepareRequestMaterialForm(idGaProjectsDetail: Int): Boolean {
        with(requestBodyMapMaterial) {
            put("id", createPartFromString(idGaProjectsDetail.toString())!!)
            put("id_user", createPartFromString(userData.id.toString())!!)
            for (i in 0 until selectedMaterialsArrayList.size - 1) {
                put(
                    "material[$i]",
                    createPartFromString(selectedMaterialsArrayList[i].idMaterial.toString())!!
                )
                put(
                    "qty_material[$i]",
                    createPartFromString(materialQuantityList[i].toString())!!
                )
            }
        }
        return requestBodyMapMaterial.isNotEmpty()
    }

    private fun prepareSubmitForm(): Boolean {
        binding.apply {
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
                            "material[$i]",
                            createPartFromString(selectedMaterialsArrayList[i].idMaterial.toString())!!
                        )
                        put(
                            "qty_material[$i]",
                            createPartFromString(materialQuantityList[i].toString())!!
                        )
                    }
                }
            }
        }

        return requestBodyMap.isNotEmpty()
    }

    private fun prepareEditForm(): Boolean {
        binding.apply {
            with(requestBodyMap) {
                put("id_user", createPartFromString(userData.id.toString())!!)
                put("id_gaprojects", createPartFromString(progressData?.idGaprojects.toString())!!)
                put(
                    "id_gaprojects_detail",
                    createPartFromString(progressData?.idGaprojectsDetail.toString())!!
                )
                if (!editMaterial) {
                    put(
                        "analisa_perbaikan",
                        createPartFromString(repairAnalysisField.text.toString())!!
                    )
                    put(
                        "keterangan_perbaikan",
                        createPartFromString(descriptionField.text.toString())!!
                    )
                }
                if (selectedMaterialsArrayList.size > 1 && materialQuantityList.size > 1) {
                    for (i in 0 until selectedMaterialsArrayList.size - 1) {
                        put(
                            "material[$i]",
                            createPartFromString(selectedMaterialsArrayList[i].idMaterial.toString())!!
                        )
                        put(
                            "qty_material[$i]",
                            createPartFromString(materialQuantityList[i].toString())!!
                        )
                    }
                }
            }
        }

        return requestBodyMap.isNotEmpty()
    }

    private fun createPartFromString(stringData: String?): RequestBody? {
        return stringData?.toRequestBody("text/plain".toMediaTypeOrNull())
    }
}