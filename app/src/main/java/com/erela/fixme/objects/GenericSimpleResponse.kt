package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class GenericSimpleResponse(
    @field:SerializedName("code")
    val code: Int? = null,
    @field:SerializedName("message")
    val message: String? = null
)
