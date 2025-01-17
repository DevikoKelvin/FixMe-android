package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import com.erela.fixme.R
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.bottom_sheets.SelectMaterialsBottomSheet
import com.erela.fixme.databinding.ListItemSelectedItemsBinding
import com.erela.fixme.objects.MaterialListResponse
import com.erela.fixme.objects.SubmissionDetailResponse

class SelectedMaterialsRvAdapters(
    val context: Context, private val selectedMaterialsArrayList: ArrayList<MaterialListResponse>,
    private val materialQuantityList: ArrayList<Int>, private val detailData:
    SubmissionDetailResponse
) : RecyclerView.Adapter<SelectedMaterialsRvAdapters.ViewHolder>(),
    SelectMaterialsBottomSheet.OnMaterialsSetListener {
    private lateinit var onMaterialsSetListener: OnMaterialsSetListener

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): ViewHolder = ViewHolder(
        ListItemSelectedItemsBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        ).root
    )

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = selectedMaterialsArrayList[position]
        with(holder) {
            binding.apply {
                itemText.text = item.namaMaterial
                if (position == selectedMaterialsArrayList.size - 1) {
                    amountText.visibility = View.GONE
                    mainContainer.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.custom_toast_background_success
                        )
                    )
                } else {
                    amountText.visibility = View.VISIBLE
                    mainContainer.setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.custom_toast_background_normal
                        )
                    )
                }
                amountText.text = if (materialQuantityList.isNotEmpty())
                        "(${materialQuantityList[position]})"
                else
                    ""
                if (position == selectedMaterialsArrayList.size - 1) {
                    deleteButton.visibility = View.GONE
                } else {
                    deleteButton.visibility = View.VISIBLE
                }
                deleteButton.setOnClickListener {
                    onMaterialsSetListener.onMaterialsUnselected(item, position)
                }
                itemView.setOnClickListener {
                    if (position == selectedMaterialsArrayList.size - 1) {
                        val bottomSheet =
                            SelectMaterialsBottomSheet(
                                context,
                                selectedMaterialsArrayList,
                                detailData
                            ).also {
                                with(it) {
                                    onMaterialsSetListener(this@SelectedMaterialsRvAdapters)
                                }
                            }

                        if (bottomSheet.window != null)
                            bottomSheet.show()
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = selectedMaterialsArrayList.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemSelectedItemsBinding.bind(view)
    }

    fun setOnMaterialsSetListener(onMaterialsSetListener: OnMaterialsSetListener) {
        this.onMaterialsSetListener = onMaterialsSetListener
    }

    override fun onMaterialsSelected(data: MaterialListResponse) {
        onMaterialsSetListener.onMaterialsSelected(data)
    }

    override fun onMaterialsUnselected(data: MaterialListResponse, position: Int) {
        onMaterialsSetListener.onMaterialsUnselected(data, position)
    }

    interface OnMaterialsSetListener {
        fun onMaterialsSelected(data: MaterialListResponse)
        fun onMaterialsUnselected(data: MaterialListResponse, position: Int)
    }
}