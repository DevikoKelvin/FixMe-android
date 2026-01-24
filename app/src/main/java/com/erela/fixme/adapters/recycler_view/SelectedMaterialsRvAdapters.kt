package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.bottom_sheets.MaterialQuantityBottomSheet
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
            LayoutInflater.from(parent.context), parent, false
        ).root
    )

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = selectedMaterialsArrayList[holder.adapterPosition]
        with(holder) {
            binding.apply {
                itemText.text = item.namaMaterial
                if (adapterPosition == selectedMaterialsArrayList.size - 1) {
                    amountText.visibility = View.GONE
                    mainContainerColor.background = ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.gradient_add_color,
                        context.theme
                    )
                } else {
                    amountText.visibility = View.VISIBLE
                    mainContainerColor.background = ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.gradient_logout_color,
                        context.theme
                    )
                }
                amountText.text = if (materialQuantityList.isNotEmpty())
                    "(${materialQuantityList[adapterPosition]})"
                else
                    ""
                if (adapterPosition == selectedMaterialsArrayList.size - 1) {
                    deleteButton.visibility = View.GONE
                } else {
                    deleteButton.visibility = View.VISIBLE
                }
                deleteButton.setOnClickListener {
                    onMaterialsSetListener.onMaterialsUnselected(item, adapterPosition)
                }
                itemView.setOnClickListener {
                    if (adapterPosition == selectedMaterialsArrayList.size - 1) {
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
                    } else {
                        val bottomSheet =
                            MaterialQuantityBottomSheet(
                                context, item.namaMaterial!!, materialQuantityList[adapterPosition]
                            ).also {
                                with(it) {
                                    setOnQuantityConfirmListener(object :
                                        MaterialQuantityBottomSheet.OnQuantityConfirmListener {
                                        override fun onQuantityConfirm(quantity: Int) {
                                            onMaterialsSetListener.onMaterialsQuantityEdited(
                                                quantity, adapterPosition
                                            )
                                        }

                                        override fun onBottomSheetDismissed(quantity: Int) {}
                                    })
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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemSelectedItemsBinding.bind(view)
    }

    fun setOnMaterialsSetListener(onMaterialsSetListener: OnMaterialsSetListener) {
        this.onMaterialsSetListener = onMaterialsSetListener
    }

    override fun onMaterialsSelected(
        data: MaterialListResponse, checkBox: CheckBox, isChecked: Boolean
    ) {
        onMaterialsSetListener.onMaterialsSelected(data, checkBox, isChecked)
    }

    override fun onMaterialsUnselected(data: MaterialListResponse, position: Int) {
        onMaterialsSetListener.onMaterialsUnselected(data, position)
    }

    interface OnMaterialsSetListener {
        fun onMaterialsSelected(data: MaterialListResponse, checkBox: CheckBox, isChecked: Boolean)
        fun onMaterialsUnselected(data: MaterialListResponse, position: Int)
        fun onMaterialsQuantityEdited(quantity: Int, position: Int)
    }
}