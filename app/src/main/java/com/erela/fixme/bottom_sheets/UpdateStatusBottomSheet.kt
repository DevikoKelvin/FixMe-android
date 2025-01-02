package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.SelectedSupervisorTechniciansRvAdapter
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.BsUpdateStatusBinding
import com.erela.fixme.dialogs.ConfirmationDialog
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.networking.InitAPI
import com.erela.fixme.objects.GenericSimpleResponse
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.SupervisorTechnicianListResponse
import com.erela.fixme.objects.UserData
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
    private var selectedSupervisorsArrayList: ArrayList<SupervisorTechnicianListResponse> = ArrayList()
    private var selectedTechniciansArrayList: ArrayList<SupervisorTechnicianListResponse> = ArrayList()
    private lateinit var supervisorsRvAdapter: SelectedSupervisorTechniciansRvAdapter
    private lateinit var techniciansRvAdapter: SelectedSupervisorTechniciansRvAdapter
    private var isFormEmpty = arrayOf(
        false,
        false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun init() {
        binding.apply {
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
                        descriptionFieldLayout.error = "Make sure all fields are filled."
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            actionsButtonContainer.visibility = View.VISIBLE
            if (!deployTech) {
                if (cancel) {
                    selectSupervisorText.visibility = View.GONE
                    rvSupervisor.visibility = View.GONE
                    selectTechniciansText.visibility = View.GONE
                    rvTechnicians.visibility = View.GONE
                    approveButton.visibility = View.GONE
                    rejectButton.visibility = View.GONE
                    cancelButton.visibility = View.VISIBLE
                    deployTechButton.visibility = View.GONE
                    cancelButton.setOnClickListener {
                        executeUpdate()
                    }
                } else {
                    if (approve) {
                        selectSupervisorText.visibility = View.VISIBLE
                        rvSupervisor.visibility = View.VISIBLE
                        selectTechniciansText.visibility = View.GONE
                        rvTechnicians.visibility = View.GONE
                        approveButton.visibility = View.VISIBLE
                        rejectButton.visibility = View.GONE
                        cancelButton.visibility = View.GONE
                        deployTechButton.visibility = View.GONE
                        descriptionField.setText("Approved!")
                        isFormEmpty[0] = true
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
                                setOnSupervisorSetListener(object : SelectedSupervisorTechniciansRvAdapter.OnSupervisorSetListener {
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
                        selectSupervisorText.visibility = View.GONE
                        rvSupervisor.visibility = View.GONE
                        selectTechniciansText.visibility = View.GONE
                        rvTechnicians.visibility = View.GONE
                        approveButton.visibility = View.GONE
                        rejectButton.visibility = View.VISIBLE
                        cancelButton.visibility = View.GONE
                        deployTechButton.visibility = View.GONE
                        descriptionField.setText("Rejected!")
                        isFormEmpty[0] = true
                        rejectButton.setOnClickListener {
                            executeUpdate()
                        }
                    }
                }
            } else {
                descriptionFieldLayout.visibility = View.GONE
                selectSupervisorText.visibility = View.GONE
                rvSupervisor.visibility = View.GONE
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
                            override fun onTechniciansSelected(data: SupervisorTechnicianListResponse) {
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

                            override fun onTechniciansUnselected(data: SupervisorTechnicianListResponse) {
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
            for (element in isFormEmpty) {
                if (element)
                    validated++
            }

            return if (!deployTech) {
                if (cancel)
                    validated == 1
                else {
                    if (approve)
                        validated == isFormEmpty.size
                    else
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
            if (!deployTech) {
                if (formCheck()) {
                    if (cancel) {
                        Log.e("Canceled", "Canceled")
                        val confirmationDialog = ConfirmationDialog(
                            context,
                            "Are you sure you want to cancel this issue?",
                            "Yes"
                        ).also {
                            with(it) {
                                setConfirmationDialogListener(object :
                                    ConfirmationDialog.ConfirmationDialogListener {
                                    override fun onConfirm() {
                                        cancelLoading.visibility = View.VISIBLE
                                        try {
                                            InitAPI.getAPI.cancelSubmission(
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
                                                                        "Submission canceled successfully."
                                                                    ).show()
                                                                this@UpdateStatusBottomSheet.dismiss()
                                                                onUpdateSuccessListener.onCanceled()
                                                            } else {
                                                                CustomToast.getInstance(context)
                                                                    .setMessage("Can't cancel this issue. Please try again later.")
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
                                                                .setMessage("Can't cancel this issue. Please try again later.")
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
                                                            .setMessage("Can't cancel this issue. Please try again later.")
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
                                                    CustomToast.getInstance(context)
                                                        .setMessage("Something went wrong. Please try again later.")
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
                                            CustomToast
                                                .getInstance(context)
                                                .setMessage("Something went wrong. Please try again later.")
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
                                    for (i in 0 until selectedSupervisorsArrayList.size - 1) {
                                        put(
                                            "user_supervisor[$i]",
                                            createPartFromString(
                                                selectedSupervisorsArrayList[i].idUser.toString()
                                            )!!
                                        )
                                    }
                                }
                                InitAPI.getAPI.approveSubmission(data)
                                    .enqueue(object : Callback<GenericSimpleResponse> {
                                        override fun onResponse(
                                            call: Call<GenericSimpleResponse>,
                                            response: Response<GenericSimpleResponse>
                                        ) {
                                            approveLoading.visibility = View.GONE
                                            rejectLoading.visibility = View.GONE
                                            deployTechLoading.visibility = View.GONE
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
                                                            .setMessage("Failed to approve submission.")
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
                                                        .setMessage("Failed to approve submission.")
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
                                                    .setMessage("Failed to approve submission.")
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
                                                .setMessage("Failed to approve submission.").show()
                                            Log.e("ERROR", throwable.message.toString())
                                            throwable.printStackTrace()
                                        }
                                    })
                            } catch (jsonException: JSONException) {
                                approveLoading.visibility = View.GONE
                                rejectLoading.visibility = View.GONE
                                deployTechLoading.visibility = View.GONE
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
                                    .setMessage("Failed to approve submission.").show()
                                jsonException.printStackTrace()
                            }
                        } else {
                            try {
                                InitAPI.getAPI.rejectSubmission(
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
                                                        .setMessage("Failed to reject submission.")
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
                                                    .setMessage("Failed to reject submission.")
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
                                                .setMessage("Failed to reject submission.").show()
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
                                            .setMessage("Failed to reject submission.").show()
                                        Log.e("ERROR", throwable.message.toString())
                                        throwable.printStackTrace()
                                    }
                                })
                            } catch (jsonException: JSONException) {
                                approveLoading.visibility = View.GONE
                                rejectLoading.visibility = View.GONE
                                deployTechLoading.visibility = View.GONE
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
                                    .setMessage("Failed to reject submission.").show()
                                jsonException.printStackTrace()
                            }
                        }
                    }
                } else {
                    approveLoading.visibility = View.GONE
                    rejectLoading.visibility = View.GONE
                    deployTechLoading.visibility = View.GONE
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
                        .setMessage("Make sure all fields are filled.").show()
                if (descriptionField.text.toString().isEmpty())
                    descriptionFieldLayout.error = "Make sure all fields are filled."
                }
            } else {
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
                    }
                    InitAPI.getAPI.deployTechnicians(data)
                        .enqueue(object : Callback<GenericSimpleResponse> {
                            override fun onResponse(
                                call: Call<GenericSimpleResponse>,
                                response: Response<GenericSimpleResponse>
                            ) {
                                approveLoading.visibility = View.GONE
                                rejectLoading.visibility = View.GONE
                                deployTechLoading.visibility = View.GONE
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
                                                .setMessage("Failed to deploy technicians.")
                                                .show()
                                            Log.e(
                                                "ERROR ${result?.code}", result?.message.toString()
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
                                            .setMessage("Failed to deploy technicians.")
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
                                        .setMessage("Failed to deploy technicians.").show()
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
                                    .setMessage("Failed to deploy technicians.").show()
                                Log.e("ERROR", throwable.message.toString())
                                throwable.printStackTrace()
                            }
                        })
                } catch (jsonException: JSONException) {
                    approveLoading.visibility = View.GONE
                    rejectLoading.visibility = View.GONE
                    deployTechLoading.visibility = View.GONE
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
                        .setMessage("Failed to deploy technicians.").show()
                    jsonException.printStackTrace()
                }
            }
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