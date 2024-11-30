package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import com.erela.fixme.databinding.BsSubmissionListFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class SubmissionListFilterBottomSheet(context: Context, private val selectedFilter: Int) :
    BottomSheetDialog(context) {
    private lateinit var binding: BsSubmissionListFilterBinding
    private lateinit var onFilterListener: OnFilterListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BsSubmissionListFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    private fun init() {
        binding.apply {
            when (selectedFilter) {
                -1 -> {
                    pendingSelector.isChecked = false
                    rejectedSelector.isChecked = false
                    approvedSelector.isChecked = false
                    onProgressSelector.isChecked = false
                    onTrialSelector.isChecked = false
                }

                1 -> pendingSelector.isChecked = true
                0 -> rejectedSelector.isChecked = true
                2 -> approvedSelector.isChecked = true
                3 -> onProgressSelector.isChecked = true
                31 -> onTrialSelector.isChecked = true
                4 -> doneSelector.isChecked = true
            }

            clearFilterButton.setOnClickListener {
                onFilterListener.onFilter(-1, -1)
                dismiss()
            }

            pendingSelector.setOnClickListener {
                onFilterListener.onFilter(1, 1)
                dismiss()
            }

            rejectedSelector.setOnClickListener {
                onFilterListener.onFilter(0, 0)
                dismiss()
            }

            approvedSelector.setOnClickListener {
                onFilterListener.onFilter(2, 2)
                dismiss()
            }

            onProgressSelector.setOnClickListener {
                onFilterListener.onFilter(3, 3)
                dismiss()
            }

            onTrialSelector.setOnClickListener {
                onFilterListener.onFilter(31, 31)
                dismiss()
            }

            doneSelector.setOnClickListener {
                onFilterListener.onFilter(4, 4)
                dismiss()
            }
        }
    }

    fun setOnFilterListener(onFilterListener: OnFilterListener) {
        this.onFilterListener = onFilterListener
    }

    interface OnFilterListener {
        fun onFilter(filter: Int, selectedFilter: Int)
    }
}