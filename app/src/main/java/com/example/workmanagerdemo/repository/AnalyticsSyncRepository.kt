package com.example.workmanagerdemo.repository

import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.workers.AnalyticsSyncWorker
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository layer for Unique Work & Backoff Criteria demonstration.
 *
 * Demonstrates:
 * 1. **`enqueueUniqueWork`**:
 *    Ensures only a single analytical upload pipeline runs for [WorkerConstants.UNIQUE_WORK_ANALYTICS_SYNC].
 * 2. **`ExistingWorkPolicy` Options**:
 *    - `REPLACE`: Cancels active/pending work with that name and runs the new request.
 *    - `KEEP`: Discards the new request if work is already queued or executing.
 *    - `APPEND`: Chains the new work to run after the existing work finishes.
 *    - `APPEND_OR_REPLACE`: Chains, or replaces if the previous request failed/cancelled.
 * 3. **`setBackoffCriteria`**:
 *    Configures [BackoffPolicy.EXPONENTIAL] or [BackoffPolicy.LINEAR] with an initial
 *    retry delay of 10 seconds (minimum enforced by AndroidX WorkManager).
 */
@Singleton
class AnalyticsSyncRepository @Inject constructor(
    private val workManager: WorkManager
) {
    private var currentWorkId: UUID? = null

    /**
     * Enqueues a unique analytics sync request.
     *
     * @param batchId Unique identifier for the telemetry batch.
     * @param eventsCount Number of events to simulate syncing.
     * @param policy Conflict resolution policy.
     * @param backoffPolicy Exponential vs Linear delay progression.
     * @param simulateRetry If true, worker returns Result.retry() to demonstrate backoff.
     */
    fun enqueueUniqueAnalyticsSync(
        batchId: String,
        eventsCount: Int = 250,
        policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
        backoffPolicy: BackoffPolicy = BackoffPolicy.EXPONENTIAL,
        simulateRetry: Boolean = false
    ): UUID {
        val inputData = workDataOf(
            WorkerConstants.KEY_ANALYTICS_BATCH_ID to batchId,
            WorkerConstants.KEY_EVENTS_COUNT to eventsCount,
            WorkerConstants.KEY_RETRY_BACKOFF_ACTIVE to simulateRetry
        )

        val workRequest = OneTimeWorkRequestBuilder<AnalyticsSyncWorker>()
            .setInputData(inputData)
            .setBackoffCriteria(
                backoffPolicy = backoffPolicy,
                backoffDelay = 10,
                timeUnit = TimeUnit.SECONDS
            )
            .addTag(WorkerConstants.TAG_ANALYTICS_SYNC)
            .build()

        currentWorkId = workRequest.id

        // Enqueue with the selected unique policy
        workManager.enqueueUniqueWork(
            WorkerConstants.UNIQUE_WORK_ANALYTICS_SYNC,
            policy,
            workRequest
        )

        return workRequest.id
    }

    /**
     * Observes all work requests associated with the unique name.
     */
    fun observeUniqueAnalyticsWork(): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(WorkerConstants.UNIQUE_WORK_ANALYTICS_SYNC)

    /**
     * Cancels the unique analytics work.
     */
    fun cancelAnalyticsSync() {
        workManager.cancelUniqueWork(WorkerConstants.UNIQUE_WORK_ANALYTICS_SYNC)
    }

    fun getCurrentWorkId(): UUID? = currentWorkId
}
