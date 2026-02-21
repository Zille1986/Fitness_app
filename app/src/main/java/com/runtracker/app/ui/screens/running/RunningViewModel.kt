package com.runtracker.app.ui.screens.running

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.app.service.RunTrackingService
import com.runtracker.app.service.TrackingState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunningViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var trackingService: RunTrackingService? = null
    private var isBound = false

    private val _trackingState = MutableStateFlow(TrackingState())
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private val _finishedRunId = MutableStateFlow<Long?>(null)
    val finishedRunId: StateFlow<Long?> = _finishedRunId.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RunTrackingService.LocalBinder
            trackingService = binder.getService()
            isBound = true
            
            viewModelScope.launch {
                trackingService?.trackingState
                    ?.catch { e -> Log.e("RunningViewModel", "Error collecting tracking state", e) }
                    ?.collect { state ->
                        _trackingState.value = state
                    }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            isBound = false
        }
    }

    init {
        bindService()
    }

    private fun bindService() {
        Intent(context, RunTrackingService::class.java).also { intent ->
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun startRun() {
        Intent(context, RunTrackingService::class.java).apply {
            action = RunTrackingService.ACTION_START
        }.also {
            context.startForegroundService(it)
        }
    }

    fun pauseRun() {
        trackingService?.pauseTracking()
    }

    fun resumeRun() {
        trackingService?.resumeTracking()
    }

    fun stopRun() {
        trackingService?.stopTracking()
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }
}
