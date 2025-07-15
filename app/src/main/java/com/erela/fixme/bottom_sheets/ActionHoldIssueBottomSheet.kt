package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.erela.fixme.R
import com.erela.fixme.databinding.BsActionHoldIssueBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class ActionHoldIssueBottomSheet(context: Context): BottomSheetDialog(context) {
    private val binding: BsActionHoldIssueBinding by lazy {
        BsActionHoldIssueBinding.inflate(layoutInflater)
    }
    private lateinit var onButtonClickListener: OnHoldButtonClickListener

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
            holdIssueButton.isEnabled = false
            holdIssueButton.alpha = 0.5f

            descriptionField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s.toString().isNotEmpty()) {
                        descriptionFieldLayout.error = null
                        holdIssueButton.isEnabled = true
                        holdIssueButton.alpha = 1f
                    } else {
                        descriptionFieldLayout.error = if (context.getString(R.string.lang) == "in")
                            "Pastikan semua kolom terisi."
                        else
                            "Make sure all fields are filled."
                        holdIssueButton.isEnabled = false
                        holdIssueButton.alpha = 0.5f
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            holdIssueButton.setOnClickListener {
                dismiss()
                onButtonClickListener.onHoldButtonClicked(descriptionField.text.toString())
            }
        }
    }

    fun setOnButtonClickListener(onButtonClickListener: OnHoldButtonClickListener) {
        this.onButtonClickListener = onButtonClickListener
    }

    interface OnHoldButtonClickListener {
        fun onHoldButtonClicked(reason: String)
    }
}