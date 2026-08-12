package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.databinding.ListItemAcTaskBinding
import com.erela.fixme.objects.ac.AcTaskItem

class AcTaskAdapter(
    private val context: Context,
    private val onItemClick: (AcTaskItem) -> Unit
) : RecyclerView.Adapter<AcTaskAdapter.ViewHolder>() {
    private val tasks = mutableListOf<AcTaskItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newTasks: List<AcTaskItem>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemAcTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    inner class ViewHolder(private val binding: ListItemAcTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: AcTaskItem) {
            binding.apply {
                tvScheduleName.text = item.scheduleName
                when (item.itemStatus) {
                    "pending" -> {
                        setRoundedBackground(
                            statusColor,
                            R.drawable.gradient_pending_color
                        )
                        statusText.setTextColor(
                            ResourcesCompat.getColorStateList(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                    }

                    "in_progress" -> {
                        setRoundedBackground(
                            statusColor,
                            R.drawable.gradient_on_progress_color
                        )
                        statusText.setTextColor(
                            ResourcesCompat.getColorStateList(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                    }

                    "completed" -> {
                        setRoundedBackground(
                            statusColor,
                            R.drawable.gradient_approved_color
                        )
                        statusText.setTextColor(
                            ResourcesCompat.getColorStateList(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                    }

                    "overdue" -> {
                        setRoundedBackground(
                            statusColor,
                            R.drawable.gradient_rejected_color
                        )
                        statusText.setTextColor(
                            ResourcesCompat.getColorStateList(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                    }

                    "skipped" -> {
                        setRoundedBackground(statusColor, R.drawable.gradient_menu_color)
                        statusText.setTextColor(
                            ResourcesCompat.getColorStateList(
                                context.resources,
                                R.color.black,
                                context.theme
                            )
                        )
                    }
                }
                statusText.text = when (item.itemStatus) {
                    "pending" -> "Pending"
                    "in_progress" -> "In Progress"
                    "completed" -> "Completed"
                    "overdue" -> "Overdue"
                    "skipped" -> "Skipped"
                    else -> item.itemStatus
                }
                tvAcCode.text = item.acCode

                row(rowLocation, R.string.location, item.location)
                row(rowDetail, R.string.detail, item.detail)
                row(rowArea, R.string.area, item.area)
                row(rowFloor, R.string.floor, item.floor)
                row(rowBrand, R.string.brand, item.brand)
                row(rowModel, R.string.model_type, item.modelType)
                // Trailing ".0" reads wrong for a 1.5 PK unit; drop it only when whole.
                row(
                    rowCapacity, R.string.capacity,
                    item.capacityPk?.let { pk ->
                        val trimmed = if (pk % 1.0 == 0.0) pk.toInt().toString() else pk.toString()
                        "$trimmed PK"
                    }
                )
                // Always shown, even unassigned: since the list started including every
                // technician's items, "whose job is this" is the thing that was missing.
                row(
                    rowTechnician, R.string.technician,
                    item.assignedTechnician?.trim()?.takeIf { it.isNotEmpty() }
                        ?: context.getString(R.string.unassigned),
                )
                row(rowLastMaintenance, R.string.last_maintenance, formatDate(item.lastMaintenanceAt))

                val note = item.scheduleNotes?.trim()
                noteBlock.visibility = if (note.isNullOrEmpty()) View.GONE else View.VISIBLE
                tvScheduleNote.text = note.orEmpty()

                tvDeadline.text = "${context.getString(R.string.deadline)} ${item.dateEnd}"

                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }
    }

    /**
     * Fills one included row, or hides it when there is nothing to say. Views are recycled, so
     * the visibility has to be set on both branches — not just the hiding one.
     */
    private fun row(
        binding: com.erela.fixme.databinding.PartialAcTaskRowBinding,
        labelRes: Int,
        value: String?
    ) {
        val text = value?.trim()?.takeIf { it.isNotEmpty() }
        binding.root.visibility = if (text == null) View.GONE else View.VISIBLE
        if (text != null) {
            binding.rowLabel.setText(labelRes)
            binding.rowValue.text = text
        }
    }

    /** "2026-05-12 08:30:00" -> "12/05/2026". Left as-is if it is not a date we recognise. */
    private fun formatDate(raw: String?): String? {
        val datePart = raw?.trim()?.takeIf { it.isNotEmpty() }?.substringBefore(' ') ?: return null
        val parts = datePart.split('-')
        return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else datePart
    }

    private fun setRoundedBackground(view: View, drawableId: Int) {
        view.background = ResourcesCompat.getDrawable(
            context.resources,
            drawableId,
            context.theme
        )
    }
}