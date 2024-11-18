package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    @field:SerializedName("expires")
    val expires: Int? = null,
    @field:SerializedName("topic")
    val topic: String? = null,
    @field:SerializedName("id")
    val id: String? = null,
    @field:SerializedName("time")
    val time: Int? = null,
    @field:SerializedName("event")
    val event: String? = null,
    @field:SerializedName("message")
    val message: String? = null
)
