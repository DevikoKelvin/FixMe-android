package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.erela.fixme.R
import com.erela.fixme.databinding.ActivityMainBinding
import com.erela.fixme.helpers.InitAPI
import com.erela.fixme.helpers.PermissionHelper
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.NotificationResponse
import com.erela.fixme.objects.UserData
import com.erela.fixme.services.ForegroundServicesHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import retrofit2.Call

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var userData: UserData

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
        val notificationCompatBuilder = NotificationCompat.Builder(this@MainActivity, "CHANNEL_ID")
            .setSmallIcon(R.drawable.fixme_logo)
            .setContentTitle("Erela FixMe")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (ActivityCompat.checkSelfPermission(
                this, PermissionHelper.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        )
            return
        NotificationManagerCompat.from(this@MainActivity)
            .notify(1, notificationCompatBuilder.build())
    }
}