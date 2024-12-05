package com.erela.fixme.adapters.recycler_view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.databinding.ListItemAttachmentBinding
import com.erela.fixme.dialogs.PhotoPreviewDialog
import com.erela.fixme.objects.FotoItem

class OldAttachmentProgressRvAdapter(
    private val context: Context, private val imageArray: MutableList<FotoItem>
): RecyclerView.Adapter<OldAttachmentProgressRvAdapter.ViewHolder>() {
    private lateinit var onAttachmentItemActionListener: OnAttachmentItemActionListener

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val binding = ListItemAttachmentBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ListItemAttachmentBinding.inflate(
            LayoutInflater.from(context), parent, false
        ).root
    )

    override fun getItemCount(): Int = imageArray.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = imageArray[position]

        with(holder) {
            binding.apply {
                fileName.text = item.foto

                seePhotoButton.setOnClickListener {
                    val photoPreviewDialog = PhotoPreviewDialog(context, null, item.foto)

                    if (photoPreviewDialog.window != null)
                        photoPreviewDialog.show()
                }
                deletePhotoButton.setOnClickListener {
                    imageArray.removeAt(position)
                    notifyItemRemoved(position)
                    onAttachmentItemActionListener.onDeleteOldPhoto(item)
                }
            }
        }
    }

    fun onAttachmentItemActionListener(
        onAttachmentItemActionListener: OnAttachmentItemActionListener
    ) {
        this.onAttachmentItemActionListener = onAttachmentItemActionListener
    }

    interface OnAttachmentItemActionListener {
        fun onDeleteOldPhoto(photo: FotoItem)
    }
}