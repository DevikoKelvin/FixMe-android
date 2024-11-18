package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
import com.erela.fixme.activities.SubmissionListActivity
import com.erela.fixme.adapters.recycler_view.InboxRvAdapter
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityNotificationBinding
import com.erela.fixme.helpers.InitAPI
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.InboxResponse
import com.erela.fixme.objects.UserData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationBinding
    private lateinit var userData: UserData
    private lateinit var adapter: InboxRvAdapter
    private var inboxArrayList: ArrayList<InboxResponse> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userData = UserDataHelper(this@NotificationActivity).getUserData()

        init()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun init() {
        binding.apply {
            backButton.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            adapter = InboxRvAdapter(applicationContext, inboxArrayList)
            rvInbox.layoutManager = LinearLayoutManager(applicationContext)
            rvInbox.adapter = adapter

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
            loadingBar.visibility = View.VISIBLE
            try {
                InitAPI.getAPI.showInbox(userData.id)
                    .enqueue(object : Callback<List<InboxResponse>> {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun onResponse(
                            call: Call<List<InboxResponse>?>,
                            response: Response<List<InboxResponse>?>
                        ) {
                            loadingBar.visibility = View.GONE
                            if (response.isSuccessful) {
                                for (i in 0 until response.body()?.size.toString().toInt()) {
                                    inboxArrayList.add(
                                        response.body()!![i]
                                    )
                                }
                                adapter.notifyDataSetChanged()
                            } else {
                                CustomToast.getInstance(applicationContext)
                                    .setMessage("Something went wrong, please try again.")
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
                            CustomToast.getInstance(applicationContext)
                                .setMessage("Something went wrong, please try again.")
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
                            loadingBar.visibility = View.GONE
                            finish()
                        }
                    })
            } catch (exception: Exception) {
                loadingBar.visibility = View.GONE
                CustomToast.getInstance(applicationContext)
                    .setMessage("Something went wrong, please try again.")
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
}