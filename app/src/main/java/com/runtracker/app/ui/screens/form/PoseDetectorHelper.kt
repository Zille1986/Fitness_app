package com.runtracker.app.ui.screens.form

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.runtracker.shared.ai.Point3D
import com.runtracker.shared.ai.PoseData
import com.runtracker.shared.ai.PoseLandmark as AppPoseLandmark
import java.util.concurrent.Executors

class PoseDetectorHelper(
    private val context: Context,
    private val onPoseDetected: (PoseData) -> Unit,
    private val onError: (String) -> Unit,
    private val onFrameCaptured: ((android.graphics.Bitmap) -> Unit)? = null
) {
    private val executor = Executors.newSingleThreadExecutor()
    
    private val options = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        .build()
    
    private val poseDetector: PoseDetector = PoseDetection.getClient(options)
    
    private var lastAnalysisTime = 0L
    private val analysisIntervalMs = 100L // Analyze every 100ms (10 fps)
    
    @Volatile
    private var isClosed = false
    
    val imageAnalyzer = ImageAnalysis.Analyzer { imageProxy ->
        // Skip if detector is closed
        if (isClosed) {
            imageProxy.close()
            return@Analyzer
        }
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < analysisIntervalMs) {
            imageProxy.close()
            return@Analyzer
        }
        lastAnalysisTime = currentTime
        
        processImage(imageProxy)
    }
    
    @androidx.camera.core.ExperimentalGetImage
    private fun processImage(imageProxy: ImageProxy) {
        if (isClosed) {
            imageProxy.close()
            return
        }
        
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        
        // Capture bitmap for AI analysis if callback is provided
        val bitmap = if (onFrameCaptured != null) {
            try {
                imageProxy.toBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
        
        poseDetector.process(inputImage)
            .addOnSuccessListener { pose ->
                if (pose.allPoseLandmarks.isNotEmpty()) {
                    val poseData = convertToPoseData(pose, imageProxy.width, imageProxy.height)
                    onPoseDetected(poseData)
                    
                    // Pass captured frame for AI analysis
                    bitmap?.let { onFrameCaptured?.invoke(it) }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Pose detection failed", e)
                onError("Pose detection failed: ${e.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
    
    private fun convertToPoseData(pose: Pose, imageWidth: Int, imageHeight: Int): PoseData {
        val landmarks = mutableMapOf<AppPoseLandmark, Point3D>()
        
        // Map ML Kit landmarks to our app's landmark format
        pose.getPoseLandmark(PoseLandmark.NOSE)?.let {
            landmarks[AppPoseLandmark.NOSE] = normalizePoint(it, imageWidth, imageHeight)
        }
        
        pose.getPoseLandmark(PoseLandmark.LEFT_EYE)?.let {
            landmarks[AppPoseLandmark.LEFT_EYE] = normalizePoint(it, imageWidth, imageHeight)
        }
        pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)?.let {
            landmarks[AppPoseLandmark.RIGHT_EYE] = normalizePoint(it, imageWidth, imageHeight)
        }
        
        pose.getPoseLandmark(PoseLandmark.LEFT_EAR)?.let {
            landmarks[AppPoseLandmark.LEFT_EAR] = normalizePoint(it, imageWidth, imageHeight)
        }
        pose.getPoseLandmark(PoseLandmark.RIGHT_EAR)?.let {
            landmarks[AppPoseLandmark.RIGHT_EAR] = normalizePoint(it, imageWidth, imageHeight)
        }
        
        pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.let {
            landmarks[AppPoseLandmark.LEFT_SHOULDER] = normalizePoint(it, imageWidth, imageHeight)
        }
        pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.let {
            landmarks[AppPoseLandmark.RIGHT_SHOULDER] = normalizePoint(it, imageWidth, imageHeight)
        }
        
        pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)?.let {
            landmarks[AppPoseLandmark.LEFT_ELBOW] = normalizePoint(it, imageWidth, imageHeight)
        }
        pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)?.let {
            landmarks[AppPoseLandmark.RIGHT_ELBOW] = normalizePoint(it, imageWidth, imageHeight)
        }
        
        pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)?.let {
            landmarks[AppPoseLandmark.LEFT_WRIST] = normalizePoint(it, imageWidth, imageHeight)
        }
        pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)?.let {
            landmarks[AppPoseLandmark.RIGHT_WRIST] = normalizePoint(it, imageWidth, imageHeight)
        }
        
        pose.getPoseLandmark(PoseLandmark.LEFT_HIP)?.let {
            landmarks[AppPoseLandmark.LEFT_HIP] = normalizePoint(it, imageWidth, imageHeight)
        }
        pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)?.let {
            landmarks[AppPoseLandmark.RIGHT_HIP] = normalizePoint(it, imageWidth, imageHeight)
        }
        
        pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)?.let {
            landmarks[AppPoseLandmark.LEFT_KNEE] = normalizePoint(it, imageWidth, imageHeight)
        }
        pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)?.let {
            landmarks[AppPoseLandmark.RIGHT_KNEE] = normalizePoint(it, imageWidth, imageHeight)
        }
        
        pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)?.let {
            landmarks[AppPoseLandmark.LEFT_ANKLE] = normalizePoint(it, imageWidth, imageHeight)
        }
        pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)?.let {
            landmarks[AppPoseLandmark.RIGHT_ANKLE] = normalizePoint(it, imageWidth, imageHeight)
        }
        
        pose.getPoseLandmark(PoseLandmark.LEFT_HEEL)?.let {
            landmarks[AppPoseLandmark.LEFT_HEEL] = normalizePoint(it, imageWidth, imageHeight)
        }
        pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL)?.let {
            landmarks[AppPoseLandmark.RIGHT_HEEL] = normalizePoint(it, imageWidth, imageHeight)
        }
        
        pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX)?.let {
            landmarks[AppPoseLandmark.LEFT_FOOT_INDEX] = normalizePoint(it, imageWidth, imageHeight)
        }
        pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX)?.let {
            landmarks[AppPoseLandmark.RIGHT_FOOT_INDEX] = normalizePoint(it, imageWidth, imageHeight)
        }
        
        return PoseData(
            landmarks = landmarks,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun normalizePoint(landmark: PoseLandmark, imageWidth: Int, imageHeight: Int): Point3D {
        // Normalize coordinates to 0-1 range
        return Point3D(
            x = landmark.position.x / imageWidth,
            y = landmark.position.y / imageHeight,
            z = landmark.position3D.z
        )
    }
    
    fun close() {
        isClosed = true
        poseDetector.close()
        executor.shutdown()
    }
    
    companion object {
        private const val TAG = "PoseDetectorHelper"
    }
}
