package com.erela.fixme.adapters.recycler_view

import android.app.Activity
import android.content.Context
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
import com.erela.fixme.R
import com.erela.fixme.adapters.pager.ImageCarouselPagerAdapter
import com.erela.fixme.databinding.ListItemProgressBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.networking.InitAPI
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

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ListItemProgressBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ListItemProgressBinding.inflate(LayoutInflater.from(context), parent, false).root
    )

    override fun getItemCount(): Int = data.size

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
                    dateTimeText.text = item.progress.tglWaktu
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
                    materialList.text = if (item.progress.material!!.isNotEmpty()) {
                        material
                    } else {
                        "No materials needed"
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
                            imageData.add(item.progress.foto[i]?.foto!!.toString())
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
                            .placeholder(R.drawable.image_placeholder)
                            .into(submissionImage)
                    } else {
                        expandShrinkButton.visibility = View.GONE
                        imageContainer.visibility = View.GONE
                        circleIndicator.visibility = View.GONE
                    }
                }

                imageCarouselHolder.setOnLongClickListener {
                    if (userData.id == item.progress?.idUser)
                        if (detail.stsGaprojects == 3 && item.progress.stsDetail == 0)
                            onItemHoldTapListener.onItemHoldTap(item.progress)
                    true
                }

                submissionImage.setOnLongClickListener {
                    if (userData.id == item.progress?.idUser)
                        if (detail.stsGaprojects == 3 && item.progress.stsDetail == 0)
                            onItemHoldTapListener.onItemHoldTap(item.progress)
                    true
                }

                itemView.setOnLongClickListener {
                    if (userData.id == item.progress?.idUser)
                        if (detail.stsGaprojects == 3 && item.progress.stsDetail == 0)
                            onItemHoldTapListener.onItemHoldTap(item.progress)
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
        fun onItemHoldTap(item: ProgressItem?)
    }
}