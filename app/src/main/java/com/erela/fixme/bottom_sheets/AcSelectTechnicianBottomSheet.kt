package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.SupervisorTechniciansRvAdapter
import com.erela.fixme.databinding.BsSelectTechniciansBinding
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.SelectedSupervisorTechniciansList
import com.erela.fixme.objects.SupervisorTechnician
import com.erela.fixme.objects.SupervisorTechnicianListResponse
import com.google.android.material.bottomsheet.BottomSheetDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AcSelectTechnicianBottomSheet(
    context: Context,
    private val currentUserId: Int,
    private val alreadySelected: ArrayList<SupervisorTechnician>
) : BottomSheetDialog(context) {
    private val lang: String get() = context.getString(R.string.lang)
    private val binding: BsSelectTechniciansBinding by lazy {
        BsSelectTechniciansBinding.inflate(layoutInflater)
    }

    private var onTechnicianSelectedListener: OnTechnicianSelectedListener? = null
    private val techniciansList: ArrayList<SelectedSupervisorTechniciansList> = ArrayList()
    private lateinit var adapter: SupervisorTechniciansRvAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        loadTechnicians()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadTechnicians() {
        binding.loadingBar.visibility = View.VISIBLE

        InitAPI.getEndpoint.acGetTechnicians(currentUserId, lang).enqueue(
            object : Callback<SupervisorTechnicianListResponse> {
                override fun onResponse(
                    call: Call<SupervisorTechnicianListResponse>,
                    response: Response<SupervisorTechnicianListResponse>
                ) {
                    binding.loadingBar.visibility = View.GONE
                    val list = response.body()?.data ?: return

                    techniciansList.clear()
                    list.forEach { tech ->
                        if (tech != null) {
                            techniciansList.add(
                                SelectedSupervisorTechniciansList(
                                    isSelected = alreadySelected.any { it.idUser == tech.idUser },
                                    supervisorTechnician = tech
                                )
                            )
                        }
                    }

                    adapter = SupervisorTechniciansRvAdapter(
                        context,
                        techniciansList,
                        alreadySelected,
                        false
                    ).also { rv ->
                        rv.setOnTechniciansSetListener(object :
                            SupervisorTechniciansRvAdapter.OnTechniciansSetListener {
                            override fun onTechnicianSelected(data: SupervisorTechnician) {
                                onTechnicianSelectedListener?.onTechnicianSelected(data)
                                dismiss()
                            }

                            override fun onTechnicianUnselected(data: SupervisorTechnician) {
                                onTechnicianSelectedListener?.onTechnicianUnselected(data)
                                dismiss()
                            }
                        })
                    }

                    binding.rvSupervisorsTechnicians.adapter = adapter
                    binding.rvSupervisorsTechnicians.setItemViewCacheSize(1000)
                    binding.rvSupervisorsTechnicians.layoutManager = LinearLayoutManager(context)
                    adapter.notifyDataSetChanged()
                }

                override fun onFailure(call: Call<SupervisorTechnicianListResponse>, t: Throwable) {
                    binding.loadingBar.visibility = View.GONE
                    dismiss()
                }
            }
        )
    }

    fun setOnTechnicianSelectedListener(listener: OnTechnicianSelectedListener) {
        onTechnicianSelectedListener = listener
    }

    interface OnTechnicianSelectedListener {
        fun onTechnicianSelected(data: SupervisorTechnician)
        fun onTechnicianUnselected(data: SupervisorTechnician)
    }
}
