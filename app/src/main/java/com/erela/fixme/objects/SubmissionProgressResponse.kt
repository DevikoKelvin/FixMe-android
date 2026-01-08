package com.erela.fixme.objects

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubmissionProgressResponse(
    @field:SerializedName("code")
    val code: Int? = null,
    @field:SerializedName("data")
    val data: List<ProgressDataItem?>? = null,
    @field:SerializedName("message")
    val message: String? = null
) : Parcelable

@Parcelize
data class FotoItem(
    @field:SerializedName("foto")
    val foto: String? = null,
    @field:SerializedName("id_foto_gaprojects_detail")
    val idFotoGaprojectsDetail: Int? = null,
    @field:SerializedName("id_gaprojects")
    val idGaprojects: Int? = null,
    @field:SerializedName("id_gaprojects_detail")
    val idGaprojectsDetail: Int? = null,
    @field:SerializedName("tgl_waktu")
    val tglWaktu: String? = null
) : Parcelable

@Parcelize
data class ProgressItems(
    var isExpanded: Boolean = false,
    val progress: ProgressDataItem? = null
) : Parcelable

@Parcelize
data class ProgressDataItem(
    @field:SerializedName("keterangan")
    val keterangan: String? = null,
    @field:SerializedName("sts_aktif")
    val stsAktif: Int? = null,
    @field:SerializedName("approve_material_tgl_waktu")
    val approveMaterialTglWaktu: String? = null,
    @field:SerializedName("approve_material_status")
    val approveMaterialStatus: Int? = null,
    @field:SerializedName("analisa")
    val analisa: String? = null,
    @field:SerializedName("id_user")
    val idUser: Int? = null,
    @field:SerializedName("nama_user")
    val namaUser: String? = null,
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
    @field:SerializedName("approve_material_id_user")
    val approveMaterialIdUser: Int? = null,
    @field:SerializedName("approve_material_user")
    val approveMaterialUser: String? = null,
    @field:SerializedName("sts_gaprojects")
    val stsGaprojects: Int? = null,
    @field:SerializedName("keterangan_approve")
    val keteranganApprove: String? = null,
    @field:SerializedName("sts_detail")
    val stsDetail: Int? = null,
    @field:SerializedName("tgl_waktu_done")
    val tglWaktuDone: String? = null,
    @field:SerializedName("material")
    val material: List<MaterialItem?>? = null,
) : Parcelable

@Parcelize
data class MaterialItem(
    @field:SerializedName("nama_material")
    val namaMaterial: String? = null,
    @field:SerializedName("id_material")
    val idMaterial: Int? = null,
    @field:SerializedName("kode_material")
    val kodeMaterial: String? = null,
    @field:SerializedName("qty_material")
    val qtyMaterial: Int? = null,
    @field:SerializedName("satuan")
    val satuan: String? = null,
    @field:SerializedName("harga")
    val harga: Int? = null,
    @field:SerializedName("id_kategori")
    val idKategori: Int? = null,
    @field:SerializedName("sts_aktif")
    val stsAktif: Int? = null
) : Parcelable
