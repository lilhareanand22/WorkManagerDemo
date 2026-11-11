package com.example.workmanagerdemo.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.workmanagerdemo.WorkerConstants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sub-task 2.6 ─ AnalyticsSyncWorker
 * ============================================================
 * A [CoroutineWorker] demonstrating **Unique Work** and **Backoff / Retry**:
 *
 * 1. **Unique Work Conflict Policies ([androidx.work.ExistingWorkPolicy])**:
 *    Ensures only a single analytics upload operates for a given unique name.
 *    - `REPLACE`: Cancels active sync and restarts with the new request.
 *    - `KEEP`: Discards the new request if sync is already running or queued.
 *    - `APPEND`: Chains the new batch to execute after the current batch finishes.
 *    - `APPEND_OR_REPLACE`: Chains, or replaces if the previous attempt failed.
 *
 * 2. **Retry with Backoff Criteria ([androidx.work.BackoffPolicy])**:
 *    When network encounters a transient error (e.g., HTTP 503 Service Unavailable),
 *    the worker returns [Result.retry]. WorkManager inspects [runAttemptCount]
 *    and reschedules the task after a calculated backoff delay (Linear or Exponential).
 */
@HiltWorker
class AnalyticsSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "AnalyticsSyncWorker"
        private const val MAX_RETRY_COUNT = 2 // Will succeed on attempt 2
    }

    override suspend fun doWork(): Result {
        val currentAttempt = runAttemptCount
        val batchId = inputData.getString(WorkerConstants.KEY_ANALYTICS_BATCH_ID) ?: "BATCH_${System.currentTimeMillis() % 10000}"
        val eventsCount = inputData.getInt(WorkerConstants.KEY_EVENTS_COUNT, 320)
        val simulateRetry = inputData.getBoolean(WorkerConstants.KEY_RETRY_BACKOFF_ACTIVE, false)

        Log.d(TAG, "AnalyticsSyncWorker starting — ID=$id, Batch=$batchId, Attempt=$currentAttempt, SimulateRetry=$simulateRetry")

        return try {
            if (isStopped) return Result.failure()

            reportProgress(20, "Batching $eventsCount analytics events [$batchId]…")
            delay(800L)

            // ── Simulate Transient Network Failure to demonstrate Retry & Backoff ──
            if (simulateRetry && currentAttempt < MAX_RETRY_COUNT) {
                Log.w(TAG, "Simulating HTTP 503 Server Unavailable on attempt #$currentAttempt. Returning Result.retry()!")
                reportProgress(
                    50,
                    "⚠️ HTTP 503 Gateway Timeout! Rescheduling attempt #${currentAttempt + 1} with Backoff Policy…"
                )
                delay(600L)

                // Returning Result.retry() instructs WorkManager to re-enqueue
                // this worker after the configured Backoff delay.
                return Result.retry()
            }

            if (isStopped) return Result.failure()
            reportProgress(75, "Uploading batch payload to analytics endpoint…")
            delay(1_000L)

            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            Log.d(TAG, "Batch $batchId successfully synced on attempt #$currentAttempt at $timestamp")

            val outputData = workDataOf(
                WorkerConstants.KEY_ANALYTICS_BATCH_ID to batchId,
                WorkerConstants.KEY_EVENTS_COUNT to eventsCount,
                WorkerConstants.KEY_ATTEMPT_COUNT to currentAttempt,
                WorkerConstants.PROGRESS_MESSAGE to "Synced $eventsCount events in $batchId (Attempt #$currentAttempt) at $timestamp"
            )

            Result.success(outputData)

        } catch (e: Exception) {
            Log.e(TAG, "AnalyticsSyncWorker failed: ${e.message}", e)
            Result.failure(
                workDataOf(WorkerConstants.PROGRESS_MESSAGE to "Sync failed: ${e.message}")
            )
        }
    }

    private suspend fun reportProgress(percent: Int, message: String) {
        setProgress(
            workDataOf(
                WorkerConstants.PROGRESS_VALUE to percent,
                WorkerConstants.PROGRESS_MESSAGE to message
            )
        )
    }


}
