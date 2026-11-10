package com.example.workmanagerdemo.repository

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.workers.DailyCleanupWorker
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository layer for the Periodic Work (Daily Cleanup) use case.
 *
 * Demonstrates:
 * 1. **`PeriodicWorkRequestBuilder`**: Constructing repeating background work.
 * 2. **The 15-Minute Rule**: WorkManager enforces a minimum repeat interval of 15 minutes
 *    ([PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS]). Values smaller than 15m are
 *    automatically clamped by the OS.
 * 3. **Flex Interval**: Minimum 5 minutes ([PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS]).
 *    Defines the execution window near the end of each period for OS battery batching.
 * 4. **`ExistingPeriodicWorkPolicy`**:
 *    - `UPDATE`: Updates interval/constraints of existing work without resetting execution timer.
 *    - `KEEP`: Ignores the new request if one is already scheduled.
 *    - `CANCEL_AND_REENQUEUE`: Cancels the existing periodic work and schedules a fresh one.
 */
@Singleton
class DailyCleanupRepository @Inject constructor(
    private val workManager: WorkManager
) {
    private var currentWorkId: UUID? = null

    /**
     * Schedules a unique periodic cleanup job.
     *
     * @param repeatIntervalMinutes Repeat interval (min 15m enforced).
     * @param flexIntervalMinutes Flex window (min 5m enforced).
     * @param requireCharging Device must be plugged in.
     * @param requireBatteryNotLow Battery must not be low (<20%).
     * @param policy Conflict resolution policy (UPDATE, KEEP, CANCEL_AND_REENQUEUE).
     * @return The UUID of the periodic work request.
     */
    fun schedulePeriodicCleanup(
        repeatIntervalMinutes: Long = 15L,
        flexIntervalMinutes: Long = 5L,
        requireCharging: Boolean = false,
        requireBatteryNotLow: Boolean = true,
        policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE
    ): UUID {
        // Enforce the system minimums:
        // Repeat interval >= 15 min
        // Flex interval >= 5 min
        val effectiveRepeat = maxOf(repeatIntervalMinutes, 15L)
        val effectiveFlex = maxOf(flexIntervalMinutes, 5L)

        val constraints = Constraints.Builder()
            .apply {
                if (requireCharging) setRequiresCharging(true)
                if (requireBatteryNotLow) setRequiresBatteryNotLow(true)
            }
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<DailyCleanupWorker>(
            repeatInterval = effectiveRepeat,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = effectiveFlex,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(WorkerConstants.TAG_DAILY_CLEANUP)
            .build()

        currentWorkId = periodicRequest.id

        // Enqueue as unique periodic work to prevent duplicate schedules
        workManager.enqueueUniquePeriodicWork(
            WorkerConstants.UNIQUE_PERIODIC_CLEANUP,
            policy,
            periodicRequest
        )

        return periodicRequest.id
    }

    /**
     * Observes the active unique periodic work request.
     * Emits a List of [WorkInfo] (empty if not yet scheduled).
     */
    fun observePeriodicWork(): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(WorkerConstants.UNIQUE_PERIODIC_CLEANUP)

    /**
     * Cancels the periodic cleanup work request.
     */
    fun cancelPeriodicCleanup() {
        workManager.cancelUniqueWork(WorkerConstants.UNIQUE_PERIODIC_CLEANUP)
    }

    fun getCurrentWorkId(): UUID? = currentWorkId
}
