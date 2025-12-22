package com.erela.fixme.adapters.recycler_view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.databinding.ListItemSelectionBinding
import com.erela.fixme.objects.MaterialListResponse
import com.erela.fixme.objects.SelectedMaterialList

class MaterialsRvAdapters(
    val context: Context, private val materialsList: List<SelectedMaterialList>,
    private val selectedMaterialsArrayList: ArrayList<MaterialListResponse>
) : RecyclerView.Adapter<MaterialsRvAdapters.ViewHolder>() {
    private lateinit var onMaterialsSetListener: OnMaterialsSetListener

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): ViewHolder = ViewHolder(
        ListItemSelectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ).root
    )

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = materialsList[position]

        with(holder) {
            binding.apply {
                itemCheckboxText.text = item.material!!.namaMaterial

                for (i in 0 until selectedMaterialsArrayList.size) {
                    if (selectedMaterialsArrayList[i].idMaterial == item.material.idMaterial) {
                        itemCheckboxText.isChecked = true
                        break
                    }
                }

                itemCheckboxText.setOnClickListener {
                    if (itemCheckboxText.isChecked) {
                        onMaterialsSetListener.onMaterialsSelected(
                            itemCheckboxText, true, item.material
                        )
                    } else
                        onMaterialsSetListener.onMaterialsUnselected(
                            itemCheckboxText, false, item.material, position
                        )
                }
            }
        }
    }

    override fun getItemCount(): Int = materialsList.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemSelectionBinding.bind(view)
    }

    fun setOnMaterialsSetListener(onMaterialsSetListener: OnMaterialsSetListener) {
        this.onMaterialsSetListener = onMaterialsSetListener
    }

    interface OnMaterialsSetListener {
        fun onMaterialsSelected(checkBox: CheckBox, isChecked: Boolean, data: MaterialListResponse)
        fun onMaterialsUnselected(
            checkBox: CheckBox, isChecked: Boolean, data: MaterialListResponse, position: Int
        )
    }
}

class MaterialDiffUtilCallback(
    private val oldList: List<SelectedMaterialList>,
    private val newList: List<SelectedMaterialList>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].material?.idMaterial == newList[newItemPosition].material?.idMaterial
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}