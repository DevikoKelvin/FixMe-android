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
    val photo: String? = null,
    @field:SerializedName("id_foto_gaprojects_detail")
    val caseDetailPhotoId: Int? = null,
    @field:SerializedName("id_gaprojects")
    val caseId: Int? = null,
    @field:SerializedName("id_gaprojects_detail")
    val caseDetailId: Int? = null,
    @field:SerializedName("tgl_waktu")
    val dateTime: String? = null
) : Parcelable

@Parcelize
data class ProgressItems(
    var isExpanded: Boolean = false,
    val progress: ProgressDataItem? = null
) : Parcelable

@Parcelize
data class ProgressDataItem(
    @field:SerializedName("keterangan")
    val description: String? = null,
    @field:SerializedName("sts_aktif")
    val isActive: Int? = null,
    @field:SerializedName("approve_material_tgl_waktu")
    val approvedMaterialDateTime: String? = null,
    @field:SerializedName("approve_material_status")
    val approvedMaterialStatus: Int? = null,
    @field:SerializedName("analisa")
    val analysis: String? = null,
    @field:SerializedName("id_user")
    val userId: Int? = null,
    @field:SerializedName("nama_user")
    val fullName: String? = null,
    @field:SerializedName("id_gaprojects_detail")
    val caseDetailId: Int? = null,
    @field:SerializedName("id_gaprojects")
    val caseId: Int? = null,
    @field:SerializedName("id_user_spv")
    val supervisorUserId: Int? = null,
    @field:SerializedName("tgl_waktu")
    val dateTime: String? = null,
    @field:SerializedName("foto")
    val photo: List<FotoItem?>? = null,
    @field:SerializedName("approve_material_id_user")
    val approvedMaterialUserId: Int? = null,
    @field:SerializedName("approve_material_user")
    val approvedMaterialFullName: String? = null,
    @field:SerializedName("sts_gaprojects")
    val status: Int? = null,
    @field:SerializedName("keterangan_approve")
    val approveDesc: String? = null,
    @field:SerializedName("sts_detail")
    val detailStatus: Int? = null,
    @field:SerializedName("tgl_waktu_done")
    val doneDateTime: String? = null,
    @field:SerializedName("material")
    val materials: List<MaterialItem?>? = null,
) : Parcelable

@Parcelize
data class MaterialItem(
    @field:SerializedName("nama_material")
    val materialName: String? = null,
    @field:SerializedName("id_material")
    val materialId: Int? = null,
    @field:SerializedName("kode_material")
    val materialCode: String? = null,
    @field:SerializedName("qty_material")
    val materialQuantity: Int? = null,
    @field:SerializedName("satuan")
    val unit: String? = null,
    @field:SerializedName("harga")
    val price: Int? = null,
    @field:SerializedName("id_kategori")
    val categoryId: Int? = null,
    @field:SerializedName("sts_aktif")
    val isActive: Int? = null
) : Parcelable
