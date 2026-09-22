package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.erela.fixme.databinding.ActivitySplashScreenBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    private val binding: ActivitySplashScreenBinding by lazy {
        ActivitySplashScreenBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.apply {
            Handler(mainLooper).postDelayed({
                erelaMotoSplash.visibility = View.VISIBLE
                TransitionManager.beginDelayedTransition(mainSplashContainer, AutoTransition())
                Handler(mainLooper).postDelayed({
                    openNextScreen()
                }, 1000)
            }, 2000)
        }
    }

    /**
     * Straight to the main menu when the session is good, and to login when it is not.
     *
     * IT ALWAYS WENT TO LOGIN [GA, 22 September 2026], so a valid token bought nothing: the
     * operator typed their password every morning while the phone already held a live session.
     *
     * THE PROBE IS WHY THIS IS NOT JUST A NULL CHECK. A stored token says this phone REMEMBERS a
     * login, not that the server still honours it - and almost nothing under `apimobile` returns
     * 401, because the group answers unauthenticated callers rather than refusing them. Only
     * Smart Wash sits behind `auth:sanctum`, which is why an expired session used to survive
     * until somebody opened that menu. `/session` asks the question directly.
     *
     * A 401 NEEDS NOTHING HERE. InitAPI's interceptor sees it first, purges the user data and
     * launches LoginActivity with CLEAR_TASK; by the time this resumes there is no token left, so
     * the same branch sends us to login without a second opinion.
     *
     * OFFLINE IS NOT EXPIRED. The call is wrapped rather than trusted: a counter with no signal
     * must still reach its screens, and every request made once there is a network checks the
     * token again anyway.
     */
    private fun openNextScreen() {
        val userData = UserDataHelper(this)

        lifecycleScope.launch {
            if (!userData.getToken().isNullOrBlank()) {
                runCatching { InitAPI.getEndpoint.session() }
            }

            val next = if (userData.getToken().isNullOrBlank()) {
                LoginActivity::class.java
            } else {
                MainActivity::class.java
            }

            startActivity(Intent(this@SplashScreenActivity, next))
            finish()
        }
    }
}