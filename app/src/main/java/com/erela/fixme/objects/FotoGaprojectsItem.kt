package com.erela.fixme.objects

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class FotoGaprojectsItem(
    @field:SerializedName("foto")
    val photo: String? = null,
    @field:SerializedName("id_foto_gaprojects")
    val photoId: Int? = null,
    @field:SerializedName("id_gaprojects")
    val caseId: Int? = null,
    @field:SerializedName("tgl_waktu")
    val dateTime: String? = null
) : Parcelable
