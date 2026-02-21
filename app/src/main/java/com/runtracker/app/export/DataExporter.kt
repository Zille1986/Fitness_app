package com.runtracker.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.runtracker.shared.data.model.GymWorkout
import com.runtracker.shared.data.model.Run
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val gpxDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun exportRunsToCSV(runs: List<Run>): Uri? {
        val fileName = "runtracker_runs_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        try {
            FileWriter(file).use { writer ->
                // Header
                writer.append("Date,Distance (km),Duration (min),Pace (min/km),Calories,Avg HR,Max HR,Elevation Gain (m),Notes\n")

                runs.forEach { run ->
                    val date = dateFormat.format(Date(run.startTime))
                    val distanceKm = run.distanceMeters / 1000.0
                    val durationMin = run.durationMillis / 60000.0
                    val paceMinKm = if (distanceKm > 0) durationMin / distanceKm else 0.0

                    writer.append("$date,")
                    writer.append("${String.format("%.2f", distanceKm)},")
                    writer.append("${String.format("%.1f", durationMin)},")
                    writer.append("${String.format("%.2f", paceMinKm)},")
                    writer.append("${run.caloriesBurned},")
                    writer.append("${run.avgHeartRate ?: ""},")
                    writer.append("${run.maxHeartRate ?: ""},")
                    writer.append("${String.format("%.1f", run.elevationGainMeters)},")
                    writer.append("\"${run.notes?.replace("\"", "\"\"") ?: ""}\"\n")
                }
            }

            return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportRunToGPX(run: Run): Uri? {
        val fileName = "run_${run.id}_${System.currentTimeMillis()}.gpx"
        val file = File(context.cacheDir, fileName)

        try {
            FileWriter(file).use { writer ->
                writer.append("""<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="RunTracker"
     xmlns="http://www.topografix.com/GPX/1/1"
     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">
  <metadata>
    <name>Run ${dateFormat.format(Date(run.startTime))}</name>
    <time>${gpxDateFormat.format(Date(run.startTime))}</time>
  </metadata>
  <trk>
    <name>Run</name>
    <trkseg>
""")

                run.routePoints.forEach { point ->
                    writer.append("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">\n")
                    point.altitude?.let { writer.append("        <ele>$it</ele>\n") }
                    writer.append("        <time>${gpxDateFormat.format(Date(point.timestamp))}</time>\n")
                    point.heartRate?.let {
                        writer.append("        <extensions><hr>$it</hr></extensions>\n")
                    }
                    writer.append("      </trkpt>\n")
                }

                writer.append("""    </trkseg>
  </trk>
</gpx>
""")
            }

            return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportGymWorkoutsToCSV(workouts: List<GymWorkout>): Uri? {
        val fileName = "runtracker_gym_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        try {
            FileWriter(file).use { writer ->
                // Header
                writer.append("Date,Workout Name,Duration (min),Exercises,Sets,Reps,Volume (kg),Notes\n")

                workouts.forEach { workout ->
                    val date = dateFormat.format(Date(workout.startTime))
                    val durationMin = workout.durationMillis / 60000.0

                    writer.append("$date,")
                    writer.append("\"${workout.name.replace("\"", "\"\"")}\",")
                    writer.append("${String.format("%.1f", durationMin)},")
                    writer.append("${workout.exercises.size},")
                    writer.append("${workout.totalSets},")
                    writer.append("${workout.totalReps},")
                    writer.append("${String.format("%.1f", workout.totalVolume)},")
                    writer.append("\"${workout.notes?.replace("\"", "\"\"") ?: ""}\"\n")
                }
            }

            return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportDetailedGymWorkoutsToCSV(workouts: List<GymWorkout>): Uri? {
        val fileName = "runtracker_gym_detailed_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        try {
            FileWriter(file).use { writer ->
                // Header
                writer.append("Date,Workout Name,Exercise,Set,Weight (kg),Reps,Volume (kg)\n")

                workouts.forEach { workout ->
                    val date = dateFormat.format(Date(workout.startTime))

                    workout.exercises.forEach { exercise ->
                        exercise.sets.filter { it.isCompleted }.forEach { set ->
                            writer.append("$date,")
                            writer.append("\"${workout.name.replace("\"", "\"\"")}\",")
                            writer.append("\"${exercise.exerciseName.replace("\"", "\"\"")}\",")
                            writer.append("${set.setNumber},")
                            writer.append("${set.weight},")
                            writer.append("${set.reps},")
                            writer.append("${set.weight * set.reps}\n")
                        }
                    }
                }
            }

            return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun createShareIntent(uri: Uri, mimeType: String, title: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
