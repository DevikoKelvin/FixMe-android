package com.erela.fixme.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.erela.fixme.objects.laundry.LaundryCheckInItem
import com.erela.fixme.objects.laundry.LaundryCheckInResponse
import com.erela.fixme.objects.laundry.LaundryCounterResponse
import com.erela.fixme.objects.laundry.LaundryGarment
import com.erela.fixme.objects.laundry.LaundryMyCheckInsResponse
import com.erela.fixme.objects.laundry.LaundryScanResponse
import com.erela.fixme.repository.LaundryRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * The bundle lives here, not in the Activity.
 *
 * NOTHING IS SENT UNTIL SUBMIT, exactly as on the web: a hand-over is one event, and a
 * half-written transaction is worse than none. So the scanned garments accumulate in this
 * ViewModel and survive rotation — a courier a dozen patches into a bundle must not lose it to a
 * screen turn, which is precisely when they would be holding the phone loosely.
 *
 * KEYED BY CODE, so scanning the same patch twice is caught here rather than by the server after
 * the courier has walked away. The server checks it too; this only makes the answer immediate.
 */
class LaundryCheckInViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LaundryRepository(application)

    /** The bundle, in scan order. `LinkedHashMap` because the order is what the courier sees. */
    private val bundle = LinkedHashMap<String, LaundryGarment>()

    private val _items = MutableLiveData<List<LaundryGarment>>(emptyList())
    val items: LiveData<List<LaundryGarment>> = _items

    private val _counterResult = MutableLiveData<LaundryCounterResponse>()
    val counterResult: LiveData<LaundryCounterResponse> = _counterResult

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

    /** The counter this bundle is being handed in at. Set by a successful counter scan. */
    var counterCode: String? = null
        private set

    var counterName: String? = null
        private set

    /** False until a counter scan says this courier's department is on the laundry list. */
    var mayCheckIn: Boolean = false
        private set

    private fun fail(throwable: Throwable) {
        _errorCode.value = (throwable as? HttpException)?.code()
        _error.value = throwable.message
    }

    fun scanCounter(code: String) {
        _isLoading.value = true

        viewModelScope.launch {
            repository.counter(code)
                .onSuccess { response ->
                    if (response.isSuccess && response.data != null) {
                        counterCode = response.data.counterCode
                        counterName = response.data.name
                        mayCheckIn = response.data.mayCheckIn
                    }

                    _counterResult.value = response
                }
                .onFailure { fail(it) }

            _isLoading.value = false
        }
    }

    /**
     * Resolve one scanned patch, then add it.
     *
     * The duplicate check happens BEFORE the request: a courier who scans the same tag twice gets
     * an immediate answer instead of a round trip that would have succeeded and then been
     * rejected at submit.
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

    /**
     * Hand the bundle over.
     *
     * The counter code is the one the scan established, never one the screen holds: if no counter
     * has been scanned there is nothing to submit to, and guessing would file the hand-over
     * against the wrong drop point.
     */
    fun submit(note: String?) {
        val counter = counterCode

        if (counter.isNullOrBlank() || bundle.isEmpty()) {
            return
        }

        _isSubmitting.value = true

        val items = bundle.values.map { LaundryCheckInItem(it.qrCode, it.note) }

        viewModelScope.launch {
            repository.checkIn(counter, note, items)
                .onSuccess { response ->
                    // Cleared only on success. A failed submit must leave the bundle intact, or
                    // the courier rebuilds thirty scans because the network blinked.
                    if (response.isSuccess) {
                        clearBundle()
                    }

                    _checkInResult.value = response
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
