package com.erela.fixme.repository

import android.content.Context
import com.erela.fixme.R
import com.erela.fixme.helpers.api.GetEndpoint
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.laundry.LaundryCheckInItem
import com.erela.fixme.objects.laundry.LaundryCheckInRequest

/**
 * Smart Wash counter check-in, as `AcRepository` does it: `runCatching` around each call.
 *
 * NO `userId` PARAMETER ANYWHERE, unlike `AcRepository`. The server takes the courier's identity
 * from the bearer token and overwrites any `user_id` in the body, so passing one would be dead
 * weight that reads as if it mattered — and it is the check-in spoofing guard that makes it not
 * matter.
 */
class LaundryRepository(
    private val context: Context,
    private val api: GetEndpoint = InitAPI.getEndpoint
) {
    private val lang: String
        get() = context.getString(R.string.lang)

    /** The counter sticker. Also answers whether this courier's department may hand laundry in. */
    suspend fun counter(code: String) =
        runCatching { api.laundryCounter(code, lang) }

    /** One patch, resolved before it joins the bundle. */
    suspend fun scan(qrCode: String) =
        runCatching { api.laundryScan(qrCode, lang) }

    /** The whole bundle, together or not at all. */
    suspend fun checkIn(counterCode: String, note: String?, items: List<LaundryCheckInItem>) =
        runCatching {
            api.laundryCheckIn(LaundryCheckInRequest(counterCode, note, items))
        }

    suspend fun myCheckIns() =
        runCatching { api.laundryMyCheckIns(lang) }
}
