package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.databinding.ListItemSubmissionBinding
import com.erela.fixme.objects.SubmissionListResponse
import java.util.Locale

class SubmissionRvAdapter(val context: Context, val data: ArrayList<SubmissionListResponse>) :
    RecyclerView.Adapter<SubmissionRvAdapter.ViewHolder>() {
    private lateinit var onSubmissionClickListener: OnSubmissionClickListener

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
                val item = data[position]

                submissionName.text = item.judulKasus
                inputDate.text = item.setTglinput
                noRequest.text = item.nomorRequest
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
                reportedBy.text = item.namaUser
                departmentFrom.text = item.deptUser

                complexityText.text =
                    item.complexity.toString().replaceFirstChar {
                        if (it.isLowerCase())
                            it.titlecase(Locale.getDefault())
                        else
                            it.toString()
                    }
                when (item.complexity) {
                    "low" -> {
                        complexity.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.custom_toast_font_success,
                                context.theme
                            )
                        )
                    }

                    "middle" -> {
                        complexity.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.custom_toast_font_warning,
                                context.theme
                            )
                        )
                    }

                    "high" -> {
                        complexity.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.custom_toast_font_failed,
                                context.theme
                            )
                        )
                    }

                    else -> {
                        complexity.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                    }
                }

                when (item.stsGaprojects) {
                    0 -> {
                        header.setBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_rejected,
                                context.theme
                            )
                        )
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
                        header.setBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_pending,
                                context.theme
                            )
                        )
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

                    11 -> {
                        header.setBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_waiting,
                                context.theme
                            )
                        )
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_waiting,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "Waiting"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                    }

                    2 -> {
                        header.setBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_approved,
                                context.theme
                            )
                        )
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

                    22 -> {
                        header.setBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_hold,
                                context.theme
                            )
                        )
                        submissionStatus.setCardBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_hold,
                                context.theme
                            )
                        )
                        submissionStatusText.text = "Hold"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                    }

                    3 -> {
                        header.setBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_on_progress,
                                context.theme
                            )
                        )
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
                        header.setBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_progress_done,
                                context.theme
                            )
                        )
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
                        header.setBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_done,
                                context.theme
                            )
                        )
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
                        header.setBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.custom_toast_background_failed,
                                context.theme
                            )
                        )
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
                        header.setBackgroundColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.status_on_trial,
                                context.theme
                            )
                        )
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