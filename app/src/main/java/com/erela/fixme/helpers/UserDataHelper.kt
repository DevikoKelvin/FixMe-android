package com.erela.fixme.helpers

import android.content.Context
import com.erela.fixme.objects.UserData

class UserDataHelper(private val context: Context) {
    private val keyId = "key.id"
    private val keyIdStarConnect = "key.id.starconnect"
    private val keyUsername = "key.username"
    private val keyName = "key.name"
    private val keyStatusPrev = "key.stat.prev"
    private val keyIdDept = "key.id.dept"
    private val keyDept = "key.dept"
    private val keySubDept = "key.sub.dept"

    fun setUserData(
        id: Int, idStarConnect: Int, username: String, name: String, privilege: Int, idDept: Int,
        dept: String, subDept: String
    ) {
        SharedPreferencesHelper.getSharedPreferences(context).edit().also {
            it.apply {
                putInt(keyId, id)
                putInt(keyIdStarConnect, idStarConnect)
                putString(keyUsername, username)
                putString(keyName, name)
                putInt(keyStatusPrev, privilege)
                putInt(keyIdDept, idDept)
                putString(keyDept, dept)
                putString(keySubDept, subDept)
            }
        }.apply()
    }

    fun getUserData(): UserData {
        val userData: UserData
        SharedPreferencesHelper.getSharedPreferences(context).also {
            it.apply {
                userData = UserData(
                    getInt(keyId, 0),
                    getInt(keyIdStarConnect, 0),
                    getString(keyUsername, "").toString(),
                    getString(keyName, "").toString(),
                    getInt(keyStatusPrev, 0),
                    getInt(keyIdDept, 0),
                    getString(keyDept, "").toString(),
                    getString(keySubDept, "").toString()
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