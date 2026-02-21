package com.runtracker.wear.safety

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.runtracker.shared.sync.DataLayerPaths
import kotlinx.coroutines.tasks.await

object WatchSafetyService {
    
    suspend fun triggerSos(context: Context, latitude: Double? = null, longitude: Double? = null) {
        try {
            val dataClient = Wearable.getDataClient(context)
            
            val putDataRequest = PutDataMapRequest
                .create(DataLayerPaths.SAFETY_SOS_PATH)
                .apply {
                    dataMap.putLong(DataLayerPaths.KEY_TIMESTAMP, System.currentTimeMillis())
                    latitude?.let { dataMap.putDouble(DataLayerPaths.KEY_LATITUDE, it) }
                    longitude?.let { dataMap.putDouble(DataLayerPaths.KEY_LONGITUDE, it) }
                }
                .asPutDataRequest()
                .setUrgent()
            
            dataClient.putDataItem(putDataRequest).await()
            android.util.Log.d("WatchSafety", "SOS sent to phone")
        } catch (e: Exception) {
            android.util.Log.e("WatchSafety", "Failed to send SOS", e)
        }
    }
    
    suspend fun triggerPanicAlarm(context: Context) {
        try {
            val dataClient = Wearable.getDataClient(context)
            
            val putDataRequest = PutDataMapRequest
                .create(DataLayerPaths.SAFETY_PANIC_PATH)
                .apply {
                    dataMap.putLong(DataLayerPaths.KEY_TIMESTAMP, System.currentTimeMillis())
                }
                .asPutDataRequest()
                .setUrgent()
            
            dataClient.putDataItem(putDataRequest).await()
            android.util.Log.d("WatchSafety", "Panic alarm sent to phone")
        } catch (e: Exception) {
            android.util.Log.e("WatchSafety", "Failed to send panic alarm", e)
        }
    }
    
    suspend fun triggerFakeCall(context: Context) {
        try {
            val dataClient = Wearable.getDataClient(context)
            
            val putDataRequest = PutDataMapRequest
                .create(DataLayerPaths.SAFETY_FAKE_CALL_PATH)
                .apply {
                    dataMap.putLong(DataLayerPaths.KEY_TIMESTAMP, System.currentTimeMillis())
                }
                .asPutDataRequest()
                .setUrgent()
            
            dataClient.putDataItem(putDataRequest).await()
            android.util.Log.d("WatchSafety", "Fake call sent to phone")
        } catch (e: Exception) {
            android.util.Log.e("WatchSafety", "Failed to send fake call", e)
        }
    }
    
    suspend fun checkIn(context: Context) {
        try {
            val dataClient = Wearable.getDataClient(context)
            
            val putDataRequest = PutDataMapRequest
                .create(DataLayerPaths.SAFETY_CHECK_IN_PATH)
                .apply {
                    dataMap.putLong(DataLayerPaths.KEY_TIMESTAMP, System.currentTimeMillis())
                }
                .asPutDataRequest()
                .setUrgent()
            
            dataClient.putDataItem(putDataRequest).await()
            android.util.Log.d("WatchSafety", "Check-in sent to phone")
        } catch (e: Exception) {
            android.util.Log.e("WatchSafety", "Failed to send check-in", e)
        }
    }
}
