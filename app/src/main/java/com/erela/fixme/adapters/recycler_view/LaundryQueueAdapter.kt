package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.databinding.ListItemLaundryQueueBinding
import com.erela.fixme.objects.laundry.LaundryWaitingCourier

/**
 * The operator's work queue — couriers who have scanned the counter and are waiting.
 *
 * OLDEST FIRST, and the server orders it that way: the courier who has been standing there longest
 * is the one to serve next. Nothing here re-sorts it.
 *
 * THE DEPARTMENT IS CARRIED FORWARD, not just displayed. Picking a row sets what the bundle rows
 * compare their owner against, so a garment belonging to somebody else is flagged against the
 * ARRIVING department rather than against the operator's own, which is always GA and would
 * therefore mark every single garment as mixed.
 */
class LaundryQueueAdapter(
    private val context: Context,
    private val onPick: (LaundryWaitingCourier) -> Unit
) : RecyclerView.Adapter<LaundryQueueAdapter.ViewHolder>() {
    private val items = mutableListOf<LaundryWaitingCourier>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<LaundryWaitingCourier>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemLaundryQueueBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position + 1)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ListItemLaundryQueueBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: LaundryWaitingCourier, ordinal: Int) {
            binding.apply {
                tvOrdinal.text = "$ordinal."
                tvCourier.text = item.pengantar ?: "-"

                tvDept.text = listOfNotNull(
                    item.namaDept,
                    item.subDept
                ).joinToString(" · ").ifBlank { "-" }

                tvTrxNo.text = item.trxNo

                // The whole stamp, date included, rather than just the clock. An arrival nobody
                // ever scanned stays `waiting` with no lines and keeps its place in this queue, so
                // rows are not all from today - and a day-old one is precisely what the operator
                // should be able to spot.
                tvWaitingSince.text = context.getString(
                    R.string.laundry_waiting_since,
                    item.checkedInAt ?: "-"
                )

                mainContainer.setOnClickListener { onPick(item) }
            }
        }
    }
}
