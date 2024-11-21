package com.erela.fixme.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat.startForeground
import com.erela.fixme.R
import com.erela.fixme.databinding.ActivityMainBinding
import com.erela.fixme.helpers.InitAPI
import com.erela.fixme.helpers.PermissionHelper
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.WebSocketClient
import com.erela.fixme.objects.NotificationResponse
import com.erela.fixme.objects.UserData
import com.erela.fixme.services.ForegroundServicesHelper.Companion.CHANNEL_ID
import com.erela.fixme.services.ForegroundServicesHelper.Companion.NOTIFICATION_ID
import kotlinx.coroutines.DelicateCoroutinesApi
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var userData: UserData
    private lateinit var webSocketClient: WebSocketClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userData = UserDataHelper(this@MainActivity).getUserData()

        init()
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    private fun init() {
        binding.apply {
            if (!PermissionHelper.isPermissionGranted(
                    this@MainActivity, PermissionHelper.POST_NOTIFICATIONS
                )
            ) {
                PermissionHelper.requestPermission(
                    this@MainActivity, arrayOf(PermissionHelper.POST_NOTIFICATIONS)
                )
            }

            webSocketClient = WebSocketClient.getInstance()
            webSocketClient.setSocketUrl(InitAPI.SOCKET_URL)
            webSocketClient.setListener(object : WebSocketClient.SocketListener {
                override fun onMessage(message: String) {
                    val jsonObject = JSONObject(message)
                    Log.e("Message", jsonObject.toString())
                    val notification: NotificationResponse = NotificationResponse(
                        jsonObject.getInt("expires") ?: null,
                        jsonObject.getString("topic"),
                        jsonObject.getString("id"),
                        jsonObject.getInt("time"),
                        jsonObject.getString("event"),
                        jsonObject.getString("message") ?: null
                    )
                    showNotification(notification.message.toString())
                }
            })
            webSocketClient.connect()

            usernameText.text = "${userData.username}!"

            notificationButton.setOnClickListener {
                notificationAnimation.playAnimation()
                startActivity(Intent(this@MainActivity, NotificationActivity::class.java))
            }

            changePasswordMenu.setOnClickListener {
                startActivity(Intent(this@MainActivity, ChangePasswordActivity::class.java))
            }

            makeSubmissionMenu.setOnClickListener {
                startActivity(Intent(this@MainActivity, CreateSubmissionActivity::class.java))
            }

            submissionListMenu.setOnClickListener {
                startActivity(Intent(this@MainActivity, SubmissionListActivity::class.java))
            }

            logOutButton.setOnClickListener {
                UserDataHelper(this@MainActivity).purgeUserData()
                Toast.makeText(
                    this@MainActivity, "Successfully logged out!", Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java)).also {
                    finish()
                }
            }
        }
    }

    private fun createNotification() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                "CHANNEL_ID", "FixMe", NotificationManager.IMPORTANCE_DEFAULT
            ).also {
                it.description = "Description"
            }
        )
    }

    private fun showNotification(message: String) {
        createNotification()
        /*val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentText(message)
            .setContentTitle("Erela FixMe")
            .setSmallIcon(R.drawable.fixme_logo)
            .setPriority(Notification.PRIORITY_DEFAULT)
            .build()*/
        val notification =
            NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
                .setContentText(message)
                .setSmallIcon(R.drawable.fixme_logo)
                .setContentTitle("Erela FixMe")
                .setPriority(Notification.PRIORITY_DEFAULT)
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        )
            return
        NotificationManagerCompat.from(this@MainActivity)
            .notify(1, notification.build())
        /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            startForeground(
                NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            )
        }*/
    }
}