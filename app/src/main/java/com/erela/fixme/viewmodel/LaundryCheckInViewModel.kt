package com.erela.fixme.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.erela.fixme.objects.laundry.LaundryArrivalResponse
import com.erela.fixme.objects.laundry.LaundryArrivalsResponse
import com.erela.fixme.objects.laundry.LaundryCheckInItem
import com.erela.fixme.objects.laundry.LaundryCheckInResponse
import com.erela.fixme.objects.laundry.LaundryGarment
import com.erela.fixme.objects.laundry.LaundryMyCheckInsResponse
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
