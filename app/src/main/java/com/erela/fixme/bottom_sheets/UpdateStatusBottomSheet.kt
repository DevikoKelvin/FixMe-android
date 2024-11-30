package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.erela.fixme.helpers.networking.InitAPI
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UpdateStatusResponse
import com.erela.fixme.objects.UserData
import com.erela.fixme.objects.UserDetailResponse
import com.erela.fixme.objects.UserListResponse
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class UpdateStatusBottomSheet(
    context: Context, private val dataDetail: SubmissionDetailResponse, private val approve: Boolean
) :
    BottomSheetDialog(context) {
    private lateinit var binding: BsUpdateStatusBinding
    private lateinit var userDetail: UserDetailResponse
    private lateinit var onUpdateSuccessListener: OnUpdateSuccessListener
    private lateinit var userData: UserData
    private var targetUserId = 0
    private var calendar = Calendar.getInstance()
    private var isFormEmpty = arrayOf(
        false,
        false,
        false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BsUpdateStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userData = UserDataHelper(context).getUserData()

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
            approveRejectButtonContainer.visibility = View.VISIBLE
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            startWorkDateText.text = dateFormat.format(calendar.time)
            startWorkTimeText.text = timeFormat.format(calendar.time)
            isFormEmpty[1] = true
            if (approve) {
                approveButton.visibility = View.VISIBLE
                rejectButton.visibility = View.GONE
                descriptionField.setText("Approved!")
                isFormEmpty[0] = true
                startWorkDateButton.setOnClickListener {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selectedDate = Calendar.getInstance()
                            selectedDate.set(year, month, dayOfMonth)
                            val formattedDate = dateFormat.format(selectedDate.time)
                            startWorkDateText.text = formattedDate
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
                startWorkTimeButton.setOnClickListener {
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val selectedTime = Calendar.getInstance()
                            selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            selectedTime.set(Calendar.MINUTE, minute)
                            val formattedTime = timeFormat.format(selectedTime.time)
                            startWorkTimeText.text = formattedTime
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
                    ).show()
                }
                try {
                    InitAPI.getAPI.getUserList().enqueue(object : Callback<List<UserListResponse>> {
                        override fun onResponse(
                            call: Call<List<UserListResponse>?>,
                            response: Response<List<UserListResponse>?>
                        ) {
                            if (response.isSuccessful) {
                                if (response.body() != null) {
                                    val data: ArrayList<String> = ArrayList()
                                    data.add("Select User")
                                    for (i in 0 until response.body()!!.size) {
                                        data.add(
                                            "${response.body()!![i].usern} (ID: ${response.body()!![i].idUser}, Starconnect ID: ${response.body()!![i].idUserStarconnect})"
                                        )
                                    }
                                    val dropdownAdapter = ArrayAdapter(
                                        context,
                                        R.layout.general_dropdown_item,
                                        R.id.dropdownItemText,
                                        data
                                    )
                                    userTargetDropdown.adapter = dropdownAdapter
                                    userTargetDropdown.onItemSelectedListener =
                                        object : AdapterView.OnItemSelectedListener {
                                            override fun onItemSelected(
                                                parent: AdapterView<*>?,
                                                view: View?, position: Int,
                                                id: Long
                                            ) {
                                                targetUserId =
                                                    if (position == 0) 0 else response.body()!![position - 1].idUser!!.toInt()
                                                Log.e("USER ID", "$targetUserId")
                                                if (targetUserId == 0) {
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
                                                    "Selected User is valid?", "${isFormEmpty[2]}"
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
                                    .setMessage("Can't retrieve user list. Please try again later.")
                                    .show()
                                Log.e("ERROR", response.message())
                                dismiss()
                            }
                        }

                        override fun onFailure(
                            call: Call<List<UserListResponse>?>,
                            throwable: Throwable
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
                                .setMessage("Can't retrieve user list. Please try again later.")
                                .show()
                            Log.e("ERROR", throwable.toString())
                            throwable.printStackTrace()
                            dismiss()
                        }
                    })
                } catch (exception: JSONException) {
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
                    exception.printStackTrace()
                }
                approveButton.setOnClickListener {
                    executeUpdate(
                        2, descriptionField.text.toString(), startWorkDateText.text.toString(),
                        startWorkTimeText.text.toString(), targetUserId
                    )
                }
            } else {
                approveButton.visibility = View.GONE
                rejectButton.visibility = View.VISIBLE
                descriptionField.setText("Rejected!")
                isFormEmpty[0] = true
                val data: ArrayList<String> = ArrayList()
                try {
                    InitAPI.getAPI.getUserDetail(dataDetail.idUser!!.toInt())
                        .enqueue(object : Callback<UserDetailResponse> {
                            override fun onResponse(
                                call: Call<UserDetailResponse?>,
                                response: Response<UserDetailResponse?>
                            ) {
                                if (response.isSuccessful) {
                                    if (response.body() != null) {
                                        userDetail = UserDetailResponse(
                                            response.body()!!.stsAktif,
                                            response.body()!!.nama,
                                            response.body()!!.usern,
                                            response.body()!!.idDept,
                                            response.body()!!.hakAkses,
                                            response.body()!!.idUser,
                                            response.body()!!.idUserStarconnect
                                        )
                                        data.add(
                                            "${userDetail.usern} (ID: ${userDetail.idUser}, Starconnect ID: ${userDetail.idUserStarconnect})"
                                        )
                                        val dropdownAdapter = ArrayAdapter(
                                            context,
                                            R.layout.general_dropdown_item,
                                            R.id.dropdownItemText,
                                            data
                                        )
                                        userTargetDropdown.adapter = dropdownAdapter
                                        userTargetDropdown.onItemSelectedListener =
                                            object : AdapterView.OnItemSelectedListener {
                                                override fun onItemSelected(
                                                    parent: AdapterView<*>?,
                                                    view: View?, position: Int,
                                                    id: Long
                                                ) {
                                                    targetUserId = userDetail.idUser!!.toInt()
                                                    isFormEmpty[2] = true
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
                                        ).show()
                                    Log.e("ERROR", response.message())
                                    dismiss()
                                }
                            }

                            override fun onFailure(
                                call: Call<UserDetailResponse?>,
                                throwable: Throwable
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
                                    .setMessage("Can't retrieve user list. Please try again later.")
                                    .show()
                                Log.e("ERROR", throwable.toString())
                                throwable.printStackTrace()
                                dismiss()
                            }
                        })
                } catch (exception: Exception) {
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
                    exception.printStackTrace()
                }
            }
            rejectButton.setOnClickListener {
                executeUpdate(
                    0, descriptionField.text.toString(), startWorkDateText.text.toString(),
                    startWorkTimeText.text.toString(), targetUserId
                )
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
        }

        return validated == isFormEmpty.size
    }

    private fun executeUpdate(
        status: Int, description: String, workingDate: String, workingTime: String,
        targetUserId: Int
    ) {
        binding.apply {
            when (status) {
                0 -> rejectLoading.visibility = View.VISIBLE
                2 -> approveLoading.visibility = View.VISIBLE
            }
            if (formCheck()) {
                try {
                    InitAPI.getAPI.updateSubmissionStatus(
                        userData.id,
                        dataDetail.idGaprojects!!.toInt(),
                        status,
                        description,
                        targetUserId,
                        workingDate,
                        workingTime
                    ).enqueue(object : Callback<UpdateStatusResponse> {
                        override fun onResponse(
                            call: Call<UpdateStatusResponse?>,
                            response: Response<UpdateStatusResponse?>
                        ) {
                            when (status) {
                                0 -> rejectLoading.visibility = View.GONE
                                2 -> approveLoading.visibility = View.GONE
                            }
                            if (response.isSuccessful) {
                                if (response.body() != null) {
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
                                        .setMessage("Status updated!").show()
                                    dismiss()
                                    onUpdateSuccessListener.onUpdateSuccess()
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
                                    .setMessage("Failed to update status").show()
                            }
                        }

                        override fun onFailure(
                            call: Call<UpdateStatusResponse?>,
                            throwable: Throwable
                        ) {
                            when (status) {
                                0 -> rejectLoading.visibility = View.GONE
                                2 -> approveLoading.visibility = View.GONE
                            }
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
                                .setMessage("Something went wrong, please try again later.").show()
                            Log.e("ERROR", throwable.toString())
                            throwable.printStackTrace()
                        }
                    })
                } catch (exception: Exception) {
                    when (status) {
                        0 -> rejectLoading.visibility = View.GONE
                        2 -> approveLoading.visibility = View.GONE
                    }
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
                        .setMessage("Something went wrong, please try again later.").show()
                    Log.e("ERROR", exception.toString())
                    exception.printStackTrace()
                }
            } else {
                when (status) {
                    0 -> rejectLoading.visibility = View.GONE
                    2 -> approveLoading.visibility = View.GONE
                }
                CustomToast.getInstance(context)
                    .setMessage("Please make sure all fields in the form are filled in.")
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
                    ).show()
            }
        }
    }

    fun setOnUpdateSuccessListener(listener: OnUpdateSuccessListener) {
        this.onUpdateSuccessListener = listener
    }

    interface OnUpdateSuccessListener {
        fun onUpdateSuccess()
    }
}