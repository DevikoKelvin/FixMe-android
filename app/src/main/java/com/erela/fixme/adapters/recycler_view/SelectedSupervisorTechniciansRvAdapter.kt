package com.erela.fixme.adapters.recycler_view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.bottom_sheets.SelectSupervisorTechniciansBottomSheet
import com.erela.fixme.databinding.ListItemSelectedItemsBinding
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.SupervisorTechnicianListResponse

class SelectedSupervisorTechniciansRvAdapter(
    val context: Context, private val detailData: SubmissionDetailResponse,
    private val selectedSupervisorTechniciansList: ArrayList<SupervisorTechnicianListResponse>,
    private val isForSupervisor: Boolean
) : RecyclerView.Adapter<SelectedSupervisorTechniciansRvAdapter.ViewHolder>(),
    SelectSupervisorTechniciansBottomSheet.OnSelectTechniciansListener,
    SelectSupervisorTechniciansBottomSheet.OnSelectSupervisorListener {
    private lateinit var onTechniciansSetListener: OnTechniciansSetListener
    private lateinit var onSupervisorSetListener: OnSupervisorSetListener

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemSelectedItemsBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ListItemSelectedItemsBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        ).root
    )

    override fun getItemCount(): Int = selectedSupervisorTechniciansList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = selectedSupervisorTechniciansList[position]

        with(holder) {
            binding.apply {
                itemText.text = item.namaUser
                if (position == selectedSupervisorTechniciansList.size - 1) {
                    mainContainer.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.custom_toast_background_success
                        )
                    )
                } else {
                    mainContainer.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.custom_toast_background_normal
                        )
                    )
                }
                if (position == selectedSupervisorTechniciansList.size - 1) {
                    deleteButton.visibility = View.GONE
                } else {
                    deleteButton.visibility = View.VISIBLE
                }
                deleteButton.setOnClickListener {
                    if (isForSupervisor)
                        onSupervisorSetListener.onSupervisorsUnselected(item)
                    else
                        onTechniciansSetListener.onTechniciansUnselected(item)
                }
                itemView.setOnClickListener {
                    if (position == selectedSupervisorTechniciansList.size - 1) {
                        val bottomSheet =
                            SelectSupervisorTechniciansBottomSheet(
                                context,
                                detailData,
                                selectedSupervisorTechniciansList,
                                isForSupervisor
                            ).also {
                                with(it) {
                                    if (isForSupervisor)
                                        setOnSelectSupervisorsListener(
                                            this@SelectedSupervisorTechniciansRvAdapter
                                        )
                                    else
                                        setOnSelectTechniciansListener(
                                            this@SelectedSupervisorTechniciansRvAdapter
                                        )
                                }
                            }

                        if (bottomSheet.window != null)
                            bottomSheet.show()
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
        fun onTechniciansSelected(data: SupervisorTechnicianListResponse)
        fun onTechniciansUnselected(data: SupervisorTechnicianListResponse)
    }

    interface OnSupervisorSetListener {
        fun onSupervisorsSelected(data: SupervisorTechnicianListResponse)
        fun onSupervisorsUnselected(data: SupervisorTechnicianListResponse)
    }

    override fun onTechnicianSelected(data: SupervisorTechnicianListResponse) {
        onTechniciansSetListener.onTechniciansSelected(data)
    }

    override fun onTechnicianUnselected(data: SupervisorTechnicianListResponse) {
        onTechniciansSetListener.onTechniciansUnselected(data)
    }

    override fun onSupervisorSelected(data: SupervisorTechnicianListResponse) {
        onSupervisorSetListener.onSupervisorsSelected(data)
    }

    override fun onSupervisorUnselected(data: SupervisorTechnicianListResponse) {
        onSupervisorSetListener.onSupervisorsUnselected(data)
    }
}