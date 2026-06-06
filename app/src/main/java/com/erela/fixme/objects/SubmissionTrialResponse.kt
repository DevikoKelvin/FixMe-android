package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class SubmissionTrialResponse(
	@field:SerializedName("code")
	val code: Int? = null,
	@field:SerializedName("data")
	val data: List<TrialDataItem?>? = null,
	@field:SerializedName("message")
	val message: String? = null
)

data class TrialDataItem(
	@field:SerializedName("keterangan")
	val description: String? = null,
	@field:SerializedName("sts_aktif")
	val isActive: Int? = null,
	@field:SerializedName("foto")
	val photo: Any? = null,
	@field:SerializedName("id_user")
	val userId: Int? = null,
	@field:SerializedName("id_gaprojects")
	val caseId: Int? = null,
	@field:SerializedName("id_gaprojects_ket_trial")
	val trialDescCaseId: Int? = null,
	@field:SerializedName("tgl_waktu")
	val dateTime: String? = null,
	@field:SerializedName("status")
	val status: Int? = null
)
