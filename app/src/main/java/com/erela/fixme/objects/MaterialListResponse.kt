package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class MaterialListResponse(
    @field:SerializedName("sts_aktif")
    val stsAktif: Int? = null,
    @field:SerializedName("harga")
    val harga: Int? = null,
    @field:SerializedName("nama_material")
    val namaMaterial: String? = null,
    @field:SerializedName("satuan")
    val satuan: String? = null,
    @field:SerializedName("id_material")
    val idMaterial: Int? = null,
    @field:SerializedName("kode_material")
    val kodeMaterial: String? = null,
    @field:SerializedName("id_kategori")
    val idKategori: Int? = null
)

data class SelectedMaterialList(
    var isSelected: Boolean = false,
    var quantity: Int? = null,
    val material: MaterialListResponse? = null
)