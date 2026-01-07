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

                complexity.visibility = if (item.complexity == "null" || item.complexity == null)
                    View.GONE
                else
                    View.VISIBLE

                complexityText.text =
                    item.complexity.toString().replaceFirstChar {
                        if (it.isLowerCase())
                            it.titlecase(Locale.getDefault())
                        else
                            it.toString()
                    }
                when (item.complexity) {
                    "low" -> {
                        complexityColor.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_low_complexity,
                            context.theme
                        )
                    }

                    "middle" -> {
                        complexityColor.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_middle_complexity,
                            context.theme
                        )
                    }

                    "high" -> {
                        complexityColor.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_high_complexity,
                            context.theme
                        )
                    }

                    else -> {
                        complexityColor.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_logout_color,
                            context.theme
                        )
                    }
                }

                when (item.stsGaprojects) {
                    0 -> {
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_rejected_color,
                            context.theme
                        )
                        statusColor.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_rejected_color,
                            context.theme
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
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_pending_color,
                            context.theme
                        )
                        statusColor.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_pending_color,
                            context.theme
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
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_waiting_color,
                            context.theme
                        )
                        statusColor.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_waiting_color,
                            context.theme
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
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_approved_color,
                            context.theme
                        )
                        statusColor.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_approved_color,
                            context.theme
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
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_hold_color,
                            context.theme
                        )
                        statusColor.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_hold_color,
                            context.theme
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
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_on_progress_color,
                            context.theme
                        )
                        statusColor.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_on_progress_color,
                            context.theme
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
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_progress_done_color,
                            context.theme
                        )
                        statusColor.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_progress_done_color,
                            context.theme
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
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_done_color,
                            context.theme
                        )
                        statusColor.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_done_color,
                            context.theme
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
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_canceled_color,
                            context.theme
                        )
                        statusColor.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_canceled_color,
                            context.theme
                        )
                        submissionStatusText.text = "Canceled"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                    }

                    31 -> {
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_on_trial_color,
                            context.theme
                        )
                        statusColor.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_on_trial_color,
                            context.theme
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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemSubmissionBinding.bind(view)
    }

    fun onSubmissionClickListener(onSubmissionClickListener: OnSubmissionClickListener) {
        this.onSubmissionClickListener = onSubmissionClickListener
    }

    interface OnSubmissionClickListener {
        fun onSubmissionClick(data: SubmissionListResponse)
    }
}