package com.erela.fixme.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import com.erela.fixme.databinding.DialogPhotoPreviewBinding

class PhotoPreviewDialog(context: Context, val imageUri: Uri) : Dialog(context) {
    private lateinit var binding: DialogPhotoPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogPhotoPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(false)

        init()
    }

    private fun init() {
        binding.apply {
            closeButton.setOnClickListener {
                dismiss()
            }
            photoView.setImageURI(imageUri)
        }
    }
}