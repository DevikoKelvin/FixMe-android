package com.erela.fixme.activities

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.InboxRvAdapter
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityNotificationBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.InboxResponse
import com.erela.fixme.objects.UserData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationActivity : AppCompatActivity(), InboxRvAdapter.OnNotificationItemClickListener {
    private val binding: ActivityNotificationBinding by lazy {
        ActivityNotificationBinding.inflate(layoutInflater)
    }
    private lateinit var userData: UserData
    private lateinit var adapter: InboxRvAdapter
    private var inboxArrayList: ArrayList<InboxResponse> = ArrayList()
    private var isScrollToTopButtonAnimating = false

    @SuppressLint("NotifyDataSetChanged")
    private val activityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            inboxArrayList.clear()
            adapter.notifyDataSetChanged()
            getNotification()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userData = UserDataHelper(this@NotificationActivity).getUserData()

        init()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun init() {
        binding.apply {
            backButton.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            adapter = InboxRvAdapter(applicationContext, inboxArrayList).also {
                it.setOnItemClickListener(this@NotificationActivity)
            }
            rvInbox.layoutManager = LinearLayoutManager(applicationContext)
            rvInbox.setItemViewCacheSize(inboxArrayList.size)
            rvInbox.adapter = adapter

            rvInbox.addOnScrollListener(object: RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (!isScrollToTopButtonAnimating) {
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                        if (firstVisibleItemPosition > 0) {
                            if (!scrollToTopButton.isVisible)
                                showScrollToTopButton()
                        } else {
                            if (scrollToTopButton.isVisible)
                                hideScrollToTopButton()
                        }
                    }
                }
            })

            scrollToTopButton.setOnClickListener {
                rvInbox.smoothScrollToPosition(0)
            }

            getNotification()

            swipeRefresh.setOnRefreshListener {
                inboxArrayList.clear()
                adapter.notifyDataSetChanged()
                getNotification()
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun getNotification() {
        binding.apply {
            loadingManager(true)
            try {
                InitAPI.getEndpoint.showInbox(userData.id)
                    .enqueue(object : Callback<List<InboxResponse>> {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun onResponse(
                            call: Call<List<InboxResponse>?>,
                            response: Response<List<InboxResponse>?>
                        ) {
                            loadingManager(false)
                            if (response.isSuccessful) {
                                for (i in 0 until response.body()?.size.toString().toInt()) {
                                    inboxArrayList.add(
                                        response.body()!![i]
                                    )
                                }
                                adapter.notifyDataSetChanged()
                                rvInbox.setItemViewCacheSize(inboxArrayList.size)
                            } else {
                                CustomToast.getInstance(applicationContext)
                                    .setMessage(
                                        if (getString(R.string.lang) == "in")
                                            "Ada yang salah, silakan coba lagi."
                                        else
                                            "Something went wrong, please try again."
                                    )
                                    .setFontColor(
                                        ContextCompat.getColor(
                                            this@NotificationActivity,
                                            R.color.custom_toast_font_failed
                                        )
                                    )
                                    .setBackgroundColor(
                                        ContextCompat.getColor(
                                            this@NotificationActivity,
                                            R.color.custom_toast_background_failed
                                        )
                                    ).show()
                                Log.e("ERROR", response.message())
                                finish()
                            }
                        }

                        override fun onFailure(
                            call: Call<List<InboxResponse>?>,
                            throwable: Throwable
                        ) {
                            loadingManager(false)
                            CustomToast.getInstance(applicationContext)
                                .setMessage(
                                    if (getString(R.string.lang) == "in")
                                        "Ada yang salah, silakan coba lagi."
                                    else
                                        "Something went wrong, please try again."
                                )
                                .setFontColor(
                                    ContextCompat.getColor(
                                        this@NotificationActivity,
                                        R.color.custom_toast_font_failed
                                    )
                                )
                                .setBackgroundColor(
                                    ContextCompat.getColor(
                                        this@NotificationActivity,
                                        R.color.custom_toast_background_failed
                                    )
                                ).show()
                            Log.e("ERROR", throwable.toString())
                            throwable.printStackTrace()
                            finish()
                        }
                    })
            } catch (exception: Exception) {
                loadingManager(false)
                CustomToast.getInstance(applicationContext)
                    .setMessage(
                        if (getString(R.string.lang) == "in")
                            "Ada yang salah, silakan coba lagi."
                        else
                            "Something went wrong, please try again."
                    )
                    .setFontColor(
                        ContextCompat.getColor(
                            this@NotificationActivity,
                            R.color.custom_toast_font_failed
                        )
                    )
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            this@NotificationActivity,
                            R.color.custom_toast_background_failed
                        )
                    ).show()
                Log.e("ERROR", exception.toString())
                exception.printStackTrace()
                finish()
            }
        }
    }

    private fun loadingManager(isLoading: Boolean) {
        binding.apply {
            if (isLoading) {
                shimmerLayout.apply {
                    visibility = View.VISIBLE
                    startShimmer()
                }
                rvInbox.visibility = View.GONE
            } else {
                shimmerLayout.apply {
                    visibility = View.GONE
                    stopShimmer()
                }
                rvInbox.visibility = View.VISIBLE
            }
        }
    }

    private fun showScrollToTopButton() {
        binding.apply {
            if (!scrollToTopButton.isVisible) {
                // Cancel any existing animation
                scrollToTopButton.animate().cancel()
                isScrollToTopButtonAnimating = true

                scrollToTopButton.visibility = View.VISIBLE
                scrollToTopButton.alpha = 0f
                scrollToTopButton.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationEnd(animation: Animator) {
                            isScrollToTopButtonAnimating = false
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            isScrollToTopButtonAnimating = false
                        }

                        override fun onAnimationRepeat(animation: Animator) {}
                    })
                    .start()
            }
        }
    }

    private fun hideScrollToTopButton() {
        binding.apply {
            if (scrollToTopButton.isVisible) {
                // Cancel any existing animation
                scrollToTopButton.animate().cancel()
                isScrollToTopButtonAnimating = true

                scrollToTopButton.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationEnd(animation: Animator) {
                            scrollToTopButton.visibility = View.GONE
                            isScrollToTopButtonAnimating = false
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            isScrollToTopButtonAnimating = false
                        }

                        override fun onAnimationRepeat(animation: Animator) {}
                    })
                    .start()
            }
        }
    }

    override fun onItemClick(item: InboxResponse) {
        activityResultLauncher.launch(
            SubmissionDetailActivity.initiate(
                this@NotificationActivity,
                item.idGaprojects.toString()
            )
        )
    }
}