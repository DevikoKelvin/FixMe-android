package com.erela.fixme.helpers

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import com.erela.fixme.activities.LaundryCheckInActivity
import com.erela.fixme.activities.LaundryCounterActivity
import android.os.SystemClock
import androidx.core.content.edit
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
    private const val MIN_INTERVAL_MS = 30 * 1000L
    private var lastAt = 0L

    /** True once `/session` has answered in this process, so the stored flags are the server's word and not a default. */
    @Volatile
    var confirmed = false
        private set

    /**
     * Is the one-time "Smart Wash is now available" notice due for this person? Yes the first time
     * the server has confirmed the menu is on for them; asking marks it shown. Kept in "app_prefs"
     * (survives logout, like the changelog) and per account, so a second person on the phone is
     * told as well. Never from the stored default: without [confirmed] an offline start of an
     * upgraded install would announce a feature the server still refuses.
     */
    fun takeSmartWashIntro(context: Context): Boolean {
        val user = UserDataHelper(context).getUserData()
        if (!confirmed || !user.showLaundryMenu) return false

        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val key = "key.laundry.intro.${user.id}"
        if (prefs.getBoolean(key, false)) return false

        prefs.edit { putBoolean(key, true) }
        return true
    }

    /** @return true when a menu flag changed, so the caller should redraw. */
    suspend fun run(context: Context, force: Boolean = false): Boolean {
        val helper = UserDataHelper(context)
        if (helper.getToken().isNullOrBlank()) return false

        val now = SystemClock.elapsedRealtime()
        if (!force && lastAt != 0L && now - lastAt < MIN_INTERVAL_MS) return false
        lastAt = now

        val data = runCatching { InitAPI.getEndpoint.session() }.getOrNull()?.data ?: return false
        confirmed = true
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
