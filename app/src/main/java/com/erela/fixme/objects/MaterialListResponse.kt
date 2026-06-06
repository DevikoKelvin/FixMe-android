package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class MaterialListResponse(
    @field:SerializedName("sts_aktif")
    val status: Int? = null,
    @field:SerializedName("harga")
    val price: Int? = null,
    @field:SerializedName("nama_material")
    val materialName: String? = null,
    @field:SerializedName("satuan")
    val unit: String? = null,
    @field:SerializedName("id_material")
    val materialId: Int? = null,
    @field:SerializedName("kode_material")
    val materialCode: String? = null,
    @field:SerializedName("id_kategori")
    val categoryId: Int? = null
)

data class SelectedMaterialList(
    var isSelected: Boolean = false,
    var quantity: Int? = null,
    val material: MaterialListResponse? = null
)