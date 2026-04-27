package com.erela.fixme.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.erela.fixme.objects.ac.AcCheckInResponse
import com.erela.fixme.objects.ac.AcScanResponse
import com.erela.fixme.objects.ac.AcSimpleResponse
import com.erela.fixme.objects.ac.AcTaskListResponse
import com.erela.fixme.repository.AcRepository
import kotlinx.coroutines.launch
import java.io.File

class AcMaintenanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AcRepository(application)

    private val _scanResult = MutableLiveData<AcScanResponse>()
    val scanResult: LiveData<AcScanResponse> = _scanResult

    private val _checkInResult = MutableLiveData<AcCheckInResponse>()
    val checkInResult: LiveData<AcCheckInResponse> = _checkInResult

    private val _taskListResult = MutableLiveData<AcTaskListResponse>()
    val taskListResult: LiveData<AcTaskListResponse> = _taskListResult

    private val _actionResult = MutableLiveData<AcSimpleResponse>()
    val actionResult: LiveData<AcSimpleResponse> = _actionResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun onQrScanned(acCode: String, userId: Int) {
        // does NOT touch _isLoading — scan runs in the background without
        // hiding the task list; result surfaces via scanResult LiveData
        viewModelScope.launch {
            repository.scan(acCode, userId)
                .onSuccess { _scanResult.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    fun checkIn(itemId: Int, userId: Int, lat: Double?, lng: Double?) {
        // deliberately does NOT touch _isLoading — caller (AcTaskListActivity)
        // manages the LoadingDialog directly so the task list shimmer never fires
        viewModelScope.launch {
            repository.checkIn(itemId, userId, lat, lng)
                .onSuccess { _checkInResult.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    fun getTaskList(userId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.taskList(userId)
                .onSuccess {
                    _isLoading.value = false
                    _taskListResult.value = it
                }
                .onFailure {
                    _isLoading.value = false
                    _error.value = it.message
                }
        }
    }

    fun addTechnician(logId: Int, userId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.addTechnician(logId, userId)
                .onSuccess {
                    _isLoading.value = false
                    _actionResult.value = it
                }
                .onFailure {
                    _isLoading.value = false
                    _error.value = it.message
                }
        }
    }

    fun removeTechnician(logId: Int, userId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.removeTechnician(logId, userId)
                .onSuccess {
                    _isLoading.value = false
                    _actionResult.value = it
                }
                .onFailure {
                    _isLoading.value = false
                    _error.value = it.message
                }
        }
    }

    fun checkOut(
        logId: Int,
        userId: Int,
        acCondition: String,
        photos: List<File>,
        photoTypes: List<String>,
        findings: String?,
        actionsTaken: String?,
        lat: Double?,
        lng: Double?
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.checkOut(
                logId, userId, acCondition, photos, photoTypes,
                findings, actionsTaken, lat, lng
            )
                .onSuccess {
                    _isLoading.value = false
                    _actionResult.value = it
                }
                .onFailure {
                    _isLoading.value = false
                    _error.value = it.message
                }
        }
    }
}