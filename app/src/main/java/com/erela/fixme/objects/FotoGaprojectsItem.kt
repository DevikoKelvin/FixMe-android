package com.erela.fixme.objects

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class FotoGaprojectsItem(
    @field:SerializedName("foto")
    val foto: String? = null,
    @field:SerializedName("id_foto_gaprojects")
    val idFotoGaprojects: Int? = null,
    @field:SerializedName("id_gaprojects")
    val idGaprojects: Int? = null,
    @field:SerializedName("tgl_waktu")
    val tglWaktu: String? = null
) : Parcelable
