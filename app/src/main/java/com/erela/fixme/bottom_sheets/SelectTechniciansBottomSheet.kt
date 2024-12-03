package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.TechniciansRvAdapter
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.BsSelectTechniciansBinding
import com.erela.fixme.helpers.networking.InitAPI
import com.erela.fixme.objects.SelectedTechniciansList
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.TechnicianListResponse
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SelectTechniciansBottomSheet(
    context: Context, private val detailData: SubmissionDetailResponse,
    private val selectedTechniciansList: ArrayList<TechnicianListResponse>
) : BottomSheetDialog(context), TechniciansRvAdapter.OnTechniciansSetListener {
    private val binding: BsSelectTechniciansBinding by lazy {
        BsSelectTechniciansBinding.inflate(layoutInflater)
    }
    private lateinit var onSelectTechniciansListener: OnSelectTechniciansListener
    val techniciansList: ArrayList<SelectedTechniciansList> = ArrayList()
    private lateinit var adapter: TechniciansRvAdapter

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
            try {
                InitAPI.getAPI.getTechnicianList(detailData.idGaprojects!!).enqueue(
                    object : Callback<List<TechnicianListResponse>> {
                        override fun onResponse(
                            call: Call<List<TechnicianListResponse>>,
                            response: Response<List<TechnicianListResponse>>
                        ) {
                            if (response.isSuccessful) {
                                if (response.body() != null) {
                                    Log.e("Technician List", response.body().toString())
                                    for (i in 0 until response.body()!!.size) {
                                        techniciansList.add(
                                            SelectedTechniciansList(
                                                response.body()!![i],
                                                false
                                            )
                                        )
                                    }
                                    adapter = TechniciansRvAdapter(
                                        context, techniciansList, selectedTechniciansList
                                    ).also {
                                        with(it) {
                                            setOnTechniciansSetListener(
                                                this@SelectTechniciansBottomSheet
                                            )
                                        }
                                    }
                                    rvTechnicians.adapter = adapter
                                    rvTechnicians.layoutManager = LinearLayoutManager(context)
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
                                        .setMessage("Can't retrieve technicians list.")
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
                                    .setMessage("Can't retrieve technicians list.")
                                    .show()
                                Log.e("ERROR", response.message().toString())
                                dismiss()
                            }
                        }

                        override fun onFailure(
                            call: Call<List<TechnicianListResponse>>, throwable: Throwable
                        ) {
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
                                .setMessage("Can't retrieve technicians list.")
                                .show()
                            Log.e("ERROR", throwable.toString())
                            throwable.printStackTrace()
                            dismiss()
                        }
                    }
                )
            } catch (jsonException: JSONException) {
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
                    .setMessage("Can't retrieve technicians list.")
                    .show()
                Log.e("ERROR", jsonException.message.toString())
                jsonException.printStackTrace()
                dismiss()
            }
        }
    }

    fun setOnSelectTechniciansListener(onSelectTechniciansListener: OnSelectTechniciansListener) {
        this.onSelectTechniciansListener = onSelectTechniciansListener
    }

    interface OnSelectTechniciansListener {
        fun onTechnicianSelected(data: TechnicianListResponse)
        fun onTechnicianUnselected(data: TechnicianListResponse)
    }

    override fun onTechnicianSelected(data: TechnicianListResponse) {
        onSelectTechniciansListener.onTechnicianSelected(data)
    }

    override fun onTechnicianUnselected(data: TechnicianListResponse) {
        onSelectTechniciansListener.onTechnicianUnselected(data)
    }
}