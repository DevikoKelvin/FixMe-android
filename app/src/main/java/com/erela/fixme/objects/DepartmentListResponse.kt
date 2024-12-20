package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class DepartmentListResponse(
    @field:SerializedName("sub_dept")
    val subDept: String? = null,
    @field:SerializedName("sts_aktif")
    val stsAktif: String? = null,
    @field:SerializedName("id_dept")
    val idDept: String? = null,
    @field:SerializedName("singkatan")
    val singkatan: String? = null,
    @field:SerializedName("nama_dept")
    val namaDept: String? = null
)
