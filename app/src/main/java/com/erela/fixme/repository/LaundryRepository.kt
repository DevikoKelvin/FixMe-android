package com.erela.fixme.repository

import android.content.Context
import com.erela.fixme.R
import com.erela.fixme.helpers.api.GetEndpoint
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.laundry.LaundryAddItemsRequest
import com.erela.fixme.objects.laundry.LaundryCheckInItem

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

    /** The courier: "I am here with a bundle". Opens a transaction with no lines. */
    suspend fun arrive(counterCode: String) =
        runCatching { api.laundryArrive(counterCode, lang) }

    /** The operator: couriers waiting to be served, oldest first. */
    suspend fun arrivals() =
        runCatching { api.laundryArrivals(lang) }

    /** The operator: write scanned garments onto a courier's arrival. */
    suspend fun addItems(idTrx: Int, note: String?, items: List<LaundryCheckInItem>) =
        runCatching { api.laundryAddItems(LaundryAddItemsRequest(idTrx, note, items)) }

    /** One patch, resolved before it joins the bundle. */
    suspend fun scan(qrCode: String) =
        runCatching { api.laundryScan(qrCode, lang) }

    suspend fun myCheckIns() =
        runCatching { api.laundryMyCheckIns(lang) }
}
