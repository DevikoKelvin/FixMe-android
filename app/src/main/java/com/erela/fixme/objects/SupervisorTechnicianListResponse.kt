package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class SupervisorTechnicianListResponse(
    @field:SerializedName("code")
    val code: Int? = null,
    @field:SerializedName("data")
    val data: List<SupervisorTechnician?>? = null,
    @field:SerializedName("message")
    val message: String? = null
)

data class SupervisorTechnician(
    @field:SerializedName("sub_dept")
    val subDept: String? = null,
    @field:SerializedName("lokasi_nama")
    val lokasiNama: String? = null,
    @field:SerializedName("id_dept")
    val idDept: Int? = null,
    @field:SerializedName("hak_akses")
    val hakAkses: Int? = null,
    @field:SerializedName("singkatan")
    val singkatan: String? = null,
    @field:SerializedName("id_user")
    val idUser: Int? = null,
    @field:SerializedName("nama_user")
    val namaUser: String? = null,
    @field:SerializedName("id_user_starconnect")
    val idUserStarconnect: Int? = null,
    @field:SerializedName("lokasi_id")
    val lokasiId: Int? = null,
    @field:SerializedName("nama_dept")
    val namaDept: String? = null
)

data class SelectedSupervisorTechniciansList(
    var isSelected: Boolean = false,
    val supervisorTechnician: SupervisorTechnician? = null
)