package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.databinding.ListItemLaundryBatchLineBinding
import com.erela.fixme.objects.laundry.LaundryBatchLine

/**
 * The garments on one batch, as the courier sees them.
 *
 * READ-ONLY. The courier judges nothing — the counter recorded both conditions already — so there
 * is no action on a row. `ready` is the only line state they can act on, and it is the only one
 * highlighted; everything else reads as plain status text.
 *
 * THE STATUS IS TRANSLATED, NOT PRINTED. `item_status` is a database enum — a courier was reading
 * `picked_up` and `dialihkan` off their own batch until 15 Sep 2026. `dialihkan` is the worst of
 * them: it is a word from the schema, and what it means to the person holding the phone is that
 * the garment stayed at the laundry for the department that owns it [T-02].
 *
 * The unknown case falls through to the raw value rather than a blank, so a new enum member looks
 * wrong on screen instead of disappearing from it.
 */
class LaundryBatchLineAdapter(
    private val context: Context
) : RecyclerView.Adapter<LaundryBatchLineAdapter.ViewHolder>() {
    private val items = mutableListOf<LaundryBatchLine>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<LaundryBatchLine>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemLaundryBatchLineBinding.inflate(
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

    inner class ViewHolder(private val binding: ListItemLaundryBatchLineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LaundryBatchLine) {
            binding.apply {
                tvCode.text = item.qrCode ?: "-"
                tvItemType.text = item.itemType ?: "-"
                tvOwner.text = item.ownerName ?: "-"
                tvLineStatus.text = when (item.itemStatus) {
                    "waiting" -> context.getString(R.string.laundry_item_waiting)
                    "accepted" -> context.getString(R.string.laundry_item_accepted)
                    "ready" -> context.getString(R.string.laundry_item_ready)
                    "picked_up" -> context.getString(R.string.laundry_item_picked_up)
                    "lost" -> context.getString(R.string.laundry_item_lost)
                    "dialihkan" -> context.getString(R.string.laundry_item_held)
                    "repaired" -> context.getString(R.string.laundry_item_repaired)
                    else -> item.itemStatus
                }

                tvLineStatus.setTextColor(
                    context.getColor(
                        if (item.itemStatus == "ready") R.color.status_hold
                        else R.color.custom_toast_font_normal_gray
                    )
                )
            }
        }
    }
}
