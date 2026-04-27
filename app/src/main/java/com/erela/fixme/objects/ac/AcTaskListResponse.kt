package com.erela.fixme.objects.ac

import com.google.gson.annotations.SerializedName

data class AcTaskListResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: List<AcTaskItem>?
) {
    val isSuccess get() = code == 1
}
