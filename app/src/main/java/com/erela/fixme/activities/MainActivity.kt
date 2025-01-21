package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erela.fixme.R
import com.erela.fixme.databinding.ActivityMainBinding
import com.erela.fixme.dialogs.ConfirmationDialog
import com.erela.fixme.helpers.PermissionHelper
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.UserData
import com.erela.fixme.services.NotificationService
import com.pusher.pushnotifications.PushNotifications
import java.time.LocalDateTime

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
            when (userData.privilege) {
                0 -> {
                    privilegeContainer.background = ContextCompat.getDrawable(
                            applicationContext, R
                                .drawable.toolbar_background_owner
                        )
                    privilegeText.text = "Owner"
                    privilegeText.setTextColor(
                        ContextCompat.getColor(
                            applicationContext, R.color.white
                        )
                    )
                }

                1 -> {
                    privilegeContainer.background = ContextCompat.getDrawable(
                            applicationContext, R
                                .drawable.toolbar_background_manager
                        )
                    privilegeText.text = "Manager/Assistant/Admin"
                    privilegeText.setTextColor(
                        ContextCompat.getColor(
                            applicationContext, R.color.white
                        )
                    )
                }

                2 -> {
                    privilegeContainer.background = ContextCompat.getDrawable(
                            applicationContext, R
                                .drawable.toolbar_background_supervisor
                        )
                    privilegeText.text = "Supervisor"
                    privilegeText.setTextColor(
                        ContextCompat.getColor(
                            applicationContext, R.color.black
                        )
                    )
                }

                3 -> {
                    privilegeContainer.background = ContextCompat.getDrawable(
                            applicationContext, R
                                .drawable.toolbar_background_technician
                        )
                    privilegeText.text = "Technicians"
                    privilegeText.setTextColor(
                        ContextCompat.getColor(
                            applicationContext, R.color.black
                        )
                    )
                }

                4 -> {
                    privilegeContainer.background = ContextCompat.getDrawable(
                            applicationContext, R
                                .drawable.toolbar_background_staff
                        )
                    privilegeText.text = "Staff/Reporter"
                    privilegeText.setTextColor(
                        ContextCompat.getColor(
                            applicationContext, R.color.black
                        )
                    )
                }
            }
            val currentDateTime = LocalDateTime.now()
            greetingText.text = "Good ${
                when (currentDateTime.hour) {
                    in 0 .. 11 -> "morning,"
                    in 12 .. 18 -> "afternoon,"
                    else -> "evening,"
                }
            }"
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