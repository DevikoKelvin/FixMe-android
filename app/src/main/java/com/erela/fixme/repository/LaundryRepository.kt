package com.erela.fixme.repository

import android.content.Context
import com.erela.fixme.R
import com.erela.fixme.helpers.api.GetEndpoint
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.laundry.LaundryAddItemsRequest
import com.erela.fixme.objects.laundry.LaundryCheckInItem
import com.erela.fixme.objects.laundry.LaundryConditionOutRequest
import com.erela.fixme.objects.laundry.LaundryPickupRequest
import com.erela.fixme.objects.laundry.LaundryHandoverMarkRequest

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

    /** The courier: their department's batches [T-08], open ones first. */
    suspend fun myBatches() = runCatching { api.laundryMyBatches(lang) }

    /** One of those batches, with its garments. */
    suspend fun batch(idTrx: Int) = runCatching { api.laundryBatch(idTrx, lang) }

    /** The operator: batches waiting to be handed back, oldest ready first. */
    suspend fun handoverQueue() = runCatching { api.laundryHandoverQueue(lang) }

    /** The operator: scan the bundle back out and mark it ready to hand over. */
    suspend fun markHandover(idTrx: Int, qrCodes: List<String>) =
        runCatching { api.laundryHandoverMark(LaundryHandoverMarkRequest(idTrx, qrCodes)) }

    /** The courier: collect, by scanning the counter sticker. */
    suspend fun collect(counterCode: String, idTrx: Int) =
        runCatching { api.laundryCollect(counterCode, idTrx, lang) }

    /** The counter: bundles already scanned in, not yet ready to collect. */
    suspend fun activeQueue() = runCatching { api.laundryActiveQueue(lang) }

    /** The counter: any batch, with the two counts the Accept button turns on. */
    suspend fun counterBatch(idTrx: Int) = runCatching { api.laundryCounterBatch(idTrx, lang) }

    /** The counter's process actions. Rules server-side; these only carry the request. */
    suspend fun verify(idTrx: Int, qrCode: String, conditionIn: String, note: String?) =
        runCatching { api.laundryVerify(idTrx, qrCode, conditionIn, note, lang) }

    suspend fun accept(idTrx: Int, override: Boolean = false, reason: String? = null) =
        runCatching { api.laundryAccept(idTrx, override, reason, lang) }

    suspend fun washStart(idTrx: Int) = runCatching { api.laundryWashStart(idTrx, lang) }

    suspend fun conditionOut(idLines: List<Int>, condition: String, note: String?) =
        runCatching { api.laundryConditionOut(LaundryConditionOutRequest(idLines, condition, note)) }

    suspend fun markReady(idTrx: Int) = runCatching { api.laundryReady(idTrx, lang) }

    suspend fun collectors(idTrx: Int) = runCatching { api.laundryCollectors(idTrx, lang) }

    suspend fun pickup(idTrx: Int, idCollector: Int, itemIds: List<Int>) =
        runCatching { api.laundryPickup(LaundryPickupRequest(idTrx, idCollector, itemIds)) }

    /** The counter: finished batches, optionally within a date range. */
    suspend fun history(from: String? = null, to: String? = null) =
        runCatching { api.laundryHistory(from, to, lang) }

    /** The counter: one slip as printable lines. The server logs the print. */
    suspend fun slip(idTrx: Int, out: String? = null, reason: String? = null, cols: Int = 48) =
        runCatching { api.laundrySlip(idTrx, out, reason, cols, lang) }

    /** One patch, resolved before it joins the bundle. */
    suspend fun scan(qrCode: String) =
        runCatching { api.laundryScan(qrCode, lang) }

    suspend fun myCheckIns() =
        runCatching { api.laundryMyCheckIns(lang) }
}
