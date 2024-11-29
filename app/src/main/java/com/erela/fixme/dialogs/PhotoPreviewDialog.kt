package com.erela.fixme.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import com.bumptech.glide.Glide
import com.erela.fixme.databinding.DialogPhotoPreviewBinding
import com.erela.fixme.helpers.InitAPI

class PhotoPreviewDialog(
    context: Context, private val imageUri: Uri?, private val photoName: String?
) : Dialog(context) {
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
            if (photoName == null) {
                photoView.setImageURI(imageUri)
            } else {
                Glide.with(context)
                    .load(InitAPI.IMAGE_URL + photoName)
                    .into(photoView)
            }
        }
    }
}