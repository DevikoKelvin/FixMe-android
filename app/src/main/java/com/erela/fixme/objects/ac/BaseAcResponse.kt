package com.erela.fixme.objects.ac

import com.google.gson.annotations.SerializedName

data class BaseAcResponse<T>(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String
) {
    val isSuccess get() = code == 1
    val isAlreadyDone get() = code == 2  // acCheckIn resume case
}