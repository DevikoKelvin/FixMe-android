package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
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

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    private fun init() {
        binding.apply {
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