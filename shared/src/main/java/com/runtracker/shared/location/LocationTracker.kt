package com.runtracker.shared.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.runtracker.shared.data.model.RoutePoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationTracker(context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        1000L
    ).apply {
        setMinUpdateDistanceMeters(5f)
        setGranularity(Granularity.GRANULARITY_FINE)
        setWaitForAccurateLocation(true)
    }.build()

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<Location> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location? {
        return try {
            fusedLocationClient.lastLocation.result
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun Location.toRoutePoint(heartRate: Int? = null, cadence: Int? = null): RoutePoint {
            return RoutePoint(
                latitude = latitude,
                longitude = longitude,
                altitude = if (hasAltitude()) altitude else null,
                timestamp = time,
                accuracy = if (hasAccuracy()) accuracy else null,
                heartRate = heartRate,
                cadence = cadence,
                speed = if (hasSpeed()) speed else null
            )
        }

        fun calculateDistance(points: List<RoutePoint>): Double {
            if (points.size < 2) return 0.0
            
            var totalDistance = 0.0
            for (i in 1 until points.size) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    points[i - 1].latitude,
                    points[i - 1].longitude,
                    points[i].latitude,
                    points[i].longitude,
                    results
                )
                totalDistance += results[0]
            }
            return totalDistance
        }

        fun calculateElevationGain(points: List<RoutePoint>): Double {
            if (points.size < 2) return 0.0
            
            var gain = 0.0
            for (i in 1 until points.size) {
                val prev = points[i - 1].altitude ?: continue
                val curr = points[i].altitude ?: continue
                if (curr > prev) {
                    gain += (curr - prev)
                }
            }
            return gain
        }

        fun calculateElevationLoss(points: List<RoutePoint>): Double {
            if (points.size < 2) return 0.0
            
            var loss = 0.0
            for (i in 1 until points.size) {
                val prev = points[i - 1].altitude ?: continue
                val curr = points[i].altitude ?: continue
                if (curr < prev) {
                    loss += (prev - curr)
                }
            }
            return loss
        }
    }
}
