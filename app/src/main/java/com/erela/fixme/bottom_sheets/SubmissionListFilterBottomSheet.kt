package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.graphics.drawable.toDrawable
import com.erela.fixme.R
import com.erela.fixme.databinding.BsSubmissionListFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class SubmissionListFilterBottomSheet(
    context: Context, private val selectedFilter: Int, private val selectedComplexity: String
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
        fun onFilter(filter: Int, selectedFilter: Int, selectedComplexity: String)
    }
}