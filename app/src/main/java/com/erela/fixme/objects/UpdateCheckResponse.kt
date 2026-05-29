package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class UpdateCheckResponse(
    @field:SerializedName("code") val code: Int = 0,
    @field:SerializedName("message") val message: String? = null,
    @field:SerializedName("version_name") val versionName: String? = null,
    @field:SerializedName("download_url") val downloadUrl: String? = null,
    @field:SerializedName("changelog") val changelog: String? = null,
    @field:SerializedName("force_update") val forceUpdate: Boolean = false
)
