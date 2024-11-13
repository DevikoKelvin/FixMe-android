package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

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
