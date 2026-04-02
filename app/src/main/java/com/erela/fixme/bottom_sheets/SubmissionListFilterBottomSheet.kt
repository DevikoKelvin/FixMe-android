package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentManager
import com.erela.fixme.R
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.BsSubmissionListFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class SubmissionListFilterBottomSheet(
    context: Context,
    private val selectedFilter: Int,
    private var startDate: String,
    private var endDate: String,
    private val selectedComplexity: String,
    private val fragmentManager: FragmentManager
) :
    BottomSheetDialog(context) {
    private val binding: BsSubmissionListFilterBinding by lazy {
        BsSubmissionListFilterBinding.inflate(layoutInflater)
    }
    private lateinit var onFilterListener: OnFilterListener
    private var filterBy = -1
    private var selectFilterByStatus = -3
    private var complexity = "All"

    companion object {
        private const val ALL_DONE = -2
        private const val ALL_ON_GOING = -1
        const val ALL = 100
        private const val REJECTED = 0
        const val PENDING = 1
        const val WAITING = 11
        const val APPROVED = 2
        const val HOLD = 22
        const val ON_PROGRESS = 3
        const val PROGRESS_DONE = 30
        const val ON_TRIAL = 31
        private const val DONE = 4
        private const val CANCELED = 5
        private var arrayOfStatusOnGoing = arrayOf(
            "All",
            "Pending",
            "Waiting",
            "Approved",
            "Hold",
            "On Progress",
            "Progress Done",
            "On Trial"
        )
        private var arrayOfStatusDone = arrayOf(
            "All", "Rejected", "Done", "Canceled"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    private fun init() {
        binding.apply {
            // Initialize selectFilterByStatus with the current selectedFilter
            selectFilterByStatus = selectedFilter

            filterBy = when (selectedFilter) {
                ALL_ON_GOING, PENDING, WAITING, APPROVED, HOLD, ON_PROGRESS, PROGRESS_DONE, ON_TRIAL -> 0
                ALL_DONE, REJECTED, DONE, CANCELED -> 1
                else -> -1
            }
            val filterList = ArrayList<String>()
            filterList.add(
                if (context.getString(R.string.lang) == "in")
                    "Semua Kasus"
                else
                    "All Case"
            )
            filterList.add(
                if (context.getString(R.string.lang) == "in")
                    "Kasus Berjalan"
                else
                    "On Going Case"
            )
            filterList.add(
                if (context.getString(R.string.lang) == "in")
                    "Kasus Selesai"
                else
                    "Finished Case"
            )
            val filterAdapter = ArrayAdapter(
                context,
                R.layout.general_dropdown_item,
                filterList
            )
            filterByDropdown.adapter = filterAdapter
            when (filterBy) {
                0 -> {
                    filterByDropdown.setSelection(1)
                    statusDropdownLayout.visibility = View.VISIBLE
                    initStatusList(0, selectedFilter)
                }

                1 -> {
                    filterByDropdown.setSelection(2)
                    statusDropdownLayout.visibility = View.VISIBLE
                    initStatusList(1, selectedFilter)
                }

                else -> {
                    filterByDropdown.setSelection(0)
                }
            }
            filterByDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    if (position != 0) {
                        filterBy = if (position == 1)
                            0
                        else
                            1
                        statusDropdownLayout.visibility = View.VISIBLE
                        doneButton.visibility = View.VISIBLE
                        initStatusList(filterBy, selectedFilter)
                    } else {
                        filterBy = -1
                        statusDropdownLayout.visibility = View.GONE
                        selectFilterByStatus = ALL
                        doneButton.visibility = View.VISIBLE
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("id-ID"))
            val serverDateFormat =
                SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag("id-ID")).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            val startCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                if (startDate.isNotEmpty()) {
                    time = serverDateFormat.parse(startDate)!!
                }
            }

            // Show placeholder if startDate is empty, otherwise show formatted date
            dateFromText.text = if (startDate.isNotEmpty())
                dateFormat.format(startCalendar.time)
            else
                context.getString(R.string.date_placeholder)

            dateFrom.setOnClickListener {
                val datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(
                        if (context.getString(R.string.lang) == "in")
                            "Pilih Tanggal Dimulai"
                        else
                            "Select Start Date"
                    )
                    .setSelection(startCalendar.timeInMillis)
                    .build()
                datePicker.addOnPositiveButtonClickListener { selection ->
                    startCalendar.apply {
                        timeInMillis = selection
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    dateFromText.text = dateFormat.format(startCalendar.time)
                    startDate = serverDateFormat.format(startCalendar.time)
                }
                datePicker.show(fragmentManager, "START")
            }

            val nowCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                if (endDate.isNotEmpty()) {
                    time = serverDateFormat.parse(endDate)!!
                }
            }

            // Show placeholder if endDate is empty, otherwise show formatted date
            dateToText.text = if (endDate.isNotEmpty())
                dateFormat.format(nowCalendar.time)
            else
                context.getString(R.string.date_placeholder)

            dateTo.setOnClickListener {
                // Set minimum selectable date to startDate if it exists
                val datePickerBuilder = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(
                        if (context.getString(R.string.lang) == "in")
                            "Pilih Tanggal Akhir"
                        else
                            "Select End Date"
                    )

                // If startDate is selected, set it as minimum date
                if (startDate.isNotEmpty()) {
                    datePickerBuilder.setSelection(maxOf(nowCalendar.timeInMillis, startCalendar.timeInMillis))
                } else {
                    datePickerBuilder.setSelection(nowCalendar.timeInMillis)
                }

                val datePicker = datePickerBuilder.build()
                datePicker.addOnPositiveButtonClickListener { selection ->
                    // Validate that endDate is not before startDate
                    if (startDate.isNotEmpty()) {
                        val selectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = selection
                        }

                        if (selectedCalendar.before(startCalendar)) {
                            // Show error toast
                            CustomToast.getInstance(context)
                                .setMessage(
                                    if (context.getString(R.string.lang) == "in")
                                        "Tanggal akhir tidak boleh kurang dari tanggal awal"
                                    else
                                        "End date cannot be earlier than start date"
                                )
                                .setFontColor(context.getColor(R.color.custom_toast_font_failed))
                                .setBackgroundColor(context.getColor(R.color.custom_toast_background_failed))
                                .show()
                            return@addOnPositiveButtonClickListener
                        }
                    }

                    nowCalendar.apply {
                        timeInMillis = selection
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    dateToText.text = dateFormat.format(nowCalendar.time)
                    endDate = serverDateFormat.format(nowCalendar.time)
                }
                datePicker.show(fragmentManager, "END")
            }

            clearDateFilterButton.setOnClickListener {
                dateFromText.text = context.getString(R.string.date_placeholder)
                dateToText.text = context.getString(R.string.date_placeholder)
                startDate = ""
                endDate = ""
            }

            when (selectedComplexity) {
                "All" -> allSelector.isChecked = true
                "Low" -> lowSelector.isChecked = true
                "Middle" -> midSelector.isChecked = true
                "High" -> highSelector.isChecked = true
            }
            if (allSelector.isChecked) {
                complexity = "All"
            } else if (lowSelector.isChecked) {
                complexity = "Low"
            } else if (midSelector.isChecked) {
                complexity = "Middle"
            } else if (highSelector.isChecked) {
                complexity = "High"
            }
            complexityRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.allSelector -> complexity = "All"
                    R.id.lowSelector -> complexity = "Low"
                    R.id.midSelector -> complexity = "Middle"
                    R.id.highSelector -> complexity = "High"
                }
            }

            doneButton.setOnClickListener {
                onFilterListener.onFilter(
                    selectFilterByStatus,
                    selectFilterByStatus,
                    startDate,
                    endDate,
                    complexity
                )
                dismiss()
            }
        }
    }

    private fun initStatusList(filterBy: Int, status: Int) {
        binding.apply {
            val statusList = ArrayList<String>()
            if (filterBy == 0) {
                for (i in arrayOfStatusOnGoing.indices) {
                    statusList.add(arrayOfStatusOnGoing[i])
                }
            } else {
                for (i in arrayOfStatusDone.indices) {
                    statusList.add(arrayOfStatusDone[i])
                }
            }
            val statusAdapter = ArrayAdapter(
                context,
                R.layout.general_dropdown_item,
                statusList
            )
            statusDropdown.adapter = statusAdapter
            val statusText: String = when (status) {
                -2 -> "All"
                -1 -> "All"
                0 -> "Rejected"
                1 -> "Pending"
                11 -> "Waiting"
                2 -> "Approved"
                22 -> "Hold"
                3 -> "On Progress"
                30 -> "Progress Done"
                31 -> "On Trial"
                4 -> "Done"
                5 -> "Canceled"
                else -> ""
            }
            statusDropdown.setSelection(statusList.indexOf(statusText))
            statusDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    if (filterBy == 1) {
                        when (position) {
                            0 -> selectFilterByStatus = ALL_DONE
                            1 -> selectFilterByStatus = REJECTED
                            2 -> selectFilterByStatus = DONE
                            3 -> selectFilterByStatus = CANCELED
                        }
                    } else {
                        when (position) {
                            0 -> selectFilterByStatus = ALL_ON_GOING
                            1 -> selectFilterByStatus = PENDING
                            2 -> selectFilterByStatus = WAITING
                            3 -> selectFilterByStatus = APPROVED
                            4 -> selectFilterByStatus = HOLD
                            5 -> selectFilterByStatus = ON_PROGRESS
                            6 -> selectFilterByStatus = PROGRESS_DONE
                            7 -> selectFilterByStatus = ON_TRIAL
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    fun setOnFilterListener(onFilterListener: OnFilterListener) {
        this.onFilterListener = onFilterListener
    }

    interface OnFilterListener {
        fun onFilter(
            filter: Int,
            selectedFilter: Int,
            startDate: String,
            endDate: String,
            selectedComplexity: String
        )
    }
}