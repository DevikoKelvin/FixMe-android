package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class SubmitSubmissionResponse(
    @field:SerializedName("code")
    val code: Int? = null,
    @field:SerializedName("last_id")
    val lastId: Int? = null,
    @field:SerializedName("message")
    val message: String? = null
)
