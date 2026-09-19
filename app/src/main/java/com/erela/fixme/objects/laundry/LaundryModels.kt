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
    @SerializedName("pengantar") val pengantar: String?,
    /**
     * How many garments are on it - 0 for a courier still waiting, by definition.
     *
     * `laundryArrivals` returns arrivals with NO lines, so the field is absent there and 0 is the
     * truth rather than a fallback. The active and history lists send the same row shape with a
     * real count, which is the only thing that makes those rows worth reading.
     */
    @SerializedName("item_count") val itemCount: Int = 0,
    /**
     * `waiting` or `accepted`, on the active queue only.
     *
     * WHAT THE COUNTER STILL OWES THIS BATCH [GA, 19 Sep 2026]. A `waiting` bundle has garments on
     * it that nobody has verified; an `accepted` one is in the wash and wants nothing. Without
     * this the two drew the same caption under a tab called "In wash", so receipt and verification
     * looked like something only the web could do.
     */
    @SerializedName("status") val status: String? = null,
    /** Garments with no `condition_in` yet - the verification that is left. */
    @SerializedName("unverified") val unverified: Int = 0,
    /** Only the history sends this: the moment the last garment left. */
    @SerializedName("completed_at") val completedAt: String? = null
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

/**
 * The courier's own screen: their DEPARTMENT's batches, open ones first.
 *
 * SCOPED BY DEPARTMENT, NOT BY WHO CARRIED IT IN [T-08]. Any active account of the department may
 * collect, so a list of only this person's own hand-overs would hide the batch they were sent to
 * fetch. `pengantar` travels as a field instead - who brought it is still worth seeing.
 */
data class LaundryBatchesResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<LaundryBatch>?
) {
    val isSuccess get() = code == 1
}

data class LaundryBatch(
    @SerializedName("id") val id: Int,
    @SerializedName("trx_no") val trxNo: String,
    @SerializedName("status") val status: String,
    @SerializedName("checked_in_at") val checkedInAt: String?,
    @SerializedName("ready_at") val readyAt: String?,
    @SerializedName("completed_at") val completedAt: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("pengantar") val pengantar: String?,
    @SerializedName("item_count") val itemCount: Int,
    /**
     * When the counter finished checking this batch back out, or null.
     *
     * The gate is the SERVER'S - scanning the counter sticker is refused until this is set. What
     * it buys the screen is the difference between "wait for the counter" and sending a courier
     * across the factory to be told no.
     */
    @SerializedName("handover_marked_at") val handoverMarkedAt: String? = null,
    /**
     * Garments still collectable - what the Collect button is enabled by.
     *
     * NOT DERIVED FROM `status` on the phone. A batch reads `ready` until its LAST line moves, so
     * a colleague from the same department can have taken everything while the header still says
     * collectable. The server counts the lines; the screen trusts the count.
     */
    @SerializedName("ready_count") val readyCount: Int
)

/** One batch with its garments - the courier's detail screen. */
data class LaundryBatchDetailResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: LaundryBatchDetail?
) {
    val isSuccess get() = code == 1
}

data class LaundryBatchDetail(
    @SerializedName("transaction") val transaction: LaundryBatchHeader,
    @SerializedName("items") val items: List<LaundryBatchLine>,
    @SerializedName("ready_count") val readyCount: Int,
    /**
     * What the courier claimed, and how much of it the counter has checked.
     *
     * ONLY `laundryCounterBatch` SENDS THESE, so both default to 0 and the courier's screen never
     * reads them. They are what the Accept button turns on: equal counts mean the bundle matches.
     */
    @SerializedName("claimed") val claimed: Int = 0,
    @SerializedName("verified") val verified: Int = 0
)

data class LaundryBatchHeader(
    @SerializedName("id") val id: Int,
    @SerializedName("trx_no") val trxNo: String,
    @SerializedName("status") val status: String,
    @SerializedName("checked_in_at") val checkedInAt: String?,
    @SerializedName("accepted_at") val acceptedAt: String?,
    @SerializedName("washing_started_at") val washingStartedAt: String?,
    @SerializedName("ready_at") val readyAt: String?,
    @SerializedName("handover_marked_at") val handoverMarkedAt: String? = null,
    @SerializedName("completed_at") val completedAt: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("pengantar") val pengantar: String?
)

data class LaundryBatchLine(
    @SerializedName("id") val id: Int,
    @SerializedName("qr_code") val qrCode: String?,
    @SerializedName("nama_item_type") val itemType: String?,
    @SerializedName("owner_name") val ownerName: String?,
    @SerializedName("item_status") val itemStatus: String,
    @SerializedName("condition_in") val conditionIn: String?,
    @SerializedName("condition_out") val conditionOut: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("source") val source: String?
)

/**
 * The courier collecting, by scanning the counter sticker.
 *
 * NO LINE LIST GOES OUT. The server derives what this department may take from the transaction
 * itself, so the request cannot name a garment belonging to somebody else - and a partial pickup
 * is decided by what is `ready` rather than by anything the phone chose [T-07].
 */
data class LaundryCollectResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: LaundryCollected?
) {
    val isSuccess get() = code == 1
}

data class LaundryCollected(
    @SerializedName("id_trx") val idTrx: Int,
    @SerializedName("trx_no") val trxNo: String,
    @SerializedName("collected") val collected: Int,
    @SerializedName("left") val left: Int,
    @SerializedName("held") val held: Int,
    @SerializedName("completed") val completed: Boolean
)

/**
 * The OPERATOR'S queue for collection: every batch waiting to be handed back, oldest ready first.
 *
 * NOT [LaundryBatchesResponse]. That one is the courier's and is scoped to their department
 * [T-08]; this is the counter's and is scoped to nothing, because the counter is holding all of
 * them. Two scopes behind one model is how the two would quietly start disagreeing.
 */
data class LaundryHandoverQueueResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<LaundryReadyBatch>?
) {
    val isSuccess get() = code == 1
}

data class LaundryReadyBatch(
    @SerializedName("id") val id: Int,
    @SerializedName("trx_no") val trxNo: String,
    @SerializedName("ready_at") val readyAt: String?,
    /** Set once this operator, or another, has scanned the bundle back out. */
    @SerializedName("handover_marked_at") val handoverMarkedAt: String?,
    @SerializedName("nama_dept") val namaDept: String?,
    @SerializedName("sub_dept") val subDept: String?,
    @SerializedName("pengantar") val pengantar: String?,
    /** What is actually collectable, which is not the item count once a partial pickup [T-07]. */
    @SerializedName("ready_count") val readyCount: Int
)

/**
 * The counter's scan-out: every collectable garment, checked back out before anybody may take it.
 *
 * A JSON BODY for the same reason [LaundryAddItemsRequest] is one - a list cannot be form-encoded
 * without hand-building `qr_codes[0]` keys.
 *
 * ALL OF THEM. The server refuses a short scan with the number missing rather than staging a
 * subset, because a partial stage hands the courier a bundle nobody finished checking.
 */
/**
 * A plain yes/no from the counter's process actions.
 *
 * ONE SHAPE FOR ALL SIX, because the server speaks one: `LaundryProcess` answers
 * `{ok, message, data?}` and the mobile controller maps it to `{code, message}` for every action.
 */
data class LaundryActionResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String
) {
    val isSuccess get() = code == 1
}

/** One account that may sign for a batch. */
data class LaundryCollector(
    @SerializedName("id_user") val idUser: Int,
    @SerializedName("usern") val usern: String?,
    /** `COALESCE(MEMNAME, usern)` - the name the operator reads off somebody across a counter. */
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("nama_dept") val namaDept: String?,
    @SerializedName("sub_dept") val subDept: String?
)

data class LaundryCollectorsResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<LaundryCollector>?
) {
    val isSuccess get() = code == 1
}

/** JSON bodies: a list of ids is not something form encoding expresses cleanly. */
data class LaundryConditionOutRequest(
    @SerializedName("id_lines") val idLines: List<Int>,
    @SerializedName("condition_out") val conditionOut: String,
    @SerializedName("note") val note: String? = null
)

data class LaundryPickupRequest(
    @SerializedName("id_trx") val idTrx: Int,
    @SerializedName("id_collector") val idCollector: Int,
    @SerializedName("item_ids") val itemIds: List<Int>
)

/** One slip, already laid out to the paper's width by the server. */
/**
 * One row of a slip: `t` is "text" or "qr", `v` is the characters or the payload.
 *
 * TYPED BY THE SERVER, WHICH OWNS THE LAYOUT. The phone never decides what belongs on a slip or
 * where it goes - the web template and this come from one renderer.
 */
data class LaundrySlipRow(
    @SerializedName("t") val type: String,
    @SerializedName("v") val value: String
) {
    val isQr get() = type == "qr"
}

data class LaundrySlipLines(
    @SerializedName("trx_no") val trxNo: String?,
    @SerializedName("lines") val lines: List<LaundrySlipRow>?,
    /**
     * The server refused because this slip has been printed before [3e].
     *
     * A FLAG, NOT A MESSAGE MATCH. The refusal sentence is Indonesian, written server-side and
     * translated per caller - an app that decided what to do by reading it would break the day
     * somebody rewords it.
     */
    @SerializedName("needs_reason") val needsReason: Boolean = false
)

data class LaundrySlipResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: LaundrySlipLines?
) {
    val isSuccess get() = code == 1
}

data class LaundryHandoverMarkRequest(
    @SerializedName("id_trx") val idTrx: Int,
    @SerializedName("qr_codes") val qrCodes: List<String>
)

data class LaundryHandoverMarkResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: LaundryHandoverMarked?
) {
    val isSuccess get() = code == 1
}

data class LaundryHandoverMarked(
    @SerializedName("id_trx") val idTrx: Int,
    @SerializedName("trx_no") val trxNo: String,
    @SerializedName("marked") val marked: Int
)
