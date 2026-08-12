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
    @SerializedName("location_name")
    val location: String,
    @SerializedName("detail")
    val detail: String?,
    @SerializedName("area")
    val area: String?,
    // ac_units.floor is varchar: real values include "2M", "4M", "GOJ", "KANTIN".
    // Typing it Int? made IntegerTypeAdapter silently coerce those to 0.
    @SerializedName("floor")
    val floor: String?,
    @SerializedName("brand")
    val brand: String?,
    @SerializedName("model_type")
    val modelType: String?,
    // Nullable across the board: the card hides a row rather than printing a dash, so "no
    // value" has to survive as null instead of being defaulted to 0 or "".
    //
    // capacity_pk is DECIMAL(5,2), which PDO hands over as a string, so the JSON carries
    // "2.00" rather than 2.0. Double is still correct — JsonReader.nextDouble() parses a
    // quoted number — but do not "fix" the quotes by retyping this as String.
    @SerializedName("capacity_pk")
    val capacityPk: Double? = null,
    @SerializedName("last_maintenance_at")
    val lastMaintenanceAt: String? = null,
    /** Supervisor's brief for the whole schedule, from ac_maintenance_schedules.notes. */
    @SerializedName("schedule_notes")
    val scheduleNotes: String? = null,
    @SerializedName("assigned_technician")
    val assignedTechnician: String?,
    @SerializedName("log_id")
    val logId: Int?
)
