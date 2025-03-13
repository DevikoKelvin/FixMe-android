package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class PusherData(
    @field:SerializedName("title")
    val title: String,
    @field:SerializedName("message")
    val message: String,
    @field:SerializedName("id_user")
    val idUser: Int
)
