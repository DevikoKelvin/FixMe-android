package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @field:SerializedName("code")
    val code: Int,
    @field:SerializedName("hak_akses")
    val hakAkses: String,
    @field:SerializedName("sts_login")
    val stsLogin: Int,
    @field:SerializedName("id_user")
    val idUser: String,
    @field:SerializedName("message")
    val message: String?
)
