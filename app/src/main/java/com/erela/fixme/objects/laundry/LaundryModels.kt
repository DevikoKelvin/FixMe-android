package com.erela.fixme.objects.laundry

import com.google.gson.annotations.SerializedName

/**
 * Smart Wash counter check-in — every model for the flow, in one file.
 *
 * The AC feature keeps one data class per file, and these are deliberately not: seven eight-line
 * files for one screen is boilerplate, and all of these are meaningless apart from each other.
 * Split them the day any of them is used by a second feature.
 *
 * The envelope is the house mobile shape — `{code, message, data}` with **HTTP 200 even on
 * failure**, because that is what the installed 1.4.x clients parse. `code == 1` is the only
 * success test.
 */

/**
 * One scanned patch, resolved before it joins the bundle.
 *
 * THIS ENDPOINT HAS NO WEB EQUIVALENT AND THAT IS THE POINT. On the web the operator reads the
 * code off the tag and can see the garment in their hand. Scanning is blind: the camera reports six
 * digits, and without this the courier cannot tell a wrong garment from a right one until submit.
 */
data class LaundryScanResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: LaundryGarment?
) {
    val isSuccess get() = code == 1
}

data class LaundryGarment(
    @SerializedName("qr_code") val qrCode: String,
    @SerializedName("id_laundry_item") val idLaundryItem: Int,
    @SerializedName("item_type") val itemType: String?,
    @SerializedName("size") val size: String?,
    @SerializedName("condition_initial") val conditionInitial: String?,
    @SerializedName("master_status") val masterStatus: String?,
    @SerializedName("owner_name") val ownerName: String?,
    @SerializedName("owner_dept") val ownerDept: String?,
    @SerializedName("owner_sub_dept") val ownerSubDept: String?
) {
    /** A per-garment note the courier may add before submitting. Not from the server. */
    var note: String? = null
}

/**
 * One garment in a saved batch.
 *
 * A list of objects is why [LaundryAddItemsRequest] goes out as a JSON body: form encoding cannot
 * express it without building `items[0][qr_code]` keys by hand, and Gson is already configured on
 * this Retrofit instance.
 */
data class LaundryCheckInItem(
    @SerializedName("qr_code") val qrCode: String,
    @SerializedName("note") val note: String?
)

/**
 * The operator's scanned garments, written onto a courier's arrival.
 *
 * No counter code: the arrival already knows which counter it was opened at, and re-sending it
 * would let the two disagree.
 */
data class LaundryAddItemsRequest(
    @SerializedName("id_trx") val idTrx: Int,
    @SerializedName("note") val note: String?,
    @SerializedName("items") val items: List<LaundryCheckInItem>
)

data class LaundryCheckInResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: LaundryTransaction?
) {
    val isSuccess get() = code == 1
}

data class LaundryTransaction(
    @SerializedName("id_trx") val idTrx: Int,
    @SerializedName("trx_no") val trxNo: String,
    @SerializedName("status") val status: String,
    @SerializedName("item_count") val itemCount: Int,
    @SerializedName("owner_count") val ownerCount: Int,
    /** Garments in this bundle belonging to ANOTHER department — they become held items. */
    @SerializedName("mixed_count") val mixedCount: Int,
    /**
     * Garments HELD from an earlier batch that joined this one, already clean.
     *
     * The bundle grew: the courier is about to be handed more than they brought, and the slip
     * will show each of these as `+1`.
     */
    @SerializedName("attached_count") val attachedCount: Int
)

/**
 * The courier's arrival: "I am here with a bundle".
 *
 * NO ITEMS. GA split check-in on 12 Sep 2026 - the courier scans the counter and stops there, and
 * the counter operator scans the garments onto this transaction afterwards.
 */
data class LaundryArrivalResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: LaundryArrival?
) {
    val isSuccess get() = code == 1
}

data class LaundryArrival(
    @SerializedName("id_trx") val idTrx: Int,
    @SerializedName("trx_no") val trxNo: String,
    @SerializedName("item_count") val itemCount: Int,
    /**
     * True when this arrival already existed.
     *
     * Scanning the counter twice is an ordinary slip - the phone is in their hand and the sticker
     * is right there - so the server returns the SAME arrival rather than opening a second empty
     * one. The screen says "already registered" instead of implying a fresh one was made.
     */
    @SerializedName("already_open") val alreadyOpen: Boolean = false
)

/** The operator's queue: couriers waiting for their bundle to be scanned, oldest first. */
data class LaundryArrivalsResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<LaundryWaitingCourier>?
) {
    val isSuccess get() = code == 1
}

data class LaundryWaitingCourier(
    @SerializedName("id") val id: Int,
    @SerializedName("trx_no") val trxNo: String,
    @SerializedName("checked_in_at") val checkedInAt: String?,
    @SerializedName("nama_dept") val namaDept: String?,
    @SerializedName("sub_dept") val subDept: String?,
    @SerializedName("pengantar") val pengantar: String?
)

/** The caller's own recent hand-overs, so the app can confirm one landed. */
data class LaundryMyCheckInsResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<LaundryRecentTransaction>?
) {
    val isSuccess get() = code == 1
}

data class LaundryRecentTransaction(
    @SerializedName("id") val id: Int,
    @SerializedName("trx_no") val trxNo: String,
    @SerializedName("status") val status: String,
    @SerializedName("checked_in_at") val checkedInAt: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("item_count") val itemCount: Int
)
