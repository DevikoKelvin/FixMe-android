package com.erela.fixme.adapters.recycler_view

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.databinding.ListItemAttachmentBinding
import com.erela.fixme.dialogs.PhotoPreviewDialog

class AttachmentRvAdapter(private val context: Context, val data: ArrayList<Uri>) :
    RecyclerView.Adapter<AttachmentRvAdapter.ViewHolder>() {
    private lateinit var onAttachmentItemActionListener: OnAttachmentItemActionListener

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): ViewHolder = ViewHolder(
        ListItemAttachmentBinding.inflate(
            LayoutInflater.from(context), parent, false
        ).root
    )

    override fun onBindViewHolder(
        holder: ViewHolder, position: Int
    ) {
        val item = data[position]

        with(holder) {
            binding.apply {
                fileName.text = getName(item)
                seePhotoButton.setOnClickListener {
                    val photoPreviewDialog = PhotoPreviewDialog(context, item)

                    if (photoPreviewDialog.window != null)
                        photoPreviewDialog.show()
                }
                deletePhotoButton.setOnClickListener {
                    data.removeAt(position)
                    notifyItemRemoved(position)
                    onAttachmentItemActionListener.onDeletePhoto(item)
                }
            }
        }
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemAttachmentBinding.bind(view)
    }

    private fun getName(uri: Uri): String? {
        val returnCursor = context.contentResolver.query(uri, null, null, null, null)
        val nameIndex = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor?.moveToFirst()
        val fileName = returnCursor?.getString(nameIndex!!)
        returnCursor?.close()
        return fileName
    }

    fun setOnAttachmentItemActionListener(
        onAttachmentItemActionListener: OnAttachmentItemActionListener
    ) {
        this.onAttachmentItemActionListener = onAttachmentItemActionListener
    }

    interface OnAttachmentItemActionListener {
        fun onDeletePhoto(uri: Uri)
    }
}