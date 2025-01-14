package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @field:SerializedName("code")
    val code: Int? = null,
    @field:SerializedName("nama")
    val nama: String? = null,
    @field:SerializedName("hak_akses")
    val hakAkses: String? = null,
    @field:SerializedName("id_dept")
    val idDept: String? = null,
    @field:SerializedName("nama_dept")
    val dept: String? = null,
    @field:SerializedName("sts_login")
    val stsLogin: Int? = null,
    @field:SerializedName("id_user")
    val idUser: String? = null,
    @field:SerializedName("message")
    val message: String? = null
)
