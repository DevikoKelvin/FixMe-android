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
                usernameText.text = userData.username
                notificationContent.text = item.actions?.replaceFirstChar {
                    if (it.isLowerCase()) it.uppercase() else it.toString()
                }
                sentTime.text = item.tglWaktu
                readTime.text = item.tglWaktuBaca
            }
        }
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemInboxBinding.bind(view)
    }
}