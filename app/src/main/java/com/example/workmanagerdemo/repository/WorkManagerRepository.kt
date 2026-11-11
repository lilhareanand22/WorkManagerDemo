package com.example.workmanagerdemo.repository

import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Phase 3 ─ Consolidated WorkManager Repository Interface
 * =======================================================
 * Defines a clean, unified architectural abstraction over AndroidX WorkManager.
 *
 * Responsibilities:
 * 1. Decouples ViewModels and UI from direct WorkManager framework dependencies.
 * 2. Exposes system-wide queries (observing all active work, querying by tag).
 * 3. Provides clean entry points for all 7 use cases demonstrated in the app.
 * 4. Enables frictionless unit and UI testing via test doubles / mocks.
 */
interface WorkManagerRepository {

    // ── Global System Observers & Commands ───────────────────────────────────

    /**
     * Observes all work requests currently in active states (ENQUEUED, RUNNING, BLOCKED).
     */
    fun observeAllActiveWork(): Flow<List<WorkInfo>>

    /**
     * Observes work requests filtered by tag.
     */
    fun observeWorkByTag(tag: String): Flow<List<WorkInfo>>

    /**
     * Cancels all scheduled, enqueued, and running background work across the entire app.
     */
    fun cancelAllWork()

    /**
     * Prunes finished work (SUCCEEDED, FAILED, CANCELLED) from WorkManager's internal database.
     */
    fun pruneWork()

    // ── Use Case 1: One-Time Work with Constraints ───────────────────────────

    fun enqueueSyncLogs(
        requireNetwork: Boolean,
        requireCharging: Boolean,
        requireBatteryNotLow: Boolean
    ): UUID

    fun observeSyncLogsWork(id: UUID): Flow<WorkInfo?>

    fun cancelSyncLogsWork()

    // ── Use Case 2: Input / Output Data & Progress ───────────────────────────

    fun enqueueImageCompression(imageUri: String, quality: Int): UUID

    fun observeImageCompression(id: UUID): Flow<WorkInfo?>

    fun cancelImageCompression()

    // ── Use Case 3: Periodic Work ────────────────────────────────────────────

    fun schedulePeriodicCleanup(
        repeatIntervalMinutes: Long,
        flexIntervalMinutes: Long,
        requireCharging: Boolean,
        requireBatteryNotLow: Boolean,
        policy: ExistingPeriodicWorkPolicy
    ): UUID

    fun observePeriodicCleanup(): Flow<List<WorkInfo>>

    fun cancelPeriodicCleanup()

    // ── Use Case 4: Expedited Work ───────────────────────────────────────────

    fun dispatchCriticalAlert(
        title: String,
        message: String,
        severity: String,
        policy: OutOfQuotaPolicy
    ): UUID

    fun observeCriticalAlert(id: UUID): Flow<WorkInfo?>

    fun cancelCriticalAlert()

    // ── Use Case 5: Chained Pipeline DAG ─────────────────────────────────────

    fun enqueuePipeline(simulateDownloadFailure: Boolean): PipelineWorkIds

    fun observePipelineNode(id: UUID): Flow<WorkInfo?>

    fun cancelPipeline()

    // ── Use Case 6: Unique Work & Backoff ────────────────────────────────────

    fun enqueueUniqueAnalyticsSync(
        batchId: String,
        eventsCount: Int,
        policy: ExistingWorkPolicy,
        backoffPolicy: BackoffPolicy,
        simulateRetry: Boolean
    ): UUID

    fun observeUniqueAnalyticsWork(): Flow<List<WorkInfo>>

    fun cancelUniqueAnalyticsWork()

    // ── Use Case 7: Long-Running Foreground Work ─────────────────────────────

    fun startVideoExport(title: String, resolution: String, format: String): UUID

    fun observeVideoExport(id: UUID): Flow<WorkInfo?>

    fun cancelVideoExport()
}
