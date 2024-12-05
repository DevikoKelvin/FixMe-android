package com.erela.fixme.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
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
            editButton.setOnClickListener {
                onProgressOptionDialogListener.onProgressEdited(progress)
                dismiss()
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
    }
}