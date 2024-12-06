package com.erela.fixme.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.erela.fixme.R
import com.erela.fixme.databinding.DialogProgressOptionBinding
import com.erela.fixme.objects.ProgressItem

class ProgressOptionDialog(context: Context, private val progress: ProgressItem) : Dialog(context) {
    private val binding: DialogProgressOptionBinding by lazy {
        DialogProgressOptionBinding.inflate(layoutInflater)
    }
    private lateinit var onProgressOptionDialogListener: OnProgressOptionDialogListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(true)

        init()
    }

    private fun init() {
        binding.apply {
            deleteButton.setOnClickListener {
                onProgressOptionDialogListener.onProgressDeleted(progress)
                dismiss()
            }
            if (progress.stsDetail == 0) {
                doneButton.isEnabled = true
                doneButton.setOnClickListener {
                    onProgressOptionDialogListener.onProgressSetDone(progress)
                    dismiss()
                }
                editButton.isEnabled = true
                editButton.setOnClickListener {
                    onProgressOptionDialogListener.onProgressEdited(progress)
                    dismiss()
                }
            } else {
                doneButton.isEnabled = false
                doneButton.alpha = 0.5f
                editButton.isEnabled = false
                editButton.alpha = 0.5f
            }
        }
    }

    fun setOnProgressOptionDialogListener(
        onProgressOptionDialogListener: OnProgressOptionDialogListener
    ) {
        this.onProgressOptionDialogListener = onProgressOptionDialogListener
    }

    interface OnProgressOptionDialogListener {
        fun onProgressDeleted(data: ProgressItem)
        fun onProgressEdited(data: ProgressItem)
        fun onProgressSetDone(data: ProgressItem)
    }
}