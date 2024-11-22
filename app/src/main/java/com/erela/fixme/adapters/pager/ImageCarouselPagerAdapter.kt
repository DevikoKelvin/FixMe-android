package com.erela.fixme.adapters.pager

import android.annotation.SuppressLint
import com.erela.fixme.R
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.erela.fixme.custom_views.zoom_helper.ImageZoomHelper
import com.erela.fixme.databinding.ViewImageCarouselBinding
import com.erela.fixme.helpers.Base64Helper
import com.erela.fixme.helpers.InitAPI
import com.erela.fixme.objects.FotoGaprojectsItem

class ImageCarouselPagerAdapter(
    private val context: Context,
    private val imageUrlArrayList: ArrayList<FotoGaprojectsItem>,
    private val activity: Activity,
) : PagerAdapter() {
    private lateinit var binding: ViewImageCarouselBinding

    companion object {
        const val WITH_FULL_URL = 2
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        /*return super.instantiateItem(container, position)*/
        val data = imageUrlArrayList[position]

        binding = ViewImageCarouselBinding.bind(
            LayoutInflater.from(context).inflate(R.layout.view_image_carousel, null)
        )

        binding.apply {
            val imageZoomHelper = ImageZoomHelper(activity, displayImage)
            displayImage.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(
                    v: View?, event: MotionEvent?
                ): Boolean {
                    imageZoomHelper.init(event!!)
                    return true
                }
            })

            try {
                if (Base64Helper.isBase64Encoded(data.foto.toString())) {
                    val decodedImageURL = Base64Helper.decodeBase64(
                        data.foto.toString()
                    )
                    Glide.with(context)
                        .load(decodedImageURL)
                        .placeholder(R.drawable.image_placeholder)
                        .into(displayImage)
                } else {
                    Glide.with(context)
                        .load(InitAPI.IMAGE_URL + data.foto)
                        .placeholder(R.drawable.image_placeholder)
                        .into(displayImage)
                }
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            }
            container.addView(binding.root)
        }

        return binding.root
    }

    override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
        container.removeView(view as View)
    }

    override fun getItemPosition(`object`: Any): Int = POSITION_NONE

    override fun getCount(): Int = imageUrlArrayList.size

    override fun isViewFromObject(view: View, `object`: Any): Boolean = `object` == view
}