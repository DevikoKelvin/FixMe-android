package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class UpdateSubmissionResponse(
    @field:SerializedName("code")
    val code: Int? = null,
    @field:SerializedName("test_foto")
    val testFoto: Int? = null,
    @field:SerializedName("message")
    val message: String? = null
)
