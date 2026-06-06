package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class SubDepartmentListResponse(
    @field:SerializedName("sub_dept")
    val subDept: String? = null,
    @field:SerializedName("sts_aktif")
    val status: Int? = null,
    @field:SerializedName("updated_at")
    val updatedAt: Any? = null,
    @field:SerializedName("id_dept")
    val deptId: Int? = null,
    @field:SerializedName("singkatan")
    val abbreviation: String? = null,
    @field:SerializedName("updated_by")
    val updatedBy: Any? = null,
    @field:SerializedName("deleted_by")
    val deletedBy: Any? = null,
    @field:SerializedName("nama_dept")
    val deptName: String? = null,
    @field:SerializedName("deleted_at")
    val deletedAt: Any? = null
)
