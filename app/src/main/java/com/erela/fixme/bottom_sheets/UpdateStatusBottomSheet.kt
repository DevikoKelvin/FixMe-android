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
import com.erela.fixme.dialogs.LoadingDialog
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.CategoryListResponse
import com.erela.fixme.objects.GenericSimpleResponse
import com.erela.fixme.objects.SubDepartmentListResponse
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.SupervisorTechnician
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
    private val approve: Boolean, private val cancel: Boolean, private val deployTech: Boolean,
    private val isEdit: Boolean, private val editCategoryComplexity: Boolean = false
) : BottomSheetDialog(context) {
    private val binding: BsUpdateStatusBinding by lazy {
        BsUpdateStatusBinding.inflate(layoutInflater)
    }
    private val userData: UserData by lazy {
        UserDataHelper(context).getUserData()
    }
    private lateinit var onUpdateSuccessListener: OnUpdateSuccessListener
    private var selectedSupervisorsArrayList: ArrayList<SupervisorTechnician> =
        ArrayList()
    private var selectedTechniciansArrayList: ArrayList<SupervisorTechnician> =
        ArrayList()
    private lateinit var supervisorsRvAdapter: SelectedSupervisorTechniciansRvAdapter
    private lateinit var techniciansRvAdapter: SelectedSupervisorTechniciansRvAdapter
    private var isFormEmpty = arrayOf(
        false,
        false
    )
    private lateinit var complexity: String
    private var workByVendor = "N"
    private var selectedCategoryId: Int = 0
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
            // Hide category fields by default — only shown in editCategoryComplexity mode
            selectCategoryText.visibility = View.GONE
            categoryDropdownLayout.visibility = View.GONE

            // Edit Category & Complexity mode
            if (editCategoryComplexity) {
                issueTitle.text = if (context.getString(R.string.lang) == "in")
                    "Edit Kategori & Kompleksitas"
                else
                    "Edit Category & Complexity"

                descriptionFieldLayout.visibility = View.GONE
                subDeptText.visibility = View.GONE
                subDeptDropdownLayout.visibility = View.GONE
                workByText.visibility = View.GONE
                workBySelectorContainer.visibility = View.GONE
                vendorNameFieldLayout.visibility = View.GONE
                selectSupervisorText.visibility = View.GONE
                rvSupervisor.visibility = View.GONE
                selectTechniciansText.visibility = View.GONE
                rvTechnicians.visibility = View.GONE
                approveButton.visibility = View.GONE
                rejectButton.visibility = View.GONE
                cancelButton.visibility = View.GONE

                selectCategoryText.visibility = View.VISIBLE
                categoryDropdownLayout.visibility = View.VISIBLE
                selectComplexityText.visibility = View.VISIBLE
                complexityRadioGroup.visibility = View.VISIBLE

                // Pre-populate from existing data
                selectedCategoryId = dataDetail.idKategori ?: 0
                if (!dataDetail.difficulty.isNullOrEmpty() && dataDetail.difficulty != "null") {
                    complexity = dataDetail.difficulty
                    when (dataDetail.difficulty) {
                        "low" -> lowSelector.isChecked = true
                        "middle" -> midSelector.isChecked = true
                        "high" -> highSelector.isChecked = true
                    }
                }

                complexityRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                    when (checkedId) {
                        R.id.lowSelector -> complexity =
                            lowSelector.text.toString().lowercase(Locale.getDefault())

                        R.id.midSelector -> complexity =
                            midSelector.text.toString().lowercase(Locale.getDefault())

                        R.id.highSelector -> complexity =
                            highSelector.text.toString().lowercase(Locale.getDefault())
                    }
                }

                actionsButtonContainer.visibility = View.VISIBLE
                deployTechButton.visibility = View.VISIBLE
                deployTechText.text =
                    if (context.getString(R.string.lang) == "in") "Simpan" else "Save"
                deployTechButton.setOnClickListener { executeUpdate() }

                getCategoryListForEditMode()
                return
            }

            // In edit mode, show different title
            if (isEdit && deployTech) {
                issueTitle.text = if (context.getString(R.string.lang) == "in")
                    "Edit Teknisi"
                else
                    "Edit Technicians"
            } else {
                issueTitle.text = dataDetail.judulKasus?.uppercase(Locale.ROOT)
            }
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
                            if (isEdit) {
                                dataDetail.keterangan ?: ""
                            } else {
                                if (context.getString(R.string.lang) == "in")
                                    "Disetujui!"
                                else
                                    "Approved!"
                            }
                        )
                        if (isEdit) {
                            approveText.text = if (context.getString(R.string.lang) == "in")
                                "Perbarui"
                            else
                                "Update"
                            workByVendor =
                                if (dataDetail.isVendor.equals("Y", ignoreCase = true)) "Y"
                                else "N"
                        }
                        if (dataDetail.deptTujuan != userData.dept) {
                            subDeptText.visibility = View.GONE
                            subDeptDropdownLayout.visibility = View.GONE
                            workByText.visibility = View.GONE
                            workBySelectorContainer.visibility = View.GONE
                            vendorNameFieldLayout.visibility = View.GONE
                            selectSupervisorText.visibility = View.GONE
                            rvSupervisor.visibility = View.GONE
                        } else {
                            subDeptText.visibility = View.VISIBLE
                            subDeptDropdownLayout.visibility = View.VISIBLE
                            workByText.visibility = View.VISIBLE
                            workBySelectorContainer.visibility = View.VISIBLE
                            if (workByVendor == "n" || workByVendor == "N") {
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
                                vendorText.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.black
                                    )
                                )
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
                                vendorText.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.white
                                    )
                                )
                                vendorNameFieldLayout.visibility = View.VISIBLE
                                if (isEdit) vendorNameField.setText(dataDetail.vendorName ?: "")
                            }

                            internalButton.setOnClickListener {
                                workByVendor = "N"
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
                                vendorText.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.black
                                    )
                                )
                                vendorNameFieldLayout.visibility = View.GONE
                                vendorNameField.setText("")
                            }

                            vendorButton.setOnClickListener {
                                workByVendor = "Y"
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
                                vendorText.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.white
                                    )
                                )
                                vendorNameFieldLayout.visibility = View.VISIBLE
                            }

                            isFormEmpty[0] = descriptionField.text.toString().isNotEmpty()
                            if (dataDetail.deptTujuan == userData.dept) {
                                selectSupervisorText.visibility = View.VISIBLE
                                rvSupervisor.visibility = View.VISIBLE
                            } else {
                                selectSupervisorText.visibility = View.GONE
                                rvSupervisor.visibility = View.GONE
                            }
                            if (isEdit && !dataDetail.usernUserSpv.isNullOrEmpty()) {
                                dataDetail.usernUserSpv.forEach { spv ->
                                    if (spv != null) {
                                        selectedSupervisorsArrayList.add(
                                            SupervisorTechnician(
                                                idUser = spv.idUser,
                                                namaUser = spv.namaUser,
                                                namaDept = spv.deptUser
                                            )
                                        )
                                    }
                                }
                                if (selectedSupervisorsArrayList.isNotEmpty()) isFormEmpty[1] = true
                            }
                            selectedSupervisorsArrayList.add(
                                SupervisorTechnician(
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "+",
                                    null
                                )
                            )

                            supervisorsRvAdapter = SelectedSupervisorTechniciansRvAdapter(
                                context,
                                dataDetail,
                                selectedSupervisorsArrayList,
                                true
                            ) { selectedSubDept }.also {
                                with(it) {
                                    setOnSupervisorSetListener(object :
                                        SelectedSupervisorTechniciansRvAdapter.OnSupervisorSetListener {
                                        override fun onSupervisorsSelected(
                                            data: SupervisorTechnician
                                        ) {
                                            selectedSupervisorsArrayList.remove(
                                                SupervisorTechnician(
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    "+",
                                                    null
                                                )
                                            )
                                            selectedSupervisorsArrayList.add(data)
                                            selectedSupervisorsArrayList.add(
                                                SupervisorTechnician(
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    "+",
                                                    null
                                                )
                                            )
                                            supervisorsRvAdapter.notifyDataSetChanged()
                                            if (supervisorsRvAdapter.itemCount > 1)
                                                isFormEmpty[1] = true
                                        }

                                        override fun onSupervisorsUnselected(
                                            data: SupervisorTechnician
                                        ) {
                                            selectedSupervisorsArrayList.remove(
                                                SupervisorTechnician(
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    "+",
                                                    null
                                                )
                                            )
                                            selectedSupervisorsArrayList.remove(data)
                                            selectedSupervisorsArrayList.add(
                                                SupervisorTechnician(
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    "+",
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
                        }
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
                // Edit mode: Show only technicians selection
                if (isEdit) {
                    // Show title for header
                    issueTitle.visibility = View.VISIBLE

                    // Hide all non-technician-related UI
                    descriptionFieldLayout.visibility = View.GONE
                    selectComplexityText.visibility = View.GONE
                    complexityRadioGroup.visibility = View.GONE
                    workByText.visibility = View.GONE
                    workBySelectorContainer.visibility = View.GONE
                    vendorNameFieldLayout.visibility = View.GONE
                    subDeptText.visibility = View.GONE
                    subDeptDropdownLayout.visibility = View.GONE
                    // Don't hide the actions button container so deployTechButton can be visible
                    actionsButtonContainer.visibility = View.VISIBLE

                    // Show only technicians selection
                    deployTechText.text = if (context.getString(R.string.lang) == "in")
                        "Perbarui"
                    else
                        "Update"
                    approveButton.visibility = View.GONE
                    rejectButton.visibility = View.GONE
                    cancelButton.visibility = View.GONE

                    // Set up deploy button click listener
                    deployTechButton.setOnClickListener {
                        executeUpdate()
                    }

                    setupTechnician()
                } else {
                    // Normal deploy mode
                    workByVendor = if (dataDetail.isVendor == "Y") "Y" else "N"
                    subDeptText.visibility = View.GONE
                    subDeptDropdownLayout.visibility = View.GONE
                    workByText.visibility = View.VISIBLE
                    workBySelectorContainer.visibility = View.VISIBLE
                    if (workByVendor == "n" || workByVendor == "N") {
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
                        setupTechnician()
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
                        vendorNameField.setText(dataDetail.vendorName ?: "")
                        selectTechniciansText.visibility = View.GONE
                        rvTechnicians.visibility = View.GONE
                        deployTechButton.visibility = View.VISIBLE
                        deployTechText.text = context.getString(R.string.action_on_progress)
                    }

                    internalButton.setOnClickListener {
                        workByVendor = "N"
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
                        setupTechnician()
                    }

                    vendorButton.setOnClickListener {
                        workByVendor = "Y"
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
                        selectTechniciansText.visibility = View.GONE
                        rvTechnicians.visibility = View.GONE
                        deployTechButton.visibility = View.VISIBLE
                        deployTechText.text = context.getString(R.string.action_on_progress)
                    }

                    deployTechButton.setOnClickListener {
                        executeUpdate()
                    }
                    descriptionFieldLayout.visibility = View.GONE
                    selectComplexityText.visibility = View.VISIBLE
                    complexityRadioGroup.visibility = View.VISIBLE
                    complexityRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                        when (checkedId) {
                            R.id.lowSelector -> {
                                complexity =
                                    lowSelector.text.toString().lowercase(Locale.getDefault())
                            }

                            R.id.midSelector -> {
                                complexity =
                                    midSelector.text.toString().lowercase(Locale.getDefault())
                            }

                            R.id.highSelector -> {
                                complexity =
                                    highSelector.text.toString().lowercase(Locale.getDefault())
                            }
                        }
                    }
                    approveButton.visibility = View.GONE
                    rejectButton.visibility = View.GONE
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupTechnician() {
        binding.apply {
            selectTechniciansText.visibility = View.VISIBLE
            rvTechnicians.visibility = View.VISIBLE
            deployTechButton.visibility = View.GONE
            selectedTechniciansArrayList.clear()

            // In edit mode, show the update button only when the selected technicians set changes.
            // Compare by `idUser` (fallback to `namaUser`) and ignore the "+" placeholder item.
            val initialTechnicianKeys: Set<String> = if (isEdit) {
                dataDetail.usernUserTeknisi
                    ?.mapNotNull { tech ->
                        (tech?.idUser?.toString() ?: tech?.namaUser)?.trim()
                    }
                    ?.filter { it.isNotEmpty() }
                    ?.toSet()
                    ?: emptySet()
            } else {
                emptySet()
            }

            fun currentTechnicianKeys(): Set<String> {
                return selectedTechniciansArrayList
                    .filter { it.namaUser != "+" }
                    .mapNotNull { tech ->
                        (tech.idUser?.toString() ?: tech.namaUser)?.trim()
                    }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }

            fun refreshDeployTechButtonVisibility() {
                if (!isEdit) {
                    deployTechButton.visibility =
                        if (currentTechnicianKeys().isNotEmpty()) View.VISIBLE else View.GONE
                    return
                }

                val currentKeys = currentTechnicianKeys()
                deployTechButton.visibility =
                    if (currentKeys.isNotEmpty() && currentKeys != initialTechnicianKeys) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }

            // Pre-populate technicians in edit mode
            if (isEdit && !dataDetail.usernUserTeknisi.isNullOrEmpty()) {
                dataDetail.usernUserTeknisi.forEach { tech ->
                    if (tech != null) {
                        selectedTechniciansArrayList.add(
                            SupervisorTechnician(
                                idUser = tech.idUser,
                                namaUser = tech.namaUser,
                                namaDept = tech.deptUser
                            )
                        )
                    }
                }
            }

            selectedTechniciansArrayList.add(
                SupervisorTechnician(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "+",
                    null
                )
            )
            techniciansRvAdapter = SelectedSupervisorTechniciansRvAdapter(
                context,
                dataDetail,
                selectedTechniciansArrayList,
                false
            ) { selectedSubDept }.also {
                with(it) {
                    setOnTechniciansSetListener(object :
                        SelectedSupervisorTechniciansRvAdapter.OnTechniciansSetListener {
                        override fun onTechniciansSelected(
                            data: SupervisorTechnician
                        ) {
                            selectedTechniciansArrayList.remove(
                                SupervisorTechnician(
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "+",
                                    null
                                )
                            )
                            selectedTechniciansArrayList.add(data)
                            selectedTechniciansArrayList.add(
                                SupervisorTechnician(
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "+",
                                    null
                                )
                            )
                            techniciansRvAdapter.notifyDataSetChanged()
                            refreshDeployTechButtonVisibility()
                        }

                        override fun onTechniciansUnselected(
                            data: SupervisorTechnician
                        ) {
                            selectedTechniciansArrayList.remove(
                                SupervisorTechnician(
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "+",
                                    null
                                )
                            )
                            selectedTechniciansArrayList.remove(data)
                            selectedTechniciansArrayList.add(
                                SupervisorTechnician(
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "+",
                                    null
                                )
                            )
                            techniciansRvAdapter.notifyDataSetChanged()
                            refreshDeployTechButtonVisibility()
                        }
                    })
                }
            }
            rvTechnicians.adapter = techniciansRvAdapter
            rvTechnicians.layoutManager = FlexboxLayoutManager(
                context, FlexDirection.ROW, FlexWrap.WRAP
            )
            techniciansRvAdapter.notifyDataSetChanged()
            refreshDeployTechButtonVisibility()
        }
    }

    private fun formCheck(): Boolean {
        var validated = 0

        binding.apply {
            if (editCategoryComplexity) {
                return selectedCategoryId != 0 || this@UpdateStatusBottomSheet::complexity.isInitialized
            }

            // In Edit Technicians mode, only check if technicians are selected
            if (deployTech && isEdit) {
                // Filter out the "+" placeholder and check if there are any real technicians selected
                val actualTechnicians = selectedTechniciansArrayList.filter { it.namaUser != "+" }
                return actualTechnicians.isNotEmpty()
            }

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
            if (editCategoryComplexity) {
                if (formCheck()) {
                    val confirmationDialog = ConfirmationDialog(
                        context,
                        if (context.getString(R.string.lang) == "in")
                            "Apakah Anda yakin ingin mengubah kategori dan kompleksitas?"
                        else
                            "Are you sure you want to update the category and complexity?",
                        if (context.getString(R.string.lang) == "in") "Ya" else "Yes"
                    ).also {
                        with(it) {
                            setConfirmationDialogListener(object :
                                ConfirmationDialog.ConfirmationDialogListener {
                                override fun onConfirm() {
                                    val loadingDialog = LoadingDialog(context)
                                    if (loadingDialog.window != null) loadingDialog.show()
                                    deployTechLoading.visibility = View.VISIBLE
                                    deployTechText.visibility = View.GONE

                                    val categoryToSend =
                                        if (selectedCategoryId != 0) selectedCategoryId else dataDetail.idKategori
                                            ?: 0
                                    val complexityToSend =
                                        if (this@UpdateStatusBottomSheet::complexity.isInitialized) complexity else dataDetail.difficulty
                                            ?: ""

                                    try {
                                        InitAPI.getEndpoint.updateCategoryComplexity(
                                            userId = userData.id,
                                            caseId = dataDetail.idGaprojects!!,
                                            category = categoryToSend,
                                            complexity = complexityToSend
                                        ).enqueue(object : Callback<GenericSimpleResponse> {
                                            override fun onResponse(
                                                call: Call<GenericSimpleResponse>,
                                                response: Response<GenericSimpleResponse>
                                            ) {
                                                loadingDialog.dismiss()
                                                deployTechLoading.visibility = View.GONE
                                                deployTechText.visibility = View.VISIBLE
                                                if (response.isSuccessful) {
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
                                                                    "Kategori dan kompleksitas berhasil diperbarui."
                                                                else
                                                                    "Category and complexity updated successfully."
                                                            ).show()
                                                        this@UpdateStatusBottomSheet.dismiss()
                                                        onUpdateSuccessListener.onCategoryComplexityUpdated()
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
                                                                    "Gagal memperbarui kategori dan kompleksitas."
                                                                else
                                                                    "Failed to update category and complexity."
                                                            ).show()
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
                                                                "Gagal memperbarui kategori dan kompleksitas."
                                                            else
                                                                "Failed to update category and complexity."
                                                        ).show()
                                                    Log.e(
                                                        "ERROR ${response.code()}",
                                                        response.message()
                                                    )
                                                }
                                            }

                                            override fun onFailure(
                                                call: Call<GenericSimpleResponse>,
                                                throwable: Throwable
                                            ) {
                                                loadingDialog.dismiss()
                                                deployTechLoading.visibility = View.GONE
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
                                                            "Terjadi kesalahan. Silakan coba lagi nanti."
                                                        else
                                                            "Something went wrong. Please try again later."
                                                    ).show()
                                                throwable.printStackTrace()
                                                Log.e(
                                                    "ERROR",
                                                    "Update Category Complexity Failure | $throwable"
                                                )
                                            }
                                        })
                                    } catch (jsonException: JSONException) {
                                        loadingDialog.dismiss()
                                        deployTechLoading.visibility = View.GONE
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
                                                    "Terjadi kesalahan. Silakan coba lagi nanti."
                                                else
                                                    "Something went wrong. Please try again later."
                                            ).show()
                                        jsonException.printStackTrace()
                                    }
                                }
                            })
                        }
                    }
                    if (confirmationDialog.window != null) confirmationDialog.show()
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
                                "Pilih minimal satu perubahan (kategori atau kompleksitas)."
                            else
                                "Please select at least one change (category or complexity)."
                        ).show()
                }
                return
            }

            approveLoading.visibility = View.VISIBLE
            rejectLoading.visibility = View.VISIBLE
            approveText.visibility = View.GONE
            rejectText.visibility = View.GONE
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
                                    put("user_id", createPartFromString(userData.id.toString())!!)
                                    put(
                                        "case_id",
                                        createPartFromString(dataDetail.idGaprojects.toString())!!
                                    )
                                    put(
                                        "description",
                                        createPartFromString(descriptionField.text.toString())!!
                                    )
                                    /*if (dataDetail.deptTujuan == userData.dept) {
                                        for (i in 0 until selectedSupervisorsArrayList.size - 1) {
                                            put(
                                                "user_supervisor[$i]",
                                                createPartFromString(
                                                    selectedSupervisorsArrayList[i].idUser.toString()
                                                )!!
                                            )
                                        }
                                    }*/
                                }
                                val targetData: MutableMap<String, RequestBody> = mutableMapOf()
                                with(targetData) {
                                    put("user_id", createPartFromString(userData.id.toString())!!)
                                    put(
                                        "case_id",
                                        createPartFromString(dataDetail.idGaprojects.toString())!!
                                    )
                                    put(
                                        "description",
                                        createPartFromString(descriptionField.text.toString())!!
                                    )
                                    put(
                                        "department",
                                        createPartFromString(selectedSubDept?.idDept.toString())!!
                                    )
                                    put(
                                        "is_vendor",
                                        createPartFromString(workByVendor)!!
                                    )
                                    put(
                                        "vendor_name",
                                        createPartFromString(vendorNameField.text.toString())!!
                                    )
                                    if (dataDetail.deptTujuan == userData.dept) {
                                        for (i in 0 until selectedSupervisorsArrayList.size - 1) {
                                            put(
                                                "supervisors[$i]",
                                                createPartFromString(
                                                    selectedSupervisorsArrayList[i].idUser.toString()
                                                )!!
                                            )
                                        }
                                    }
                                }
                                (if (isEdit) InitAPI.getEndpoint.editApprovals(targetData)
                                else if (dataDetail.deptTujuan == userData.dept)
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
                    // Show confirmation dialog for Edit/Deploy Technicians
                    val confirmationMessage =
                        if (context.getString(R.string.lang) == "in") {
                            if (isEdit)
                                "Apakah Anda yakin ingin mengubah teknisi?"
                            else
                                "Apakah Anda yakin ingin mengerahkan teknisi sekarang?"
                        } else {
                            if (isEdit)
                                "Are you sure you want to change the technicians?"
                            else
                                "Are you sure you want to assign technicians now?"
                        }
                    val confirmationDialog = ConfirmationDialog(
                        context,
                        confirmationMessage,
                        if (context.getString(R.string.lang) == "in") "Ya" else "Yes"
                    ).also {
                        with(it) {
                            setConfirmationDialogListener(object :
                                ConfirmationDialog.ConfirmationDialogListener {
                                override fun onConfirm() {
                                    val loadingDialog = LoadingDialog(context)
                                    if (loadingDialog.window != null)
                                        loadingDialog.show()

                                    deployTechText.visibility = View.GONE
                                    deployTechLoading.visibility = View.VISIBLE

                                    try {
                                        // In Edit mode, only prepare edit data; in Deploy mode, prepare deploy data
                                        val dataToSend = if (isEdit) {
                                            val editData: MutableMap<String, RequestBody> =
                                                mutableMapOf()
                                            with(editData) {
                                                put(
                                                    "case_id",
                                                    createPartFromString(dataDetail.idGaprojects.toString())!!
                                                )
                                                put(
                                                    "user_id",
                                                    createPartFromString(userData.id.toString())!!
                                                )
                                                for (i in 0 until selectedTechniciansArrayList.size - 1) {
                                                    put(
                                                        "technicians[$i]",
                                                        createPartFromString(
                                                            selectedTechniciansArrayList[i].idUser.toString()
                                                        )!!
                                                    )
                                                }
                                            }
                                            editData
                                        } else {
                                            val data: MutableMap<String, RequestBody> =
                                                mutableMapOf()
                                            with(data) {
                                                put(
                                                    "case_id",
                                                    createPartFromString(dataDetail.idGaprojects.toString())!!
                                                )
                                                put(
                                                    "user_id",
                                                    createPartFromString(userData.id.toString())!!
                                                )
                                                put(
                                                    "complexity",
                                                    createPartFromString(complexity)!!
                                                )
                                                put(
                                                    "is_vendor",
                                                    createPartFromString(workByVendor)!!
                                                )
                                                put(
                                                    "vendor_name",
                                                    createPartFromString(vendorNameField.text.toString())!!
                                                )
                                                for (i in 0 until selectedTechniciansArrayList.size - 1) {
                                                    put(
                                                        "technicians[$i]",
                                                        createPartFromString(
                                                            selectedTechniciansArrayList[i].idUser.toString()
                                                        )!!
                                                    )
                                                }
                                            }
                                            data
                                        }

                                        Log.e("Is it Edit?", isEdit.toString())
                                        (if (isEdit) InitAPI.getEndpoint.editTechnicians(dataToSend)
                                        else InitAPI.getEndpoint.deployTechnicians(dataToSend))
                                            .enqueue(object : Callback<GenericSimpleResponse> {
                                                override fun onResponse(
                                                    call: Call<GenericSimpleResponse>,
                                                    response: Response<GenericSimpleResponse>
                                                ) {
                                                    loadingDialog.dismiss()
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
                                                                        if (isEdit) {
                                                                            if (context.getString(R.string.lang) == "in")
                                                                                "Teknisi berhasil diperbarui!"
                                                                            else
                                                                                "Technicians successfully updated!"
                                                                        } else {
                                                                            if (context.getString(R.string.lang) == "in")
                                                                                "Teknisi berhasil dikerahkan!"
                                                                            else
                                                                                "Technicians successfully deployed!"
                                                                        }
                                                                    ).show()
                                                                this@UpdateStatusBottomSheet.dismiss()
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
                                                    call: Call<GenericSimpleResponse>,
                                                    throwable: Throwable
                                                ) {
                                                    loadingDialog.dismiss()
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
                                        loadingDialog.dismiss()
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
                                }
                            })
                        }
                    }

                    if (confirmationDialog.window != null)
                        confirmationDialog.show()
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

    private fun getCategoryListForEditMode() {
        binding.apply {
            categoryLoading.visibility = View.VISIBLE
            categoryDropdown.visibility = View.GONE
            try {
                InitAPI.getEndpoint.getCategoryList()
                    .enqueue(object : Callback<CategoryListResponse> {
                        override fun onResponse(
                            call: Call<CategoryListResponse>,
                            response: Response<CategoryListResponse>
                        ) {
                            categoryLoading.visibility = View.GONE
                            categoryDropdown.visibility = View.VISIBLE
                            if (response.isSuccessful && response.body() != null) {
                                val names: ArrayList<String> = ArrayList()
                                names.add(
                                    if (context.getString(R.string.lang) == "in")
                                        "Pilih Kategori"
                                    else
                                        "Select Category"
                                )
                                val categories = response.body()?.data
                                categories?.forEach { it?.categoryName?.let { name -> names.add(name) } }

                                val dropdownAdapter = ArrayAdapter(
                                    context,
                                    R.layout.general_dropdown_item,
                                    names
                                )
                                categoryDropdown.adapter = dropdownAdapter

                                if (selectedCategoryId != 0 && categories != null) {
                                    val index =
                                        categories.indexOfFirst { it?.categoryId == selectedCategoryId }
                                    if (index != -1) categoryDropdown.setSelection(index + 1)
                                }

                                categoryDropdown.onItemSelectedListener =
                                    object : AdapterView.OnItemSelectedListener {
                                        override fun onItemSelected(
                                            parent: AdapterView<*>?,
                                            view: View?,
                                            position: Int,
                                            id: Long
                                        ) {
                                            selectedCategoryId = if (position == 0) 0
                                            else categories?.get(position - 1)?.categoryId ?: 0
                                        }

                                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                                    }
                                dropdownAdapter.notifyDataSetChanged()
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
                                            "Gagal mendapatkan daftar kategori."
                                        else
                                            "Failed to get category list."
                                    ).show()
                            }
                        }

                        override fun onFailure(call: Call<CategoryListResponse>, t: Throwable) {
                            categoryLoading.visibility = View.GONE
                            categoryDropdown.visibility = View.VISIBLE
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
                                        "Terjadi kesalahan. Silakan coba lagi nanti."
                                    else
                                        "Something went wrong. Please try again later."
                                ).show()
                            t.printStackTrace()
                        }
                    })
            } catch (e: Exception) {
                categoryLoading.visibility = View.GONE
                categoryDropdown.visibility = View.VISIBLE
                e.printStackTrace()
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
                                subDeptDropdown.onItemSelectedListener =
                                    object : AdapterView.OnItemSelectedListener {
                                        override fun onItemSelected(
                                            parent: AdapterView<*>?,
                                            view: View?,
                                            position: Int,
                                            id: Long
                                        ) {
                                            if (position != 0) {
                                                val subDept =
                                                    parent?.getItemAtPosition(position).toString()
                                                selectedSubDept =
                                                    result[subDepartmentList.indexOf(subDept) - 1]
                                                Log.e(
                                                    "Selected Sub Department",
                                                    selectedSubDept.toString()
                                                )
                                            } else {
                                                selectedSubDept = null
                                            }
                                        }

                                        override fun onNothingSelected(parent: AdapterView<*>?) {
                                        }
                                    }
                                if (isEdit && dataDetail.idDeptTujuan != null) {
                                    val index =
                                        result.indexOfFirst { it.idDept == dataDetail.idDeptTujuan }
                                    if (index != -1) {
                                        subDeptDropdown.setSelection(index + 1)
                                        selectedSubDept = result[index]
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
                                    "Terjadi kesalahan. Silakan coba lagi nanti."
                                else
                                    "Something went wrong. Please try again later."
                            ).show()
                        t.printStackTrace()
                        Log.e("ERROR", "Get Sub Department List Failure | $t")
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
        fun onCategoryComplexityUpdated()
    }
}
