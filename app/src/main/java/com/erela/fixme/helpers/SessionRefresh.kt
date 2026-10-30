package com.erela.fixme.helpers

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import com.erela.fixme.activities.LaundryCheckInActivity
import com.erela.fixme.activities.LaundryCounterActivity
import android.os.SystemClock
import com.erela.fixme.helpers.api.InitAPI

/**
 * Re-reads the Smart Wash flags from `/session`, at most once every [MIN_INTERVAL_MS].
 *
 * Called by the splash (forced) and by MainActivity every time the app comes to the foreground, so
 * a courier GA appointed or removed is picked up without signing out. The throttle is in memory:
 * a fresh process always checks, a quick app switch does not.
 *
 * A failure (offline, 401) changes nothing: a 401 is already handled by InitAPI's interceptor, and
 * offline must not take the menu away.
 */
object SessionRefresh {
    private const val MIN_INTERVAL_MS = 5 * 60 * 1000L
    private var lastAt = 0L

    /** @return true when a menu flag changed, so the caller should redraw. */
    suspend fun run(context: Context, force: Boolean = false): Boolean {
        val helper = UserDataHelper(context)
        if (helper.getToken().isNullOrBlank()) return false

        val now = SystemClock.elapsedRealtime()
        if (!force && lastAt != 0L && now - lastAt < MIN_INTERVAL_MS) return false
        lastAt = now

        val data = runCatching { InitAPI.getEndpoint.session() }.getOrNull()?.data ?: return false
        val before = helper.getUserData()
        helper.saveLaundryFlags(data.laundryCounter, data.laundryMenu)
        val after = helper.getUserData()

        return before.showLaundryMenu != after.showLaundryMenu ||
                before.isLaundryCounter != after.isLaundryCounter
    }
}

/**
 * For a Smart Wash screen: refresh the flags and, if they changed what this person gets, leave
 * (the menu was taken away) or swap to the other screen (counter <-> courier).
 */
suspend fun ComponentActivity.reconcileSmartWash(isCounterScreen: Boolean) {
    if (!SessionRefresh.run(this)) return

    val user = UserDataHelper(this).getUserData()

    when {
        !user.showLaundryMenu -> finish()
        user.isLaundryCounter != isCounterScreen -> {
            startActivity(
                Intent(
                    this,
                    if (user.isLaundryCounter) LaundryCounterActivity::class.java
                    else LaundryCheckInActivity::class.java
                )
            )
            finish()
        }
    }
}
