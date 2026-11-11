package com.example.workmanagerdemo.repository

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.workers.CriticalAlertWorker
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository layer for Expedited Work (Critical Alert Worker).
 *
 * Demonstrates:
 * 1. **`setExpedited()`**: Instructs WorkManager to execute the task immediately
 *    with high priority, bypassing regular deferral mechanisms.
 * 2. **`OutOfQuotaPolicy` Configuration**:
 *    - `RUN_AS_NON_EXPEDITED_WORK_REQUEST`: If the system quota is exhausted,
 *      downgrade gracefully to a standard deferred background task.
 *    - `DROP_WORK_REQUEST`: If quota is exceeded, cancel and drop the task immediately.
 */
@Singleton
class CriticalAlertRepository @Inject constructor(
    private val workManager: WorkManager
) {
    private var currentWorkId: UUID? = null

    /**
     * Enqueues an expedited critical alert request.
     *
     * @param title Title of the critical alert.
     * @param message Detailed notification text.
     * @param severity Priority rating (CRITICAL, HIGH, NORMAL).
     * @param policy Behavior when OS standby quota is exceeded.
     * @return The UUID of the enqueued work request.
     */
    fun dispatchCriticalAlert(
        title: String,
        message: String,
        severity: String = "CRITICAL",
        policy: OutOfQuotaPolicy = OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
    ): UUID {
        val inputData = workDataOf(
            WorkerConstants.KEY_ALERT_TITLE to title,
            WorkerConstants.KEY_ALERT_MESSAGE to message,
            WorkerConstants.KEY_ALERT_SEVERITY to severity
        )

        val workRequest = OneTimeWorkRequestBuilder<CriticalAlertWorker>()
            .setInputData(inputData)
            .setExpedited(policy) // Core Expedited Work declaration!
            .addTag(WorkerConstants.TAG_CRITICAL_ALERT)
            .build()

        currentWorkId = workRequest.id

        workManager.enqueue(workRequest)

        return workRequest.id
    }

    /**
     * Observes real-time execution state for the dispatched alert.
     */
    fun observeAlertWorkById(id: UUID): Flow<WorkInfo?> =
        workManager.getWorkInfoByIdFlow(id)

    /**
     * Cancels the currently tracked alert task.
     */
    fun cancelCurrentAlert() {
        currentWorkId?.let { workManager.cancelWorkById(it) }
    }

    fun getCurrentWorkId(): UUID? = currentWorkId
}
