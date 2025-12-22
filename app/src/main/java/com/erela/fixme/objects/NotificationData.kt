package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class NotificationData(
    @SerializedName("id_user_terkait")
    val relatedUserId: Int,

    @SerializedName("status")
    val status: String,

    @SerializedName("id_gaprojects")
    val idGaProjects: Int
)
