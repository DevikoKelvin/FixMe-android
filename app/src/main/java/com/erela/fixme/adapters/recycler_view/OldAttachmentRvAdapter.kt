package com.erela.fixme.adapters.recycler_view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.databinding.ListItemAttachmentBinding
import com.erela.fixme.dialogs.PhotoPreviewDialog
import com.erela.fixme.objects.FotoGaprojectsItem

class OldAttachmentRvAdapter(
    private val context: Context, private val oldData: ArrayList<FotoGaprojectsItem>?
) : RecyclerView.Adapter<OldAttachmentRvAdapter.ViewHolder>() {
    private lateinit var onAttachmentItemActionListener: OnAttachmentItemActionListener

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemAttachmentBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ListItemAttachmentBinding.inflate(
            LayoutInflater.from(context), parent, false
        ).root
    )

    override fun getItemCount(): Int = oldData!!.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = oldData!![position]

        with(holder) {
            binding.apply {
                fileName.text = item.foto
                seePhotoButton.setOnClickListener {
                    val photoPreviewDialog = PhotoPreviewDialog(context, null, item.foto)

                    if (photoPreviewDialog.window != null)
                        photoPreviewDialog.show()
                }
                deletePhotoButton.setOnClickListener {
                    oldData.removeAt(position)
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
        fun onDeleteOldPhoto(fotoGaProjectsItem: FotoGaprojectsItem)
    }
}