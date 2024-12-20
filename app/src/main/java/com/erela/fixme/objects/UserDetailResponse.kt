package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class UserDetailResponse(
    @field:SerializedName("sts_aktif")
    val stsAktif: String? = null,
    @field:SerializedName("nama")
    val nama: String? = null,
    @field:SerializedName("usern")
    val usern: String? = null,
    @field:SerializedName("id_dept")
    val idDept: String? = null,
    @field:SerializedName("hak_akses")
    val hakAkses: String? = null,
    @field:SerializedName("id_user")
    val idUser: String? = null,
    @field:SerializedName("id_user_starconnect")
    val idUserStarconnect: String? = null
)
