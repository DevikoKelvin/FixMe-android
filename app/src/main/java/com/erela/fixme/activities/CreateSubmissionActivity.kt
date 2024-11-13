package com.erela.fixme.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.erela.fixme.R
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityCreateSubmissionBinding
import com.erela.fixme.helpers.InitAPI
import com.erela.fixme.helpers.PermissionHelper
import com.erela.fixme.objects.DepartmentListResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateSubmissionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateSubmissionBinding
    private var selectedDepartment: String = ""
    val mArrayUri = ArrayList<Uri>()
    private var calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateSubmissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
    }

    private fun init() {
        binding.apply {
            backButton.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            try {
                InitAPI.getAPI.getDepartmentList()
                    .enqueue(object : Callback<List<DepartmentListResponse>> {
                        override fun onResponse(
                            call: Call<List<DepartmentListResponse>>,
                            response: Response<List<DepartmentListResponse>>
                        ) {
                            if (response.isSuccessful) {
                                if (response.body() != null) {
                                    val data: ArrayList<String> = ArrayList()
                                    data.add("Select Department")
                                    for (i in 0 until response.body()!!.size) {
                                        data.add(
                                            response.body()!![i].namaDept.toString()
                                        )
                                    }
                                    val dropdownAdapter = ArrayAdapter(
                                        this@CreateSubmissionActivity,
                                        R.layout.department_dropdown_item,
                                        R.id.dropdownItemText,
                                        data
                                    )
                                    departmentDropdown.adapter = dropdownAdapter
                                    departmentDropdown.onItemSelectedListener =
                                        object : AdapterView.OnItemSelectedListener {
                                            override fun onItemSelected(
                                                parent: AdapterView<*>?,
                                                view: View?,
                                                position: Int,
                                                id: Long
                                            ) {
                                                selectedDepartment = if (position == 0)
                                                    ""
                                                else
                                                    data[position]
                                            }

                                            override fun onNothingSelected(p0: AdapterView<*>?) {}
                                        }
                                    Log.e("Department List", response.body().toString())
                                }
                            } else {
                                Log.e("ERROR", response.message())
                            }
                        }

                        override fun onFailure(
                            call: Call<List<DepartmentListResponse>>,
                            throwable: Throwable
                        ) {
                            throwable.printStackTrace()
                        }
                    })
            } catch (exception: Exception) {
                exception.printStackTrace()
            }

            reportDateButton.setOnClickListener {
                DatePickerDialog(
                    this@CreateSubmissionActivity,
                    { _, year, month, dayOfMonth ->
                        val selectedDate = Calendar.getInstance()
                        selectedDate.set(year, month, dayOfMonth)
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val formattedDate = dateFormat.format(selectedDate.time)
                        reportDateText.text = formattedDate
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            reportTimeButton.setOnClickListener {
                TimePickerDialog(
                    this@CreateSubmissionActivity,
                    object : TimePickerDialog.OnTimeSetListener {
                        override fun onTimeSet(
                            view: TimePicker?, hourOfDay: Int,
                            minute: Int
                        ) {
                            val selectedTime = Calendar.getInstance()
                            selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            selectedTime.set(Calendar.MINUTE, minute)
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val formattedTime = timeFormat.format(selectedTime.time)
                            reportTimeText.text = formattedTime
                        }
                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
                ).show()
            }

            actualDateButton.setOnClickListener {
                DatePickerDialog(
                    this@CreateSubmissionActivity,
                    { _, year, month, dayOfMonth ->
                        val selectedDate = Calendar.getInstance()
                        selectedDate.set(year, month, dayOfMonth)
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val formattedDate = dateFormat.format(selectedDate.time)
                        actualDateText.text = formattedDate
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            actualTimeButton.setOnClickListener {
                TimePickerDialog(
                    this@CreateSubmissionActivity,
                    object : TimePickerDialog.OnTimeSetListener {
                        override fun onTimeSet(
                            view: TimePicker?, hourOfDay: Int,
                            minute: Int
                        ) {
                            val selectedTime = Calendar.getInstance()
                            selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            selectedTime.set(Calendar.MINUTE, minute)
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val formattedTime = timeFormat.format(selectedTime.time)
                            actualTimeText.text = formattedTime
                        }
                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
                ).show()
            }

            chooseFileButton.setOnClickListener {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    if (PermissionHelper.isPermissionGranted(
                            this@CreateSubmissionActivity,
                            PermissionHelper.READ_MEDIA_VISUAL_USER_SELECTED
                        )
                    ) {
                        PermissionHelper.requestPermission(
                            this@CreateSubmissionActivity,
                            arrayOf(PermissionHelper.READ_MEDIA_VISUAL_USER_SELECTED)
                        )
                    } else {
                        openGallery()
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (PermissionHelper.isPermissionGranted(
                            this@CreateSubmissionActivity,
                            PermissionHelper.READ_MEDIA_IMAGES
                        ) || PermissionHelper.isPermissionGranted(
                            this@CreateSubmissionActivity,
                            PermissionHelper.READ_MEDIA_VIDEO
                        )
                    ) {
                        PermissionHelper.requestPermission(
                            this@CreateSubmissionActivity, arrayOf(
                                PermissionHelper.READ_MEDIA_IMAGES,
                                PermissionHelper.READ_MEDIA_VIDEO
                            )
                        )
                    } else {
                        openGallery()
                    }
                } else {
                    if (PermissionHelper.isPermissionGranted(
                            this@CreateSubmissionActivity,
                            PermissionHelper.READ_EXTERNAL_STORAGE
                        )
                    ) {
                        PermissionHelper.requestPermission(
                            this@CreateSubmissionActivity,
                            arrayOf(PermissionHelper.READ_EXTERNAL_STORAGE)
                        )
                    } else {
                        openGallery()
                    }
                }
            }

            submitButton.setOnClickListener {
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionHelper.REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                if (grantResults[0] == PERMISSION_GRANTED) {
                    openGallery()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PermissionHelper.REQUEST_CODE_OPEN_GALLERY && resultCode == RESULT_OK && data != null) {
            if (data.clipData != null) {
                val mClipData: ClipData = data.clipData!!
                val count = mClipData.itemCount
                for (i in 0 until count) {
                    val imageUrl: Uri = mClipData.getItemAt(i).uri
                    mArrayUri.add(imageUrl)
                }
                Log.e("Image Data", mArrayUri.toString())
            } else {
                val imageUrl: Uri? = data.data
                if (imageUrl != null) {
                    mArrayUri.add(imageUrl)
                }
                Log.e("Image Data", mArrayUri.toString())
            }
        }/*else {
            CustomToast.getInstance(applicationContext)
                .setFontColor(
                    ContextCompat.getColor(applicationContext, R.color.custom_toast_font_failed)
                )
                .setBackgroundColor(
                    ContextCompat.getColor(
                        applicationContext, R.color.custom_toast_background_failed
                    )
                )
                .setMessage("You haven't picked Image")
                .show()
        }*/
    }

    private fun openGallery() {
        startActivityForResult(
            Intent.createChooser(
                Intent().also {
                    it.type = "image/*"
                    it.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    it.action = Intent.ACTION_GET_CONTENT
                }, "Select Picture"
            ), PermissionHelper.REQUEST_CODE_OPEN_GALLERY
        )
    }
}