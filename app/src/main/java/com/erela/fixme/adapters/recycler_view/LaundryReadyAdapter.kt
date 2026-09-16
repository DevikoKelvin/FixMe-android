package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.databinding.ListItemLaundryQueueBinding
import com.erela.fixme.objects.laundry.LaundryReadyBatch

/**
 * Batches waiting to be handed back, in the operator's collection queue.
 *
 * IT REUSES THE ARRIVAL ROW'S LAYOUT, deliberately. The two queues answer the same shape of
 * question — whose bundle, which number, how long has it been sitting — at the two ends of the
 * same day, and a second near-identical XML file would drift the first time either is restyled.
 * What differs is the third line, and that is a string.
 *
 * ALREADY-STAGED BATCHES STAY ON THE LIST. A batch checked back out an hour ago whose courier has
 * not turned up is still the counter's problem, and hiding it would leave the operator with no way
 * to see that the check was already done — which is exactly when they would do it twice.
 */
class LaundryReadyAdapter(
    private val context: Context,
    private val onPick: (LaundryReadyBatch) -> Unit
) : RecyclerView.Adapter<LaundryReadyAdapter.ViewHolder>() {
    private val items = mutableListOf<LaundryReadyBatch>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<LaundryReadyBatch>) {
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
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ListItemLaundryQueueBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: LaundryReadyBatch, position: Int) {
            binding.apply {
                tvOrdinal.text = "${position + 1}."
                tvCourier.text = item.pengantar ?: "-"

                tvDept.text = listOfNotNull(item.namaDept, item.subDept).joinToString(" · ")
                    .ifBlank { "-" }

                // The count is what is COLLECTABLE, not what the batch holds: a partial pickup
                // leaves the rest ready while the header still reads ready [T-07], so an item
                // count would have the operator scanning towards a number that already left.
                tvWaitingSince.text = if (item.handoverMarkedAt != null) {
                    context.getString(R.string.laundry_already_marked, item.handoverMarkedAt)
                } else {
                    context.getString(
                        R.string.laundry_ready_since,
                        item.readyCount,
                        item.readyAt ?: "-"
                    )
                }

                tvTrxNo.text = item.trxNo

                mainContainer.setOnClickListener { onPick(item) }
            }
        }
    }
}
