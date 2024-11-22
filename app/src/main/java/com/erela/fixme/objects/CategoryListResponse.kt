package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class CategoryListResponse(
    @field:SerializedName("sts_aktif")
    val stsAktif: String? = null,
    @field:SerializedName("id_kategori")
    val idKategori: String? = null,
    @field:SerializedName("nama_kategori")
    val namaKategori: String? = null
)
