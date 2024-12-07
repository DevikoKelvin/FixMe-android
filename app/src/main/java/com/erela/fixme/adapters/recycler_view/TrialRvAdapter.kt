package com.erela.fixme.adapters.recycler_view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.databinding.ListItemProgressBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.TrialItem
import com.erela.fixme.objects.UserData

class TrialRvAdapter(
    val context: Context, private val detail: SubmissionDetailResponse, val data: List<TrialItem?>?
) : RecyclerView.Adapter<TrialRvAdapter.ViewHolder>() {
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
                    if (item.status == 1) {
                        usernameText.setBackgroundColor(
                            ContextCompat.getColor(
                                context, R.color.custom_toast_background_failed
                            )
                        )
                        usernameText.setTextColor(
                            ContextCompat.getColor(
                                context, R.color.custom_toast_font_failed
                            )
                        )
                    } else {
                        usernameText.setBackgroundColor(
                            ContextCompat.getColor(
                                context, R.color.custom_toast_background_success
                            )
                        )
                        usernameText.setTextColor(
                            ContextCompat.getColor(
                                context, R.color.custom_toast_font_success
                            )
                        )
                    }
                    usernameText.text = detail.namaUserBuat?.trimEnd()
                    progressDescription.text = item.keterangan
                    dateTimeText.text = item.tglWaktu
                    imageContainer.visibility = View.GONE
                }
            }
        }
    }
}