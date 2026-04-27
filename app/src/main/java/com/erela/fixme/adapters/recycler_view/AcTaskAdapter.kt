package com.erela.fixme.adapters.recycler_view

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
                tvBrand.text = item.brand ?: "-"
                tvArea.text = "${item.area ?: "-"} (Floor ${item.floor ?: "-"})"
                tvDeadline.text = "Deadline: ${item.dateEnd}"

                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }
    }

    private fun setRoundedBackground(view: View, drawableId: Int) {
        view.background = ResourcesCompat.getDrawable(
            context.resources,
            drawableId,
            context.theme
        )
    }
}