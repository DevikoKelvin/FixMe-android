package com.erela.fixme.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.erela.fixme.objects.laundry.LaundryActionResponse
import com.erela.fixme.objects.laundry.LaundryCollector
import com.erela.fixme.objects.laundry.LaundryArrivalResponse
import com.erela.fixme.objects.laundry.LaundryArrivalsResponse
import com.erela.fixme.objects.laundry.LaundryBatchDetailResponse
import com.erela.fixme.objects.laundry.LaundryBatchesResponse
import com.erela.fixme.objects.laundry.LaundryCollectResponse
import com.erela.fixme.objects.laundry.LaundryCheckInItem
import com.erela.fixme.objects.laundry.LaundryCheckInResponse
import com.erela.fixme.objects.laundry.LaundryGarment
import com.erela.fixme.objects.laundry.LaundryHandoverMarkResponse
import com.erela.fixme.objects.laundry.LaundryHandoverQueueResponse
import com.erela.fixme.objects.laundry.LaundryReadyBatch
import com.erela.fixme.objects.laundry.LaundryMyCheckInsResponse
import com.erela.fixme.objects.laundry.LaundrySlipRow
import com.erela.fixme.objects.laundry.LaundryScanResponse
import com.erela.fixme.objects.laundry.LaundryWaitingCourier
import com.erela.fixme.repository.LaundryRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Both halves of the counter flow: the courier's arrival, and the operator's scanning.
 *
 * ONE VIEWMODEL FOR TWO ROLES because they share everything that matters - the same repository,
 * the same error handling, and the operator's bundle is the same bundle the courier's used to be.
 * The two screens observe different halves of it. Split it the day a third role appears.
 *
 * The bundle lives here, not in the Activity.
 *
 * NOTHING IS SENT UNTIL SAVE. The scanned garments accumulate here and survive rotation — an
 * operator a dozen patches into a bundle must not lose them to a screen turn, which is precisely
 * when they would be holding the phone loosely.
 *
 * KEYED BY CODE, so scanning the same patch twice is caught here rather than by the server after
 * the bundle is done. The server checks it too; this only makes the answer immediate.
 */
class LaundryCheckInViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LaundryRepository(application)

    /** The bundle, in scan order. `LinkedHashMap` because the order is what the operator sees. */
    private val bundle = LinkedHashMap<String, LaundryGarment>()

    private val _items = MutableLiveData<List<LaundryGarment>>(emptyList())
    val items: LiveData<List<LaundryGarment>> = _items

    private val _arrivalResult = MutableLiveData<LaundryArrivalResponse>()
    val arrivalResult: LiveData<LaundryArrivalResponse> = _arrivalResult

    private val _arrivals = MutableLiveData<LaundryArrivalsResponse>()
    val arrivals: LiveData<LaundryArrivalsResponse> = _arrivals

    private val _batches = MutableLiveData<LaundryBatchesResponse>()
    val batches: LiveData<LaundryBatchesResponse> = _batches

    /**
     * The counter's own view of a batch, and the five things they can do to it.
     *
     * A SEPARATE LiveData FROM `batchDetail`, which the COURIER's screen observes. The two come
     * from different endpoints - the courier's is scoped to their department, the counter's is
     * not - and one stream feeding both screens is how a stale reply lands on the wrong one.
     */
    private val _counterBatch = MutableLiveData<LaundryBatchDetailResponse>()
    val counterBatch: LiveData<LaundryBatchDetailResponse> = _counterBatch

    private val _actionResult = MutableLiveData<LaundryActionResponse>()
    val actionResult: LiveData<LaundryActionResponse> = _actionResult

    private val _collectors = MutableLiveData<List<LaundryCollector>>(emptyList())
    val collectors: LiveData<List<LaundryCollector>> = _collectors

    /**
     * Two lists of the same row shape, and two fields rather than one with a mode.
     *
     * `active` is work in the building; `history` is what is finished. One screen shows one of
     * them at a time, but a single field would let a slow reply from the list you just left
     * render as the list you are looking at.
     */
    private val _active = MutableLiveData<LaundryArrivalsResponse>()
    val active: LiveData<LaundryArrivalsResponse> = _active

    private val _history = MutableLiveData<LaundryArrivalsResponse>()
    val history: LiveData<LaundryArrivalsResponse> = _history

    /**
     * The slip the phone is holding, waiting for a printer to be chosen.
     *
     * FETCHED ONCE, PRINTED AS OFTEN AS THE SOCKET NEEDS. The server counts the fetch as the
     * print, so a failed connection must not re-fetch - that would log a reprint for a slip that
     * never reached paper, and the retry would then be refused for want of a reason.
     */
    private val _slip = MutableLiveData<List<LaundrySlipRow>>(emptyList())
    val slip: LiveData<List<LaundrySlipRow>> = _slip

    /**
     * The server wants a reason before it will print this slip again.
     *
     * ITS OWN SIGNAL, not a toast. A reprint is refused until somebody says why [3e], and a
     * refusal with no way to answer it is a dead end - which is exactly what the operator hit.
     */
    private val _slipNeedsReason = MutableLiveData<String>()
    val slipNeedsReason: LiveData<String> = _slipNeedsReason

    private val _batchDetail = MutableLiveData<LaundryBatchDetailResponse>()
    val batchDetail: LiveData<LaundryBatchDetailResponse> = _batchDetail

    private val _collectResult = MutableLiveData<LaundryCollectResponse>()
    val collectResult: LiveData<LaundryCollectResponse> = _collectResult

    private val _handoverQueue = MutableLiveData<LaundryHandoverQueueResponse>()
    val handoverQueue: LiveData<LaundryHandoverQueueResponse> = _handoverQueue

    private val _markResult = MutableLiveData<LaundryHandoverMarkResponse>()
    val markResult: LiveData<LaundryHandoverMarkResponse> = _markResult

    /**
     * The codes scanned OUT, in scan order.
     *
     * A PLAIN SET OF CODES, not resolved garments. `laundryScan` answers "is this a registered
     * uniform" against the master register, which cannot say whether it belongs to THIS batch -
     * the only thing the operator needs to know here. The server answers that on submit and names
     * the offender, so a round trip per garment would buy a slower scan and no more truth.
     */
    private val outScanned = LinkedHashSet<String>()

    private val _outItems = MutableLiveData<List<String>>(emptyList())
    val outItems: LiveData<List<String>> = _outItems

    private val _scanResult = MutableLiveData<LaundryScanResponse>()
    val scanResult: LiveData<LaundryScanResponse> = _scanResult

    private val _checkInResult = MutableLiveData<LaundryCheckInResponse>()
    val checkInResult: LiveData<LaundryCheckInResponse> = _checkInResult

    private val _recentResult = MutableLiveData<LaundryMyCheckInsResponse>()
    val recentResult: LiveData<LaundryMyCheckInsResponse> = _recentResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSubmitting = MutableLiveData<Boolean>()
    val isSubmitting: LiveData<Boolean> = _isSubmitting

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    /**
     * HTTP status of the last failure, or null when it was not an HTTP error at all.
     *
     * Kept separate from [error] so the screen can react to a 401 — the token expired, and the
     * interceptor's `onUnauthorized` is already handling the sign-out — without matching on
     * Retrofit's own wording, which is not ours and changes.
     */
    private val _errorCode = MutableLiveData<Int?>()
    val errorCode: LiveData<Int?> = _errorCode

    /**
     * The arrival the OPERATOR is filling. Null until they pick a courier off the queue.
     *
     * Held here rather than passed through an Intent so a rotation mid-bundle does not strand the
     * scans against no transaction.
     */
    var workingIdTrx: Int? = null
        private set

    var workingCourier: String? = null
        private set

    private fun fail(throwable: Throwable) {
        _errorCode.value = (throwable as? HttpException)?.code()
        _error.value = throwable.message
    }

    /**
     * The courier's whole job: say they have arrived at this counter.
     *
     * No bundle is enumerated here. The operator scans the garments afterwards, so all this needs
     * to carry is the counter - the server takes the courier from the token.
     */
    fun arrive(counterCode: String) {
        _isSubmitting.value = true

        viewModelScope.launch {
            repository.arrive(counterCode)
                .onSuccess { _arrivalResult.value = it }
                .onFailure { fail(it) }

            _isSubmitting.value = false
        }
    }

    /** The courier's list. Reloaded on every return: a colleague may have collected since. */
    fun loadBatches() {
        _isLoading.value = true

        viewModelScope.launch {
            repository.myBatches()
                .onSuccess { _batches.value = it }
                .onFailure { fail(it) }

            _isLoading.value = false
        }
    }

    fun loadBatch(idTrx: Int) {
        _isLoading.value = true

        viewModelScope.launch {
            repository.batch(idTrx)
                .onSuccess { _batchDetail.value = it }
                .onFailure { fail(it) }

            _isLoading.value = false
        }
    }

    /**
     * Collect this batch, by scanning the counter sticker.
     *
     * NO ITEM LIST GOES OUT. The server derives what this department may take from the transaction
     * itself, so nothing the phone sends can name somebody else's garment, and a partial pickup is
     * decided by what is `ready` [T-07].
     */
    fun collect(counterCode: String, idTrx: Int) {
        _isSubmitting.value = true

        viewModelScope.launch {
            repository.collect(counterCode, idTrx)
                .onSuccess { response ->
                    // Reloaded rather than patched in memory: the server decides what is left.
                    if (response.isSuccess) {
                        loadBatch(idTrx)
                    }

                    _collectResult.value = response
                }
                .onFailure { fail(it) }

            _isSubmitting.value = false
        }
    }

    /** The counter's door onto a batch - any department's, unlike [loadBatch]. */
    fun loadCounterBatch(idTrx: Int) {
        _isLoading.value = true

        viewModelScope.launch {
            repository.counterBatch(idTrx)
                .onSuccess { _counterBatch.value = it }
                .onFailure { fail(it) }

            _isLoading.value = false
        }
    }

    /**
     * The counter's five process actions, each the same four lines: run it, say what happened,
     * reload the batch so the buttons match the new truth.
     *
     * RELOADING IS NOT OPTIONAL. Every one of these changes which button is legal next - accepting
     * turns on Mulai Cuci, the wash start turns on the condition ticks - and a screen left holding
     * the previous payload offers an action the server will refuse.
     */
    private fun act(idTrx: Int, run: suspend () -> Result<LaundryActionResponse>) {
        _isSubmitting.value = true

        viewModelScope.launch {
            run()
                .onSuccess { response ->
                    _actionResult.value = response
                    loadCounterBatch(idTrx)
                }
                .onFailure { fail(it) }

            _isSubmitting.value = false
        }
    }

    fun verifyGarment(idTrx: Int, qrCode: String, conditionIn: String, note: String? = null) =
        act(idTrx) { repository.verify(idTrx, qrCode, conditionIn, note) }

    fun acceptBatch(idTrx: Int) = act(idTrx) { repository.accept(idTrx) }

    fun startWash(idTrx: Int) = act(idTrx) { repository.washStart(idTrx) }

    fun recordConditionOut(idTrx: Int, idLines: List<Int>, condition: String, note: String? = null) =
        act(idTrx) { repository.conditionOut(idLines, condition, note) }

    fun markReady(idTrx: Int) = act(idTrx) { repository.markReady(idTrx) }

    fun handOverTo(idTrx: Int, idCollector: Int, itemIds: List<Int>) =
        act(idTrx) { repository.pickup(idTrx, idCollector, itemIds) }

    /** Who may sign for this batch. Loaded when the picker opens, not with the batch. */
    fun loadCollectors(idTrx: Int) {
        viewModelScope.launch {
            repository.collectors(idTrx)
                .onSuccess { _collectors.value = it.data.orEmpty() }
                .onFailure { _collectors.value = emptyList() }
        }
    }

    /** Bundles in the building: banked, not yet collectable. */
    fun loadActiveQueue() {
        _isLoading.value = true

        viewModelScope.launch {
            repository.activeQueue()
                .onSuccess { _active.value = it }
                .onFailure { fail(it) }

            _isLoading.value = false
        }
    }

    /** Finished batches, optionally within a date range. */
    fun loadHistory(from: String? = null, to: String? = null) {
        _isLoading.value = true

        viewModelScope.launch {
            repository.history(from, to)
                .onSuccess { _history.value = it }
                .onFailure { fail(it) }

            _isLoading.value = false
        }
    }

    fun clearSlip() {
        _slip.value = emptyList()
    }

    /**
     * Ask for a slip. `reason` is required only when this transaction has been printed before,
     * which the server decides and says.
     */
    fun loadSlip(idTrx: Int, out: List<Int>? = null, reason: String? = null, cols: Int = 48) {
        _isSubmitting.value = true

        viewModelScope.launch {
            repository.slip(idTrx, out?.joinToString(","), reason, cols)
                .onSuccess { response ->
                    val data = response.data

                    val lines = data?.lines

                    when {
                        response.isSuccess && lines != null -> _slip.value = lines

                        // Answerable: ask for the reason and come back with it.
                        data?.needsReason == true -> _slipNeedsReason.value = response.message

                        else -> _actionResult.value = LaundryActionResponse(0, response.message)
                    }
                }
                .onFailure { fail(it) }

            _isSubmitting.value = false
        }
    }

    /** The operator's queue of waiting couriers. */
    fun loadArrivals() {
        _isLoading.value = true

        viewModelScope.launch {
            repository.arrivals()
                .onSuccess { _arrivals.value = it }
                .onFailure { fail(it) }

            _isLoading.value = false
        }
    }

    /**
     * The arriving courier's DEPARTMENT, which the bundle rows compare each garment against.
     *
     * Not the operator's own: theirs is GA, and comparing against it would mark every garment in
     * every bundle as belonging to another department.
     */
    var workingDept: String? = null
        private set

    /** The operator picks a courier off the queue; scanning then applies to their arrival. */
    fun workOn(arrival: LaundryWaitingCourier) {
        workingIdTrx = arrival.id
        workingCourier = arrival.pengantar
        workingDept = arrival.namaDept
        clearBundle()
    }

    /**
     * Write the scanned garments onto the arrival being worked on.
     *
     * The server allows this more than once for the same arrival, so the bundle is cleared on
     * success and the operator can keep going rather than starting the batch again.
     */
    fun saveItems(note: String?) {
        val idTrx = workingIdTrx

        if (idTrx == null || bundle.isEmpty()) {
            return
        }

        _isSubmitting.value = true

        val items = bundle.values.map { LaundryCheckInItem(it.qrCode, it.note) }

        viewModelScope.launch {
            repository.addItems(idTrx, note, items)
                .onSuccess { response ->
                    // Cleared only on success. A failed save must leave the bundle intact, or the
                    // operator rescans thirty garments because the network blinked.
                    if (response.isSuccess) {
                        clearBundle()
                    }

                    _checkInResult.value = response
                }
                .onFailure { fail(it) }

            _isSubmitting.value = false
        }
    }

    /**
     * Resolve one scanned patch, then add it.
     *
     * The duplicate check happens BEFORE the request: an operator who scans the same tag twice
     * gets an immediate answer instead of a round trip that would have succeeded and then been
     * rejected on save.
     */
    fun onQrScanned(qrCode: String) {
        val code = qrCode.trim()

        if (bundle.containsKey(code)) {
            _error.value = DUPLICATE_PREFIX + code
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            repository.scan(code)
                .onSuccess { response ->
                    if (response.isSuccess && response.data != null) {
                        bundle[response.data.qrCode] = response.data
                        _items.value = bundle.values.toList()
                    }

                    _scanResult.value = response
                }
                .onFailure { fail(it) }

            _isLoading.value = false
        }
    }

    fun setNote(qrCode: String, note: String?) {
        bundle[qrCode]?.note = note
    }

    fun remove(qrCode: String) {
        bundle.remove(qrCode)
        _items.value = bundle.values.toList()
    }

    fun clearBundle() {
        bundle.clear()
        _items.value = emptyList()
    }

    fun count(): Int = bundle.size

    fun loadHandoverQueue() {
        _isLoading.value = true

        viewModelScope.launch {
            repository.handoverQueue()
                .onSuccess { _handoverQueue.value = it }
                .onFailure { fail(it) }

            _isLoading.value = false
        }
    }

    /** Start a scan-out. The list is cleared, because it belongs to one batch. */
    fun beginHandover(batch: LaundryReadyBatch) {
        workingIdTrx = batch.id
        workingCourier = batch.pengantar
        workingDept = batch.namaDept
        clearOutScan()
    }

    /** Local duplicate check, the same courtesy the check-in scanner does. */
    fun onOutScanned(qrCode: String) {
        val code = qrCode.trim()

        if (!outScanned.add(code)) {
            _error.value = DUPLICATE_PREFIX + code
            return
        }

        _outItems.value = outScanned.toList()
    }

    fun removeOutScan(qrCode: String) {
        outScanned.remove(qrCode)
        _outItems.value = outScanned.toList()
    }

    fun clearOutScan() {
        outScanned.clear()
        _outItems.value = emptyList()
    }

    fun outCount(): Int = outScanned.size

    /**
     * Mark the working batch ready to hand over.
     *
     * CLEARED ONLY ON SUCCESS, like `saveItems`: a failed submit must leave twelve scans intact,
     * or the network blinking costs the operator the whole bundle again.
     */
    fun markHandover() {
        val idTrx = workingIdTrx

        if (idTrx == null || outScanned.isEmpty()) {
            return
        }

        _isSubmitting.value = true

        viewModelScope.launch {
            repository.markHandover(idTrx, outScanned.toList())
                .onSuccess { response ->
                    if (response.isSuccess) {
                        clearOutScan()
                    }

                    _markResult.value = response
                }
                .onFailure { fail(it) }

            _isSubmitting.value = false
        }
    }

    fun loadRecent() {
        viewModelScope.launch {
            repository.myCheckIns()
                .onSuccess { _recentResult.value = it }
                .onFailure { fail(it) }
        }
    }

    companion object {
        /** Marks a duplicate so the screen can word it as a slip rather than an error. */
        const val DUPLICATE_PREFIX = "DUPLICATE:"
    }
}
