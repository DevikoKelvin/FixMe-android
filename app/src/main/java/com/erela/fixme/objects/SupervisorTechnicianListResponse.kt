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
    val locationName: String? = null,
    @field:SerializedName("id_dept")
    val deptId: Int? = null,
    @field:SerializedName("hak_akses")
    val privilege: Int? = null,
    @field:SerializedName("singkatan")
    val abbreviation: String? = null,
    @field:SerializedName("id_user")
    val userId: Int? = null,
    @field:SerializedName("nama_user")
    val fullName: String? = null,
    @field:SerializedName("id_user_starconnect")
    val starConnectId: Int? = null,
    @field:SerializedName("lokasi_id")
    val locationId: Int? = null,
    @field:SerializedName("nama_dept")
    val deptName: String? = null
)

data class SelectedSupervisorTechniciansList(
    var isSelected: Boolean = false,
    val supervisorTechnician: SupervisorTechnician? = null
)