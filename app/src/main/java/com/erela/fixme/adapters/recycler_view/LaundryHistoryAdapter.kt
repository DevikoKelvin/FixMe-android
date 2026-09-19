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
 * Finished batches. The same row as the waiting queue, one field further along its life.
 *
 * THE SAME LAYOUT, DELIBERATELY — it is the same bundle, later. What differs is the stamp: the
 * queue counts how long somebody has been standing there, this says when the last garment left,
 * and the item count is the only thing that makes an archive row worth reading at all.
 *
 * NOT `LaundryQueueAdapter` WITH A FLAG. That one exists to pick a courier to SERVE - it opens a
 * bundle for scanning. These rows open a batch to look at.
 *
 * BOTH LISTS OPEN [GA, 17 Sep 2026]. History rows were inert on the reasoning that a finished
 * batch is a record rather than a task. But a record is exactly the thing somebody opens - to see
 * what was in it, and to reprint the slip - and the process screen already shows a completed batch
 * with no action buttons and the print button live. The row was the only thing in the way.
 */
class LaundryHistoryAdapter(
    private val context: Context,
    /**
     * The archive says when it finished; the in-progress list says when it came in.
     *
     * A `var` since 18 Sep 2026: both lists are tabs of one screen now, sharing one adapter, so
     * which caption to draw is answered per submit rather than per construction.
     */
    var showCompletedAt: Boolean = true,
    private val onPick: (LaundryWaitingCourier) -> Unit = {}
) : RecyclerView.Adapter<LaundryHistoryAdapter.ViewHolder>() {
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

                tvDept.text = listOfNotNull(item.namaDept, item.subDept)
                    .joinToString(" · ")
                    .ifBlank { "-" }

                tvTrxNo.text = item.trxNo

                tvWaitingSince.text = if (showCompletedAt) {
                    context.getString(
                        R.string.laundry_history_finished,
                        item.itemCount,
                        item.completedAt ?: "-"
                    )
                } else if (item.status == "waiting") {
                    // THE ROW SAYS WHOSE MOVE IT IS. A bundle nobody has verified reads as one
                    // already washing otherwise, and the operator goes to the web for a step this
                    // screen has had all along.
                    context.getString(
                        R.string.laundry_needs_verify,
                        item.unverified,
                        item.itemCount
                    )
                } else {
                    context.getString(
                        R.string.laundry_washing_since,
                        item.itemCount,
                        item.checkedInAt ?: "-"
                    )
                }

                // CLICKABILITY IS NOT THE CAPTION'S BUSINESS. These were tied to one flag,
                // so choosing the "finished" wording also made the row dead - two unrelated
                // things decided by one boolean, which is how a list silently stops opening.
                mainContainer.setOnClickListener { onPick(item) }
            }
        }
    }
}
