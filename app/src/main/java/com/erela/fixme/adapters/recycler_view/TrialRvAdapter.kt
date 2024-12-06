package com.erela.fixme.adapters.recycler_view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.databinding.ListItemProgressBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.TrialItem
import com.erela.fixme.objects.UserData

class TrialRvAdapter(
    val context: Context, private val detail: SubmissionDetailResponse, val data: List<TrialItem?>?
) : RecyclerView.Adapter<TrialRvAdapter.ViewHolder>() {
    /*private lateinit var onItemHoldTapListener: OnItemHoldTapListener*/
    private val userData: UserData by lazy {
        UserDataHelper(context).getUserData()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemProgressBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ListItemProgressBinding.inflate(LayoutInflater.from(context), parent, false).root
    )

    override fun getItemCount(): Int = data!!.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data?.get(position)

        with(holder) {
            binding.apply {
                if (item != null) {
                    usernameText.text = detail.usernUserSpv?.get(0)!!.namaUser?.trimEnd()
                    progressDescription.text = item.keterangan
                    dateTimeText.text = item.tglWaktu
                    imageContainer.visibility = View.GONE
                }
                /*itemView.setOnLongClickListener {
                    if (userData.id == item?.idUser) {
                        onItemHoldTapListener.onItemHoldTap(item)
                    }
                    true
                }*/
            }
        }
    }
    /*fun setOnItemHoldTapListener(onItemHoldTapListener: OnItemHoldTapListener) {
        this.onItemHoldTapListener = onItemHoldTapListener
    }

    interface OnItemHoldTapListener {
        fun onItemHoldTap(item: TrialItem?)
    }*/
}