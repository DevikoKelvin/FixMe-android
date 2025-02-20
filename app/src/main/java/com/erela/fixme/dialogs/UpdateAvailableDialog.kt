package com.erela.fixme.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.erela.fixme.databinding.DialogUpdateAvailableBinding

class UpdateAvailableDialog(
    context: Context, private val updateUrl: String
) : Dialog(context) {
    private val binding: DialogUpdateAvailableBinding by lazy {
        DialogUpdateAvailableBinding.inflate(layoutInflater)
    }
    private lateinit var onDownloadListener: OnDownloadListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(false)

        init()
    }

    private fun init() {
        binding.apply {
            cancelButton.setOnClickListener {
                dismiss()
            }

            confirmButton.setOnClickListener {
                onDownloadListener.onDownload(updateUrl)
                dismiss()
            }
        }
    }

    fun setOnDownloadListener(onDownloadListener: OnDownloadListener) {
        this.onDownloadListener = onDownloadListener
    }

    interface OnDownloadListener {
        fun onDownload(url: String)
    }
}