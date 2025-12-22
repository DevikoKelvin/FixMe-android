package com.erela.fixme.adapters.recycler_view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.erela.fixme.R
import com.erela.fixme.adapters.pager.ImageCarouselPagerAdapter
import com.erela.fixme.databinding.ListItemProgressBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.ProgressItem
import com.erela.fixme.objects.ProgressItems
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData

class ProgressRvAdapter(
    val context: Context, val activity: Activity, private val detail: SubmissionDetailResponse,
    val data: ArrayList<ProgressItems>
) : RecyclerView.Adapter<ProgressRvAdapter.ViewHolder>() {
    private lateinit var imageCarouselAdapter: ImageCarouselPagerAdapter
    private lateinit var onItemHoldTapListener: OnItemHoldTapListener
    private val userData: UserData by lazy {
        UserDataHelper(context).getUserData()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemProgressBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ListItemProgressBinding.inflate(LayoutInflater.from(context), parent, false).root
    )

    override fun getItemCount(): Int = data.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        with(holder) {
            binding.apply {
                if (item.progress != null) {
                    usernameText.background = if (item.progress.stsDetail == 1)
                        ContextCompat.getDrawable(context, R.color.status_approved)
                    else
                        ContextCompat.getDrawable(context, R.color.highlight_blue)
                    usernameText.text = item.progress.namaUserProgress?.trimEnd()
                    progressAnalysis.text = item.progress.analisa
                    progressDescription.text = item.progress.keterangan
                    dateTimeText.text = item.progress.tglWaktu?.replace(
                        Regex("(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2})"),
                        "$3-$2-$1 $4:$5:$6"
                    ) ?: item.progress.tglWaktu
                    val material = StringBuilder().also {
                        with(it) {
                            for (i in 0 until item.progress.material!!.size) {
                                if (i < item.progress.material.size - 1)
                                    append(
                                        "${item.progress.material[i]?.namaMaterial} " +
                                                "(${item.progress.material[i]?.qtyMaterial} Pcs)\n"
                                    )
                                else
                                    append(
                                        "${item.progress.material[i]?.namaMaterial} " +
                                                "(${item.progress.material[i]?.qtyMaterial} Pcs)"
                                    )
                            }
                        }
                    }
                    materialTitle.visibility = View.VISIBLE
                    materialList.visibility = View.VISIBLE
                    val materials = item.progress.material
                    if (materials != null) {
                        if (materials.isNotEmpty()) {
                            if (item.progress.approveMaterialStatus == 1) {
                                materialApprovedStatus.visibility = View.VISIBLE
                                materialApprovalIcon.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        context,
                                        R.drawable.baseline_check_circle_outline_24
                                    )
                                )
                                materialApprovalMessage.text =
                                    if (context.getString(R.string.lang) == "in")
                                        "Material disetujui oleh ${
                                            item.progress.approveMaterialUser
                                        } pada ${item.progress.approveMaterialTglWaktu}"
                                    else
                                        "Material approved by ${
                                            item.progress.approveMaterialUser
                                        } on ${item.progress.approveMaterialTglWaktu}"
                                materialApprovalMessage.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.custom_toast_font_success
                                    )
                                )
                            } else {
                                materialApprovedStatus.visibility = View.VISIBLE
                                materialApprovalIcon.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        context,
                                        R.drawable.outline_cancel_24
                                    )
                                )
                                materialApprovalMessage.text =
                                    if (context.getString(R.string.lang) == "in")
                                        "Material butuh persetujuan oleh Supervisor sebelum bisa diselesaikan."
                                    else
                                        "Material need to be approved by Supervisors before it can be done."
                                materialApprovalMessage.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.custom_toast_font_failed
                                    )
                                )
                            }
                        } else
                            materialApprovedStatus.visibility = View.GONE
                        materialList.text = if (materials.isNotEmpty())
                            material
                        else {
                            if (context.getString(R.string.lang) == "in")
                                "Tidak ada material yang dibutuhkan"
                            else
                                "No materials needed"
                        }
                    }

                    arrowExpandShrink.rotation = if (item.isExpanded) 90f else 270f
                    progressDoneContainer.visibility = if (item.isExpanded) View.VISIBLE else
                        View.GONE

                    if (item.progress.foto?.size!! > 1) {
                        expandShrinkButton.visibility = View.VISIBLE
                        imageCarouselHolder.visibility = View.VISIBLE
                        circleIndicator.visibility = View.VISIBLE
                        submissionImage.visibility = View.GONE
                        showHideView(
                            mainContainer,
                            progressDoneContainer,
                            mainContainer,
                            arrowExpandShrink,
                            item
                        )
                        showHideView(
                            progressDoneContainer,
                            progressDoneContainer,
                            mainContainer,
                            arrowExpandShrink,
                            item
                        )
                        showHideView(
                            imageCarouselHolder,
                            progressDoneContainer,
                            mainContainer,
                            arrowExpandShrink,
                            item
                        )
                        val imageData = ArrayList<String>()
                        for (i in 0 until item.progress.foto.size) {
                            imageData.add(item.progress.foto[i]?.foto!!)
                        }
                        imageCarouselAdapter = ImageCarouselPagerAdapter(
                            context, imageData, activity
                        )
                        imageCarouselHolder.adapter = imageCarouselAdapter
                        circleIndicator.setViewPager(imageCarouselHolder)
                        imageCarouselAdapter.registerDataSetObserver(
                            circleIndicator.dataSetObserver
                        )
                    } else if (item.progress.foto.size == 1) {
                        expandShrinkButton.visibility = View.VISIBLE
                        imageCarouselHolder.visibility = View.GONE
                        circleIndicator.visibility = View.GONE
                        submissionImage.visibility = View.VISIBLE
                        showHideView(
                            mainContainer,
                            progressDoneContainer,
                            mainContainer,
                            arrowExpandShrink,
                            item
                        )
                        showHideView(
                            progressDoneContainer,
                            progressDoneContainer,
                            mainContainer,
                            arrowExpandShrink,
                            item
                        )
                        showHideView(
                            submissionImage,
                            progressDoneContainer,
                            mainContainer,
                            arrowExpandShrink,
                            item
                        )
                        val image = item.progress.foto[0]?.foto
                        Glide.with(context)
                            .load(InitAPI.IMAGE_URL + image)
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: com.bumptech.glide.request.target.Target<Drawable?>?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    Log.e("ProgressImage", "Glide onLoadFailed: ", e)
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable?,
                                    model: Any?,
                                    target: Target<Drawable?>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    return false
                                }
                            })
                            .placeholder(R.drawable.image_placeholder)
                            .into(submissionImage)
                    } else {
                        expandShrinkButton.visibility = View.GONE
                        imageContainer.visibility = View.GONE
                        circleIndicator.visibility = View.GONE
                    }
                    progressResultDescription.text =
                        if (item.progress.keteranganApprove != "null" || item.progress.keteranganApprove != null)
                            item.progress.keteranganApprove
                        else {
                            if (context.getString(R.string.lang) == "in")
                                "Tidak ada keterangan"
                            else
                                "No remarks"
                        }
                }

                imageCarouselHolder.setOnLongClickListener {
                    if (userData.id == item.progress?.idUser) {
                        if (detail.stsGaprojects == 3 && item.progress.stsDetail == 0)
                            onItemHoldTapListener.onItemHoldTap(item.progress, false)
                    } else {
                        for (i in 0 until detail.usernUserSpv!!.size) {
                            if (userData.id == detail.usernUserSpv[i]?.idUser) {
                                if (detail.stsGaprojects == 3 && item.progress?.stsDetail == 0 &&
                                    item.progress.approveMaterialStatus == 0
                                )
                                    onItemHoldTapListener.onItemHoldTap(item.progress, true)
                                break
                            }
                        }
                    }
                    true
                }

                submissionImage.setOnLongClickListener {
                    if (userData.id == item.progress?.idUser) {
                        if (detail.stsGaprojects == 3 && item.progress.stsDetail == 0)
                            onItemHoldTapListener.onItemHoldTap(item.progress, false)
                    } else {
                        for (i in 0 until detail.usernUserSpv!!.size) {
                            if (userData.id == detail.usernUserSpv[i]?.idUser) {
                                if (detail.stsGaprojects == 3 && item.progress?.stsDetail == 0 &&
                                    item.progress.approveMaterialStatus == 0
                                )
                                    onItemHoldTapListener.onItemHoldTap(item.progress, true)
                                break
                            }
                        }
                    }
                    true
                }

                itemView.setOnLongClickListener {
                    if (userData.id == item.progress?.idUser) {
                        if (detail.stsGaprojects == 3 && item.progress.stsDetail == 0)
                            onItemHoldTapListener.onItemHoldTap(item.progress, false)
                    } else {
                        for (i in 0 until detail.usernUserSpv!!.size) {
                            if (userData.id == detail.usernUserSpv[i]?.idUser) {
                                if (detail.stsGaprojects == 3 && item.progress?.stsDetail == 0
                                )
                                    onItemHoldTapListener.onItemHoldTap(item.progress, true)
                                break
                            }
                        }
                    }
                    true
                }
            }
        }
    }

    private fun showHideView(
        actionView: View, view: LinearLayout, parentView: CardView, arrow: ImageView,
        item: ProgressItems
    ) {
        actionView.setOnClickListener {
            if (!item.isExpanded) {
                item.isExpanded = true
                TransitionManager.beginDelayedTransition(parentView)
                arrow.rotation = 90f
                view.visibility = View.VISIBLE
            } else {
                item.isExpanded = false
                view.visibility = View.GONE
                arrow.rotation = 270f
                TransitionManager.beginDelayedTransition(parentView)
            }
        }
    }

    fun setOnItemHoldTapListener(onItemHoldTapListener: OnItemHoldTapListener) {
        this.onItemHoldTapListener = onItemHoldTapListener
    }

    interface OnItemHoldTapListener {
        fun onItemHoldTap(item: ProgressItem?, forSpv: Boolean)
    }
}