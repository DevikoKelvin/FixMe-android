package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.erela.fixme.R
import com.erela.fixme.databinding.ActivitySplashScreenBinding

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
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
}