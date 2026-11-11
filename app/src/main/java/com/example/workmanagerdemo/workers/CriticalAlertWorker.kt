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
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.workmanagerdemo.R
import com.example.workmanagerdemo.WorkManagerDemoApp
import com.example.workmanagerdemo.WorkerConstants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sub-task 2.4 ─ CriticalAlertWorker
 * ============================================================
 * A [CoroutineWorker] demonstrating **Expedited Work** (`setExpedited()`):
 *
 * 1. **Why Expedited Work? (Android 12+ / API 31+ FGS restrictions)**:
 *    Starting with Android 12, apps cannot launch background Foreground Services.
 *    Expedited Work is Google's recommended solution for short, high-priority
 *    tasks that must execute immediately (e.g., processing a critical push notification,
 *    sending a high-priority chat message, or triggering an emergency system alert).
 *
 * 2. **Mandatory [getForegroundInfo] Fallback**:
 *    On Android 12+ (API 31+), expedited work is handled directly by the OS JobScheduler
 *    without an ongoing notification. However, on **API < 31 (Android 11 and below)**,
 *    WorkManager emulates expedited work using an internal **Foreground Service**!
 *    Therefore, any Worker using `setExpedited()` MUST implement [getForegroundInfo].
 *    If missing, WorkManager throws an [IllegalStateException] on older Android versions.
 *
 * 3. **Quota & OutOfQuotaPolicy**:
 *    Expedited tasks are subject to system quotas allocated by App Standby Buckets.
 *    If the quota is exceeded, [androidx.work.OutOfQuotaPolicy] decides whether to
 *    downgrade the job to a standard deferred task or drop it.
 */
@HiltWorker
class CriticalAlertWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "CriticalAlertWorker"
    }

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "CriticalAlertWorker started immediately — ID=$id, isExpedited=$runAttemptCount")

        // ── 1. Read Input Data ───────────────────────────────────────
        val alertTitle = inputData.getString(WorkerConstants.KEY_ALERT_TITLE) ?: "Emergency Notice"
        val alertMessage = inputData.getString(WorkerConstants.KEY_ALERT_MESSAGE)
            ?: "Critical system alert dispatched"
        val severity = inputData.getString(WorkerConstants.KEY_ALERT_SEVERITY) ?: "CRITICAL"

        Log.d(TAG, "Dispatching alert: [$severity] $alertTitle - $alertMessage")

        return try {
            if (isStopped) return Result.failure()

            // ── 2. Simulate Immediate Network Alert Handshake ───────────
            // Expedited tasks are intended to be short (< 10 seconds typical execution)
            reportProgress(25, "Authenticating expedited channel…")
            delay(500L)

            if (isStopped) return Result.failure()
            reportProgress(60, "Broadcasting [$severity] alert to dispatch servers…")
            delay(800L)

            if (isStopped) return Result.failure()
            reportProgress(90, "Verifying receipt confirmation…")
            delay(500L)

            // ── 3. Compile Output Data & Timing Metrics ──────────────────
            val latencyMs = System.currentTimeMillis() - startTime
            val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())

            Log.d(TAG, "Critical alert dispatched successfully in ${latencyMs}ms at $timestamp")

            val outputData = workDataOf(
                WorkerConstants.KEY_ALERT_TITLE to alertTitle,
                WorkerConstants.KEY_ALERT_SEVERITY to severity,
                WorkerConstants.KEY_DISPATCH_TIMESTAMP to timestamp,
                WorkerConstants.KEY_EXECUTION_LATENCY_MS to latencyMs,
                WorkerConstants.PROGRESS_MESSAGE to "Dispatched in ${latencyMs}ms (Severity: $severity)"
            )

            Result.success(outputData)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch critical alert: ${e.message}", e)
            Result.failure(
                workDataOf(WorkerConstants.PROGRESS_MESSAGE to "Alert failed: ${e.message}")
            )
        }
    }

    /**
     * Mandatory method for Expedited Workers!
     *
     * Used by WorkManager on API < 31 to display an ongoing Foreground Service notification
     * while the expedited task runs.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(
            title = "Dispatching High-Priority Alert",
            message = "Critical communication in progress…"
        )
    }

    /**
     * Constructs the [ForegroundInfo] notification wrapper.
     */
    private fun createForegroundInfo(title: String, message: String): ForegroundInfo {
        val notification: Notification = NotificationCompat.Builder(
            context,
            WorkManagerDemoApp.CHANNEL_ID_GENERAL_WORK
        )
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                WorkerConstants.NOTIFICATION_ID_EXPEDITED_ALERT,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                WorkerConstants.NOTIFICATION_ID_EXPEDITED_ALERT,
                notification
            )
        }
    }

    private suspend fun reportProgress(percent: Int, message: String) {
        val progressData = workDataOf(
            WorkerConstants.PROGRESS_VALUE to percent,
            WorkerConstants.PROGRESS_MESSAGE to message
        )
        setProgress(progressData)
    }

}