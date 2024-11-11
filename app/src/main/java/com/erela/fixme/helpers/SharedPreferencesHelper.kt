package com.erela.fixme.helpers

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesHelper {
    fun getSharedPreferences(context: Context): SharedPreferences = context.getSharedPreferences(
        "FixMe",
        Context.MODE_PRIVATE
    )
}