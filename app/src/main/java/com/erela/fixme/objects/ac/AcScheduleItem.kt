package com.erela.fixme.objects.ac

import com.google.gson.annotations.SerializedName

data class AcScheduleItem(
    @SerializedName("item_id")
    val itemId: Int,
    @SerializedName("schedule_id")
    val scheduleId: Int,
    @SerializedName("assigned_to")
    val assignedTo: Int?,
    @SerializedName("item_status")
    val itemStatus: String,
    @SerializedName("date_start")
    val dateStart: String,
    @SerializedName("date_end")
    val dateEnd: String,
    @SerializedName("schedule_name")
    val scheduleName: String
)
