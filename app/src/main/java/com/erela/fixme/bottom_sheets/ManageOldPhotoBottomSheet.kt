package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.adapters.recycler_view.OldAttachmentProgressRvAdapter
import com.erela.fixme.adapters.recycler_view.OldAttachmentSubmissionRvAdapter
import com.erela.fixme.databinding.BsManagePhotoBinding
import com.erela.fixme.objects.FotoGaprojectsItem
import com.erela.fixme.objects.FotoItem
import com.google.android.material.bottomsheet.BottomSheetDialog

class ManageOldPhotoBottomSheet(context: Context) : BottomSheetDialog(context) {
    private val binding: BsManagePhotoBinding by lazy {
        BsManagePhotoBinding.inflate(layoutInflater)
    }
    private var imageArrayUri: ArrayList<FotoGaprojectsItem>? = null
    private var imageArrayProgress: MutableList<FotoItem>? = null
    private lateinit var submissionAdapter: OldAttachmentSubmissionRvAdapter
    private lateinit var progressAdapter: OldAttachmentProgressRvAdapter
    private lateinit var onSubmissionAttachmentActionListener: OnSubmissionAttachmentActionListener
    private lateinit var onProgressAttachmentActionListener: OnProgressAttachmentActionListener

    constructor(context: Context, imageArrayUri: ArrayList<FotoGaprojectsItem>) : this(context) {
        this.imageArrayUri = imageArrayUri
    }

    constructor(context: Context, imageArrayProgress: MutableList<FotoItem>) : this(context) {
        this.imageArrayProgress = imageArrayProgress
    }

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
            if (imageArrayUri != null) {
                submissionAdapter = OldAttachmentSubmissionRvAdapter(context, imageArrayUri).also {
                    with(it) {
                        onAttachmentItemActionListener(object :
                            OldAttachmentSubmissionRvAdapter.OnAttachmentItemActionListener {
                            override fun onDeleteOldPhoto(fotoGaProjectsItem: FotoGaprojectsItem) {
                                imageArrayUri!!.remove(fotoGaProjectsItem)
                                submissionAdapter.notifyDataSetChanged()
                                onSubmissionAttachmentActionListener.onDeleteOldPhoto(
                                    fotoGaProjectsItem
                                )
                                if (imageArrayUri!!.isEmpty()) {
                                    dismiss()
                                }
                            }
                        })
                    }
                }
                attachmentRv.adapter = submissionAdapter
                attachmentRv.layoutManager = LinearLayoutManager(context)
            } else {
                progressAdapter =
                    OldAttachmentProgressRvAdapter(context, imageArrayProgress!!).also {
                        with(it) {
                            onAttachmentItemActionListener(object :
                                OldAttachmentProgressRvAdapter.OnAttachmentItemActionListener {
                                override fun onDeleteOldPhoto(photo: FotoItem) {
                                    imageArrayProgress!!.remove(photo)
                                    progressAdapter.notifyDataSetChanged()
                                    onProgressAttachmentActionListener.onDeleteOldPhoto(
                                        photo
                                    )
                                    if (imageArrayProgress!!.isEmpty()) {
                                        dismiss()
                                    }
                                }
                            })
                        }
                    }
                attachmentRv.adapter = progressAdapter
                attachmentRv.layoutManager = LinearLayoutManager(context)
            }
        }
    }

    fun setOnAttachmentActionListener(
        onSubmissionAttachmentActionListener: OnSubmissionAttachmentActionListener
    ) {
        this.onSubmissionAttachmentActionListener = onSubmissionAttachmentActionListener
    }

    fun setOnProgressAttachmentActionListener(
        onProgressAttachmentActionListener: OnProgressAttachmentActionListener
    ) {
        this.onProgressAttachmentActionListener = onProgressAttachmentActionListener
    }

    interface OnSubmissionAttachmentActionListener {
        fun onDeleteOldPhoto(fotoGaProjectsItem: FotoGaprojectsItem)
    }

    interface OnProgressAttachmentActionListener {
        fun onDeleteOldPhoto(photo: FotoItem)
    }
}