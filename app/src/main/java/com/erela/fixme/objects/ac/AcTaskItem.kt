package com.erela.fixme.objects.ac

import com.google.gson.annotations.SerializedName

data class AcTaskItem(
    @SerializedName("item_id")
    val itemId: Int,
    @SerializedName("ac_id")
    val acId: Int,
    @SerializedName("assigned_to")
    val assignedTo: Int?,
    @SerializedName("item_status")
    val itemStatus: String,
    @SerializedName("schedule_id")
    val scheduleId: Int,
    @SerializedName("schedule_name")
    val scheduleName: String,
    @SerializedName("date_start")
    val dateStart: String,
    @SerializedName("date_end")
    val dateEnd: String,
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
    @SerializedName("frequency_months")
    val frequencyMonths: Int,
    @SerializedName("assigned_technician")
    val assignedTechnician: String?
)
