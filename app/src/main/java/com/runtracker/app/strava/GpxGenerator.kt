package com.runtracker.app.strava

import com.runtracker.shared.data.model.Run
import com.runtracker.shared.data.model.CyclingWorkout
import com.runtracker.shared.data.model.CyclingType
import com.runtracker.shared.data.model.RoutePoint
import java.text.SimpleDateFormat
import java.util.*

object GpxGenerator {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    fun generateGpx(run: Run): String {
        val sb = StringBuilder()
        
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<gpx version="1.1" creator="RunTracker" xmlns="http://www.topografix.com/GPX/1/1" xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1">""")
        sb.appendLine("  <metadata>")
        sb.appendLine("    <name>${generateRunName(run)}</name>")
        sb.appendLine("    <time>${dateFormat.format(Date(run.startTime))}</time>")
        sb.appendLine("  </metadata>")
        sb.appendLine("  <trk>")
        sb.appendLine("    <name>${generateRunName(run)}</name>")
        sb.appendLine("    <type>running</type>")
        sb.appendLine("    <trkseg>")
        
        run.routePoints.forEach { point ->
            sb.appendLine(generateTrackPoint(point))
        }
        
        sb.appendLine("    </trkseg>")
        sb.appendLine("  </trk>")
        sb.appendLine("</gpx>")
        
        return sb.toString()
    }
    
    private fun generateTrackPoint(point: RoutePoint): String {
        val sb = StringBuilder()
        sb.append("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">")
        
        if (point.altitude != null && point.altitude != 0.0) {
            sb.append("<ele>${point.altitude}</ele>")
        }
        
        sb.append("<time>${dateFormat.format(Date(point.timestamp))}</time>")
        
        // Add heart rate extension if available
        point.heartRate?.let { hr ->
            sb.append("<extensions>")
            sb.append("<gpxtpx:TrackPointExtension>")
            sb.append("<gpxtpx:hr>$hr</gpxtpx:hr>")
            sb.append("</gpxtpx:TrackPointExtension>")
            sb.append("</extensions>")
        }
        
        sb.append("</trkpt>")
        return sb.toString()
    }
    
    private fun generateRunName(run: Run): String {
        val distanceKm = run.distanceMeters / 1000.0
        val calendar = Calendar.getInstance().apply { timeInMillis = run.startTime }
        val timeOfDay = when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..20 -> "Evening"
            else -> "Night"
        }
        return "$timeOfDay Run - %.1f km".format(distanceKm)
    }
    
    fun generateGpxForCycling(ride: CyclingWorkout): String {
        val sb = StringBuilder()
        
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<gpx version="1.1" creator="RunTracker" xmlns="http://www.topografix.com/GPX/1/1" xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1">""")
        sb.appendLine("  <metadata>")
        sb.appendLine("    <name>${generateCyclingName(ride)}</name>")
        sb.appendLine("    <time>${dateFormat.format(Date(ride.startTime))}</time>")
        sb.appendLine("  </metadata>")
        sb.appendLine("  <trk>")
        sb.appendLine("    <name>${generateCyclingName(ride)}</name>")
        sb.appendLine("    <type>cycling</type>")
        sb.appendLine("    <trkseg>")
        
        ride.routePoints.forEach { point ->
            sb.appendLine(generateTrackPoint(point))
        }
        
        sb.appendLine("    </trkseg>")
        sb.appendLine("  </trk>")
        sb.appendLine("</gpx>")
        
        return sb.toString()
    }
    
    private fun generateCyclingName(ride: CyclingWorkout): String {
        val distanceKm = ride.distanceMeters / 1000.0
        val calendar = Calendar.getInstance().apply { timeInMillis = ride.startTime }
        val timeOfDay = when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..20 -> "Evening"
            else -> "Night"
        }
        val rideType = if (ride.cyclingType != CyclingType.OUTDOOR) "Indoor Ride" else "Ride"
        return "$timeOfDay $rideType - %.1f km".format(distanceKm)
    }
}
