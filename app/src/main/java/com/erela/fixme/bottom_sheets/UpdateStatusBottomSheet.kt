package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.SelectedSupervisorTechniciansRvAdapter
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.BsUpdateStatusBinding
import com.erela.fixme.dialogs.ConfirmationDialog
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.GenericSimpleResponse
import com.erela.fixme.objects.SubDepartmentListResponse
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.SupervisorTechnicianListResponse
import com.erela.fixme.objects.UserData
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class UpdateStatusBottomSheet(
    context: Context, private val dataDetail: SubmissionDetailResponse,
    private val approve: Boolean, private val cancel: Boolean, private val deployTech: Boolean
) : BottomSheetDialog(context) {
    private val binding: BsUpdateStatusBinding by lazy {
        BsUpdateStatusBinding.inflate(layoutInflater)
    }
    private val userData: UserData by lazy {
        UserDataHelper(context).getUserData()
    }
    private lateinit var onUpdateSuccessListener: OnUpdateSuccessListener
    private var selectedSupervisorsArrayList: ArrayList<SupervisorTechnicianListResponse> =
        ArrayList()
    private var selectedTechniciansArrayList: ArrayList<SupervisorTechnicianListResponse> =
        ArrayList()
    private lateinit var supervisorsRvAdapter: SelectedSupervisorTechniciansRvAdapter
    private lateinit var techniciansRvAdapter: SelectedSupervisorTechniciansRvAdapter
    private var isFormEmpty = arrayOf(
        false,
        false
    )
    private lateinit var complexity: String
    private var workByVendor = "n"
    private lateinit var subDepartmentList: ArrayList<String>
    private var selectedSubDept: SubDepartmentListResponse? = null

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

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun init() {
        binding.apply {
            issueTitle.text = dataDetail.judulKasus?.uppercase(Locale.ROOT)
            descriptionField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int,
                    count: Int
                ) {
                    if (s.toString().isNotEmpty()) {
                        isFormEmpty[0] = true
                        descriptionFieldLayout.error = null
                    } else {
                        isFormEmpty[0] = false
                        descriptionFieldLayout.error = if (context.getString(R.string.lang) == "in")
                            "Pastikan semua kolom terisi."
                        else
                            "Make sure all fields are filled."
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            actionsButtonContainer.visibility = View.VISIBLE
            if (!deployTech) {
                if (cancel) {
                    subDeptText.visibility = View.GONE
                    subDeptDropdownLayout.visibility = View.GONE
                    workByText.visibility = View.GONE
                    workBySelectorContainer.visibility = View.GONE
                    vendorNameFieldLayout.visibility = View.GONE
                    selectSupervisorText.visibility = View.GONE
                    rvSupervisor.visibility = View.GONE
                    selectComplexityText.visibility = View.GONE
                    complexityRadioGroup.visibility = View.GONE
                    selectTechniciansText.visibility = View.GONE
                    rvTechnicians.visibility = View.GONE
                    approveButton.visibility = View.GONE
                    rejectButton.visibility = View.GONE
                    cancelButton.visibility = View.VISIBLE
                    deployTechButton.visibility = View.GONE
                    descriptionFieldLayout.visibility = View.VISIBLE
                    cancelButton.setOnClickListener {
                        executeUpdate()
                    }
                } else {
                    if (approve) {
                        subDepartmentList = ArrayList()
                        subDepartmentList.add(
                            if (context.getString(R.string.lang) == "in")
                                "Pilih Departemen"
                            else
                                "Select Department"
                        )
                        getSubDepartmentList()
                        selectComplexityText.visibility = View.GONE
                        complexityRadioGroup.visibility = View.GONE
                        selectTechniciansText.visibility = View.GONE
                        rvTechnicians.visibility = View.GONE
                        approveButton.visibility = View.VISIBLE
                        rejectButton.visibility = View.GONE
                        cancelButton.visibility = View.GONE
                        deployTechButton.visibility = View.GONE
                        descriptionFieldLayout.visibility = View.VISIBLE
                        descriptionField.setText(
                            if (context.getString(R.string.lang) == "in")
                                "Disetujui!"
                            else
                                "Approved!"
                        )
                        subDeptText.visibility = View.VISIBLE
                        subDeptDropdownLayout.visibility = View.VISIBLE
                        workByText.visibility = View.VISIBLE
                        workBySelectorContainer.visibility = View.VISIBLE
                        if (workByVendor == "n") {
                            internalButton.strokeColor =
                                ContextCompat.getColor(context, android.R.color.transparent)
                            internalColor.background = ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.gradient_accent_color,
                                context.theme
                            )
                            internalText.setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.white
                                )
                            )
                            vendorButton.strokeColor =
                                ContextCompat.getColor(context, R.color.button_color)
                            vendorColor.background = null
                            vendorText.setTextColor(ContextCompat.getColor(context, R.color.black))
                            vendorNameFieldLayout.visibility = View.GONE
                            vendorNameField.setText("")
                        } else {
                            internalButton.strokeColor =
                                ContextCompat.getColor(context, R.color.button_color)
                            internalColor.background = null
                            internalText.setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.black
                                )
                            )
                            vendorButton.strokeColor =
                                ContextCompat.getColor(context, android.R.color.transparent)
                            vendorColor.background = ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.gradient_accent_color,
                                context.theme
                            )
                            vendorText.setTextColor(ContextCompat.getColor(context, R.color.white))
                            vendorNameFieldLayout.visibility = View.VISIBLE
                        }

                        internalButton.setOnClickListener {
                            workByVendor = "n"
                            internalButton.strokeColor =
                                ContextCompat.getColor(context, android.R.color.transparent)
                            internalColor.background = ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.gradient_accent_color,
                                context.theme
                            )
                            internalText.setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.white
                                )
                            )
                            vendorButton.strokeColor =
                                ContextCompat.getColor(context, R.color.button_color)
                            vendorColor.background = null
                            vendorText.setTextColor(ContextCompat.getColor(context, R.color.black))
                            vendorNameFieldLayout.visibility = View.GONE
                            vendorNameField.setText("")
                        }

                        vendorButton.setOnClickListener {
                            workByVendor = "y"
                            internalButton.strokeColor =
                                ContextCompat.getColor(context, R.color.button_color)
                            internalColor.background = null
                            internalText.setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.black
                                )
                            )
                            vendorButton.strokeColor =
                                ContextCompat.getColor(context, android.R.color.transparent)
                            vendorColor.background = ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.gradient_accent_color,
                                context.theme
                            )
                            vendorText.setTextColor(ContextCompat.getColor(context, R.color.white))
                            vendorNameFieldLayout.visibility = View.VISIBLE
                        }

                        isFormEmpty[0] = true
                        if (dataDetail.deptTujuan == userData.dept) {
                            selectSupervisorText.visibility = View.VISIBLE
                            rvSupervisor.visibility = View.VISIBLE
                        } else {
                            selectSupervisorText.visibility = View.GONE
                            rvSupervisor.visibility = View.GONE
                        }
                        selectedSupervisorsArrayList.add(
                            SupervisorTechnicianListResponse(
                                null,
                                null,
                                null,
                                null,
                                "+",
                                null,
                                null,
                                null
                            )
                        )
                        supervisorsRvAdapter = SelectedSupervisorTechniciansRvAdapter(
                            context,
                            dataDetail,
                            selectedSupervisorsArrayList,
                            true
                        ).also {
                            with(it) {
                                setOnSupervisorSetListener(object :
                                    SelectedSupervisorTechniciansRvAdapter.OnSupervisorSetListener {
                                    override fun onSupervisorsSelected(
                                        data: SupervisorTechnicianListResponse
                                    ) {
                                        selectedSupervisorsArrayList.remove(
                                            SupervisorTechnicianListResponse(
                                                null,
                                                null,
                                                null,
                                                null,
                                                "+",
                                                null,
                                                null,
                                                null
                                            )
                                        )
                                        selectedSupervisorsArrayList.add(data)
                                        selectedSupervisorsArrayList.add(
                                            SupervisorTechnicianListResponse(
                                                null,
                                                null,
                                                null,
                                                null,
                                                "+",
                                                null,
                                                null,
                                                null
                                            )
                                        )
                                        supervisorsRvAdapter.notifyDataSetChanged()
                                        if (supervisorsRvAdapter.itemCount > 1)
                                            isFormEmpty[1] = true
                                    }

                                    override fun onSupervisorsUnselected(
                                        data: SupervisorTechnicianListResponse
                                    ) {
                                        selectedSupervisorsArrayList.remove(
                                            SupervisorTechnicianListResponse(
                                                null,
                                                null,
                                                null,
                                                null,
                                                "+",
                                                null,
                                                null,
                                                null
                                            )
                                        )
                                        selectedSupervisorsArrayList.remove(data)
                                        selectedSupervisorsArrayList.add(
                                            SupervisorTechnicianListResponse(
                                                null,
                                                null,
                                                null,
                                                null,
                                                "+",
                                                null,
                                                null,
                                                null
                                            )
                                        )
                                        supervisorsRvAdapter.notifyDataSetChanged()
                                        if (supervisorsRvAdapter.itemCount == 1)
                                            isFormEmpty[1] = false
                                    }
                                })
                            }
                        }
                        rvSupervisor.adapter = supervisorsRvAdapter
                        rvSupervisor.layoutManager = FlexboxLayoutManager(
                            context, FlexDirection.ROW, FlexWrap.WRAP
                        )
                        supervisorsRvAdapter.notifyDataSetChanged()
                        approveButton.setOnClickListener {
                            executeUpdate()
                        }
                    } else {
                        subDeptText.visibility = View.GONE
                        subDeptDropdownLayout.visibility = View.GONE
                        workByText.visibility = View.GONE
                        workBySelectorContainer.visibility = View.GONE
                        vendorNameFieldLayout.visibility = View.GONE
                        selectSupervisorText.visibility = View.GONE
                        rvSupervisor.visibility = View.GONE
                        selectComplexityText.visibility = View.GONE
                        complexityRadioGroup.visibility = View.GONE
                        selectTechniciansText.visibility = View.GONE
                        rvTechnicians.visibility = View.GONE
                        approveButton.visibility = View.GONE
                        rejectButton.visibility = View.VISIBLE
                        cancelButton.visibility = View.GONE
                        deployTechButton.visibility = View.GONE
                        descriptionFieldLayout.visibility = View.VISIBLE
                        descriptionField.setText(
                            if (context.getString(R.string.lang) == "in")
                                "Ditolak!"
                            else
                                "Rejected!"
                        )
                        isFormEmpty[0] = true
                        rejectButton.setOnClickListener {
                            executeUpdate()
                        }
                    }
                }
            } else {
                subDeptText.visibility = View.GONE
                subDeptDropdownLayout.visibility = View.GONE
                workByText.visibility = View.VISIBLE
                workBySelectorContainer.visibility = View.VISIBLE
                if (workByVendor == "n") {
                    internalButton.strokeColor =
                        ContextCompat.getColor(context, android.R.color.transparent)
                    internalColor.background = ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.gradient_accent_color,
                        context.theme
                    )
                    internalText.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    )
                    vendorButton.strokeColor =
                        ContextCompat.getColor(context, R.color.button_color)
                    vendorColor.background = null
                    vendorText.setTextColor(ContextCompat.getColor(context, R.color.black))
                    vendorNameFieldLayout.visibility = View.GONE
                    vendorNameField.setText("")
                } else {
                    internalButton.strokeColor =
                        ContextCompat.getColor(context, R.color.button_color)
                    internalColor.background = null
                    internalText.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.black
                        )
                    )
                    vendorButton.strokeColor =
                        ContextCompat.getColor(context, android.R.color.transparent)
                    vendorColor.background = ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.gradient_accent_color,
                        context.theme
                    )
                    vendorText.setTextColor(ContextCompat.getColor(context, R.color.white))
                    vendorNameFieldLayout.visibility = View.VISIBLE
                }

                internalButton.setOnClickListener {
                    workByVendor = "n"
                    internalButton.strokeColor =
                        ContextCompat.getColor(context, android.R.color.transparent)
                    internalColor.background = ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.gradient_accent_color,
                        context.theme
                    )
                    internalText.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    )
                    vendorButton.strokeColor =
                        ContextCompat.getColor(context, R.color.button_color)
                    vendorColor.background = null
                    vendorText.setTextColor(ContextCompat.getColor(context, R.color.black))
                    vendorNameFieldLayout.visibility = View.GONE
                    vendorNameField.setText("")
                }

                vendorButton.setOnClickListener {
                    workByVendor = "y"
                    internalButton.strokeColor =
                        ContextCompat.getColor(context, R.color.button_color)
                    internalColor.background = null
                    internalText.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.black
                        )
                    )
                    vendorButton.strokeColor =
                        ContextCompat.getColor(context, android.R.color.transparent)
                    vendorColor.background = ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.gradient_accent_color,
                        context.theme
                    )
                    vendorText.setTextColor(ContextCompat.getColor(context, R.color.white))
                    vendorNameFieldLayout.visibility = View.VISIBLE
                }
                descriptionFieldLayout.visibility = View.GONE
                selectSupervisorText.visibility = View.GONE
                rvSupervisor.visibility = View.GONE
                selectComplexityText.visibility = View.VISIBLE
                complexityRadioGroup.visibility = View.VISIBLE
                complexityRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                    when (checkedId) {
                        R.id.lowSelector -> {
                            complexity = lowSelector.text.toString().lowercase(Locale.getDefault())
                        }

                        R.id.midSelector -> {
                            complexity = midSelector.text.toString().lowercase(Locale.getDefault())
                        }

                        R.id.highSelector -> {
                            complexity = highSelector.text.toString().lowercase(Locale.getDefault())
                        }
                    }
                }
                selectTechniciansText.visibility = View.VISIBLE
                rvTechnicians.visibility = View.VISIBLE
                approveButton.visibility = View.GONE
                rejectButton.visibility = View.GONE
                deployTechButton.visibility = View.GONE
                selectedTechniciansArrayList.add(
                    SupervisorTechnicianListResponse(
                        null,
                        null,
                        null,
                        null,
                        "+",
                        null,
                        null,
                        null
                    )
                )
                techniciansRvAdapter = SelectedSupervisorTechniciansRvAdapter(
                    context,
                    dataDetail,
                    selectedTechniciansArrayList,
                    false
                ).also {
                    with(it) {
                        setOnTechniciansSetListener(object :
                            SelectedSupervisorTechniciansRvAdapter.OnTechniciansSetListener {
                            override fun onTechniciansSelected(
                                data: SupervisorTechnicianListResponse
                            ) {
                                selectedTechniciansArrayList.remove(
                                    SupervisorTechnicianListResponse(
                                        null,
                                        null,
                                        null,
                                        null,
                                        "+",
                                        null,
                                        null,
                                        null
                                    )
                                )
                                selectedTechniciansArrayList.add(data)
                                selectedTechniciansArrayList.add(
                                    SupervisorTechnicianListResponse(
                                        null,
                                        null,
                                        null,
                                        null,
                                        "+",
                                        null,
                                        null,
                                        null
                                    )
                                )
                                techniciansRvAdapter.notifyDataSetChanged()
                                if (techniciansRvAdapter.itemCount > 1)
                                    deployTechButton.visibility = View.VISIBLE
                            }

                            override fun onTechniciansUnselected(
                                data: SupervisorTechnicianListResponse
                            ) {
                                selectedTechniciansArrayList.remove(
                                    SupervisorTechnicianListResponse(
                                        null,
                                        null,
                                        null,
                                        null,
                                        "+",
                                        null,
                                        null,
                                        null
                                    )
                                )
                                selectedTechniciansArrayList.remove(data)
                                selectedTechniciansArrayList.add(
                                    SupervisorTechnicianListResponse(
                                        null,
                                        null,
                                        null,
                                        null,
                                        "+",
                                        null,
                                        null,
                                        null
                                    )
                                )
                                techniciansRvAdapter.notifyDataSetChanged()
                                if (techniciansRvAdapter.itemCount == 1)
                                    deployTechButton.visibility = View.GONE
                            }
                        })
                    }
                }
                rvTechnicians.adapter = techniciansRvAdapter
                rvTechnicians.layoutManager = FlexboxLayoutManager(
                    context, FlexDirection.ROW, FlexWrap.WRAP
                )
                techniciansRvAdapter.notifyDataSetChanged()
                deployTechButton.setOnClickListener {
                    executeUpdate()
                }
            }
        }
    }

    private fun formCheck(): Boolean {
        var validated = 0

        binding.apply {
            if (deployTech)
                isFormEmpty[0] = this@UpdateStatusBottomSheet::complexity.isInitialized

            for (element in isFormEmpty) {
                if (element)
                    validated++
            }

            return if (!deployTech) {
                if (cancel)
                    validated == 1
                else {
                    if (approve) {
                        if (dataDetail.deptTujuan == userData.dept)
                            validated == isFormEmpty.size
                        else
                            validated == 1
                    } else
                        validated == 1
                }
            } else
                validated == 1
        }
    }

    private fun executeUpdate() {
        binding.apply {
            approveLoading.visibility = View.VISIBLE
            rejectLoading.visibility = View.VISIBLE
            deployTechLoading.visibility = View.VISIBLE
            approveText.visibility = View.GONE
            rejectText.visibility = View.GONE
            deployTechText.visibility = View.GONE
            if (!deployTech) {
                if (formCheck()) {
                    if (cancel) {
                        val confirmationDialog = ConfirmationDialog(
                            context,
                            if (context.getString(R.string.lang) == "in")
                                "Apakah Anda yakin ingin membatalkan masalah ini?"
                            else
                                "Are you sure you want to cancel this issue?",
                            if (context.getString(R.string.lang) == "in")
                                "Ya" else "Yes"
                        ).also {
                            with(it) {
                                setConfirmationDialogListener(object :
                                    ConfirmationDialog.ConfirmationDialogListener {
                                    override fun onConfirm() {
                                        cancelLoading.visibility = View.VISIBLE
                                        cancelText.visibility = View.GONE
                                        try {
                                            InitAPI.getEndpoint.cancelSubmission(
                                                userData.id,
                                                dataDetail.idGaprojects!!,
                                                descriptionField.text.toString()
                                            ).enqueue(object :
                                                Callback<GenericSimpleResponse> {
                                                override fun onResponse(
                                                    call1: Call<GenericSimpleResponse>,
                                                    response1: Response<GenericSimpleResponse>
                                                ) {
                                                    cancelLoading.visibility = View.GONE
                                                    cancelText.visibility = View.VISIBLE
                                                    if (response1.isSuccessful) {
                                                        val result = response1.body()
                                                        if (result != null) {
                                                            if (result.code == 1) {
                                                                CustomToast.getInstance(context)
                                                                    .setBackgroundColor(
                                                                        ContextCompat.getColor(
                                                                            context,
                                                                            R.color.custom_toast_background_success
                                                                        )
                                                                    )
                                                                    .setFontColor(
                                                                        ContextCompat.getColor(
                                                                            context,
                                                                            R.color.custom_toast_font_success
                                                                        )
                                                                    )
                                                                    .setMessage(
                                                                        if (context.getString(R.string.lang) == "in")
                                                                            "Pengajuan sukses dibatalkan."
                                                                        else
                                                                            "Submission canceled successfully."
                                                                    ).show()
                                                                this@UpdateStatusBottomSheet.dismiss()
                                                                onUpdateSuccessListener.onCanceled()
                                                            } else {
                                                                CustomToast.getInstance(context)
                                                                    .setMessage(
                                                                        if (context.getString(R.string.lang) == "in")
                                                                            "Tidak dapat membatalkan masalah ini. Silakan coba lagi nanti."
                                                                        else
                                                                            "Can't cancel this issue. Please try again later."
                                                                    )
                                                                    .setFontColor(
                                                                        ContextCompat.getColor(
                                                                            context,
                                                                            R.color.custom_toast_font_failed
                                                                        )
                                                                    )
                                                                    .setBackgroundColor(
                                                                        ContextCompat.getColor(
                                                                            context,
                                                                            R.color.custom_toast_background_failed
                                                                        )
                                                                    ).show()
                                                                Log.e(
                                                                    "ERROR ${response1.code()}",
                                                                    "Cancel Response Code 0 | ${response1.message()}"
                                                                )
                                                            }
                                                        } else {
                                                            CustomToast.getInstance(context)
                                                                .setMessage(
                                                                    if (context.getString(R.string.lang) == "in")
                                                                        "Tidak dapat membatalkan masalah ini. Silakan coba lagi nanti."
                                                                    else
                                                                        "Can't cancel this issue. Please try again later."
                                                                )
                                                                .setFontColor(
                                                                    ContextCompat.getColor(
                                                                        context,
                                                                        R.color.custom_toast_font_failed
                                                                    )
                                                                )
                                                                .setBackgroundColor(
                                                                    ContextCompat.getColor(
                                                                        context,
                                                                        R.color.custom_toast_background_failed
                                                                    )
                                                                ).show()
                                                            Log.e(
                                                                "ERROR ${response1.code()}",
                                                                "Cancel Response null | ${response1.message()}"
                                                            )
                                                        }
                                                    } else {
                                                        CustomToast.getInstance(context)
                                                            .setMessage(
                                                                if (context.getString(R.string.lang) == "in")
                                                                    "Tidak dapat membatalkan masalah ini. Silakan coba lagi nanti."
                                                                else
                                                                    "Can't cancel this issue. Please try again later."
                                                            )
                                                            .setFontColor(
                                                                ContextCompat.getColor(
                                                                    context,
                                                                    R.color.custom_toast_font_failed
                                                                )
                                                            )
                                                            .setBackgroundColor(
                                                                ContextCompat.getColor(
                                                                    context,
                                                                    R.color.custom_toast_background_failed
                                                                )
                                                            ).show()
                                                        Log.e(
                                                            "ERROR ${response1.code()}",
                                                            "Cancel Response Fail | ${response1.message()}"
                                                        )
                                                    }
                                                }

                                                override fun onFailure(
                                                    call1: Call<GenericSimpleResponse>,
                                                    throwable: Throwable
                                                ) {
                                                    cancelLoading.visibility = View.GONE
                                                    cancelText.visibility = View.VISIBLE
                                                    CustomToast.getInstance(context)
                                                        .setMessage(
                                                            if (context.getString(R.string.lang) == "in")
                                                                "Terjadi kesalahan. Silakan coba lagi nanti."
                                                            else
                                                                "Something went wrong. Please try again later."
                                                        )
                                                        .setFontColor(
                                                            ContextCompat.getColor(
                                                                context,
                                                                R.color.custom_toast_font_failed
                                                            )
                                                        )
                                                        .setBackgroundColor(
                                                            ContextCompat.getColor(
                                                                context,
                                                                R.color.custom_toast_background_failed
                                                            )
                                                        ).show()
                                                    Log.e(
                                                        "ERROR Cancel Failure",
                                                        throwable.toString()
                                                    )
                                                    throwable.printStackTrace()
                                                }
                                            })
                                        } catch (jsonException: JSONException) {
                                            cancelLoading.visibility = View.GONE
                                            cancelText.visibility = View.VISIBLE
                                            CustomToast
                                                .getInstance(context)
                                                .setMessage(
                                                    if (context.getString(R.string.lang) == "in")
                                                        "Terjadi kesalahan. Silakan coba lagi nanti."
                                                    else
                                                        "Something went wrong. Please try again later."
                                                )
                                                .setFontColor(
                                                    ContextCompat.getColor(
                                                        context,
                                                        R.color.custom_toast_font_failed
                                                    )
                                                )
                                                .setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        context,
                                                        R.color.custom_toast_background_failed
                                                    )
                                                ).show()
                                            Log.e(
                                                "ERROR Cancel JSON Exception",
                                                jsonException.toString()
                                            )
                                            jsonException.printStackTrace()
                                        }
                                    }
                                })
                            }
                        }

                        if (confirmationDialog.window != null)
                            confirmationDialog.show()
                    } else {
                        if (approve) {
                            try {
                                val data: MutableMap<String, RequestBody> = mutableMapOf()
                                with(data) {
                                    put("id_user", createPartFromString(userData.id.toString())!!)
                                    put(
                                        "id_gaprojects",
                                        createPartFromString(dataDetail.idGaprojects.toString())!!
                                    )
                                    put(
                                        "keterangan",
                                        createPartFromString(descriptionField.text.toString())!!
                                    )
                                    if (dataDetail.deptTujuan == userData.dept) {
                                        for (i in 0 until selectedSupervisorsArrayList.size - 1) {
                                            put(
                                                "user_supervisor[$i]",
                                                createPartFromString(
                                                    selectedSupervisorsArrayList[i].idUser.toString()
                                                )!!
                                            )
                                        }
                                    }
                                }
                                val targetData: MutableMap<String, RequestBody> = mutableMapOf()
                                with(targetData) {
                                    put("id_user", createPartFromString(userData.id.toString())!!)
                                    put(
                                        "id_gaprojects",
                                        createPartFromString(dataDetail.idGaprojects.toString())!!
                                    )
                                    put(
                                        "keterangan",
                                        createPartFromString(descriptionField.text.toString())!!
                                    )
                                    put(
                                        "id_dept_tujuan",
                                        createPartFromString(selectedSubDept?.idDept.toString())!!
                                    )
                                    put(
                                        "progress_vendor",
                                        createPartFromString(workByVendor)!!
                                    )
                                    put(
                                        "progress_vendor_nama",
                                        createPartFromString(vendorNameField.text.toString())!!
                                    )
                                    if (dataDetail.deptTujuan == userData.dept) {
                                        for (i in 0 until selectedSupervisorsArrayList.size - 1) {
                                            put(
                                                "user_supervisor[$i]",
                                                createPartFromString(
                                                    selectedSupervisorsArrayList[i].idUser.toString()
                                                )!!
                                            )
                                        }
                                    }
                                }
                                (if (dataDetail.deptTujuan == userData.dept)
                                    InitAPI.getEndpoint.approveTargetManagerSubmission(targetData)
                                else InitAPI.getEndpoint.approveReportManagerSubmission(data)).enqueue(
                                    object : Callback<GenericSimpleResponse> {
                                        override fun onResponse(
                                            call: Call<GenericSimpleResponse>,
                                            response: Response<GenericSimpleResponse>
                                        ) {
                                            approveLoading.visibility = View.GONE
                                            rejectLoading.visibility = View.GONE
                                            deployTechLoading.visibility = View.GONE
                                            approveText.visibility = View.VISIBLE
                                            rejectText.visibility = View.VISIBLE
                                            deployTechText.visibility = View.VISIBLE
                                            if (response.isSuccessful) {
                                                if (response.body() != null) {
                                                    val result = response.body()
                                                    if (result?.code == 1) {
                                                        CustomToast.getInstance(context)
                                                            .setBackgroundColor(
                                                                ContextCompat.getColor(
                                                                    context,
                                                                    R.color.custom_toast_background_success
                                                                )
                                                            )
                                                            .setFontColor(
                                                                ContextCompat.getColor(
                                                                    context,
                                                                    R.color.custom_toast_font_success
                                                                )
                                                            )
                                                            .setMessage(
                                                                if (context.getString(R.string.lang) == "in")
                                                                    "Pengajuan berhasil disetujui."
                                                                else
                                                                    "Submission approved successfully."
                                                            ).show()
                                                        dismiss()
                                                        onUpdateSuccessListener.onApproved()
                                                    } else {
                                                        CustomToast.getInstance(context)
                                                            .setBackgroundColor(
                                                                ContextCompat.getColor(
                                                                    context,
                                                                    R.color.custom_toast_background_failed
                                                                )
                                                            )
                                                            .setFontColor(
                                                                ContextCompat.getColor(
                                                                    context,
                                                                    R.color.custom_toast_font_failed
                                                                )
                                                            )
                                                            .setMessage(
                                                                if (context.getString(R.string.lang) == "in")
                                                                    "Gagal menyetujui pengajuan."
                                                                else
                                                                    "Failed to approve submission."
                                                            )
                                                            .show()
                                                        Log.e(
                                                            "ERROR ${result?.code}",
                                                            result?.message.toString()
                                                        )
                                                    }
                                                } else {
                                                    CustomToast.getInstance(context)
                                                        .setBackgroundColor(
                                                            ContextCompat.getColor(
                                                                context,
                                                                R.color.custom_toast_background_failed
                                                            )
                                                        )
                                                        .setFontColor(
                                                            ContextCompat.getColor(
                                                                context,
                                                                R.color.custom_toast_font_failed
                                                            )
                                                        )
                                                        .setMessage(
                                                            if (context.getString(R.string.lang) == "in")
                                                                "Gagal menyetujui pengajuan."
                                                            else
                                                                "Failed to approve submission."
                                                        )
                                                        .show()
                                                    Log.e(
                                                        "ERROR ${response.code()}",
                                                        response.message().toString()
                                                    )
                                                }
                                            } else {
                                                CustomToast.getInstance(context)
                                                    .setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.custom_toast_background_failed
                                                        )
                                                    )
                                                    .setFontColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.custom_toast_font_failed
                                                        )
                                                    )
                                                    .setMessage(
                                                        if (context.getString(R.string.lang) == "in")
                                                            "Gagal menyetujui pengajuan."
                                                        else
                                                            "Failed to approve submission."
                                                    )
                                                    .show()
                                                Log.e(
                                                    "ERROR ${response.code()}",
                                                    response.message().toString()
                                                )
                                            }
                                        }

                                        override fun onFailure(
                                            call: Call<GenericSimpleResponse>, throwable: Throwable
                                        ) {
                                            approveLoading.visibility = View.GONE
                                            rejectLoading.visibility = View.GONE
                                            deployTechLoading.visibility = View.GONE
                                            approveText.visibility = View.VISIBLE
                                            rejectText.visibility = View.VISIBLE
                                            deployTechText.visibility = View.VISIBLE
                                            CustomToast.getInstance(context)
                                                .setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        context,
                                                        R.color.custom_toast_background_failed
                                                    )
                                                )
                                                .setFontColor(
                                                    ContextCompat.getColor(
                                                        context,
                                                        R.color.custom_toast_font_failed
                                                    )
                                                )
                                                .setMessage(
                                                    if (context.getString(R.string.lang) == "in")
                                                        "Gagal menyetujui pengajuan."
                                                    else
                                                        "Failed to approve submission."
                                                ).show()
                                            Log.e("ERROR", throwable.message.toString())
                                            throwable.printStackTrace()
                                        }
                                    })
                            } catch (jsonException: JSONException) {
                                approveLoading.visibility = View.GONE
                                rejectLoading.visibility = View.GONE
                                deployTechLoading.visibility = View.GONE
                                approveText.visibility = View.VISIBLE
                                rejectText.visibility = View.VISIBLE
                                deployTechText.visibility = View.VISIBLE
                                CustomToast.getInstance(context)
                                    .setBackgroundColor(
                                        ContextCompat.getColor(
                                            context,
                                            R.color.custom_toast_background_failed
                                        )
                                    )
                                    .setFontColor(
                                        ContextCompat.getColor(
                                            context,
                                            R.color.custom_toast_font_failed
                                        )
                                    )
                                    .setMessage(
                                        if (context.getString(R.string.lang) == "in")
                                            "Gagal menyetujui pengajuan."
                                        else
                                            "Failed to approve submission."
                                    ).show()
                                jsonException.printStackTrace()
                            }
                        } else {
                            try {
                                InitAPI.getEndpoint.rejectSubmission(
                                    userData.id, dataDetail.idGaprojects!!,
                                    descriptionField.text.toString()
                                ).enqueue(object : Callback<GenericSimpleResponse> {
                                    override fun onResponse(
                                        call: Call<GenericSimpleResponse>,
                                        response: Response<GenericSimpleResponse>
                                    ) {
                                        approveLoading.visibility = View.GONE
                                        rejectLoading.visibility = View.GONE
                                        deployTechLoading.visibility = View.GONE
                                        approveText.visibility = View.VISIBLE
                                        rejectText.visibility = View.VISIBLE
                                        deployTechText.visibility = View.VISIBLE
                                        if (response.isSuccessful) {
                                            if (response.body() != null) {
                                                val result = response.body()
                                                if (result?.code == 1) {
                                                    CustomToast.getInstance(context)
                                                        .setBackgroundColor(
                                                            ContextCompat.getColor(
                                                                context,
                                                                R.color.custom_toast_background_success
                                                            )
                                                        )
                                                        .setFontColor(
                                                            ContextCompat.getColor(
                                                                context,
                                                                R.color.custom_toast_font_success
                                                            )
                                                        )
                                                        .setMessage(
                                                            if (context.getString(R.string.lang) == "in")
                                                                "Pengajuan berhasil ditolak."
                                                            else
                                                                "Submission rejected successfully."
                                                        ).show()
                                                    dismiss()
                                                    onUpdateSuccessListener.onRejected()
                                                } else {
                                                    CustomToast.getInstance(context)
                                                        .setBackgroundColor(
                                                            ContextCompat.getColor(
                                                                context,
                                                                R.color.custom_toast_background_failed
                                                            )
                                                        )
                                                        .setFontColor(
                                                            ContextCompat.getColor(
                                                                context,
                                                                R.color.custom_toast_font_failed
                                                            )
                                                        )
                                                        .setMessage(
                                                            if (context.getString(R.string.lang) == "in")
                                                                "Gagal menolak pengajuan."
                                                            else
                                                                "Failed to reject submission."
                                                        )
                                                        .show()
                                                    Log.e(
                                                        "ERROR ${result?.code}",
                                                        result?.message.toString()
                                                    )
                                                }
                                            } else {
                                                CustomToast.getInstance(context)
                                                    .setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.custom_toast_background_failed
                                                        )
                                                    )
                                                    .setFontColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.custom_toast_font_failed
                                                        )
                                                    )
                                                    .setMessage(
                                                        if (context.getString(R.string.lang) == "in")
                                                            "Gagal menolak pengajuan."
                                                        else
                                                            "Failed to reject submission."
                                                    )
                                                    .show()
                                                Log.e(
                                                    "ERROR ${response.code()}",
                                                    response.message().toString()
                                                )
                                            }
                                        } else {
                                            CustomToast.getInstance(context)
                                                .setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        context,
                                                        R.color.custom_toast_background_failed
                                                    )
                                                )
                                                .setFontColor(
                                                    ContextCompat.getColor(
                                                        context,
                                                        R.color.custom_toast_font_failed
                                                    )
                                                )
                                                .setMessage(
                                                    if (context.getString(R.string.lang) == "in")
                                                        "Gagal menolak pengajuan."
                                                    else
                                                        "Failed to reject submission."
                                                ).show()
                                            Log.e(
                                                "ERROR ${response.code()}",
                                                response.message().toString()
                                            )
                                        }
                                    }

                                    override fun onFailure(
                                        call: Call<GenericSimpleResponse>, throwable: Throwable
                                    ) {
                                        approveLoading.visibility = View.GONE
                                        rejectLoading.visibility = View.GONE
                                        deployTechLoading.visibility = View.GONE
                                        approveText.visibility = View.VISIBLE
                                        rejectText.visibility = View.VISIBLE
                                        deployTechText.visibility = View.VISIBLE
                                        CustomToast.getInstance(context)
                                            .setBackgroundColor(
                                                ContextCompat.getColor(
                                                    context,
                                                    R.color.custom_toast_background_failed
                                                )
                                            )
                                            .setFontColor(
                                                ContextCompat.getColor(
                                                    context,
                                                    R.color.custom_toast_font_failed
                                                )
                                            )
                                            .setMessage(
                                                if (context.getString(R.string.lang) == "in")
                                                    "Gagal menolak pengajuan."
                                                else
                                                    "Failed to reject submission."
                                            ).show()
                                        Log.e("ERROR", throwable.message.toString())
                                        throwable.printStackTrace()
                                    }
                                })
                            } catch (jsonException: JSONException) {
                                approveLoading.visibility = View.GONE
                                rejectLoading.visibility = View.GONE
                                deployTechLoading.visibility = View.GONE
                                approveText.visibility = View.VISIBLE
                                rejectText.visibility = View.VISIBLE
                                deployTechText.visibility = View.VISIBLE
                                CustomToast.getInstance(context)
                                    .setBackgroundColor(
                                        ContextCompat.getColor(
                                            context,
                                            R.color.custom_toast_background_failed
                                        )
                                    )
                                    .setFontColor(
                                        ContextCompat.getColor(
                                            context,
                                            R.color.custom_toast_font_failed
                                        )
                                    )
                                    .setMessage(
                                        if (context.getString(R.string.lang) == "in")
                                            "Gagal menolak pengajuan."
                                        else
                                            "Failed to reject submission."
                                    ).show()
                                jsonException.printStackTrace()
                            }
                        }
                    }
                } else {
                    approveLoading.visibility = View.GONE
                    rejectLoading.visibility = View.GONE
                    deployTechLoading.visibility = View.GONE
                    approveText.visibility = View.VISIBLE
                    rejectText.visibility = View.VISIBLE
                    deployTechText.visibility = View.VISIBLE
                    CustomToast.getInstance(context)
                        .setBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.custom_toast_background_failed
                            )
                        )
                        .setFontColor(
                            ContextCompat.getColor(
                                context,
                                R.color.custom_toast_font_failed
                            )
                        )
                        .setMessage(
                            if (context.getString(R.string.lang) == "in")
                                "Pastikan semua kolom terisi."
                            else
                                "Make sure all fields are filled."
                        ).show()
                    if (descriptionField.text.toString().isEmpty())
                        descriptionFieldLayout.error = "Make sure all fields are filled."
                }
            } else {
                if (formCheck()) {
                    try {
                        val data: MutableMap<String, RequestBody> = mutableMapOf()
                        with(data) {
                            put("id_user", createPartFromString(userData.id.toString())!!)
                            put(
                                "id_gaprojects",
                                createPartFromString(dataDetail.idGaprojects.toString())!!
                            )
                            for (i in 0 until selectedTechniciansArrayList.size - 1) {
                                put(
                                    "user_teknisi[$i]",
                                    createPartFromString(
                                        selectedTechniciansArrayList[i].idUser.toString()
                                    )!!
                                )
                            }
                            put("difficulty", createPartFromString(complexity)!!)
                        }
                        InitAPI.getEndpoint.deployTechnicians(data)
                            .enqueue(object : Callback<GenericSimpleResponse> {
                                override fun onResponse(
                                    call: Call<GenericSimpleResponse>,
                                    response: Response<GenericSimpleResponse>
                                ) {
                                    approveLoading.visibility = View.GONE
                                    rejectLoading.visibility = View.GONE
                                    deployTechLoading.visibility = View.GONE
                                    approveText.visibility = View.VISIBLE
                                    rejectText.visibility = View.VISIBLE
                                    deployTechText.visibility = View.VISIBLE
                                    if (response.isSuccessful) {
                                        if (response.body() != null) {
                                            val result = response.body()
                                            if (result?.code == 1) {
                                                CustomToast.getInstance(context)
                                                    .setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.custom_toast_background_success
                                                        )
                                                    )
                                                    .setFontColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.custom_toast_font_success
                                                        )
                                                    )
                                                    .setMessage(
                                                        if (context.getString(R.string.lang) == "in")
                                                            "Teknisi berhasil dikerahkan!"
                                                        else
                                                            "Technicians successfully deployed!"
                                                    ).show()
                                                dismiss()
                                                onUpdateSuccessListener.onTechniciansDeployed()
                                            } else {
                                                CustomToast.getInstance(context)
                                                    .setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.custom_toast_background_failed
                                                        )
                                                    )
                                                    .setFontColor(
                                                        ContextCompat.getColor(
                                                            context,
                                                            R.color.custom_toast_font_failed
                                                        )
                                                    )
                                                    .setMessage(
                                                        if (context.getString(R.string.lang) == "in")
                                                            "Gagal mengerahkan teknisi."
                                                        else
                                                            "Failed to deploy technicians."
                                                    )
                                                    .show()
                                                Log.e(
                                                    "ERROR ${result?.code}",
                                                    result?.message.toString()
                                                )
                                            }
                                        } else {
                                            CustomToast.getInstance(context)
                                                .setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        context,
                                                        R.color.custom_toast_background_failed
                                                    )
                                                )
                                                .setFontColor(
                                                    ContextCompat.getColor(
                                                        context,
                                                        R.color.custom_toast_font_failed
                                                    )
                                                )
                                                .setMessage(
                                                    if (context.getString(R.string.lang) == "in")
                                                        "Gagal mengerahkan teknisi."
                                                    else
                                                        "Failed to deploy technicians."
                                                )
                                                .show()
                                            Log.e(
                                                "ERROR ${response.code()}",
                                                response.message().toString()
                                            )
                                        }
                                    } else {
                                        CustomToast.getInstance(context)
                                            .setBackgroundColor(
                                                ContextCompat.getColor(
                                                    context,
                                                    R.color.custom_toast_background_failed
                                                )
                                            )
                                            .setFontColor(
                                                ContextCompat.getColor(
                                                    context,
                                                    R.color.custom_toast_font_failed
                                                )
                                            )
                                            .setMessage(
                                                if (context.getString(R.string.lang) == "in")
                                                    "Gagal mengerahkan teknisi."
                                                else
                                                    "Failed to deploy technicians."
                                            ).show()
                                        Log.e(
                                            "ERROR ${response.code()}",
                                            response.message().toString()
                                        )
                                    }
                                }

                                override fun onFailure(
                                    call: Call<GenericSimpleResponse>, throwable: Throwable
                                ) {
                                    approveLoading.visibility = View.GONE
                                    rejectLoading.visibility = View.GONE
                                    deployTechLoading.visibility = View.GONE
                                    approveText.visibility = View.VISIBLE
                                    rejectText.visibility = View.VISIBLE
                                    deployTechText.visibility = View.VISIBLE
                                    CustomToast.getInstance(context)
                                        .setBackgroundColor(
                                            ContextCompat.getColor(
                                                context,
                                                R.color.custom_toast_background_failed
                                            )
                                        )
                                        .setFontColor(
                                            ContextCompat.getColor(
                                                context,
                                                R.color.custom_toast_font_failed
                                            )
                                        )
                                        .setMessage(
                                            if (context.getString(R.string.lang) == "in")
                                                "Gagal mengerahkan teknisi."
                                            else
                                                "Failed to deploy technicians."
                                        ).show()
                                    Log.e("ERROR", throwable.message.toString())
                                    throwable.printStackTrace()
                                }
                            })
                    } catch (jsonException: JSONException) {
                        approveLoading.visibility = View.GONE
                        rejectLoading.visibility = View.GONE
                        deployTechLoading.visibility = View.GONE
                        approveText.visibility = View.VISIBLE
                        rejectText.visibility = View.VISIBLE
                        deployTechText.visibility = View.VISIBLE
                        CustomToast.getInstance(context)
                            .setBackgroundColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.custom_toast_background_failed
                                )
                            )
                            .setFontColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.custom_toast_font_failed
                                )
                            )
                            .setMessage(
                                if (context.getString(R.string.lang) == "in")
                                    "Gagal mengerahkan teknisi."
                                else
                                    "Failed to deploy technicians."
                            ).show()
                        jsonException.printStackTrace()
                    }
                } else {
                    approveLoading.visibility = View.GONE
                    rejectLoading.visibility = View.GONE
                    deployTechLoading.visibility = View.GONE
                    approveText.visibility = View.VISIBLE
                    rejectText.visibility = View.VISIBLE
                    deployTechText.visibility = View.VISIBLE
                    CustomToast.getInstance(context)
                        .setBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.custom_toast_background_failed
                            )
                        )
                        .setFontColor(
                            ContextCompat.getColor(
                                context,
                                R.color.custom_toast_font_failed
                            )
                        )
                        .setMessage(
                            if (context.getString(R.string.lang) == "in")
                                "Pastikan semua kolom terisi."
                            else
                                "Make sure all fields are filled."
                        ).show()
                }
            }
        }
    }

    private fun getSubDepartmentList() {
        binding.apply {
            InitAPI.getEndpoint.getSubDepartmentList(dataDetail.deptTujuan!!)
                .enqueue(object : Callback<List<SubDepartmentListResponse>> {
                    override fun onResponse(
                        call: Call<List<SubDepartmentListResponse>?>,
                        response: Response<List<SubDepartmentListResponse>?>
                    ) {
                        if (response.isSuccessful) {
                            val result = response.body()
                            if (result != null) {
                                for (subDept in result) {
                                    subDepartmentList.add(
                                        subDept.subDept.toString()
                                    )
                                }
                                val subDepartmentAdapter = ArrayAdapter(
                                    context,
                                    R.layout.general_dropdown_item,
                                    subDepartmentList
                                )
                                subDeptDropdown.adapter = subDepartmentAdapter
                                subDeptDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(
                                        parent: AdapterView<*>?,
                                        view: View?,
                                        position: Int,
                                        id: Long
                                    ) {
                                        if (position != 0) {
                                            val subDept = parent?.getItemAtPosition(position).toString()
                                            selectedSubDept = result[subDepartmentList.indexOf(subDept)-1]
                                            Log.e("Selected Sub Department", selectedSubDept.toString())
                                        } else {
                                            selectedSubDept = null
                                        }
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {
                                    }
                                }
                                subDepartmentAdapter.notifyDataSetChanged()
                            }
                        } else {
                            CustomToast.getInstance(context)
                                .setBackgroundColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.custom_toast_background_failed
                                    )
                                )
                                .setFontColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.custom_toast_font_failed
                                    )
                                )
                                .setMessage(
                                    if (context.getString(R.string.lang) == "in")
                                        "Gagal mendapatkan daftar Sub Departemen."
                                    else
                                        "Failed to get Sub Department List."
                                ).show()
                            Log.e(
                                "ERROR ${response.code()}",
                                response.message().toString()
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<List<SubDepartmentListResponse>?>,
                        t: Throwable
                    ) {
                        TODO("Not yet implemented")
                    }
                })
        }
    }

    private fun createPartFromString(stringData: String?): RequestBody? {
        return stringData?.toRequestBody("text/plain".toMediaTypeOrNull())
    }

    fun setOnUpdateSuccessListener(listener: OnUpdateSuccessListener) {
        this.onUpdateSuccessListener = listener
    }

    interface OnUpdateSuccessListener {
        fun onApproved()
        fun onRejected()
        fun onCanceled()
        fun onTechniciansDeployed()
    }
}