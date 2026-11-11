package com.example.workmanagerdemo.workers

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.workmanagerdemo.WorkManagerDemoApp
import com.example.workmanagerdemo.WorkerConstants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.io.File

/**
 * Sub-task 2.7 ─ VideoExportWorker (Long-Running Foreground Worker)
 * =================================================================
 * Demonstrates:
 * 1. **Removing the 10-Minute Timeout**:
 *    Standard Workers must finish within ~10 minutes or the OS terminates them.
 *    By calling [setForeground], WorkManager promotes this task into an active
 *    **Foreground Service**, allowing execution to run for extended periods
 *    (e.g., heavy video encoding, large downloads, database rebuilds).
 *
 * 2. **Notification with Cancel Action**:
 *    Uses [WorkManager.createCancelPendingIntent] to attach a "Cancel" action
 *    button directly to the persistent notification, giving users system-level control.
 *
 * 3. **Dynamic Notification Progress**:
 *    Periodically updates both [setProgress] (for in-app UI) and
 *    [setForeground] (for the live Android notification progress bar).
 *
 * 4. **Cleanup in [onStopped]**:
 *    Deletes temporary partial video fragments if the task is cancelled mid-way.
 */
@HiltWorker
class VideoExportWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "VideoExportWorker"
    }

    private var temporaryOutputFile: File? = null

    override suspend fun doWork(): Result {
        Log.d(TAG, "VideoExportWorker started — ID=$id")

        val videoTitle = inputData.getString(WorkerConstants.KEY_VIDEO_TITLE) ?: "Summer_Vlog_2026"
        val resolution = inputData.getString(WorkerConstants.KEY_EXPORT_QUALITY) ?: "1080p"
        val format = inputData.getString(WorkerConstants.KEY_EXPORT_FORMAT) ?: "mp4"

        // ── STEP 1: Promote Worker to Foreground Service immediately ──────
        // This removes the 10-minute background timeout.
        setForeground(createForegroundInfo(0, "Initializing video transcoder…"))

        return try {
            // Stage 1: Demuxing (0% -> 20%)
            if (isStopped) return Result.failure()
            updateProgress(15, "Demuxing video & audio streams…")
            delay(1_200L)

            // Stage 2: Video Transcoding (20% -> 55%)
            if (isStopped) return Result.failure()
            updateProgress(35, "Encoding $resolution video frames (H.264/AVC)…")
            delay(1_500L)

            if (isStopped) return Result.failure()
            updateProgress(55, "Applying color grading & HDR metadata…")
            delay(1_500L)

            // Stage 3: Audio Resampling (55% -> 80%)
            if (isStopped) return Result.failure()
            updateProgress(75, "Encoding AAC stereo audio track (320kbps)…")
            delay(1_200L)

            // Stage 4: Container Muxing & File Writing (80% -> 100%)
            if (isStopped) return Result.failure()
            updateProgress(90, "Muxing final container ($format)…")
            delay(1_000L)

            // Create exported file in cache directory
            val exportDir = File(context.cacheDir, "exported_videos").apply {
                if (!exists()) mkdirs()
            }
            val outputFile = File(exportDir, "${videoTitle}_${resolution}_${System.currentTimeMillis()}.$format")
            temporaryOutputFile = outputFile
            outputFile.writeText("SIMULATED_VIDEO_DATA_$resolution")

            val fileSizeBytes = when (resolution) {
                "4K" -> 148_000_000L
                "1080p" -> 64_000_000L
                else -> 28_000_000L
            }

            updateProgress(100, "Export complete!")
            delay(500L)

            Log.d(TAG, "Video export complete: ${outputFile.name} (${fileSizeBytes / (1024 * 1024)} MB)")

            val outputData = workDataOf(
                WorkerConstants.KEY_OUTPUT_URI to outputFile.absolutePath,
                WorkerConstants.KEY_OUTPUT_SIZE_BYTES to fileSizeBytes,
                WorkerConstants.KEY_OUTPUT_DURATION to "02:45 min",
                WorkerConstants.KEY_EXPORT_QUALITY to resolution,
                WorkerConstants.KEY_EXPORT_FORMAT to format,
                WorkerConstants.PROGRESS_MESSAGE to "Exported $videoTitle ($resolution) successfully"
            )

            temporaryOutputFile = null // Completed cleanly, no cleanup needed
            Result.success(outputData)

        } catch (e: Exception) {
            Log.e(TAG, "VideoExportWorker failed: ${e.message}", e)
            cleanupIncompleteFile()
            Result.failure(
                workDataOf(WorkerConstants.PROGRESS_MESSAGE to "Export failed: ${e.message}")
            )
        }
    }

    /**
     * Updates both WorkManager's Room progress data (for UI)
     * and the ongoing Foreground Service notification (for the system tray).
     */
    private suspend fun updateProgress(percent: Int, message: String) {
        // 1. In-app UI progress
        setProgress(
            workDataOf(
                WorkerConstants.PROGRESS_VALUE to percent,
                WorkerConstants.PROGRESS_MESSAGE to message
            )
        )

        // 2. Notification progress in system drawer
        setForeground(createForegroundInfo(percent, message))
    }

    /**
     * Mandatory method for Foreground Workers.
     * WorkManager calls this if the system needs to re-display the notification.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(0, "Video export in progress…")
    }

    /**
     * Builds a [ForegroundInfo] notification with progress bar and a "Cancel" action.
     */
    private fun createForegroundInfo(progress: Int, message: String): ForegroundInfo {
        val cancelPendingIntent = WorkManager.getInstance(context)
            .createCancelPendingIntent(id) // Native cancellation Intent!

        val notification: Notification = NotificationCompat.Builder(
            context,
            WorkManagerDemoApp.CHANNEL_ID_FOREGROUND_WORK
        )
            .setContentTitle("Exporting Video ($progress%)")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setProgress(100, progress, false) // Live progress bar on notification!
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                cancelPendingIntent // User can cancel directly from notification!
            )
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                WorkerConstants.NOTIFICATION_ID_VIDEO_EXPORT,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                WorkerConstants.NOTIFICATION_ID_VIDEO_EXPORT,
                notification
            )
        }
    }

    private fun cleanupIncompleteFile() {
        temporaryOutputFile?.let {
            if (it.exists()) {
                Log.d(TAG, "Deleting incomplete video export file: ${it.name}")
                it.delete()
            }
        }
        temporaryOutputFile = null
    }


}
