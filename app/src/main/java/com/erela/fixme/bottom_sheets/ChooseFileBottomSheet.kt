package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.erela.fixme.R
import com.erela.fixme.databinding.BsChooseFileBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class ChooseFileBottomSheet(context: Context): BottomSheetDialog(context) {
    private val binding: BsChooseFileBinding by lazy {
        BsChooseFileBinding.inflate(layoutInflater)
    }
    private lateinit var onChooseFileListener: OnChooseFileListener

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
            cameraText.text = if (context.getString(R.string.lang) == "in") {
                "Kamera"
            } else {
                "Camera"
            }
            galleryText.text = if (context.getString(R.string.lang) == "in") {
                "Galeri"
            } else {
                "Gallery"
            }
            openCameraButton.setOnClickListener {
                onChooseFileListener.onOpenCameraClicked()
            }

            openGalleryButton.setOnClickListener {
                onChooseFileListener.onOpenGalleryClicked()
            }
        }
    }

    fun setOnChooseFileListener(onChooseFileListener: OnChooseFileListener) {
        this.onChooseFileListener = onChooseFileListener
    }

    interface OnChooseFileListener {
        fun onOpenCameraClicked()
        fun onOpenGalleryClicked()
    }
}