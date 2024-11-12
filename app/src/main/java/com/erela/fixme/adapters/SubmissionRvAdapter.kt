package com.erela.fixme.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.databinding.ListItemSubmissionBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.SubmissionListResponse
import com.erela.fixme.objects.UserData

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
                inputTime.text = item.setTglinput
                submissionDescription.text = item.keterangan
                machineCode.text = item.kodeMesin
                machineName.text = item.namaMesin

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