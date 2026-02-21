package com.runtracker.app.safety

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SafetyBroadcastReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var safetyService: SafetyService
    
    companion object {
        const val ACTION_STOP_PANIC_ALARM = "com.runtracker.app.STOP_PANIC_ALARM"
        const val ACTION_STOP_FAKE_CALL = "com.runtracker.app.STOP_FAKE_CALL"
        const val ACTION_CHECK_IN = "com.runtracker.app.CHECK_IN"
        const val ACTION_TRIGGER_SOS = "com.runtracker.app.TRIGGER_SOS"
        const val ACTION_TRIGGER_PANIC = "com.runtracker.app.TRIGGER_PANIC"
        const val ACTION_TRIGGER_FAKE_CALL = "com.runtracker.app.TRIGGER_FAKE_CALL"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_STOP_PANIC_ALARM -> {
                safetyService.stopPanicAlarm()
            }
            ACTION_CHECK_IN -> {
                safetyService.checkIn()
            }
            ACTION_TRIGGER_SOS -> {
                CoroutineScope(Dispatchers.Main).launch {
                    safetyService.triggerSos()
                }
            }
            ACTION_TRIGGER_PANIC -> {
                safetyService.startPanicAlarm()
            }
            ACTION_TRIGGER_FAKE_CALL -> {
                val delay = intent.getIntExtra("delay", 5)
                safetyService.triggerFakeCall(delay)
            }
            ACTION_STOP_FAKE_CALL -> {
                safetyService.stopFakeCallRinging()
            }
        }
    }
}
