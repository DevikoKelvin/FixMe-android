package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @field:SerializedName("code")
    val code: Int? = null,
    @field:SerializedName("nama")
    val name: String? = null,
    @field:SerializedName("hak_akses")
    val privilege: Int? = null,
    @field:SerializedName("id_dept")
    val deptId: Int? = null,
    @field:SerializedName("nama_dept")
    val deptName: String? = null,
    @field:SerializedName("sub_dept")
    val subDept: String? = null,
    @field:SerializedName("sts_login")
    val loginStatus: Int? = null,
    @field:SerializedName("id_user")
    val userId: Int? = null,
    @field:SerializedName("id_starconnect")
    val starConnectId: Int? = null,
    @field:SerializedName("message")
    val message: String? = null,
    @field:SerializedName("email")
    val email: String? = null,
    @field:SerializedName("lockout_seconds")
    val lockoutSeconds: Int? = null
)
