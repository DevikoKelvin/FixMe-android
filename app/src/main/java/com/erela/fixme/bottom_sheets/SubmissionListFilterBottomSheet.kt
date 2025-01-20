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

    companion object {
        const val ALL = -1
        const val REJECTED = 0
        const val PENDING = 1
        const val WAITING = 11
        const val APPROVED = 2
        const val HOLD = 22
        const val ON_PROGRESS = 3
        const val PROGRESS_DONE = 30
        const val ON_TRIAL = 31
        const val DONE = 4
        const val CANCELED = 5
    }

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
                    canceledSelector.isChecked = false
                    rejectedSelector.isChecked = false
                    approvedSelector.isChecked = false
                    onProgressSelector.isChecked = false
                    onTrialSelector.isChecked = false
                }

                0 -> rejectedSelector.isChecked = true
                1 -> pendingSelector.isChecked = true
                11 -> waitingSelector.isChecked = true
                2 -> approvedSelector.isChecked = true
                22 -> holdSelector.isChecked = true
                3 -> onProgressSelector.isChecked = true
                30 -> progressDoneSelector.isChecked = true
                31 -> onTrialSelector.isChecked = true
                4 -> doneSelector.isChecked = true
                5 -> canceledSelector.isChecked = true
            }

            clearFilterButton.setOnClickListener {
                onFilterListener.onFilter(ALL, ALL)
                dismiss()
            }

            pendingSelector.setOnClickListener {
                onFilterListener.onFilter(PENDING, PENDING)
                dismiss()
            }

            waitingSelector.setOnClickListener {
                onFilterListener.onFilter(WAITING, WAITING)
                dismiss()
            }

            canceledSelector.setOnClickListener {
                onFilterListener.onFilter(CANCELED, CANCELED)
                dismiss()
            }

            rejectedSelector.setOnClickListener {
                onFilterListener.onFilter(REJECTED, REJECTED)
                dismiss()
            }

            approvedSelector.setOnClickListener {
                onFilterListener.onFilter(APPROVED, APPROVED)
                dismiss()
            }

            holdSelector.setOnClickListener {
                onFilterListener.onFilter(HOLD, HOLD)
                dismiss()
            }

            onProgressSelector.setOnClickListener {
                onFilterListener.onFilter(ON_PROGRESS, ON_PROGRESS)
                dismiss()
            }

            progressDoneSelector.setOnClickListener {
                onFilterListener.onFilter(PROGRESS_DONE, PROGRESS_DONE)
                dismiss()
            }

            onTrialSelector.setOnClickListener {
                onFilterListener.onFilter(ON_TRIAL, ON_TRIAL)
                dismiss()
            }

            doneSelector.setOnClickListener {
                onFilterListener.onFilter(DONE, DONE)
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