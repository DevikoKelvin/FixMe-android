package com.erela.fixme

import android.app.Application
import android.content.Intent
import com.erela.fixme.activities.LoginActivity
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.services.SseService

/**
 * Installs the API auth hooks once per process.
 *
 * Deliberately here and not in an activity: FCMService and SseService can run with no
 * activity alive (a push arriving while the app is closed), and those calls need the bearer
 * token too.
 */
class FixMeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val userData = UserDataHelper(this)

        InitAPI.tokenProvider = { userData.getToken() }

        InitAPI.onUnauthorized = {
            // Server rejected the token: expired, revoked, or the account signed in on
            // another device. Drop local state and send the user back to login.
            userData.purgeUserData()
            stopService(Intent(this, SseService::class.java))

            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
        }
    }
}
