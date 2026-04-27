package com.erela.fixme.repository

import android.content.Context
import com.erela.fixme.R
import com.erela.fixme.helpers.api.GetEndpoint
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.ac.AcSimpleResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AcRepository(
    private val context: Context,
    private val api: GetEndpoint = InitAPI.getEndpoint
) {
    private val lang: String
        get() = context.getString(R.string.lang)

    suspend fun scan(acCode: String, userId: Int) =
        runCatching { api.acScan(acCode, userId, lang) }

    suspend fun checkIn(itemId: Int, userId: Int, lat: Double?, lng: Double?) =
        runCatching { api.acCheckIn(itemId, userId, lat, lng, lang) }

    suspend fun addTechnician(logId: Int, userId: Int) =
        runCatching { api.acAddTechnician(logId, userId, lang) }

    suspend fun removeTechnician(logId: Int, userId: Int) =
        runCatching { api.acRemoveTechnician(logId, userId, lang) }

    suspend fun taskList(userId: Int) =
        runCatching { api.acTaskList(userId, lang) }

    suspend fun checkOut(
        logId: Int,
        userId: Int,
        acCondition: String,
        photos: List<File>,
        photoTypes: List<String>,
        findings: String?,
        actionsTaken: String?,
        lat: Double?,
        lng: Double?
    ): Result<AcSimpleResponse> = runCatching {
        fun String.toBody() = toRequestBody("text/plain".toMediaTypeOrNull())
        fun Double?.toBodyOrNull() =
            this?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

        val photoParts = photos.mapIndexed { i, file ->
            val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("photos[]", file.name, reqFile)
        }
        val typeParts = photoTypes.map { type ->
            MultipartBody.Part.createFormData("photo_types[]", type)
        }

        api.acCheckOut(
            logId = logId.toString().toBody(),
            userId = userId.toString().toBody(),
            acCondition = acCondition.toBody(),
            photos = photoParts,
            photoTypes = typeParts,
            findings = findings?.toBody(),
            actionsTaken = actionsTaken?.toBody(),
            lat = lat.toBodyOrNull(),
            lng = lng.toBodyOrNull(),
            lang = lang.toBody()
        )
    }
}