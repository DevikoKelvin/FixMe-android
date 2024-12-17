package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.databinding.ListItemSubmissionBinding
import com.erela.fixme.helpers.networking.InitAPI
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.SubmissionListResponse
import com.erela.fixme.objects.UserData
import com.erela.fixme.objects.UserDetailResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SubmissionRvAdapter(val context: Context, val data: ArrayList<SubmissionListResponse>) :
    RecyclerView.Adapter<SubmissionRvAdapter.ViewHolder>() {
    private lateinit var onSubmissionClickListener: OnSubmissionClickListener
    private lateinit var userData: UserData

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ListItemSubmissionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ).root
    )

    override fun getItemCount(): Int = data.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            binding.apply {
                userData = UserDataHelper(context).getUserData()
                val item = data[position]

                submissionName.text = item.judulKasus
                inputDate.text = item.setTglinput
                submissionDescription.text = item.keterangan
                machineCodeText.text = "${context.getString(R.string.machine_code)}:"
                machineNameText.text = "${context.getString(R.string.machine_name)}:"
                machineCode.text = if (item.kodeMesin != null) {
                    item.kodeMesin.ifEmpty { "-" }
                } else {
                    "-"
                }

                machineName.text = if (item.namaMesin != null) {
                    item.namaMesin.ifEmpty { "-" }
                } else {
                    "-"
                }
                submissionLocation.text = item.lokasi?.uppercase()
                reportedBy.text = ""
                try {
                    InitAPI.getAPI.getUserDetail(item.idUser!!)
                        .enqueue(object : Callback<UserDetailResponse> {
                            override fun onResponse(
                                call: Call<UserDetailResponse?>,
                                response: Response<UserDetailResponse?>
                            ) {
                                if (response.isSuccessful) {
                                    if (response.body() != null) {
                                        reportedBy.text = response.body()?.nama
                                    }
                                } else {
                                    reportedBy.text = "Can't retrieve Reporter's name"
                                    Log.e("ERROR", response.message())
                                }
                            }

                            override fun onFailure(
                                call: Call<UserDetailResponse?>,
                                throwable: Throwable
                            ) {
                                reportedBy.text = "Can't retrieve Reporter's name"
                                Log.e("ERROR", throwable.toString())
                                throwable.printStackTrace()
                            }
                        })
                } catch (exception: Exception) {
                    reportedBy.text = "Can't retrieve Reporter's name"
                    Log.e("ERROR", exception.toString())
                    exception.printStackTrace()
                }

                when (item.stsGaprojects) {
                    0 -> {
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_rejected,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "Rejected"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                    }

                    1 -> {
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_pending,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "Pending"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                    }

                    2 -> {
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_approved,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "Approved"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                    }

                    3 -> {
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_on_progress,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "On Progress"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                    }

                    30 -> {
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_progress_done,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "Progress Done"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                    }

                    4 -> {
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_done,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "Done"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                    }

                    5 -> {
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.custom_toast_background_failed,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "Canceled"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.custom_toast_font_failed,
                                context.theme
                            )
                        )
                    }

                    31 -> {
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_on_trial,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "On Trial"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                    }
                }

                itemView.setOnClickListener {
                    onSubmissionClickListener.onSubmissionClick(item)
                }
            }
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemSubmissionBinding.bind(view)
    }

    fun onSubmissionClickListener(onSubmissionClickListener: OnSubmissionClickListener) {
        this.onSubmissionClickListener = onSubmissionClickListener
    }

    interface OnSubmissionClickListener {
        fun onSubmissionClick(data: SubmissionListResponse)
    }
}