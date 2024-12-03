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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.erela.fixme.R
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.BsUpdateStatusBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.networking.InitAPI
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.SupervisorListResponse
import com.erela.fixme.objects.UpdateStatusResponse
import com.erela.fixme.objects.UserData
import com.google.android.material.bottomsheet.BottomSheetDialog
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
    private val approve: Boolean, private val deployTech: Boolean
) : BottomSheetDialog(context) {
    private val binding: BsUpdateStatusBinding by lazy {
        BsUpdateStatusBinding.inflate(layoutInflater)
    }
    private val userData: UserData by lazy {
        UserDataHelper(context).getUserData()
    }
    private lateinit var onUpdateSuccessListener: OnUpdateSuccessListener
    private var idSupervisor = 0
    private var techniciansList: ArrayList<Int> = ArrayList()
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

    @SuppressLint("SetTextI18n")
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
                    isFormEmpty[0] = s.toString().isEmpty()
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            actionsButtonContainer.visibility = View.VISIBLE
            if (!deployTech) {
                if (approve) {
                    selectSupervisorText.visibility = View.VISIBLE
                    supervisorDropdownLayout.visibility = View.VISIBLE
                    selectTechniciansText.visibility = View.GONE
                    rvTechnicians.visibility = View.GONE
                    approveButton.visibility = View.VISIBLE
                    rejectButton.visibility = View.GONE
                    deployTechButton.visibility = View.GONE
                    descriptionField.setText("Approved!")
                    isFormEmpty[0] = true
                    try {
                        InitAPI.getAPI.getSupervisorList(dataDetail.idGaprojects!!).enqueue(
                            object : Callback<List<SupervisorListResponse>> {
                                override fun onResponse(
                                    call: Call<List<SupervisorListResponse>>,
                                    response: Response<List<SupervisorListResponse>>
                                ) {
                                    if (response.isSuccessful) {
                                        if (response.body() != null) {
                                            val data: ArrayList<String> = ArrayList()
                                            data.add("Select User")
                                            for (i in 0 until response.body()!!.size) {
                                                data.add(
                                                    "Starconnect ID: ${response.body()!![i].idUserStarconnect}\n${
                                                        response.body()!![i].namaUser?.uppercase(
                                                            Locale.getDefault()
                                                        )
                                                    }"
                                                )
                                            }
                                            val dropdownAdapter = ArrayAdapter(
                                                context,
                                                R.layout.general_dropdown_item,
                                                R.id.dropdownItemText,
                                                data
                                            )
                                            supervisorDropdown.adapter = dropdownAdapter
                                            supervisorDropdown.onItemSelectedListener =
                                                object : AdapterView.OnItemSelectedListener {
                                                    override fun onItemSelected(
                                                        parent: AdapterView<*>?, view: View?,
                                                        position: Int, id: Long
                                                    ) {
                                                        idSupervisor =
                                                            if (position == 0) 0 else response.body()!![position - 1].idUser!!.toInt()
                                                        Log.e("USER ID", "$idSupervisor")
                                                        if (idSupervisor == 0) {
                                                            Log.e(
                                                                "Selected User", "Invalid"
                                                            )
                                                            isFormEmpty[2] = false
                                                        } else {
                                                            Log.e(
                                                                "Selected User", "Valid"
                                                            )
                                                            isFormEmpty[2] = true
                                                        }
                                                        Log.e(
                                                            "Selected User is valid?",
                                                            "${isFormEmpty[2]}"
                                                        )
                                                    }

                                                    override fun onNothingSelected(
                                                        parent: AdapterView<*>?
                                                    ) {
                                                    }
                                                }
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
                                                "Can't retrieve user list. Please try again later."
                                            )
                                            .show()
                                        Log.e("ERROR", response.message())
                                        dismiss()
                                    }
                                }

                                override fun onFailure(
                                    call: Call<List<SupervisorListResponse>>, throwable: Throwable
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
                                            "Can't retrieve user list. Please try again later."
                                        )
                                        .show()
                                    Log.e("ERROR", throwable.toString())
                                    throwable.printStackTrace()
                                    dismiss()
                                }
                            }
                        )
                    } catch (jsonException: JSONException) {
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
                            .setMessage("Can't retrieve user list. Please try again later.").show()
                        jsonException.printStackTrace()
                        dismiss()
                    }
                    approveButton.setOnClickListener {
                        executeUpdate()
                    }
                } else {
                    selectSupervisorText.visibility = View.GONE
                    supervisorDropdownLayout.visibility = View.GONE
                    selectTechniciansText.visibility = View.GONE
                    rvTechnicians.visibility = View.GONE
                    approveButton.visibility = View.GONE
                    rejectButton.visibility = View.VISIBLE
                    deployTechButton.visibility = View.VISIBLE
                    descriptionField.setText("Rejected!")
                    isFormEmpty[0] = true
                    rejectButton.setOnClickListener {
                        executeUpdate()
                    }
                }
            } else {
                selectSupervisorText.visibility = View.GONE
                supervisorDropdownLayout.visibility = View.GONE
                selectTechniciansText.visibility = View.VISIBLE
                rvTechnicians.visibility = View.VISIBLE
                approveButton.visibility = View.GONE
                rejectButton.visibility = View.GONE
                deployTechButton.visibility = View.GONE
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
                if (approve)
                    validated == isFormEmpty.size
                else
                    validated == 1
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
                    if (approve) {
                        try {
                            val data: MutableMap<String, RequestBody> = mutableMapOf()
                            with(data) {
                                put("id_user", createPartFromString(userData.id.toString())!!)
                                put(
                                    "id_gaprojects",
                                    createPartFromString(dataDetail.idGaprojects.toString())!!
                                )
                                put("keterangan", createPartFromString(descriptionField.text.toString())!!)
                                put(
                                    "supervisor[]", createPartFromString(
                                        idSupervisor.toString()
                                    )!!
                                )
                            }
                            InitAPI.getAPI.approveSubmission(data)
                                .enqueue(object : Callback<UpdateStatusResponse> {
                                    override fun onResponse(
                                        call: Call<UpdateStatusResponse>,
                                        response: Response<UpdateStatusResponse>
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
                                                            "Submission approved successfully."
                                                        ).show()
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
                                                .setMessage("Failed to approve submission.").show()
                                            Log.e(
                                                "ERROR ${response.code()}",
                                                response.message().toString()
                                            )
                                        }
                                    }

                                    override fun onFailure(
                                        call: Call<UpdateStatusResponse>, throwable: Throwable
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
                                userData.id, dataDetail.idGaprojects!!, descriptionField.text.toString()
                            ).enqueue(object : Callback<UpdateStatusResponse> {
                                override fun onResponse(
                                    call: Call<UpdateStatusResponse>,
                                    response: Response<UpdateStatusResponse>
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
                                                        "Submission rejected successfully."
                                                    ).show()
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
                                    call: Call<UpdateStatusResponse>, throwable: Throwable
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
                try {
                    val data: MutableMap<String, RequestBody> = mutableMapOf()
                    with(data) {
                        put("id_user", createPartFromString(userData.id.toString())!!)
                        put(
                            "id_gaprojects",
                            createPartFromString(dataDetail.idGaprojects.toString())!!
                        )
                        put("user_teknisi[]", createPartFromString(techniciansList.toString())!!)
                    }
                    InitAPI.getAPI.deployTechnicians(data)
                        .enqueue(object : Callback<UpdateStatusResponse> {
                            override fun onResponse(
                                call: Call<UpdateStatusResponse>,
                                response: Response<UpdateStatusResponse>
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
                                                    "Technicians successfully deployed!"
                                                ).show()
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
                                call: Call<UpdateStatusResponse>, throwable: Throwable
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
        fun onTechniciansDeployed()
    }
}