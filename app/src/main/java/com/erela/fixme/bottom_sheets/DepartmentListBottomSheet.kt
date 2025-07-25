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
import com.erela.fixme.databinding.BsDepartmentListBinding
import com.erela.fixme.objects.DepartmentListResponse
import com.google.android.material.bottomsheet.BottomSheetDialog

class DepartmentListBottomSheet(
    context: Context, val data: List<DepartmentListResponse>,
    private var selectedDept: DepartmentListResponse?
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

        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    private fun init() {
        binding.apply {
            val departmentList = ArrayList<String>()
            departmentList.add(
                if (context.getString(R.string.lang) == "in")
                    "Pilih Departemen"
                else
                    "Select Department"
            )
            for (element in data) {
                departmentList.add(
                    element.namaDept.toString()
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
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
        }
    }

    private fun initSubDepartmentList(currentSelectedSubDept: String) {
        binding.apply {
            val subDepartmentList = ArrayList<String>()
            subDepartmentList.add(
                if (context.getString(R.string.lang) == "in")
                    "Pilih Sub Departemen"
                else
                    "Select Sub Department"
            )
            for (i in data.indices) {
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
                        for (i in data.indices) {
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