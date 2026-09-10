package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.databinding.ListItemLaundryBundleBinding
import com.erela.fixme.objects.laundry.LaundryGarment

/**
 * The bundle being built, one row per scanned garment.
 *
 * A ROW IS A GARMENT, NOT A COUNT. The slip prints quantities per type, but the courier building
 * the bundle needs to see each patch: the whole point of resolving a scan is that they can spot a
 * garment they did not mean to pick up, and a total of "12" hides exactly that.
 *
 * THE MIXED-OWNER WARNING IS THE REASON THE OWNER IS SHOWN AT ALL. A garment belonging to another
 * department is accepted — it is washed and becomes a held item [T-02] — but the courier is the
 * last person who can notice before walking away, so the row says so plainly rather than leaving
 * it to the slip.
 */
class LaundryBundleAdapter(
    private val context: Context,
    private val ownDept: String?,
    private val onRemove: (LaundryGarment) -> Unit,
    private val onNoteChanged: (LaundryGarment, String?) -> Unit
) : RecyclerView.Adapter<LaundryBundleAdapter.ViewHolder>() {
    private val items = mutableListOf<LaundryGarment>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<LaundryGarment>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemLaundryBundleBinding.inflate(
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

    inner class ViewHolder(private val binding: ListItemLaundryBundleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: LaundryGarment, ordinal: Int) {
            binding.apply {
                tvOrdinal.text = "$ordinal."
                tvCode.text = item.qrCode
                tvItemType.text = item.itemType ?: "-"

                tvOwner.text = listOfNotNull(
                    item.ownerName,
                    item.ownerDept
                ).joinToString(" · ").ifBlank { "-" }

                // Compared by NAME here because that is all the phone has; the server compares
                // department IDs, which is the check that decides anything [D-17].
                val isMixed = !item.ownerDept.isNullOrBlank() &&
                        !ownDept.isNullOrBlank() &&
                        !item.ownerDept.equals(ownDept, ignoreCase = true)

                tvMixedWarning.visibility = if (isMixed) View.VISIBLE else View.GONE

                if (isMixed) {
                    tvMixedWarning.text = context.getString(R.string.laundry_mixed_owner_warning)
                }

                // Set before the listener, or restoring a note fires it back as a user edit.
                etNote.setText(item.note ?: "")

                etNote.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        onNoteChanged(item, etNote.text?.toString()?.trim()?.ifBlank { null })
                    }
                }

                removeButton.setOnClickListener { onRemove(item) }
            }
        }
    }
}
