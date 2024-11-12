package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.erela.fixme.databinding.ActivityMainBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.UserData

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
}