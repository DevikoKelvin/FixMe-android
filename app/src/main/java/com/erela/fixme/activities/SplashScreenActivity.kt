package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.erela.fixme.databinding.ActivitySplashScreenBinding
import com.erela.fixme.services.ForegroundServicesHelper

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    private val binding: ActivitySplashScreenBinding by lazy {
        ActivitySplashScreenBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.apply {
            /*if (!foregroundServiceRunning()) {
                startForegroundService(
                    Intent(this@SplashScreenActivity, ForegroundServicesHelper::class.java)
                )
            } else {
                stopService(
                    Intent(this@SplashScreenActivity, ForegroundServicesHelper::class.java)
                )
            }*/

            Handler(mainLooper).postDelayed({
                erelaMotoSplash.visibility = View.VISIBLE
                TransitionManager.beginDelayedTransition(mainSplashContainer, AutoTransition())
                Handler(mainLooper).postDelayed({
                    startActivity(Intent(this@SplashScreenActivity, LoginActivity::class.java))
                        .also {
                            finish()
                        }
                }, 1000)
            }, 2000)
        }
    }

    fun foregroundServiceRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (ForegroundServicesHelper::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}