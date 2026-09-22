package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.databinding.ListItemLaundryHeldBinding
import com.erela.fixme.objects.laundry.LaundryHeldItem

/**
 * Titipan: garments the counter is holding for another department  [T-02].
 *
 * TICKED, NOT TAPPED. A held garment goes home in a group — one courier signs for everything their
 * department left behind — so the row is a checkbox and the action lives at the foot of the
 * screen. Tapping anywhere on the row toggles it, because a 24dp checkbox is not a target for
 * somebody holding a bundle of overalls.
 *
 * THE AGE IS WHY THE LIST EXISTS. GA's reminder runs at three days, so a row reading "ditahan 5
 * hari" is the one worth chasing — and it is coloured for that reason rather than for decoration.
 *
 * SELECTION LIVES IN THE ACTIVITY, not here: the hand-back button reads it, and an adapter that
 * owned it would be a second copy to keep in step.
 */
class LaundryHeldAdapter(
    private val context: Context,
    private val onToggle: (LaundryHeldItem) -> Unit
) : RecyclerView.Adapter<LaundryHeldAdapter.ViewHolder>() {
    private val items = mutableListOf<LaundryHeldItem>()
    private val selected = mutableSetOf<Int>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<LaundryHeldItem>) {
        items.clear()
        items.addAll(newItems)

        // A selection that outlives the rows it pointed at would hand back a garment the operator
        // can no longer see.
        selected.retainAll(newItems.map { it.id }.toSet())

        notifyDataSetChanged()
    }

    fun selectedIds(): List<Int> = selected.toList()

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {
        selected.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemLaundryHeldBinding.inflate(
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

    inner class ViewHolder(private val binding: ListItemLaundryHeldBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: LaundryHeldItem) {
            binding.apply {
                tvCode.text = listOfNotNull(item.qrCode, item.itemType)
                    .joinToString(" · ")
                    .ifBlank { "-" }

                tvOwner.text = item.ownerName ?: "-"

                tvHeldSince.text = context.getString(
                    R.string.laundry_held_since,
                    listOfNotNull(item.ownerDept, item.ownerSubDept)
                        .joinToString(" · ")
                        .ifBlank { "-" },
                    item.ageDays
                )

                tvHeldSince.setTextColor(
                    context.getColor(
                        if (item.ageDays >= 3) R.color.status_waiting
                        else R.color.custom_toast_font_normal_gray
                    )
                )

                // Set WITHOUT the listener attached: binding a recycled row fires the change
                // callback, which would toggle the very selection being drawn.
                checkBox.setOnCheckedChangeListener(null)
                checkBox.isChecked = item.id in selected

                val toggle = {
                    if (item.id in selected) selected.remove(item.id) else selected.add(item.id)
                    checkBox.isChecked = item.id in selected
                    onToggle(item)
                }

                root.setOnClickListener { toggle() }
                checkBox.setOnClickListener { toggle() }
            }
        }
    }
}
