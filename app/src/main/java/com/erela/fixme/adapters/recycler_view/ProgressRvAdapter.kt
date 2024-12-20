package com.erela.fixme.adapters.recycler_view

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.erela.fixme.R
import com.erela.fixme.adapters.pager.ImageCarouselPagerAdapter
import com.erela.fixme.databinding.ListItemProgressBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.networking.InitAPI
import com.erela.fixme.objects.ProgressItem
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.UserData

class ProgressRvAdapter(
    val context: Context, val activity: Activity, private val detail: SubmissionDetailResponse, val data: List<ProgressItem?>?
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

    override fun getItemCount(): Int = data!!.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data?.get(position)

        with(holder) {
            binding.apply {
                if (item != null) {
                    usernameText.background = if (item.stsDetail == 1)
                        ContextCompat.getDrawable(context, R.color.status_approved)
                    else
                        ContextCompat.getDrawable(context, R.color.highlight_blue)
                    usernameText.text = item.namaUserProgress?.trimEnd()
                    progressDescription.text = item.keterangan
                    dateTimeText.text = item.tglWaktu
                    if (item.foto?.size!! > 1) {
                        imageCarouselHolder.visibility = View.VISIBLE
                        circleIndicator.visibility = View.VISIBLE
                        submissionImage.visibility = View.GONE
                        val imageData = ArrayList<String>()
                        for (i in 0 until item.foto.size) {
                            imageData.add(item.foto[i]?.foto!!.toString())
                        }
                        imageCarouselAdapter = ImageCarouselPagerAdapter(
                            context, imageData, activity
                        )
                        imageCarouselHolder.adapter = imageCarouselAdapter
                        circleIndicator.setViewPager(imageCarouselHolder)
                        imageCarouselAdapter.registerDataSetObserver(
                            circleIndicator.dataSetObserver
                        )
                    } else if (item.foto.size == 1) {
                        imageCarouselHolder.visibility = View.GONE
                        circleIndicator.visibility = View.GONE
                        submissionImage.visibility = View.VISIBLE
                        val image = item.foto[0]?.foto
                        Glide.with(context)
                            .load(InitAPI.IMAGE_URL + image)
                            .placeholder(R.drawable.image_placeholder)
                            .into(submissionImage)
                    } else {
                        imageContainer.visibility = View.GONE
                        circleIndicator.visibility = View.GONE
                    }
                }

                imageCarouselHolder.setOnLongClickListener {
                    if (userData.id == item?.idUser)
                        if (detail.stsGaprojects == 3 && item.stsDetail == 0)
                            onItemHoldTapListener.onItemHoldTap(item)
                    true
                }

                submissionImage.setOnLongClickListener {
                    if (userData.id == item?.idUser)
                        if (detail.stsGaprojects == 3 && item.stsDetail == 0)
                            onItemHoldTapListener.onItemHoldTap(item)
                    true
                }

                itemView.setOnLongClickListener {
                    if (userData.id == item?.idUser)
                        if (detail.stsGaprojects == 3 && item.stsDetail == 0)
                            onItemHoldTapListener.onItemHoldTap(item)
                    true
                }
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