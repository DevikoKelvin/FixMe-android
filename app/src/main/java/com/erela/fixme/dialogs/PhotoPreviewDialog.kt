package com.erela.fixme.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.erela.fixme.databinding.DialogPhotoPreviewBinding
import com.erela.fixme.helpers.api.InitAPI

class PhotoPreviewDialog(
    context: Context, private val imageUri: Uri?, private val photoName: String?
) : Dialog(context) {
    private val binding: DialogPhotoPreviewBinding by lazy {
        DialogPhotoPreviewBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
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