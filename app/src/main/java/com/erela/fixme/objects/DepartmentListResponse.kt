package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class DepartmentListResponse(

	@field:SerializedName("DepartmentListResponse")
	val departmentListResponse: List<DepartmentListResponseItem?>? = null
)

data class DepartmentListResponseItem(

	@field:SerializedName("sub_dept")
	val subDept: String? = null,

	@field:SerializedName("id_dept")
	val idDept: String? = null,

	@field:SerializedName("nama_dept")
	val namaDept: String? = null
)
