package com.erela.fixme.objects

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubmissionDetailResponse(
    @field:SerializedName("usern_approve")
    val usernApprove: String? = null,
    @field:SerializedName("keterangan")
    val keterangan: String? = null,
    @field:SerializedName("tgl_waktu_trial_start")
    val tglWaktuTrialStart: String? = null,
    @field:SerializedName("usern_user_done")
    val usernUserDone: String? = null,
    @field:SerializedName("id_gaprojects")
    val idGaprojects: Int? = null,
    @field:SerializedName("id_dept_tujuan")
    val idDeptTujuan: Int? = null,
    @field:SerializedName("kode_mesin")
    val kodeMesin: String? = null,
    @field:SerializedName("id_approved")
    val idApproved: Int? = null,
    @field:SerializedName("id_user_approve")
    val idUserApprove: Int? = null,
    @field:SerializedName("sts_gaprojects")
    val stsGaprojects: Int? = null,
    @field:SerializedName("usern_user_end")
    val usernUserEnd: String? = null,
    @field:SerializedName("nama_kategori")
    val namaKategori: String? = null,
    @field:SerializedName("foto_gaprojects")
    val fotoGaprojects: List<FotoGaprojectsItem?>? = null,
    @field:SerializedName("id_kategori")
    val idKategori: Int? = null,
    @field:SerializedName("id_user")
    val idUser: Int? = null,
    @field:SerializedName("set_tglinput")
    val setTglinput: String? = null,
    @field:SerializedName("nama_mesin")
    val namaMesin: String? = null,
    @field:SerializedName("tgl_waktu_lapor")
    val tglWaktuLapor: String? = null,
    @field:SerializedName("tgl_input")
    val tglInput: String? = null,
    @field:SerializedName("ket_approved")
    val ketApproved: String? = null,
    @field:SerializedName("set_waktuinput")
    val setWaktuinput: String? = null,
    @field:SerializedName("tgl_waktu_trial_end")
    val tglWaktuTrialEnd: String? = null,
    @field:SerializedName("material")
    val material: List<MaterialItem?>? = null,
    @field:SerializedName("lokasi")
    val lokasi: String? = null,
    @field:SerializedName("tgl_waktu_kerja_start")
    val tglWaktuKerjaStart: String? = null,
    @field:SerializedName("tgl_waktu_kerja_end")
    val tglWaktuKerjaEnd: String? = null,
    @field:SerializedName("tglwaktu_approved")
    val tglwaktuApproved: String? = null,
    @field:SerializedName("judul_kasus")
    val judulKasus: String? = null,
    @field:SerializedName("id_user_done")
    val idUserDone: Int? = null,
    @field:SerializedName("dept_tujuan")
    val deptTujuan: String? = null,
    @field:SerializedName("tgl_waktu_done")
    val tglWaktuDone: String? = null
) : Parcelable

@Parcelize
data class MaterialItem(
    @field:SerializedName("nama_material")
    val namaMaterial: String? = null,
    @field:SerializedName("id_material")
    val idMaterial: Int? = null
) : Parcelable
