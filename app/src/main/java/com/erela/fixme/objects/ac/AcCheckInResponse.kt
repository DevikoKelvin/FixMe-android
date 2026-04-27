package com.erela.fixme.objects.ac

import com.google.gson.annotations.SerializedName

data class AcCheckInResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("log_id")
    val logId: Int?   // present on code=1 and code=2
) {
    val isSuccess get() = code == 1
    val isAlreadyIn get() = code == 2
}