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
import com.erela.fixme.helpers.InitAPI
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.UsernameFormatHelper
import com.erela.fixme.objects.SubmissionListResponse
import com.erela.fixme.objects.UserData
import com.erela.fixme.objects.UserListResponse
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
                machineCode.text = item.kodeMesin
                machineName.text = item.namaMesin
                submissionLocation.text = item.lokasi?.uppercase()
                reportedBy.text = ""
                try {
                    InitAPI.getAPI.getUserList()
                        .enqueue(object : Callback<List<UserListResponse>> {
                            override fun onResponse(
                                call: Call<List<UserListResponse>?>,
                                response: Response<List<UserListResponse>?>
                            ) {
                                if (response.isSuccessful) {
                                    if (response.body() != null) {
                                        for (i in 0 until response.body()!!.size) {
                                            if (item.idUser == response.body()?.get(i)?.idUser) {
                                                reportedBy.text =
                                                    UsernameFormatHelper.getRealUsername(
                                                        response.body()?.get(i)?.usern.toString()
                                                    )
                                            }
                                        }
                                    }
                                } else {
                                    reportedBy.text = "Can't retrieve Reporter's name"
                                    Log.e("ERROR", response.message())
                                }
                            }

                            override fun onFailure(
                                call: Call<List<UserListResponse>?>,
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
                    0.toString() -> {
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_rejected,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "Rejected"
                    }

                    1.toString() -> {
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_pending,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "Pending"
                    }

                    2.toString() -> {
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_approved,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "Approved"
                    }

                    3.toString() -> {
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_on_progress,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "On Progress"
                    }

                    4.toString() -> {
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_done,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "Done"
                    }

                    31.toString() -> {
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_on_trial,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "On Trial"
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