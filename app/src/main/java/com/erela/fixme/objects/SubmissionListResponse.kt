package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class SubmissionListResponse(

    @field:SerializedName("usern_approve")
    val usernApprove: String? = null,

    @field:SerializedName("keterangan")
    val keterangan: String? = null,

    @field:SerializedName("usern_user_done")
    val usernUserDone: Any? = null,

    @field:SerializedName("id_gaprojects")
    val idGaprojects: String? = null,

    @field:SerializedName("id_user_end")
    val idUserEnd: String? = null,

    @field:SerializedName("kode_mesin")
    val kodeMesin: String? = null,

    @field:SerializedName("id_approved")
    val idApproved: String? = null,

    @field:SerializedName("tgl_waktu_end")
    val tglWaktuEnd: Any? = null,

    @field:SerializedName("tgl_waktu_start")
    val tglWaktuStart: String? = null,

    @field:SerializedName("sts_gaprojects")
    val stsGaprojects: String? = null,

    @field:SerializedName("usern_user_end")
    val usernUserEnd: String? = null,

    @field:SerializedName("foto_gaprojects")
    val fotoGaprojects: List<FotoGaprojectsItem?>? = null,

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
    val idUserDone: Any? = null,

    @field:SerializedName("tgl_waktu_pengerjaan")
    val tglWaktuPengerjaan: String? = null,

    @field:SerializedName("usern_cancel")
    val usernCancel: String? = null,

    @field:SerializedName("tgl_waktu_done")
    val tglWaktuDone: Any? = null
)

data class FotoGaprojectsItem(

    @field:SerializedName("foto")
    val foto: String? = null,

    @field:SerializedName("id_foto_gaprojects")
    val idFotoGaprojects: String? = null,

    @field:SerializedName("id_gaprojects")
    val idGaprojects: String? = null,

    @field:SerializedName("tgl_waktu")
    val tglWaktu: String? = null
)
