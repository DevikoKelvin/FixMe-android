package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class SubmissionDetailResponse(
    @field:SerializedName("usern_approve")
    val usernApprove: String? = null,
    @field:SerializedName("keterangan")
    val keterangan: String? = null,
    @field:SerializedName("usern_user_done")
    val usernUserDone: String? = null,
    @field:SerializedName("id_gaprojects")
    val idGaprojects: String? = null,
    @field:SerializedName("id_user_end")
    val idUserEnd: String? = null,
    @field:SerializedName("kode_mesin")
    val kodeMesin: String? = null,
    @field:SerializedName("id_approved")
    val idApproved: String? = null,
    @field:SerializedName("tgl_waktu_end")
    val tglWaktuEnd: String? = null,
    @field:SerializedName("tgl_waktu_start")
    val tglWaktuStart: String? = null,
    @field:SerializedName("sts_gaprojects")
    val stsGaprojects: String? = null,
    @field:SerializedName("usern_user_end")
    val usernUserEnd: String? = null,
    @field:SerializedName("foto_gaprojects")
    val fotoGaprojects: List<FotoGaprojectsItem?>? = null,
    @field:SerializedName("id_user")
    val idUser: String? = null,
    @field:SerializedName("dept")
    val dept: String? = null,
    @field:SerializedName("set_tglinput")
    val setTglinput: String? = null,
    @field:SerializedName("nama_mesin")
    val namaMesin: String? = null,
    @field:SerializedName("tgl_input")
    val tglInput: String? = null,
    @field:SerializedName("ket_approved")
    val ketApproved: String? = null,
    @field:SerializedName("set_waktuinput")
    val setWaktuinput: String? = null,
    @field:SerializedName("lokasi")
    val lokasi: String? = null,
    @field:SerializedName("tglwaktu_approved")
    val tglwaktuApproved: String? = null,
    @field:SerializedName("judul_kasus")
    val judulKasus: String? = null,
    @field:SerializedName("tgl_waktu_actual")
    val tglWaktuActual: String? = null,
    @field:SerializedName("id_user_done")
    val idUserDone: Int? = null,
    @field:SerializedName("tgl_waktu_pengerjaan")
    val tglWaktuPengerjaan: String? = null,
    @field:SerializedName("usern_cancel")
    val usernCancel: String? = null,
    @field:SerializedName("gaprojects_detail")
    val gaprojectsDetail: List<GaprojectsDetailItem?>? = null,
    @field:SerializedName("tgl_waktu_done")
    val tglWaktuDone: String? = null
)

data class FotoItem(
    @field:SerializedName("foto")
    val foto: String? = null
)

data class MaterialItem(
    @field:SerializedName("harga")
    val harga: String? = null,
    @field:SerializedName("nama_material")
    val namaMaterial: String? = null,
    @field:SerializedName("satuan")
    val satuan: String? = null,
    @field:SerializedName("subtotal")
    val subtotal: String? = null,
    @field:SerializedName("qty")
    val qty: String? = null,
    @field:SerializedName("id_gaprojects_material")
    val idGaprojectsMaterial: String? = null,
    @field:SerializedName("id_gaprojects")
    val idGaprojects: String? = null,
    @field:SerializedName("id_gaprojects_detail")
    val idGaprojectsDetail: String? = null,
    @field:SerializedName("id_material")
    val idMaterial: String? = null
)

data class GaprojectsDetailItem(
    @field:SerializedName("keterangan")
    val keterangan: String? = null,
    @field:SerializedName("material")
    val material: List<MaterialItem?>? = null,
    @field:SerializedName("foto")
    val foto: List<FotoItem?>? = null,
    @field:SerializedName("usern")
    val usern: String? = null,
    @field:SerializedName("tgl")
    val tgl: String? = null,
    @field:SerializedName("waktu")
    val waktu: String? = null,
    @field:SerializedName("id_user")
    val idUser: String? = null,
    @field:SerializedName("id_gaprojects_detail")
    val idGaprojectsDetail: String? = null,
    @field:SerializedName("id_gaprojects")
    val idGaprojects: String? = null,
    @field:SerializedName("tgl_waktu")
    val tglWaktu: String? = null
)
