package com.erela.fixme.adapters.recycler_view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.databinding.ListItemProgressBinding
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.TrialDataItem

class TrialRvAdapter(
    val context: Context,
    private val detail: SubmissionDetailResponse,
    val data: List<TrialDataItem?>?
) : RecyclerView.Adapter<TrialRvAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
                        usernameText.background = ResourcesCompat.getDrawable(
                            context.resources, R.drawable.gradient_rejected_color, context.theme
                        )
                    } else {
                        usernameText.background = ResourcesCompat.getDrawable(
                                context.resources, R.drawable.gradient_approved_color, context.theme
                            )
                    }
                    usernameText.text = detail.namaUserBuat?.trimEnd()
                    progressAnalysis.text = item.keterangan
                    progressDescription.visibility = View.GONE
                    dateTimeText.text = item.tglWaktu
                    imageContainer.visibility = View.GONE
                    circleIndicator.visibility = View.GONE
                }
            }
        }
    }
}