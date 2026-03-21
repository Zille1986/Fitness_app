package com.runtracker.app.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * Detects swimming lap turns using the accelerometer.
 *
 * Algorithm: Monitors acceleration magnitude for a "wall push" pattern —
 * a sharp spike in total acceleration (push off the wall) that exceeds a
 * threshold. A cooldown window prevents double-counting.
 *
 * Typical 25m pool lap takes 20-40s for recreational swimmers, so the
 * minimum cooldown is 15 seconds.
 */
class SwimLapDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private val _lapDetected = MutableStateFlow(0)
    /** Increments each time a lap turn is detected. */
    val lapDetected: StateFlow<Int> = _lapDetected.asStateFlow()

    private var isListening = false
    private var lastLapTimestamp = 0L

    // Tuning parameters
    private val turnThreshold = 18f        // m/s² — a wall push-off produces 15-25 m/s²
    private val cooldownMillis = 15_000L   // minimum 15s between laps
    private val windowSize = 5             // smoothing window

    private val magnitudeWindow = ArrayDeque<Float>(windowSize)

    fun start() {
        if (accelerometer == null || isListening) return
        magnitudeWindow.clear()
        lastLapTimestamp = System.currentTimeMillis()
        isListening = true
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        if (!isListening) return
        isListening = false
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION) return

        val magnitude = sqrt(
            event.values[0] * event.values[0] +
            event.values[1] * event.values[1] +
            event.values[2] * event.values[2]
        )

        // Maintain smoothing window
        if (magnitudeWindow.size >= windowSize) magnitudeWindow.removeFirst()
        magnitudeWindow.addLast(magnitude)

        val smoothed = magnitudeWindow.average().toFloat()

        val now = System.currentTimeMillis()
        if (smoothed > turnThreshold && (now - lastLapTimestamp) > cooldownMillis) {
            lastLapTimestamp = now
            _lapDetected.value = _lapDetected.value + 1
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
