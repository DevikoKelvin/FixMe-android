package com.erela.fixme.objects.ac

import com.google.gson.annotations.SerializedName

data class AcScanResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("unit")
    val unit: AcUnit?,        // null when code=0 + no unit
    @SerializedName("item")
    val item: AcScheduleItem?,
    @SerializedName("assigned_name")
    val assignedName: String?
) {
    val isSuccess get() = code == 1
}
