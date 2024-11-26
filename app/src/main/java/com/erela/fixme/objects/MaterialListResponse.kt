package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class MaterialListResponse(
    @field:SerializedName("sts_aktif")
    val stsAktif: String? = null,
    @field:SerializedName("harga")
    val harga: String? = null,
    @field:SerializedName("nama_material")
    val namaMaterial: String? = null,
    @field:SerializedName("satuan")
    val satuan: String? = null,
    @field:SerializedName("qty")
    val qty: String? = null,
    @field:SerializedName("id_material")
    val idMaterial: String? = null
)

data class SelectedMaterialList(
    val material: MaterialListResponse? = null,
    var isSelected: Boolean = false
)