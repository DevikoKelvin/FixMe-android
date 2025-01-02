package com.erela.fixme.adapters.recycler_view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.databinding.ListItemSelectionBinding
import com.erela.fixme.objects.SelectedSupervisorTechniciansList
import com.erela.fixme.objects.SupervisorTechnicianListResponse

class SupervisorTechniciansRvAdapter(
    val context: Context, private val supervisorTechniciansList: List<SelectedSupervisorTechniciansList>,
    private val selectedSupervisorTechniciansList: ArrayList<SupervisorTechnicianListResponse>,
    private val isForSupervisor: Boolean
) : RecyclerView.Adapter<SupervisorTechniciansRvAdapter.ViewHolder>() {
    private lateinit var onTechniciansSetListener: OnTechniciansSetListener
    private lateinit var onSupervisorSetListener: OnSupervisorSetListener

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val binding = ListItemSelectionBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ListItemSelectionBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        ).root
    )

    override fun getItemCount(): Int = supervisorTechniciansList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = supervisorTechniciansList[position]

        with(holder) {
            binding.apply {
                itemCheckboxText.text = item.technician?.namaUser

                for (i in 0 until selectedSupervisorTechniciansList.size) {
                    if (selectedSupervisorTechniciansList[i].idUser == item.technician?.idUser) {
                        itemCheckboxText.isChecked = true
                        break
                    }
                }

                itemCheckboxText.setOnCheckedChangeListener { _, isChecked ->
                    item.isSelected = isChecked
                    if (isForSupervisor) {
                        if (isChecked) {
                            onSupervisorSetListener.onSupervisorsSelected(item.technician!!)
                        } else {
                            onSupervisorSetListener.onSupervisorsUnselected(item.technician!!)
                        }
                    } else {
                        if (isChecked) {
                            onTechniciansSetListener.onTechnicianSelected(item.technician!!)
                        } else {
                            onTechniciansSetListener.onTechnicianUnselected(item.technician!!)
                        }
                    }
                }
            }
        }
    }

    fun setOnTechniciansSetListener(onTechniciansSetListener: OnTechniciansSetListener) {
        this.onTechniciansSetListener = onTechniciansSetListener
    }

    fun setOnSupervisorSetListener(onSupervisorSetListener: OnSupervisorSetListener) {
        this.onSupervisorSetListener = onSupervisorSetListener
    }

    interface OnTechniciansSetListener {
        fun onTechnicianSelected(data: SupervisorTechnicianListResponse)
        fun onTechnicianUnselected(data: SupervisorTechnicianListResponse)
    }

    interface OnSupervisorSetListener {
        fun onSupervisorsSelected(data: SupervisorTechnicianListResponse)
        fun onSupervisorsUnselected(data: SupervisorTechnicianListResponse)
    }
}