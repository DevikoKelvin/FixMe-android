package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.adapters.recycler_view.AttachmentRvAdapter
import com.erela.fixme.databinding.BsManagePhotoBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class ManagePhotoBottomSheet(context: Context, val imageArrayUri: ArrayList<Uri>) :
    BottomSheetDialog(context) {
    private val binding: BsManagePhotoBinding by lazy {
        BsManagePhotoBinding.inflate(layoutInflater)
    }
    private lateinit var adapter: AttachmentRvAdapter
    private lateinit var onAttachmentActionListener: OnAttachmentActionListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun init() {
        binding.apply {
            adapter = AttachmentRvAdapter(context, imageArrayUri).also {
                with(it) {
                    setOnAttachmentItemActionListener(object :
                        AttachmentRvAdapter.OnAttachmentItemActionListener {
                        override fun onDeletePhoto(uri: Uri) {
                            imageArrayUri.remove(uri)
                            adapter.notifyDataSetChanged()
                            onAttachmentActionListener.onDeletePhoto(uri)
                            if (imageArrayUri.isEmpty()) {
                                dismiss()
                            }
                        }
                    })
                }
            }
            attachmentRv.adapter = adapter
            attachmentRv.layoutManager = LinearLayoutManager(context)
        }
    }

    fun setOnAttachmentActionListener(onAttachmentActionListener: OnAttachmentActionListener) {
        this.onAttachmentActionListener = onAttachmentActionListener
    }

    interface OnAttachmentActionListener {
        fun onDeletePhoto(uri: Uri)
    }
}