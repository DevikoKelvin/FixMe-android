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
                selectDepartmentButton.visibility = View.VISIBLE
                selectDepartmentButton.setOnClickListener {
                    onDepartmentClickListener.onDepartmentClick(
                        selectedDept!!
                    )
                }
            }
            departmentDropdown.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?, view: View?,
                        position: Int, id: Long
                    ) {
                        if (position != 0) {
                            selectedDepartment = departmentList.distinct()[position]
                            selectDepartmentButton.visibility = View.VISIBLE
                            selectDepartmentButton.setOnClickListener {
                                onDepartmentClickListener.onDepartmentClick(
                                    data[departmentList.distinct().indexOf(selectedDepartment)-1]
                                )
                            }
                        } else {
                            selectedDepartment = ""
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