package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.adapters.recycler_view.DepartmentRvAdapter
import com.erela.fixme.databinding.BsDepartmentListBinding
import com.erela.fixme.objects.DepartmentListResponse
import com.google.android.material.bottomsheet.BottomSheetDialog

class DepartmentListBottomSheet(context: Context, val data: List<DepartmentListResponse>) :
    BottomSheetDialog(context), DepartmentRvAdapter.OnDepartmentClickListener {
    private val binding: BsDepartmentListBinding by lazy {
        BsDepartmentListBinding.inflate(layoutInflater)
    }
    private lateinit var onDepartmentClickListener: OnDepartmentClickListener
    private lateinit var adapter: DepartmentRvAdapter

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
            adapter = DepartmentRvAdapter(context, data)
            rvDepartmentList.adapter = adapter
            rvDepartmentList.layoutManager = LinearLayoutManager(context)
            adapter.onDepartmentClickListener(this@DepartmentListBottomSheet)
        }
    }

    fun onDepartmentClickListener(onDepartmentClickListener: OnDepartmentClickListener) {
        this.onDepartmentClickListener = onDepartmentClickListener
    }

    override fun onDepartmentClick(data: DepartmentListResponse) {
        onDepartmentClickListener.onDepartmentClick(data)
        dismiss()
    }

    interface OnDepartmentClickListener {
        fun onDepartmentClick(data: DepartmentListResponse)
    }
}