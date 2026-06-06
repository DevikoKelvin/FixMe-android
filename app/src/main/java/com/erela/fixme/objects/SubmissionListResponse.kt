package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class SubmissionListResponse(
    @field:SerializedName("code")
    val code: Int? = null,
    @field:SerializedName("data")
    val data: List<DataItem>? = null,
    @field:SerializedName("message")
    val message: String? = null
)

data class DataItem(
    @field:SerializedName("complexity")
    val complexity: String? = null,
    @field:SerializedName("usern_approve")
    val approveUserName: String? = null,
    @field:SerializedName("keterangan")
    val description: String? = null,
    @field:SerializedName("tgl_waktu_trial_start")
    val trialStartDateTime: String? = null,
    @field:SerializedName("usern_user_done")
    val doneUserName: String? = null,
    @field:SerializedName("id_gaprojects")
    val caseId: Int? = null,
    @field:SerializedName("id_dept_tujuan")
    val targetedDeptId: Int? = null,
    @field:SerializedName("kode_mesin")
    val machineCode: String? = null,
    @field:SerializedName("id_approved")
    val approvedId: Int? = null,
    @field:SerializedName("id_user_approve")
    val approveUserId: Int? = null,
    @field:SerializedName("count_progress")
    val progressCount: String? = null,
    @field:SerializedName("nomor_request")
    val requestNumber: String? = null,
    @field:SerializedName("sts_gaprojects")
    val status: Int? = null,
    @field:SerializedName("usern_user_end")
    val endUserName: String? = null,
    @field:SerializedName("foto_gaprojects")
    val casePhoto: List<FotoGaprojectsItem>? = null,
    @field:SerializedName("dept_user")
    val userDept: String? = null,
    @field:SerializedName("id_user")
    val userId: Int? = null,
    @field:SerializedName("set_tglinput")
    val inputDateSet: String? = null,
    @field:SerializedName("nama_mesin")
    val machineName: String? = null,
    @field:SerializedName("tgl_waktu_lapor")
    val reportDateTime: String? = null,
    @field:SerializedName("tgl_input")
    val inputDate: String? = null,
    @field:SerializedName("ket_approved")
    val approvedDesc: String? = null,
    @field:SerializedName("set_waktuinput")
    val inputTimeSet: String? = null,
    @field:SerializedName("tgl_waktu_trial_end")
    val trialEndDateTime: String? = null,
    @field:SerializedName("lokasi")
    val location: String? = null,
    @field:SerializedName("tgl_waktu_kerja_start")
    val workStarDateTime: String? = null,
    @field:SerializedName("tgl_waktu_kerja_end")
    val workEndDateTime: String? = null,
    @field:SerializedName("tglwaktu_approved")
    val approvedDateTime: String? = null,
    @field:SerializedName("judul_kasus")
    val caseTitle: String? = null,
    @field:SerializedName("nama_user")
    val fullName: String? = null,
    @field:SerializedName("id_user_done")
    val doneUserId: Int? = null,
    @field:SerializedName("dept_tujuan")
    val targetedDept: String? = null,
    @field:SerializedName("tgl_waktu_done")
    val doneDateTime: String? = null,
    @field:SerializedName("is_exceeding")
    val isExceeding: Boolean? = null,
    @field:SerializedName("limit")
    val limitTime: Int? = null,
    @field:SerializedName("total_hours")
    val totalHours: Int? = null,
    @field:SerializedName("time_offset")
    val timeOffset: Int? = null,
)
