package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.databinding.ListItemSubmissionBinding
import com.erela.fixme.objects.SubmissionListResponse
import java.util.Locale

class SubmissionRvAdapter(val context: Context, val data: ArrayList<SubmissionListResponse>) :
    RecyclerView.Adapter<SubmissionRvAdapter.ViewHolder>() {
    private lateinit var onSubmissionClickListener: OnSubmissionClickListener

    private fun setRoundedBackground(view: View, drawableId: Int) {
        view.background = ResourcesCompat.getDrawable(
            context.resources,
            drawableId,
            context.theme
        )
    }

    private fun setProgressTextColor(textView: TextView, progress: String?) {
        textView.text = progress
        var isComplete = false
        if (!progress.isNullOrEmpty()) {
            try {
                // "Progress: 0 / 0"
                val parts = progress.substringAfter("Progress:").split("/")
                if (parts.size == 2) {
                    val current = parts[0].trim().toInt()
                    val total = parts[1].trim().toInt()
                    if (total != 0 && current != 0) {
                        if (current == total) {
                            isComplete = true
                        }
                    }
                }
            } catch (exception: Exception) {
                Log.e("ERROR", exception.toString())
            }
        }

        val colorRes = if (isComplete) R.color.status_approved else R.color.black
        textView.setTextColor(
            ResourcesCompat.getColor(context.resources, colorRes, context.theme)
        )
    }

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
                        setRoundedBackground(complexityColor, R.drawable.gradient_low_complexity)
                    }

                    "middle" -> {
                        setRoundedBackground(complexityColor, R.drawable.gradient_middle_complexity)
                    }

                    "high" -> {
                        setRoundedBackground(complexityColor, R.drawable.gradient_high_complexity)
                    }

                    else -> {
                        setRoundedBackground(complexityColor, R.drawable.gradient_logout_color)
                    }
                }

                when (item.stsGaprojects) {
                    0 -> {
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_rejected_color,
                            context.theme
                        )
                        setRoundedBackground(statusColor, R.drawable.gradient_rejected_color)
                        submissionStatusText.text = "Rejected"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                        progressCountText.visibility = View.GONE
                    }

                    1 -> {
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_pending_color,
                            context.theme
                        )
                        setRoundedBackground(statusColor, R.drawable.gradient_pending_color)
                        submissionStatusText.text = "Pending"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                        progressCountText.visibility = View.GONE
                    }

                    11 -> {
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_waiting_color,
                            context.theme
                        )
                        setRoundedBackground(statusColor, R.drawable.gradient_waiting_color)
                        submissionStatusText.text = "Waiting"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                        progressCountText.visibility = View.GONE
                    }

                    2 -> {
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_approved_color,
                            context.theme
                        )
                        setRoundedBackground(statusColor, R.drawable.gradient_approved_color)
                        submissionStatusText.text = "Approved"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                        progressCountText.visibility = View.GONE
                    }

                    22 -> {
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_hold_color,
                            context.theme
                        )
                        setRoundedBackground(statusColor, R.drawable.gradient_hold_color)
                        submissionStatusText.text = "Hold"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                        progressCountText.visibility = View.VISIBLE
                        setProgressTextColor(progressCountText, item.countProgress)
                    }

                    3 -> {
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_on_progress_color,
                            context.theme
                        )
                        setRoundedBackground(statusColor, R.drawable.gradient_on_progress_color)
                        submissionStatusText.text = "On Progress"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                        progressCountText.visibility = View.VISIBLE
                        setProgressTextColor(progressCountText, item.countProgress)
                    }

                    30 -> {
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_progress_done_color,
                            context.theme
                        )
                        setRoundedBackground(statusColor, R.drawable.gradient_progress_done_color)
                        submissionStatusText.text = "Progress Done"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                        progressCountText.visibility = View.GONE
                    }

                    4 -> {
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_done_color,
                            context.theme
                        )
                        setRoundedBackground(statusColor, R.drawable.gradient_done_color)
                        submissionStatusText.text = "Done"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                        progressCountText.visibility = View.GONE
                    }

                    5 -> {
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_canceled_color,
                            context.theme
                        )
                        setRoundedBackground(statusColor, R.drawable.gradient_canceled_color)
                        submissionStatusText.text = "Canceled"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                        progressCountText.visibility = View.GONE
                    }

                    31 -> {
                        header.background = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.gradient_on_trial_color,
                            context.theme
                        )
                        setRoundedBackground(statusColor, R.drawable.gradient_on_trial_color)
                        submissionStatusText.text = "On Trial"
                        submissionStatusText.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.white,
                                context.theme
                            )
                        )
                        progressCountText.visibility = View.GONE
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