package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.databinding.ListItemLaundryBatchBinding
import com.erela.fixme.objects.laundry.LaundryBatch

/**
 * The courier's batches — their DEPARTMENT's, open ones first.
 *
 * SCOPED BY DEPARTMENT, NOT BY WHO CARRIED IT IN [T-08], and the server orders it: any active
 * account of the department may collect, so a list of only this person's own hand-overs would hide
 * the batch they were sent to fetch. Nothing here re-sorts.
 *
 * THE READY COUNT IS SHOWN SEPARATELY FROM THE STATUS. A batch reads `ready` until its last line
 * moves, so a colleague can have taken everything while the header still says collectable — which
 * is exactly the case where a courier would otherwise walk to the counter for nothing.
 */
class LaundryBatchAdapter(
    private val context: Context,
    private val onPick: (LaundryBatch) -> Unit
) : RecyclerView.Adapter<LaundryBatchAdapter.ViewHolder>() {
    private val items = mutableListOf<LaundryBatch>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<LaundryBatch>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemLaundryBatchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ListItemLaundryBatchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LaundryBatch) {
            binding.apply {
                tvTrxNo.text = item.trxNo

                // The courier's words, not the counter's: `accepted` describes a step they never
                // see, and "sedang dicuci" is the answer they came for.
                //
                // `ready` MEANS THE WASH IS DONE, NOT THAT ANYBODY HAS COUNTED THE SHELF [GA,
                // 15 Sep 2026]. A batch the counter has not checked back out gets its own
                // sentence, matching the detail screen - two places saying different things about
                // whether a courier may set off is worse than either wording alone.
                tvStatus.text = context.getString(
                    when {
                        item.status == "ready" && item.handoverMarkedAt == null ->
                            R.string.laundry_status_awaiting_counter

                        item.status == "waiting" -> R.string.laundry_status_waiting
                        item.status == "accepted" -> R.string.laundry_status_accepted
                        item.status == "ready" -> R.string.laundry_status_ready
                        else -> R.string.laundry_status_completed
                    }
                )

                tvSummary.text = context.getString(
                    R.string.laundry_batch_summary,
                    item.itemCount,
                    item.checkedInAt ?: "-"
                )

                tvReady.visibility = if (item.readyCount > 0) View.VISIBLE else View.GONE

                if (item.readyCount > 0) {
                    tvReady.text =
                        context.getString(R.string.laundry_ready_to_collect, item.readyCount)
                }

                mainContainer.setOnClickListener { onPick(item) }
            }
        }
    }
}
