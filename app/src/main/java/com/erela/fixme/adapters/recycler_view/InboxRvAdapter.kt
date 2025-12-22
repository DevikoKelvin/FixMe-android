package com.erela.fixme.adapters.recycler_view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.databinding.ListItemInboxBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.InboxResponse
import com.erela.fixme.objects.UserData

class InboxRvAdapter(val context: Context, val data: ArrayList<InboxResponse>) :
    RecyclerView.Adapter<InboxRvAdapter.ViewHolder>() {
    private lateinit var userData: UserData
    private lateinit var onItemClickListener: OnNotificationItemClickListener

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): ViewHolder = ViewHolder(
        ListItemInboxBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ).root
    )

    override fun onBindViewHolder(
        holder: ViewHolder, position: Int
    ) {
        val item = data[position]
        userData = UserDataHelper(context).getUserData()

        with(holder) {
            binding.apply {
                usernameText.text = userData.name
                notificationContent.text = item.actions
                sentTime.text = item.tglWaktu
                readTime.text = item.tglWaktuBaca
            }

            itemView.setOnClickListener {
                if (::onItemClickListener.isInitialized) {
                    onItemClickListener.onItemClick(item)
                }
            }
        }
    }

    override fun getItemCount(): Int = data.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemInboxBinding.bind(view)
    }

    fun setOnItemClickListener(listener: OnNotificationItemClickListener) {
        onItemClickListener = listener
    }

    interface OnNotificationItemClickListener {
        fun onItemClick(item: InboxResponse)
    }
}