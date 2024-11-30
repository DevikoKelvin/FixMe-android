package com.erela.fixme.adapters.recycler_view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.databinding.ListItemMaterialsBinding
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
        ListItemMaterialsBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        ).root
    )

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = materialsList[position]

        with(holder) {
            binding.apply {
                materialCheckboxText.text = item.material!!.namaMaterial

                for (i in 0 until selectedMaterialsArrayList.size) {
                    if (selectedMaterialsArrayList[i].idMaterial == item.material.idMaterial) {
                        materialCheckboxText.isChecked = true
                        break
                    }
                }

                materialCheckboxText.setOnCheckedChangeListener { _, isChecked ->
                    item.isSelected = isChecked
                    if (isChecked) {
                        onMaterialsSetListener.onMaterialsSelected(item.material)
                    } else {
                        onMaterialsSetListener.onMaterialsUnselected(item.material)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = materialsList.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemMaterialsBinding.bind(view)
    }

    fun setOnMaterialsSetListener(onMaterialsSetListener: OnMaterialsSetListener) {
        this.onMaterialsSetListener = onMaterialsSetListener
    }

    interface OnMaterialsSetListener {
        fun onMaterialsSelected(data: MaterialListResponse)
        fun onMaterialsUnselected(data: MaterialListResponse)
    }
}