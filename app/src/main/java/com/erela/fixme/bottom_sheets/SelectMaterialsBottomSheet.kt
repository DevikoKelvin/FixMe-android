package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.adapters.recycler_view.MaterialsRvAdapters
import com.erela.fixme.databinding.BsSelectMaterialsBinding
import com.erela.fixme.helpers.networking.InitAPI
import com.erela.fixme.objects.MaterialListResponse
import com.erela.fixme.objects.SelectedMaterialList
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SelectMaterialsBottomSheet(
    context: Context, val selectedMaterialsArrayList: ArrayList<MaterialListResponse>
) : BottomSheetDialog(context),
    MaterialsRvAdapters.OnMaterialsSetListener {
    private val binding: BsSelectMaterialsBinding by lazy {
        BsSelectMaterialsBinding.inflate(layoutInflater)
    }
    private lateinit var onMaterialsSetListener: OnMaterialsSetListener
    val materialsList: ArrayList<SelectedMaterialList> = ArrayList()
    private lateinit var adapter: MaterialsRvAdapters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    private fun init() {
        binding.apply {
            try {
                InitAPI.getAPI.getMaterialList()
                    .enqueue(object : Callback<List<MaterialListResponse>> {
                        override fun onResponse(
                            call: Call<List<MaterialListResponse>?>,
                            response: Response<List<MaterialListResponse>?>
                        ) {
                            if (response.isSuccessful) {
                                if (response.body() != null) {
                                    for (i in 0 until response.body()!!.size) {
                                        materialsList.add(
                                            SelectedMaterialList(
                                                response.body()!![i],
                                                false
                                            )
                                        )
                                    }
                                    adapter = MaterialsRvAdapters(
                                        context, materialsList, selectedMaterialsArrayList
                                    ).also {
                                        with(it) {
                                            setOnMaterialsSetListener(
                                                this@SelectMaterialsBottomSheet
                                            )
                                        }
                                    }
                                    rvMaterials.adapter = adapter
                                    rvMaterials.layoutManager = LinearLayoutManager(context)
                                }
                            }
                        }

                        override fun onFailure(
                            call: Call<List<MaterialListResponse>?>,
                            throwable: Throwable
                        ) {
                            Log.e("ERROR", throwable.message.toString())
                            throwable.printStackTrace()
                        }
                    })
            } catch (jsonException: JSONException) {
                Log.e("ERROR", jsonException.message.toString())
                jsonException.printStackTrace()
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