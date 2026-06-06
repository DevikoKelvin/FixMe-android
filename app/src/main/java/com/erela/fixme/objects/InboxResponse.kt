package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class InboxResponse(
    @field:SerializedName("id_user_2")
    val receiver: String? = null,
    @field:SerializedName("id_user_1")
    val sender: String? = null,
    @field:SerializedName("id_notif")
    val notificationId: String? = null,
    @field:SerializedName("tgl_waktu_baca")
    val readDateTime: String? = null,
    @field:SerializedName("sts_baca")
    val isRead: String? = null,
    @field:SerializedName("actions")
    val actions: String? = null,
    @field:SerializedName("tgl_waktu")
    val dateTime: String? = null,
    @field:SerializedName("id_gaprojects")
    val caseId: Int? = null
)
