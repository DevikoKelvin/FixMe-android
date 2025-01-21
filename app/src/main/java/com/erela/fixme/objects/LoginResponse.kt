package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @field:SerializedName("code")
    val code: Int? = null,
    @field:SerializedName("nama")
    val nama: String? = null,
    @field:SerializedName("hak_akses")
    val hakAkses: Int? = null,
    @field:SerializedName("id_dept")
    val idDept: Int? = null,
    @field:SerializedName("nama_dept")
    val dept: String? = null,
    @field:SerializedName("sub_dept")
    val subDept: String? = null,
    @field:SerializedName("sts_login")
    val stsLogin: Int? = null,
    @field:SerializedName("id_user")
    val idUser: Int? = null,
    @field:SerializedName("id_starconnect")
    val idStarConnect: Int? = null,
    @field:SerializedName("message")
    val message: String? = null
)
