package com.erela.fixme.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erela.fixme.R
import com.erela.fixme.databinding.ActivityMainBinding
import com.erela.fixme.dialogs.ConfirmationDialog
import com.erela.fixme.helpers.PermissionHelper
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.WebSocketClient
import com.erela.fixme.objects.UserData
import com.erela.fixme.services.ForegroundServicesHelper.Companion.CHANNEL_ID

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val userData: UserData by lazy {
        UserDataHelper(this@MainActivity).getUserData()
    }
    private lateinit var webSocketClient: WebSocketClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()
    }

    @SuppressLint("SetTextI18n")
    private fun init() {
        binding.apply {
            if (!PermissionHelper.isPermissionGranted(
                    this@MainActivity, PermissionHelper.POST_NOTIFICATIONS
                )
            ) {
                PermissionHelper.requestPermission(
                    this@MainActivity, arrayOf(PermissionHelper.POST_NOTIFICATIONS),
                    PermissionHelper.REQUEST_CODE_NOTIFICATION
                )
            }

            /*webSocketClient = WebSocketClient.getInstance()
            webSocketClient.setSocketUrl(InitAPI.SOCKET_URL)
            webSocketClient.setListener(object : WebSocketClient.SocketListener {
                override fun onMessage(message: String) {
                    val jsonObject = JSONObject(message)
                    Log.e("Message", jsonObject.toString())
                    val notification = NotificationResponse(
                        jsonObject.getInt("expires"),
                        jsonObject.getString("topic"),
                        jsonObject.getString("id"),
                        jsonObject.getInt("time"),
                        jsonObject.getString("event"),
                        jsonObject.getString("message") ?: null
                    )
                    showNotification(notification.message.toString())
                }
            })
            webSocketClient.connect()*/

            usernameText.text = "${userData.name}!"

            notificationButton.setOnClickListener {
                notificationAnimation.playAnimation()
                startActivity(Intent(this@MainActivity, NotificationActivity::class.java))
            }

            changePasswordMenu.setOnClickListener {
                startActivity(Intent(this@MainActivity, ChangePasswordActivity::class.java))
            }

            makeSubmissionMenu.setOnClickListener {
                startActivity(Intent(this@MainActivity, SubmissionFormActivity::class.java))
            }

            submissionListMenu.setOnClickListener {
                startActivity(Intent(this@MainActivity, SubmissionListActivity::class.java))
            }

            logOutButton.setOnClickListener {
                val confirmationDialog =
                    ConfirmationDialog(
                        this@MainActivity,
                        "Are you sure you want to log out?",
                        "Yes"
                    ).also {
                        with(it) {
                            setConfirmationDialogListener(object :
                                ConfirmationDialog.ConfirmationDialogListener {
                                override fun onConfirm() {
                                    UserDataHelper(this@MainActivity).purgeUserData()
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Successfully logged out!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            LoginActivity::class.java
                                        )
                                    ).also {
                                        finish()
                                    }
                                }
                            })
                        }
                    }

                if (confirmationDialog.window != null)
                    confirmationDialog.show()
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
    }
}