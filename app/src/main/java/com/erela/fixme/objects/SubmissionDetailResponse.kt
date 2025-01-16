package com.erela.fixme.objects

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.io.Serial

@Parcelize
data class SubmissionDetailResponse(
    @field:SerializedName("name_user_done")
    val nameUserDone: String? = null,
    @field:SerializedName("usern_user_hold")
    val usernUserHold: String? = null,
    @field:SerializedName("keterangan_reject")
    val keteranganReject: String? = null,
    @field:SerializedName("tgl_waktu_cancel")
    val tglWaktuCancel: String? = null,
    @field:SerializedName("keterangan_hold")
    val keteranganHold: String? = null,
    @field:SerializedName("id_gaprojects")
    val idGaprojects: Int? = null,
    @field:SerializedName("keterangan_pelapor_reject")
    val keteranganPelaporReject: String? = null,
    @field:SerializedName("trial")
    val trial: List<TrialItem?>? = null,
    @field:SerializedName("id_dept_tujuan")
    val idDeptTujuan: Int? = null,
    @field:SerializedName("id_user_pelapor_reject")
    val idUserPelaporReject: Int? = null,
    @field:SerializedName("kode_mesin")
    val kodeMesin: String? = null,
    @field:SerializedName("id_user_approve")
    val idUserApprove: Int? = null,
    @field:SerializedName("nomor_request")
    val nomorRequest: String? = null,
    @field:SerializedName("sts_gaprojects")
    val stsGaprojects: Int? = null,
    @field:SerializedName("user_nama_approve")
    val userNamaApprove: String? = null,
    @field:SerializedName("tgl_waktu_reject")
    val tglWaktuReject: String? = null,
    @field:SerializedName("foto_gaprojects")
    val fotoGaprojects: List<FotoGaprojectsItem?>? = null,
    @field:SerializedName("id_user_hold")
    val idUserHold: Int? = null,
    @field:SerializedName("tgl_waktu_pelapor_approve")
    val tglWaktuPelaporApprove: String? = null,
    @field:SerializedName("nama_user_pelapor_reject")
    val namaUserPelaporReject: String? = null,
    @field:SerializedName("nama_user_buat")
    val namaUserBuat: String? = null,
    @field:SerializedName("dept_user")
    val deptUser: String? = null,
    @field:SerializedName("tgl_input")
    val tglInput: String? = null,
    @field:SerializedName("ket_approved")
    val ketApproved: String? = null,
    @field:SerializedName("difficulty")
    val difficulty: String? = null,
    @field:SerializedName("set_waktuinput")
    val setWaktuinput: String? = null,
    @field:SerializedName("nama_user_resume")
    val namaUserResume: String? = null,
    @field:SerializedName("keterangan_cancel")
    val keteranganCancel: String? = null,
    @field:SerializedName("lokasi")
    val lokasi: String? = null,
    @field:SerializedName("usern_user_pelapor_approve")
    val usernUserPelaporApprove: String? = null,
    @field:SerializedName("tgl_waktu_resume")
    val tglWaktuResume: String? = null,
    @field:SerializedName("tglwaktu_approved")
    val tglwaktuApproved: String? = null,
    @field:SerializedName("judul_kasus")
    val judulKasus: String? = null,
    @field:SerializedName("usern_user_spv")
    val usernUserSpv: List<UsernUserSpvItem?>? = null,
    @field:SerializedName("dept_tujuan")
    val deptTujuan: String? = null,
    @field:SerializedName("tgl_waktu_done")
    val tglWaktuDone: String? = null,
    @field:SerializedName("name_user_reject")
    val nameUserReject: String? = null,
    @field:SerializedName("usern_approve")
    val usernApprove: String? = null,
    @field:SerializedName("nama_user_pelapor_approve")
    val namaUserPelaporApprove: String? = null,
    @field:SerializedName("keterangan")
    val keterangan: String? = null,
    @field:SerializedName("tgl_waktu_trial_start")
    val tglWaktuTrialStart: String? = null,
    @field:SerializedName("usern_user_done")
    val usernUserDone: String? = null,
    @field:SerializedName("id_user_pelapor_approve")
    val idUserPelaporApprove: Int? = null,
    @field:SerializedName("nama_user_hold")
    val namaUserHold: String? = null,
    @field:SerializedName("tgl_waktu_pelapor_reject")
    val tglWaktuPelaporReject: String? = null,
    @field:SerializedName("usern_user_resume")
    val usernUserResume: String? = null,
    @field:SerializedName("nama_kategori")
    val namaKategori: String? = null,
    @field:SerializedName("id_kategori")
    val idKategori: Int? = null,
    @field:SerializedName("id_user_resume")
    val idUserResume: Int? = null,
    @field:SerializedName("id_user")
    val idUser: Int? = null,
    @field:SerializedName("set_tglinput")
    val setTglinput: String? = null,
    @field:SerializedName("nama_mesin")
    val namaMesin: String? = null,
    @field:SerializedName("tgl_waktu_lapor")
    val tglWaktuLapor: String? = null,
    @field:SerializedName("usern_user_teknisi")
    val usernUserTeknisi: List<UsernUserTeknisiItem?>? = null,
    @field:SerializedName("keterangan_pelapor_approve")
    val keteranganPelaporApprove: String? = null,
    @field:SerializedName("tgl_waktu_trial_end")
    val tglWaktuTrialEnd: String? = null,
    @field:SerializedName("tgl_waktu_kerja_start")
    val tglWaktuKerjaStart: String? = null,
    @field:SerializedName("tgl_waktu_kerja_end")
    val tglWaktuKerjaEnd: String? = null,
    @field:SerializedName("usern_user_pelapor_reject")
    val usernUserPelaporReject: String? = null,
    @field:SerializedName("progress")
    val progress: List<ProgressItem?>? = null,
    @field:SerializedName("id_user_reject")
    val idUserReject: Int? = null,
    @field:SerializedName("id_user_done")
    val idUserDone: Int? = null,
    @field:SerializedName("tgl_waktu_hold")
    val tglWaktuHold: String? = null
) : Parcelable

@Parcelize
data class MaterialItem(
    @field:SerializedName("nama_material")
    val namaMaterial: String? = null,
    @field:SerializedName("id_material")
    val idMaterial: Int? = null,
    @field:SerializedName("qty_material")
    val qtyMaterial: Int? = null
) : Parcelable

@Parcelize
data class FotoItem(
    @field:SerializedName("foto")
    val foto: String? = null,
    @field:SerializedName("id_foto")
    val idFoto: Int? = null,
    @field:SerializedName("tgl_waktu")
    val tglWaktu: String? = null
) : Parcelable

@Parcelize
data class ProgressItems(
    var isExpanded: Boolean = false,
    val progress: ProgressItem? = null
): Parcelable

@Parcelize
data class ProgressItem(
    @field:SerializedName("nama_spv")
    val namaSpv: String? = null,
    @field:SerializedName("keterangan")
    val keterangan: String? = null,
    @field:SerializedName("analisa")
    val analisa: String? = null,
    @field:SerializedName("usern")
    val usern: String? = null,
    @field:SerializedName("id_user")
    val idUser: Int? = null,
    @field:SerializedName("id_gaprojects_detail")
    val idGaprojectsDetail: Int? = null,
    @field:SerializedName("id_gaprojects")
    val idGaprojects: Int? = null,
    @field:SerializedName("id_user_spv")
    val idUserSpv: Int? = null,
    @field:SerializedName("tgl_waktu")
    val tglWaktu: String? = null,
    @field:SerializedName("foto")
    val foto: List<FotoItem?>? = null,
    @field:SerializedName("tgl")
    val tgl: String? = null,
    @field:SerializedName("waktu")
    val waktu: String? = null,
    @field:SerializedName("nama_user_progress")
    val namaUserProgress: String? = null,
    @field:SerializedName("tgl_waktu_done")
    val tglWaktuDone: String? = null,
    @field:SerializedName("sts_detail")
    val stsDetail: Int? = null,
    @field:SerializedName("material")
    val material: List<MaterialItem?>? = null,
    @field:SerializedName("keterangan_approve")
    val keteranganApprove: String? = null,
    @field:SerializedName("approve_material_id_user")
    val approveMaterialIdUser: Int? = null,
    @field:SerializedName("approve_material_user")
    val approveMaterialUser: String? = null,
    @field:SerializedName("approve_material_tgl_waktu")
    val approveMaterialTglWaktu: String? = null,
    @field:SerializedName("approve_material_status")
    val approveMaterialStatus: Int? = null
) : Parcelable

@Parcelize
data class TrialItem(
    @field:SerializedName("keterangan")
    val keterangan: String? = null,
    @field:SerializedName("id_gaprojects_ket_trial")
    val idGaprojectsKetTrial: Int? = null,
    @field:SerializedName("tgl_waktu")
    val tglWaktu: String? = null,
    @field:SerializedName("status")
    val status: Int? = null
) : Parcelable

@Parcelize
data class UsernUserTeknisiItem(
    @field:SerializedName("id_table_teknisi")
    val idTableTeknisi: Int? = null,
    @field:SerializedName("dept_user")
    val deptUser: String? = null,
    @field:SerializedName("id_user")
    val idUser: Int? = null,
    @field:SerializedName("nama_user")
    val namaUser: String? = null,
    @field:SerializedName("id_user_spv")
    val idUserSpv: Int? = null,
    @field:SerializedName("tgl_waktu")
    val tglWaktu: String? = null
) : Parcelable

@Parcelize
data class UsernUserSpvItem(
    @field:SerializedName("id_table_supervisor")
    val idTableSupervisor: Int? = null,
    @field:SerializedName("dept_user")
    val deptUser: String? = null,
    @field:SerializedName("id_user")
    val idUser: Int? = null,
    @field:SerializedName("nama_user")
    val namaUser: String? = null,
    @field:SerializedName("tgl_waktu")
    val tglWaktu: String? = null
) : Parcelable
