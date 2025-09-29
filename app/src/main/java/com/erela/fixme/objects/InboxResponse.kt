package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class InboxResponse(
    @field:SerializedName("id_user_2")
    val idUser2: String? = null,
    @field:SerializedName("id_user_1")
    val idUser1: String? = null,
    @field:SerializedName("id_notif")
    val idNotif: String? = null,
    @field:SerializedName("tgl_waktu_baca")
    val tglWaktuBaca: String? = null,
    @field:SerializedName("sts_baca")
    val stsBaca: String? = null,
    @field:SerializedName("actions")
    val actions: String? = null,
    @field:SerializedName("tgl_waktu")
    val tglWaktu: String? = null,
    @field:SerializedName("id_gaprojects")
    val idGaprojects: Int? = null
)
