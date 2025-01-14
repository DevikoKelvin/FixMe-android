package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.MaterialDiffUtilCallback
import com.erela.fixme.adapters.recycler_view.MaterialsRvAdapters
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.BsSelectMaterialsBinding
import com.erela.fixme.helpers.networking.InitAPI
import com.erela.fixme.objects.MaterialListResponse
import com.erela.fixme.objects.SelectedMaterialList
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class SelectMaterialsBottomSheet(
    context: Context, val selectedMaterialsArrayList: ArrayList<MaterialListResponse>
) : BottomSheetDialog(context),
    MaterialsRvAdapters.OnMaterialsSetListener {
    private val binding: BsSelectMaterialsBinding by lazy {
        BsSelectMaterialsBinding.inflate(layoutInflater)
    }
    private lateinit var onMaterialsSetListener: OnMaterialsSetListener
    private val materialsList: ArrayList<SelectedMaterialList> = ArrayList()
    private var materialsArrayList: ArrayList<SelectedMaterialList> = ArrayList()
    private lateinit var adapter: MaterialsRvAdapters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun init() {
        binding.apply {
            searchField.addTextChangedListener { s ->
                val searchText = s.toString().lowercase(Locale.getDefault())
                val filteredList = if (searchText.isEmpty()) {
                    materialsList
                } else {
                    materialsList.filter { materialItem ->
                        materialItem.material?.namaMaterial?.lowercase(Locale.getDefault())
                            ?.indexOf(searchText) != -1
                    }
                }
                val diffCallback = MaterialDiffUtilCallback(materialsArrayList, filteredList)
                val diffResult = DiffUtil.calculateDiff(diffCallback)

                materialsArrayList.clear()
                materialsArrayList.addAll(filteredList)
                diffResult.dispatchUpdatesTo(adapter)
            }

            loadingBar.visibility = View.VISIBLE
            searchFieldLayout.visibility = View.GONE
            try {
                InitAPI.getAPI.getMaterialList()
                    .enqueue(object : Callback<List<MaterialListResponse>> {
                        override fun onResponse(
                            call: Call<List<MaterialListResponse>?>,
                            response: Response<List<MaterialListResponse>?>
                        ) {
                            loadingBar.visibility = View.GONE
                            searchFieldLayout.visibility = View.VISIBLE
                            if (response.isSuccessful) {
                                if (response.body() != null) {
                                    for (i in 0 until response.body()!!.size) {
                                        materialsList.add(
                                            SelectedMaterialList(
                                                false,
                                                response.body()!![i]
                                            )
                                        )
                                    }
                                    materialsArrayList.addAll(materialsList)
                                    adapter = MaterialsRvAdapters(
                                        context, materialsArrayList, selectedMaterialsArrayList
                                    ).also {
                                        with(it) {
                                            setOnMaterialsSetListener(
                                                this@SelectMaterialsBottomSheet
                                            )
                                        }
                                    }
                                    rvMaterials.adapter = adapter
                                    rvMaterials.layoutManager = LinearLayoutManager(context)
                                    adapter.notifyDataSetChanged()
                                } else {
                                    CustomToast.getInstance(context)
                                        .setBackgroundColor(
                                            ContextCompat.getColor(
                                                context,
                                                R.color.custom_toast_background_failed
                                            )
                                        )
                                        .setFontColor(
                                            ContextCompat.getColor(
                                                context,
                                                R.color.custom_toast_font_failed
                                            )
                                        )
                                        .setMessage("Can't retrieve material list.")
                                        .show()
                                    Log.e("ERROR", response.message().toString())
                                    dismiss()
                                }
                            } else {
                                CustomToast.getInstance(context)
                                    .setBackgroundColor(
                                        ContextCompat.getColor(
                                            context,
                                            R.color.custom_toast_background_failed
                                        )
                                    )
                                    .setFontColor(
                                        ContextCompat.getColor(
                                            context,
                                            R.color.custom_toast_font_failed
                                        )
                                    )
                                    .setMessage("Can't retrieve material list.")
                                    .show()
                                Log.e("ERROR", response.message().toString())
                                dismiss()
                            }
                        }

                        override fun onFailure(
                            call: Call<List<MaterialListResponse>?>,
                            throwable: Throwable
                        ) {
                            loadingBar.visibility = View.GONE
                            CustomToast.getInstance(context)
                                .setBackgroundColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.custom_toast_background_failed
                                    )
                                )
                                .setFontColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.custom_toast_font_failed
                                    )
                                )
                                .setMessage("Can't retrieve material list.")
                                .show()
                            Log.e("ERROR", throwable.message.toString())
                            throwable.printStackTrace()
                            dismiss()
                        }
                    })
            } catch (jsonException: JSONException) {
                loadingBar.visibility = View.GONE
                CustomToast.getInstance(context)
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.custom_toast_background_failed
                        )
                    )
                    .setFontColor(
                        ContextCompat.getColor(
                            context,
                            R.color.custom_toast_font_failed
                        )
                    )
                    .setMessage("Can't retrieve material list.")
                    .show()
                Log.e("ERROR", jsonException.message.toString())
                jsonException.printStackTrace()
                dismiss()
            }
        }
    }

    fun onMaterialsSetListener(onMaterialsSetListener: OnMaterialsSetListener) {
        this.onMaterialsSetListener = onMaterialsSetListener
    }

    override fun onMaterialsSelected(data: MaterialListResponse) {
        onMaterialsSetListener.onMaterialsSelected(data)
    }

    override fun onMaterialsUnselected(data: MaterialListResponse) {
        onMaterialsSetListener.onMaterialsUnselected(data)
    }

    interface OnMaterialsSetListener {
        fun onMaterialsSelected(data: MaterialListResponse)
        fun onMaterialsUnselected(data: MaterialListResponse)
    }
}