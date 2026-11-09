package com.example.workmanagerdemo.repository

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.workers.SyncLogsWorker
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository layer for WorkManager operations.
 *
 * ── Why a Repository? ───────────────────────────────────────────────────────
 * ViewModels should not directly touch [WorkManager] — that couples them to
 * Android framework specifics and makes unit testing hard.  The Repository:
 *   • Encapsulates ALL WorkManager API calls for SyncLogsWorker.
 *   • Returns [Flow]<[WorkInfo]> so the ViewModel stays framework-agnostic.
 *   • Keeps track of the current [UUID] so the ViewModel can observe the
 *     right work request even after the screen is recreated.
 *
 * ── Singleton scope ─────────────────────────────────────────────────────────
 * @Singleton ensures one instance is shared across all ViewModels.  The
 * [currentWorkId] survives ViewModel recreation (but NOT process death —
 * for that, persist the UUID to DataStore or SharedPreferences).
 */
@Singleton
class SyncLogsRepository @Inject constructor(
    private val workManager: WorkManager
) {

    /**
     * UUID of the most recently enqueued SyncLogs work request.
     * Null if no work has been enqueued yet this session.
     */
    private var currentWorkId: UUID? = null

    // ── Enqueue ─────────────────────────────────────────────────────────────

    /**
     * Builds and enqueues a [OneTimeWorkRequestBuilder] for [SyncLogsWorker].
     *
     * @param requireNetwork        Adds a CONNECTED network constraint when true.
     * @param requireCharging       Adds a setRequiresCharging(true) constraint.
     * @param requireBatteryNotLow  Adds a setRequiresBatteryNotLow(true) constraint.
     * @param simulateFailure       When true, the worker will return failure (for demo).
     * @return The [UUID] of the enqueued work request (used for observation).
     *
     * ── OneTimeWorkRequest ───────────────────────────────────────────────────
     * [OneTimeWorkRequestBuilder] creates a request that runs exactly once.
     * WorkManager guarantees it will run even if the app process dies, as
     * long as the constraints become satisfied before the system wipes the DB.
     *
     * ── Constraints ─────────────────────────────────────────────────────────
     * Constraints are predicates the OS must satisfy BEFORE WorkManager
     * allows the Worker to execute.  If a constraint is lost mid-execution
     * (e.g. WiFi drops), WorkManager stops the Worker and re-enqueues it.
     *
     * Available constraint types:
     *   setRequiredNetworkType(NetworkType.*)
     *      CONNECTED          — Any active network (WiFi or mobile)
     *      UNMETERED          — WiFi / unmetered only (no mobile data)
     *      NOT_ROAMING        — Must not be roaming
     *      METERED            — Requires a metered network
     *      NOT_REQUIRED       — No network constraint (default)
     *
     *   setRequiresCharging(Boolean)
     *      true  → Device must be plugged in (charging)
     *
     *   setRequiresBatteryNotLow(Boolean)
     *      true  → Battery must be above the low threshold (~20%)
     *
     *   setRequiresDeviceIdle(Boolean)   [API 23+ only]
     *      true  → Device must be fully idle (screen off, not charging,
     *              no apps in foreground).  Rarely needed.
     *
     *   setRequiresStorageNotLow(Boolean)
     *      true  → Internal storage must not be critically low.
     *
     * ── Tags ────────────────────────────────────────────────────────────────
     * .addTag(TAG_SYNC_LOGS) allows:
     *   workManager.cancelAllWorkByTag(TAG_SYNC_LOGS)   — cancel by tag
     *   workManager.getWorkInfosByTagFlow(TAG_SYNC_LOGS) — observe by tag
     *
     * ── enqueueUniqueWork vs enqueue ────────────────────────────────────────
     * We use [WorkManager.enqueueUniqueWork] with [ExistingWorkPolicy.REPLACE]
     * so that tapping "Enqueue" twice replaces the old request cleanly.
     * This prevents duplicate rows accumulating in WorkManager's DB.
     */
    fun enqueueSyncLogs(
        requireNetwork: Boolean,
        requireCharging: Boolean,
        requireBatteryNotLow: Boolean,
        simulateFailure: Boolean = false
    ): UUID {

        // ── Build Constraints ────────────────────────────────────────────
        val constraints = Constraints.Builder()
            .apply {
                if (requireNetwork) {
                    // CONNECTED = any active network (WiFi or cellular).
                    // The worker will stay ENQUEUED until a network is available.
                    setRequiredNetworkType(NetworkType.CONNECTED)
                }
                if (requireCharging) {
                    // Worker will not start until the device is plugged in.
                    setRequiresCharging(true)
                }
                if (requireBatteryNotLow) {
                    // Worker will not start if battery is below the system
                    // low-battery threshold (typically ~20%).
                    setRequiresBatteryNotLow(true)
                }
            }
            .build()

        // ── Build WorkRequest ────────────────────────────────────────────
        val request = OneTimeWorkRequestBuilder<SyncLogsWorker>()
            .setConstraints(constraints)
            // Tags: arbitrary string labels attached to this request.
            // A request can have multiple tags.
            .addTag(WorkerConstants.TAG_SYNC_LOGS)
            // We can pass input data here (up to 10 KB).
            // For this demo we use a flag to simulate failure.
            // .setInputData(workDataOf("simulate_failure" to simulateFailure))
            .build()

        // Store the UUID for the observer Flow below.
        currentWorkId = request.id

        // ── Enqueue ──────────────────────────────────────────────────────
        // enqueueUniqueWork ensures there is only ONE work request with
        // the given name in the system at a time.
        //
        // ExistingWorkPolicy.REPLACE:
        //   • If another request with this name is ENQUEUED or RUNNING,
        //     cancel it and start the new one.
        //   • Useful for "re-sync" buttons where only the latest request matters.
        workManager.enqueueUniqueWork(
            /* uniqueWorkName = */ WorkerConstants.UNIQUE_WORK_ANALYTICS_SYNC + "_logs",
            /* existingWorkPolicy = */ ExistingWorkPolicy.REPLACE,
            /* work = */ request
        )

        return request.id
    }

    // ── Observe ─────────────────────────────────────────────────────────────

    /**
     * Returns a [Flow] that emits the latest [WorkInfo] for a given work ID.
     *
     * [WorkInfo] exposes:
     *   .state        — ENQUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED, BLOCKED
     *   .progress     — Data set via setProgress() inside the Worker
     *   .outputData   — Data returned via Result.success(data)
     *   .tags         — All tags attached to this request
     *   .runAttemptCount — How many times the Worker has been attempted
     *
     * The Flow is backed by WorkManager's Room database and emits a new
     * value every time the state changes — perfect for Compose's
     * collectAsStateWithLifecycle().
     *
     * Returns null emissions when the UUID is unknown or the work has been
     * pruned from WorkManager's database (after terminal state + 7 days).
     */
    fun observeWorkById(id: UUID): Flow<WorkInfo?> =
        workManager.getWorkInfoByIdFlow(id)

    /**
     * Observe ALL work with the [WorkerConstants.TAG_SYNC_LOGS] tag.
     * Returns a List<WorkInfo> that updates as each work's state changes.
     * Useful when you need a history of past runs (not filtered to one ID).
     */
    fun observeAllSyncLogWork(): Flow<List<WorkInfo>> =
        workManager.getWorkInfosByTagFlow(WorkerConstants.TAG_SYNC_LOGS)

    // ── Cancel ──────────────────────────────────────────────────────────────

    /**
     * Cancels the currently tracked work request by its [UUID].
     *
     * WorkManager sends a cancellation signal to the running Worker.
     * For a [CoroutineWorker], this cancels the coroutine Job causing
     * any active `delay()` or suspending call to throw
     * [kotlinx.coroutines.CancellationException].
     * The Worker's state transitions to CANCELLED in the Room database.
     */
    fun cancelCurrentWork() {
        currentWorkId?.let { workManager.cancelWorkById(it) }
    }

    /**
     * Cancels ALL work tagged with [WorkerConstants.TAG_SYNC_LOGS].
     * More aggressive than [cancelCurrentWork] — catches any stray requests.
     */
    fun cancelAllSyncLogWork() {
        workManager.cancelAllWorkByTag(WorkerConstants.TAG_SYNC_LOGS)
    }

    /** Exposes the current work ID for the ViewModel to observe. */
    fun getCurrentWorkId(): UUID? = currentWorkId
}
