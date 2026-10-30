package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

/**
 * `/session`: the status says whether the token is live; the body refreshes the Smart Wash flags so
 * a courier GA appointed (or removed) is picked up at app start, not only at login.
 * Every field is nullable: an older server sends neither flag, and null means "keep what is stored".
 */
data class SessionResponse(
    @field:SerializedName("code")
    val code: Int? = null,
    @field:SerializedName("data")
    val data: SessionData? = null
)

data class SessionData(
    @field:SerializedName("laundry_counter")
    val laundryCounter: Boolean? = null,
    @field:SerializedName("laundry_menu")
    val laundryMenu: Boolean? = null
)
