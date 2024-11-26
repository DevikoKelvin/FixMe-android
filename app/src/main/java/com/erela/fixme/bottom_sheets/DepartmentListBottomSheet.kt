package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.erela.fixme.R
import com.erela.fixme.databinding.BsDepartmentListBinding
import com.erela.fixme.objects.DepartmentListResponse
import com.google.android.material.bottomsheet.BottomSheetDialog

class DepartmentListBottomSheet(
    context: Context, val data: List<DepartmentListResponse>,
    var selectedDept: DepartmentListResponse?
) :
    BottomSheetDialog(context) {
    private val binding: BsDepartmentListBinding by lazy {
        BsDepartmentListBinding.inflate(layoutInflater)
    }
    private lateinit var onDepartmentClickListener: OnDepartmentClickListener
    private var selectedDepartment: String = ""
    private var currentSelectedSubDepartment: String = ""
    private lateinit var selectedSubDepartment: DepartmentListResponse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    private fun init() {
        binding.apply {
            val departmentList = ArrayList<String>()
            departmentList.add("Select Department")
            for (i in 0 until data.size) {
                departmentList.add(
                    data[i].namaDept.toString()
                )
            }
            val departmentAdapter = ArrayAdapter(
                context,
                R.layout.general_dropdown_item,
                departmentList.distinct()
            )

            departmentDropdown.adapter = departmentAdapter
            if (selectedDept != null) {
                selectedDepartment = selectedDept!!.namaDept.toString()
                departmentDropdown.setSelection(
                    departmentList.distinct().indexOf(selectedDepartment)
                )
                currentSelectedSubDepartment = selectedDept!!.subDept.toString()
                subDeptartmentDropdownLayout.visibility = View.VISIBLE
                initSubDepartmentList(currentSelectedSubDepartment)
            }
            departmentDropdown.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?, view: View?,
                        position: Int, id: Long
                    ) {
                        if (position != 0) {
                            selectedDepartment = departmentList.distinct()[position]
                            subDeptartmentDropdownLayout.visibility = View.VISIBLE
                            initSubDepartmentList(currentSelectedSubDepartment)
                        } else {
                            selectedDepartment = ""
                            subDeptartmentDropdownLayout.visibility = View.GONE
                            selectDepartmentButton.visibility = View.GONE
                        }
                        Log.e("Selected Department", selectedDepartment)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
        }
    }

    private fun initSubDepartmentList(currentSelectedSubDept: String) {
        binding.apply {
            val subDepartmentList = ArrayList<String>()
            subDepartmentList.add("Select Sub Department")
            for (i in 0 until data.size) {
                if (data[i].namaDept == selectedDepartment) {
                    subDepartmentList.add(
                        data[i].subDept.toString()
                    )
                }
            }
            val subDepartmentAdapter = ArrayAdapter(
                context,
                R.layout.general_dropdown_item,
                subDepartmentList
            )

            subDeptartmentDropdown.adapter = subDepartmentAdapter
            if (currentSelectedSubDept != "") {
                selectedSubDepartment = selectedDept!!
                subDeptartmentDropdown.setSelection(
                    subDepartmentList.indexOf(currentSelectedSubDept)
                )
            }
            subDeptartmentDropdown.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?, view: View?,
                        position: Int, id: Long
                    ) {
                        for (i in 0 until data.size) {
                            if (data[i].subDept == subDepartmentList[position]) {
                                selectedSubDepartment = data[i]
                            }
                        }
                        if (position != 0) {
                            selectDepartmentButton.visibility = View.VISIBLE
                            selectDepartmentButton.setOnClickListener {
                                onDepartmentClickListener.onDepartmentClick(
                                    selectedSubDepartment
                                )
                            }
                        } else {
                            selectDepartmentButton.visibility = View.GONE
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
        }
    }

    fun onDepartmentClickListener(onDepartmentClickListener: OnDepartmentClickListener) {
        this.onDepartmentClickListener = onDepartmentClickListener
    }

    interface OnDepartmentClickListener {
        fun onDepartmentClick(data: DepartmentListResponse)
    }
}