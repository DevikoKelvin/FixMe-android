package com.erela.fixme.objects.ac

import com.google.gson.annotations.SerializedName

data class AcUnit(
    @SerializedName("ac_id")
    val acId: Int,
    @SerializedName("ac_code")
    val acCode: String,
    @SerializedName("detail")
    val detail: String?,
    @SerializedName("area")
    val area: String?,
    @SerializedName("floor")
    val floor: Int?,
    @SerializedName("brand")
    val brand: String?,
    @SerializedName("model_type")
    val modelType: String?,
    @SerializedName("capacity_pk")
    val capacityPk: Double?,
    @SerializedName("frequency_months")
    val frequencyMonths: Int,
    @SerializedName("is_active")
    val isActive: Int,
    @SerializedName("last_maintenance_at")
    val lastMaintenanceAt: String?,
    @SerializedName("next_maintenance_date")
    val nextMaintenanceDate: String?
)
