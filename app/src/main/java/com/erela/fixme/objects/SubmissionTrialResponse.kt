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
	val keterangan: String? = null,
	@field:SerializedName("sts_aktif")
	val stsAktif: Int? = null,
	@field:SerializedName("foto")
	val foto: Any? = null,
	@field:SerializedName("id_user")
	val idUser: Int? = null,
	@field:SerializedName("id_gaprojects")
	val idGaprojects: Int? = null,
	@field:SerializedName("id_gaprojects_ket_trial")
	val idGaprojectsKetTrial: Int? = null,
	@field:SerializedName("tgl_waktu")
	val tglWaktu: String? = null,
	@field:SerializedName("status")
	val status: Int? = null
)
