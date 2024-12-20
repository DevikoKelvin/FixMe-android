package com.erela.fixme.adapters.recycler_view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.bottom_sheets.SelectTechniciansBottomSheet
import com.erela.fixme.databinding.ListItemSelectedItemsBinding
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.TechnicianListResponse

class SelectedTechniciansRvAdapter(
    val context: Context, private val detailData: SubmissionDetailResponse,
    private val selectedTechniciansList: ArrayList<TechnicianListResponse>
) : RecyclerView.Adapter<SelectedTechniciansRvAdapter.ViewHolder>(),
    SelectTechniciansBottomSheet.OnSelectTechniciansListener {
    private lateinit var onTechniciansSetListener: OnTechniciansSetListener

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemSelectedItemsBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ListItemSelectedItemsBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        ).root
    )

    override fun getItemCount(): Int = selectedTechniciansList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = selectedTechniciansList[position]

        with(holder) {
            binding.apply {
                itemText.text = item.namaUser
                if (position == selectedTechniciansList.size - 1) {
                    materialContainer.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.custom_toast_background_success
                        )
                    )
                } else {
                    materialContainer.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.custom_toast_background_normal
                        )
                    )
                }
                if (position == selectedTechniciansList.size - 1) {
                    deleteButton.visibility = View.GONE
                } else {
                    deleteButton.visibility = View.VISIBLE
                }
                deleteButton.setOnClickListener {
                    onTechniciansSetListener.onTechniciansUnselected(item)
                }
                itemView.setOnClickListener {
                    if (position == selectedTechniciansList.size - 1) {
                        val bottomSheet =
                            SelectTechniciansBottomSheet(
                                context, detailData, selectedTechniciansList
                            ).also {
                                with(it) {
                                    setOnSelectTechniciansListener(
                                        this@SelectedTechniciansRvAdapter
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

    interface OnTechniciansSetListener {
        fun onTechniciansSelected(data: TechnicianListResponse)
        fun onTechniciansUnselected(data: TechnicianListResponse)
    }

    override fun onTechnicianSelected(data: TechnicianListResponse) {
        onTechniciansSetListener.onTechniciansSelected(data)
    }

    override fun onTechnicianUnselected(data: TechnicianListResponse) {
        onTechniciansSetListener.onTechniciansUnselected(data)
    }
}