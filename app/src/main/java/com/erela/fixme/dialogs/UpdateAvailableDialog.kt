package com.erela.fixme.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.core.net.toUri
import com.erela.fixme.databinding.DialogUpdateAvailableBinding

class UpdateAvailableDialog(context: Context, private val updateUrl: String) : Dialog(context) {
    private val binding: DialogUpdateAvailableBinding by lazy {
        DialogUpdateAvailableBinding.inflate(layoutInflater)
    }

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
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW, updateUrl.toUri()
                    )
                )
                dismiss()
            }
        }
    }
}