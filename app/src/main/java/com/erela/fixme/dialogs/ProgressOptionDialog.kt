package com.erela.fixme.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.graphics.drawable.toDrawable
import com.erela.fixme.databinding.DialogProgressOptionBinding
import com.erela.fixme.objects.ProgressItem

class ProgressOptionDialog(
    context: Context, private val progress: ProgressItem, private val forSpv: Boolean
) : Dialog(context) {
    private val binding: DialogProgressOptionBinding by lazy {
        DialogProgressOptionBinding.inflate(layoutInflater)
    }
    private lateinit var onProgressOptionDialogListener: OnProgressOptionDialogListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        setCancelable(true)

        init()
    }

    private fun init() {
        binding.apply {
            if (forSpv) {
                divider3.visibility = View.GONE
                deleteButton.visibility = View.GONE
                doneButton.visibility = View.GONE
                editButton.visibility = View.GONE
                editMaterialsButton.visibility = View.VISIBLE
                editMaterialsButton.setOnClickListener {
                    onProgressOptionDialogListener.onMaterialEdited(progress)
                    dismiss()
                }
                approveMaterialButton.visibility = View.VISIBLE
                approveMaterialButton.setOnClickListener {
                    onProgressOptionDialogListener.onMaterialApproved(progress)
                    dismiss()
                }
            } else {
                divider3.visibility = View.VISIBLE
                deleteButton.visibility = View.VISIBLE
                doneButton.visibility = View.VISIBLE
                editButton.visibility = View.VISIBLE
                editMaterialsButton.visibility = View.GONE
                approveMaterialButton.visibility = View.GONE
            }
            deleteButton.setOnClickListener {
                onProgressOptionDialogListener.onProgressDeleted(progress)
                dismiss()
            }
            if (progress.stsDetail == 0) {
                if (progress.material!!.isNotEmpty()) {
                    if (progress.approveMaterialStatus == 0) {
                        doneButton.isEnabled = false
                        doneButton.alpha = 0.5f
                        editButton.isEnabled = true
                        editButton.setOnClickListener {
                            onProgressOptionDialogListener.onProgressEdited(progress)
                            dismiss()
                        }
                    } else if (progress.approveMaterialStatus == 1) {
                        doneButton.isEnabled = true
                        doneButton.setOnClickListener {
                            onProgressOptionDialogListener.onProgressSetDone(progress)
                            dismiss()
                        }
                        editButton.isEnabled = false
                        editButton.alpha = 0.5f
                        deleteButton.isEnabled = false
                        deleteButton.alpha = 0.5f
                    }
                } else {
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
        fun onMaterialEdited(data: ProgressItem)
        fun onMaterialApproved(data: ProgressItem)
    }
}