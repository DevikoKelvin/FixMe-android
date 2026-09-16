package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.databinding.ListItemLaundryOutScanBinding

/**
 * The codes scanned back OUT at the counter, in scan order.
 *
 * PLAIN STRINGS, not resolved garments, and the reason is worth keeping: resolving a code against
 * the master register answers "is this a registered uniform", which is not the question here.
 * Whether it belongs to THIS batch is, and only the server knows — it checks every code on submit
 * and names the one that does not fit. A lookup per scan would slow the count down and add nothing.
 */
class LaundryOutScanAdapter(
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<LaundryOutScanAdapter.ViewHolder>() {
    private val items = mutableListOf<String>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemLaundryOutScanBinding.inflate(
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

    inner class ViewHolder(private val binding: ListItemLaundryOutScanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(code: String, position: Int) {
            binding.apply {
                tvOrdinal.text = "${position + 1}."
                tvCode.text = code

                removeButton.setOnClickListener { onRemove(code) }
            }
        }
    }
}
