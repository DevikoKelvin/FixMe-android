package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.databinding.ListItemDepartmentBinding
import com.erela.fixme.objects.DepartmentListResponse

class DepartmentRvAdapter(val context: Context, val data: List<DepartmentListResponse>) :
    RecyclerView.Adapter<DepartmentRvAdapter.ViewHolder>() {
    private lateinit var onDepartmentClickListener: OnDepartmentClickListener

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): ViewHolder = ViewHolder(
        ListItemDepartmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ).root
    )

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = data[position]
        with(holder) {
            binding.apply {
                departmentText.text = "${item.namaDept}\n\"${item.subDept}\""
            }

            itemView.setOnClickListener {
                onDepartmentClickListener.onDepartmentClick(item)
            }
        }
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemDepartmentBinding.bind(view)
    }

    fun onDepartmentClickListener(onDepartmentClickListener: OnDepartmentClickListener) {
        this.onDepartmentClickListener = onDepartmentClickListener
    }

    interface OnDepartmentClickListener {
        fun onDepartmentClick(data: DepartmentListResponse)
    }
}