package com.erela.fixme.adapters.recycler_view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.databinding.ListItemSelectionBinding
import com.erela.fixme.objects.SelectedSupervisorTechniciansList
import com.erela.fixme.objects.SupervisorTechnician

class SupervisorTechniciansRvAdapter(
    val context: Context, private val supervisorTechniciansList: List<SelectedSupervisorTechniciansList>,
    private val selectedSupervisorTechniciansList: ArrayList<SupervisorTechnician>,
    private val isForSupervisor: Boolean
) : RecyclerView.Adapter<SupervisorTechniciansRvAdapter.ViewHolder>() {
    private lateinit var onTechniciansSetListener: OnTechniciansSetListener
    private lateinit var onSupervisorSetListener: OnSupervisorSetListener

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val binding = ListItemSelectionBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ListItemSelectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ).root
    )

    override fun getItemCount(): Int = supervisorTechniciansList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = supervisorTechniciansList[position]

        with(holder) {
            binding.apply {
                itemCheckboxText.text = item.supervisorTechnician?.namaUser

                for (i in 0 until selectedSupervisorTechniciansList.size) {
                    if (selectedSupervisorTechniciansList[i].idUser == item.supervisorTechnician?.idUser) {
                        itemCheckboxText.isChecked = true
                        break
                    }
                }

                itemCheckboxText.setOnCheckedChangeListener { _, isChecked ->
                    item.isSelected = isChecked
                    if (isForSupervisor) {
                        if (isChecked) {
                            onSupervisorSetListener.onSupervisorsSelected(item.supervisorTechnician!!)
                        } else {
                            onSupervisorSetListener.onSupervisorsUnselected(item.supervisorTechnician!!)
                        }
                    } else {
                        if (isChecked) {
                            onTechniciansSetListener.onTechnicianSelected(item.supervisorTechnician!!)
                        } else {
                            onTechniciansSetListener.onTechnicianUnselected(item.supervisorTechnician!!)
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
        fun onTechnicianSelected(data: SupervisorTechnician)
        fun onTechnicianUnselected(data: SupervisorTechnician)
    }

    interface OnSupervisorSetListener {
        fun onSupervisorsSelected(data: SupervisorTechnician)
        fun onSupervisorsUnselected(data: SupervisorTechnician)
    }
}