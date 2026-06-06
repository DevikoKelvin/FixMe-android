package com.erela.fixme.objects

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class TechniciansUserItem(
    @field:SerializedName("id_table_teknisi")
    val techId: Int? = null,
    @field:SerializedName("dept_user")
    val userDept: String? = null,
    @field:SerializedName("id_user")
    val userId: Int? = null,
    @field:SerializedName("nama_user")
    val userName: String? = null,
    @field:SerializedName("id_user_spv")
    val supervisorId: Int? = null,
    @field:SerializedName("tgl_waktu")
    val dateTime: String? = null
) : Parcelable

@Parcelize
data class SubmissionDetailResponse(
    @field:SerializedName("name_user_done")
    val doneFullName: String? = null,
    @field:SerializedName("usern_user_hold")
    val holdUserName: String? = null,
    @field:SerializedName("keterangan_reject")
    val rejectDesc: String? = null,
    @field:SerializedName("tgl_waktu_cancel")
    val cancelDateTime: String? = null,
    @field:SerializedName("keterangan_hold")
    val holdDesc: String? = null,
    @field:SerializedName("id_gaprojects")
    val caseId: Int? = null,
    @field:SerializedName("keterangan_pelapor_reject")
    val reporterRejectDesc: String? = null,
    @field:SerializedName("id_dept_tujuan")
    val targetDeptId: Int? = null,
    @field:SerializedName("id_user_pelapor_reject")
    val reporterRejectUserId: Int? = null,
    @field:SerializedName("kode_mesin")
    val machineCode: String? = null,
    @field:SerializedName("id_user_approve")
    val approveUserId: Int? = null,
    @field:SerializedName("nomor_request")
    val requestNumber: String? = null,
    @field:SerializedName("sts_gaprojects")
    val caseStatus: Int? = null,
    @field:SerializedName("user_nama_approve")
    val approveFullName: String? = null,
    @field:SerializedName("tgl_waktu_reject")
    val rejectDateTime: String? = null,
    @field:SerializedName("foto_gaprojects")
    val casePhoto: List<FotoGaprojectsItem?>? = null,
    @field:SerializedName("id_user_hold")
    val holdUserId: Int? = null,
    @field:SerializedName("tgl_waktu_pelapor_approve")
    val reporterApproveDateTime: String? = null,
    @field:SerializedName("nama_user_pelapor_reject")
    val reporterRejectFullName: String? = null,
    @field:SerializedName("dept_user")
    val userDept: String? = null,
    @field:SerializedName("nama_user_buat")
    val creatorFullName: String? = null,
    @field:SerializedName("tgl_input")
    val inputDate: String? = null,
    @field:SerializedName("ket_approved")
    val approvedDesc: String? = null,
    @field:SerializedName("difficulty")
    val complexity: String? = null,
    @field:SerializedName("set_waktuinput")
    val inputTime: String? = null,
    @field:SerializedName("nama_user_resume")
    val resumeFullName: String? = null,
    @field:SerializedName("keterangan_cancel")
    val cancelDesc: String? = null,
    @field:SerializedName("lokasi")
    val location: String? = null,
    @field:SerializedName("usern_user_pelapor_approve")
    val reporterApproveUserName: String? = null,
    @field:SerializedName("tgl_waktu_resume")
    val resumeDateTime: String? = null,
    @field:SerializedName("tglwaktu_approved")
    val approvedDateTime: String? = null,
    @field:SerializedName("judul_kasus")
    val caseTitle: String? = null,
    @field:SerializedName("sub_dept_tujuan")
    val targetedSubDept: String? = null,
    @field:SerializedName("usern_user_spv")
    val supervisorUserName: List<UsernUserSpvItem?>? = null,
    @field:SerializedName("dept_tujuan")
    val targetedDept: String? = null,
    @field:SerializedName("tgl_waktu_done")
    val doneDateTime: String? = null,
    @field:SerializedName("name_user_reject")
    val rejectFullName: String? = null,
    @field:SerializedName("usern_approve")
    val approvedUserName: String? = null,
    @field:SerializedName("nama_user_pelapor_approve")
    val reporterApproveFullName: String? = null,
    @field:SerializedName("keterangan")
    val description: String? = null,
    @field:SerializedName("tgl_waktu_trial_start")
    val trialStartDateTime: String? = null,
    @field:SerializedName("usern_user_done")
    val doneUserName: String? = null,
    @field:SerializedName("id_user_pelapor_approve")
    val reporterApproveUserId: Int? = null,
    @field:SerializedName("nama_user_hold")
    val holdFullName: String? = null,
    @field:SerializedName("tgl_waktu_pelapor_reject")
    val reporterRejectDateTime: String? = null,
    @field:SerializedName("id_starconnect_user_buat")
    val creatorStarConnectId: Int? = null,
    @field:SerializedName("usern_user_resume")
    val resumeUserName: String? = null,
    @field:SerializedName("nama_kategori")
    val categoryName: String? = null,
    @field:SerializedName("id_kategori")
    val categoryId: Int? = null,
    @field:SerializedName("id_user_resume")
    val resumeUserId: Int? = null,
    @field:SerializedName("id_user")
    val userId: Int? = null,
    @field:SerializedName("set_tglinput")
    val inputDateSet: String? = null,
    @field:SerializedName("nama_mesin")
    val machineName: String? = null,
    @field:SerializedName("tgl_waktu_lapor")
    val reportDateTime: String? = null,
    @field:SerializedName("usern_user_teknisi")
    val techniciansUser: List<TechniciansUserItem?>? = null,
    @field:SerializedName("keterangan_pelapor_approve")
    val reporterApproveDesc: String? = null,
    @field:SerializedName("tgl_waktu_trial_end")
    val trialEndDateTime: String? = null,
    @field:SerializedName("tgl_waktu_kerja_start")
    val workStartDateTime: String? = null,
    @field:SerializedName("tgl_waktu_kerja_end")
    val workEndDateTime: String? = null,
    @field:SerializedName("usern_user_pelapor_reject")
    val reporterRejectUserName: String? = null,
    @field:SerializedName("id_user_reject")
    val rejectUserId: Int? = null,
    @field:SerializedName("id_user_done")
    val doneUserId: Int? = null,
    @field:SerializedName("tgl_waktu_hold")
    val holdDateTime: String? = null,
    @field:SerializedName("is_already_trial")
    val isReadyForTrial: Boolean? = null,
    @field:SerializedName("lokasi_lat")
    val latitude: String? = null,
    @field:SerializedName("lokasi_long")
    val longitude: String? = null,
    @field:SerializedName("lokasi_nama")
    val locationName: String? = null,
    @field:SerializedName("lokasi_id")
    val locationId: Int? = null,
    @field:SerializedName("isVendor")
    val isVendor: String? = null,
    @field:SerializedName("vendorName")
    val vendorName: String? = null,
    @field:SerializedName("btnApvPlp")
    val isReporterManagerCanApprove: Boolean? = null,
    @field:SerializedName("is_exceeding")
    val isExceeding: Boolean? = null,
    @field:SerializedName("limit")
    val limitTime: Int? = null,
    @field:SerializedName("total_hours")
    val totalHours: Int? = null,
    @field:SerializedName("time_offset")
    val timeOffset: Int? = null,
) : Parcelable

@Parcelize
data class UsernUserSpvItem(
    @field:SerializedName("id_table_supervisor")
    val supervisorId: Int? = null,
    @field:SerializedName("dept_user")
    val userDept: String? = null,
    @field:SerializedName("id_user")
    val userId: Int? = null,
    @field:SerializedName("nama_user")
    val fullName: String? = null,
    @field:SerializedName("tgl_waktu")
    val dateTime: String? = null
) : Parcelable
