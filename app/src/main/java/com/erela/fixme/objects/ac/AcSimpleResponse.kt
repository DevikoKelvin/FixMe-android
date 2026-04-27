package com.erela.fixme.objects.ac

import com.google.gson.annotations.SerializedName

data class AcSimpleResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String
) {
    val isSuccess get() = code == 1
}
