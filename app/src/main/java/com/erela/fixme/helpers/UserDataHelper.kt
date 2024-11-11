package com.erela.fixme.helpers

import android.content.Context
import com.erela.fixme.objects.UserData

class UserDataHelper(private val context: Context) {
    private val keyId = "key.id"
    private val keyUsername = "key.username"
    private val keyStatusPrev = "key.stat.prev"

    fun setUserData(id: Int, username: String, privilege: Int) {
        SharedPreferencesHelper.getSharedPreferences(context).edit().also {
            it.apply {
                putInt(keyId, id)
                putString(keyUsername, username)
                putInt(keyStatusPrev, privilege)
            }
        }.apply()
    }

    fun getUserData(): UserData {
        val userData: UserData
        SharedPreferencesHelper.getSharedPreferences(context).also {
            it.apply {
                userData = UserData(
                    getInt(keyId, 0),
                    getString(keyUsername, "").toString(),
                    getInt(keyStatusPrev, 0)
                )
            }
        }

        return userData
    }

    fun isUserDataExist(): Boolean =
        SharedPreferencesHelper.getSharedPreferences(context)
            .contains(keyId) || SharedPreferencesHelper.getSharedPreferences(context)
            .contains(keyUsername)

    fun purgeUserData() {
        SharedPreferencesHelper.getSharedPreferences(context).edit().clear().apply()
    }
}