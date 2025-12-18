package com.erela.fixme.adapters.pager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.erela.fixme.R
import com.erela.fixme.custom_views.zoom_helper.ImageZoomHelper
import com.erela.fixme.databinding.ViewImageCarouselBinding
import com.erela.fixme.helpers.Base64Helper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.FotoGaprojectsItem

class ImageCarouselPagerAdapter(
    private val context: Context,
    private val activity: Activity,
) : PagerAdapter() {
    private lateinit var binding: ViewImageCarouselBinding
    private var imageUrlArrayList: ArrayList<FotoGaprojectsItem>? = null
    private var imageArrayList: List<String>? = null

    constructor(
        context: Context, imageUrlArrayList: ArrayList<FotoGaprojectsItem>, activity: Activity
    ) : this(context, activity) {
        this.imageUrlArrayList = imageUrlArrayList
    }

    constructor(
        context: Context, imageArrayList: List<String>, activity: Activity
    ) : this(context, activity) {
        this.imageArrayList = imageArrayList
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        binding = ViewImageCarouselBinding.bind(
            LayoutInflater.from(context).inflate(R.layout.view_image_carousel, null)
        )

        if (imageUrlArrayList != null) {
            val data = imageUrlArrayList?.get(position)

            binding.apply {
                val imageZoomHelper = ImageZoomHelper(activity, displayImage)
                displayImage.setOnTouchListener { _, event ->
                    imageZoomHelper.init(event!!)
                    true
                }

                try {
                    if (data?.foto != null) {
                        if (Base64Helper.isBase64Encoded(data.foto)) {
                            val decodedImageURL = Base64Helper.decodeBase64(
                                data.foto
                            )
                            Glide.with(context)
                                .load(decodedImageURL)
                                .listener(object : RequestListener<Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<Drawable?>?,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        Log.e("ImageCarousel", "Glide onLoadFailed: ", e)
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
                                .into(displayImage)
                        } else {
                            Glide.with(context)
                                .load(InitAPI.IMAGE_URL + data.foto)
                                .listener(object : RequestListener<Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<Drawable?>?,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        Log.e("ImageCarousel", "Glide onLoadFailed: ", e)
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
                                .into(displayImage)
                        }
                    } else {
                        displayImage.setImageResource(R.drawable.image_placeholder)
                    }
                } catch (e: OutOfMemoryError) {
                    e.printStackTrace()
                }
                container.addView(binding.root)
            }
        } else {
            val data = imageArrayList?.get(position)

            binding.apply {
                val imageZoomHelper = ImageZoomHelper(activity, displayImage)
                displayImage.setOnTouchListener { _, event ->
                    imageZoomHelper.init(event!!)
                    true
                }

                try {
                    if (data != null) {
                        if (Base64Helper.isBase64Encoded(data)) {
                            val decodedImageURL = Base64Helper.decodeBase64(
                                data
                            )
                            Glide.with(context)
                                .load(decodedImageURL)
                                .listener(object : RequestListener<Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<Drawable?>?,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        Log.e("ImageCarousel", "Glide onLoadFailed: ", e)
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
                                .into(displayImage)
                        } else {
                            Glide.with(context)
                                .load(InitAPI.IMAGE_URL + data)
                                .listener(object : RequestListener<Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<Drawable?>?,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        Log.e("ImageCarousel", "Glide onLoadFailed: ", e)
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
                                .into(displayImage)
                        }
                    } else {
                        displayImage.setImageResource(R.drawable.image_placeholder)
                    }
                } catch (e: OutOfMemoryError) {
                    e.printStackTrace()
                }
                container.addView(binding.root)
            }
        }

        return binding.root
    }

    override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
        container.removeView(view as View)
    }

    override fun getItemPosition(`object`: Any): Int = POSITION_NONE

    override fun getCount(): Int =
        imageUrlArrayList?.size ?: imageArrayList!!.size

    override fun isViewFromObject(view: View, `object`: Any): Boolean = `object` == view
}