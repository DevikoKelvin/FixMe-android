package com.erela.fixme.adapters.recycler_view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.databinding.ListItemSelectionBinding
import com.erela.fixme.objects.SelectedTechniciansList
import com.erela.fixme.objects.TechnicianListResponse

class TechniciansRvAdapter(
    val context: Context, private val techniciansList: List<SelectedTechniciansList>,
    private val selectedTechniciansList: ArrayList<TechnicianListResponse>
) : RecyclerView.Adapter<TechniciansRvAdapter.ViewHolder>() {
    private lateinit var onTechniciansSetListener: OnTechniciansSetListener

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val binding = ListItemSelectionBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ListItemSelectionBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        ).root
    )

    override fun getItemCount(): Int = techniciansList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = techniciansList[position]

        with(holder) {
            binding.apply {
                itemCheckboxText.text = item.technician?.namaUser

                for (i in 0 until selectedTechniciansList.size) {
                    if (selectedTechniciansList[i].idUser == item.technician?.idUser) {
                        itemCheckboxText.isChecked = true
                        break
                    }
                }

                itemCheckboxText.setOnCheckedChangeListener { _, isChecked ->
                    item.isSelected = isChecked
                    if (isChecked) {
                        onTechniciansSetListener.onTechnicianSelected(item.technician!!)
                    } else {
                        onTechniciansSetListener.onTechnicianUnselected(item.technician!!)
                    }
                }
            }
        }
    }

    fun setOnTechniciansSetListener(onTechniciansSetListener: OnTechniciansSetListener) {
        this.onTechniciansSetListener = onTechniciansSetListener
    }

    interface OnTechniciansSetListener {
        fun onTechnicianSelected(data: TechnicianListResponse)
        fun onTechnicianUnselected(data: TechnicianListResponse)
    }
}