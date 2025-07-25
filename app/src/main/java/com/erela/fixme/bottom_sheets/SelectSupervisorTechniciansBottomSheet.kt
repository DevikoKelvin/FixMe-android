package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.SupervisorTechniciansRvAdapter
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.BsSelectTechniciansBinding
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.SelectedSupervisorTechniciansList
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.SupervisorTechnicianListResponse
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SelectSupervisorTechniciansBottomSheet(
    context: Context, private val detailData: SubmissionDetailResponse,
    private val selectedSupervisorTechniciansList: ArrayList<SupervisorTechnicianListResponse>,
    private val isForManager: Boolean
) : BottomSheetDialog(context), SupervisorTechniciansRvAdapter.OnTechniciansSetListener,
    SupervisorTechniciansRvAdapter.OnSupervisorSetListener {
    private val binding: BsSelectTechniciansBinding by lazy {
        BsSelectTechniciansBinding.inflate(layoutInflater)
    }
    private lateinit var onSelectTechniciansListener: OnSelectTechniciansListener
    private lateinit var onSelectSupervisorListener: OnSelectSupervisorListener
    val supervisorsList: ArrayList<SelectedSupervisorTechniciansList> = ArrayList()
    val techniciansList: ArrayList<SelectedSupervisorTechniciansList> = ArrayList()
    private lateinit var adapter: SupervisorTechniciansRvAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun init() {
        binding.apply {
            loadingBar.visibility = View.VISIBLE
            try {
                if (isForManager) {
                    title.text = context.getString(R.string.select_supervisor)
                    InitAPI.getAPI.getSupervisorList(detailData.idGaprojects!!).enqueue(
                        object : Callback<List<SupervisorTechnicianListResponse>> {
                            override fun onResponse(
                                call: Call<List<SupervisorTechnicianListResponse>>,
                                response: Response<List<SupervisorTechnicianListResponse>>
                            ) {
                                loadingBar.visibility = View.GONE
                                if (response.isSuccessful) {
                                    if (response.body() != null) {
                                        for (i in 0 until response.body()!!.size) {
                                            supervisorsList.add(
                                                SelectedSupervisorTechniciansList(
                                                    false,
                                                    response.body()!![i]
                                                )
                                            )
                                        }
                                        adapter = SupervisorTechniciansRvAdapter(
                                            context,
                                            supervisorsList,
                                            selectedSupervisorTechniciansList,
                                            isForManager
                                        ).also {
                                            with(it) {
                                                setOnSupervisorSetListener(
                                                    this@SelectSupervisorTechniciansBottomSheet
                                                )
                                            }
                                        }
                                        rvSupervisorsTechnicians.adapter = adapter
                                        rvSupervisorsTechnicians.setItemViewCacheSize(1000)
                                        rvSupervisorsTechnicians.layoutManager =
                                            LinearLayoutManager(context)
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
                                            .setMessage(
                                                if (context.getString(R.string.lang) == "in")
                                                    "Tidak dapat mengambil daftar supervisor."
                                                else
                                                    "Can't retrieve supervisor list."
                                            )
                                            .show()
                                        Log.e("ERROR Selecting SPV Body null", response.message().toString())
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
                                        .setMessage(
                                            if (context.getString(R.string.lang) == "in")
                                                "Tidak dapat mengambil daftar supervisor."
                                            else
                                                "Can't retrieve supervisor list."
                                        )
                                        .show()
                                    Log.e("ERROR Selecting SPV Unsuccessful", response.message().toString())
                                    dismiss()
                                }
                            }

                            override fun onFailure(
                                call: Call<List<SupervisorTechnicianListResponse>>,
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
                                    .setMessage(
                                        if (context.getString(R.string.lang) == "in")
                                            "Tidak dapat mengambil daftar supervisor."
                                        else
                                            "Can't retrieve supervisor list."
                                    )
                                    .show()
                                Log.e("ERROR Selecting SPV Failure", throwable.toString())
                                throwable.printStackTrace()
                                dismiss()
                            }
                        }
                    )
                } else {
                    title.text = context.getString(R.string.select_technicians)
                    InitAPI.getAPI.getTechnicianList(detailData.idGaprojects!!).enqueue(
                        object : Callback<List<SupervisorTechnicianListResponse>> {
                            override fun onResponse(
                                call: Call<List<SupervisorTechnicianListResponse>>,
                                response: Response<List<SupervisorTechnicianListResponse>>
                            ) {
                                loadingBar.visibility = View.GONE
                                if (response.isSuccessful) {
                                    if (response.body() != null) {
                                        for (i in 0 until response.body()!!.size) {
                                            techniciansList.add(
                                                SelectedSupervisorTechniciansList(
                                                    false,
                                                    response.body()!![i]
                                                )
                                            )
                                        }
                                        adapter = SupervisorTechniciansRvAdapter(
                                            context,
                                            techniciansList,
                                            selectedSupervisorTechniciansList,
                                            isForManager
                                        ).also {
                                            with(it) {
                                                setOnTechniciansSetListener(
                                                    this@SelectSupervisorTechniciansBottomSheet
                                                )
                                            }
                                        }
                                        rvSupervisorsTechnicians.adapter = adapter
                                        rvSupervisorsTechnicians.setItemViewCacheSize(1000)
                                        rvSupervisorsTechnicians.layoutManager =
                                            LinearLayoutManager(context)
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
                                            .setMessage(
                                                if (context.getString(R.string.lang) == "in")
                                                    "Tidak dapat mengambil daftar teknisi."
                                                else
                                                    "Can't retrieve technicians list."
                                            )
                                            .show()
                                        Log.e("ERROR Deploying Tech Body null", response.message().toString())
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
                                        .setMessage(
                                            if (context.getString(R.string.lang) == "in")
                                                "Tidak dapat mengambil daftar teknisi."
                                            else
                                                "Can't retrieve technicians list."
                                        )
                                        .show()
                                    Log.e("ERROR Deploying Tech Unsuccessful", response.message().toString())
                                    dismiss()
                                }
                            }

                            override fun onFailure(
                                call: Call<List<SupervisorTechnicianListResponse>>,
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
                                    .setMessage(
                                        if (context.getString(R.string.lang) == "in")
                                            "Tidak dapat mengambil daftar teknisi."
                                        else
                                            "Can't retrieve technicians list."
                                    )
                                    .show()
                                Log.e("ERROR Deploying Tech Failure", throwable.toString())
                                throwable.printStackTrace()
                                dismiss()
                            }
                        }
                    )
                }
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
                    .setMessage(
                        if (context.getString(R.string.lang) == "in")
                            "Tidak dapat mengambil daftar pengguna."
                        else
                            "Can't retrieve user list."
                    )
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

    fun setOnSelectSupervisorsListener(onSelectSupervisorListener: OnSelectSupervisorListener) {
        this.onSelectSupervisorListener = onSelectSupervisorListener
    }

    interface OnSelectTechniciansListener {
        fun onTechnicianSelected(data: SupervisorTechnicianListResponse)
        fun onTechnicianUnselected(data: SupervisorTechnicianListResponse)
    }

    interface OnSelectSupervisorListener {
        fun onSupervisorSelected(data: SupervisorTechnicianListResponse)
        fun onSupervisorUnselected(data: SupervisorTechnicianListResponse)
    }

    override fun onTechnicianSelected(data: SupervisorTechnicianListResponse) {
        onSelectTechniciansListener.onTechnicianSelected(data)
    }

    override fun onTechnicianUnselected(data: SupervisorTechnicianListResponse) {
        onSelectTechniciansListener.onTechnicianUnselected(data)
    }

    override fun onSupervisorsSelected(data: SupervisorTechnicianListResponse) {
        onSelectSupervisorListener.onSupervisorSelected(data)
    }

    override fun onSupervisorsUnselected(data: SupervisorTechnicianListResponse) {
        onSelectSupervisorListener.onSupervisorUnselected(data)
    }
}