package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.adapters.recycler_view.OldAttachmentRvAdapter
import com.erela.fixme.databinding.BsManagePhotoBinding
import com.erela.fixme.objects.FotoGaprojectsItem
import com.google.android.material.bottomsheet.BottomSheetDialog

class ManageOldPhotoBottomSheet(
    context: Context, val imageArrayUri: ArrayList<FotoGaprojectsItem>
) : BottomSheetDialog(context) {
    private val binding: BsManagePhotoBinding by lazy {
        BsManagePhotoBinding.inflate(layoutInflater)
    }
    private lateinit var adapter: OldAttachmentRvAdapter
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
            adapter = OldAttachmentRvAdapter(context, imageArrayUri).also {
                with(it) {
                    onAttachmentItemActionListener(object :
                        OldAttachmentRvAdapter.OnAttachmentItemActionListener {
                        override fun onDeleteOldPhoto(fotoGaProjectsItem: FotoGaprojectsItem) {
                            imageArrayUri.remove(fotoGaProjectsItem)
                            adapter.notifyDataSetChanged()
                            onAttachmentActionListener.onDeleteOldPhoto(fotoGaProjectsItem)
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
        fun onDeleteOldPhoto(fotoGaProjectsItem: FotoGaprojectsItem)
    }
}