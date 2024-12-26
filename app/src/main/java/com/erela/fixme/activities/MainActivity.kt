package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erela.fixme.databinding.ActivityMainBinding
import com.erela.fixme.dialogs.ConfirmationDialog
import com.erela.fixme.helpers.NotificationsHelper
import com.erela.fixme.helpers.PermissionHelper
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.UserData
import com.erela.fixme.services.NotificationService
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange
import com.pusher.pushnotifications.PushNotifications

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val userData: UserData by lazy {
        UserDataHelper(this@MainActivity).getUserData()
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

        init()
    }

    @SuppressLint("SetTextI18n", "InlinedApi")
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

            PushNotifications.start(applicationContext, "66b9148d-50f4-4114-b258-e9e9485ce75c")
            if (!isServiceRunning(NotificationService::class.java)) {
                Intent(this@MainActivity, NotificationService::class.java).also {
                    startForegroundService(it)
                }
            }
            /*PushNotifications.addDeviceInterest("hello")*/

            usernameText.text = "${userData.name.trimEnd()}!"

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

            settingsMenu.setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
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
                                    Intent(
                                        this@MainActivity,
                                        NotificationService::class.java
                                    ).also { intent ->
                                        stopService(intent)
                                    }
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

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}