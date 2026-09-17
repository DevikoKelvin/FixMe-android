package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.databinding.ListItemLaundryProcessLineBinding
import com.erela.fixme.objects.laundry.LaundryBatchLine

/**
 * The counter's garment rows — the same list the courier sees, with a tick box.
 *
 * NOT `LaundryBatchLineAdapter` WITH A FLAG. That one is the courier's read-only row and has no
 * selection at all; teaching it one would put a checkbox nobody can use on a screen where nothing
 * is selectable, for the sake of sharing thirty lines of binding.
 *
 * THE TICK IS THE WHOLE ROW. A 20dp checkbox is the wrong target for somebody standing at a
 * machine holding a garment, so the card carries the click and the box only reports it — hence
 * `clickable="false"` in the layout, which stops the two fighting over the same tap.
 */
class LaundryProcessLineAdapter(
    private val context: Context,
    private val onToggle: (LaundryBatchLine) -> Unit
) : RecyclerView.Adapter<LaundryProcessLineAdapter.ViewHolder>() {
    private val items = mutableListOf<LaundryBatchLine>()
    private var selected = emptySet<Int>()
    private var tickable = false

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<LaundryBatchLine>, canTick: Boolean) {
        items.clear()
        items.addAll(newItems)
        tickable = canTick
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelected(ids: Set<Int>) {
        selected = ids
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemLaundryProcessLineBinding.inflate(
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

    inner class ViewHolder(
        private val binding: ListItemLaundryProcessLineBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(line: LaundryBatchLine) {
            binding.apply {
                tvCode.text = line.qrCode ?: "—"

                tvItemType.text = listOfNotNull(line.itemType, line.ownerName)
                    .joinToString(" · ")
                    .ifBlank { "—" }

                // THE PAIR, side by side. An exit condition means nothing without the one the
                // garment arrived in - that comparison IS the damage attribution [D-3].
                tvConditions.text = context.getString(
                    R.string.laundry_condition_pair,
                    line.conditionIn ?: "—",
                    line.conditionOut ?: "—"
                )

                tvLineStatus.text = context.getString(
                    when (line.itemStatus) {
                        "waiting" -> R.string.laundry_item_waiting
                        "accepted" -> R.string.laundry_item_accepted
                        "ready" -> R.string.laundry_item_ready
                        "picked_up" -> R.string.laundry_item_picked_up
                        "lost" -> R.string.laundry_item_lost
                        "dialihkan" -> R.string.laundry_item_held
                        else -> R.string.laundry_item_repaired
                    }
                )

                cbPick.visibility = if (tickable) View.VISIBLE else View.GONE
                cbPick.isChecked = line.id in selected

                root.setOnClickListener {
                    if (tickable) onToggle(line)
                }
            }
        }
    }
}
