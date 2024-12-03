package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class TechnicianListResponse(
    @field:SerializedName("sub_dept")
    val subDept: String? = null,
    @field:SerializedName("id_dept")
    val idDept: Int? = null,
    @field:SerializedName("hak_akses")
    val hakAkses: Int? = null,
    @field:SerializedName("singkatan")
    val singkatan: String? = null,
    @field:SerializedName("nama_user")
    val namaUser: String? = null,
    @field:SerializedName("id_user")
    val idUser: Int? = null,
    @field:SerializedName("id_user_starconnect")
    val idUserStarconnect: Int? = null,
    @field:SerializedName("nama_dept")
    val namaDept: String? = null
)

data class SelectedTechniciansList(
    val technician: TechnicianListResponse? = null,
    var isSelected: Boolean = false
)