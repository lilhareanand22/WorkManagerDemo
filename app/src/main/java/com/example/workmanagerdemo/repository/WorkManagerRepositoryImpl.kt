package com.example.workmanagerdemo.repository

import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [WorkManagerRepository].
 *
 * Implements the unified facade pattern, combining specialized sub-repositories
 * and system-wide WorkManager queries into a single injectable dependency.
 */
@Singleton
class WorkManagerRepositoryImpl @Inject constructor(
    private val workManager: WorkManager,
    private val syncLogsRepo: SyncLogsRepository,
    private val imageCompressRepo: ImageCompressRepository,
    private val dailyCleanupRepo: DailyCleanupRepository,
    private val criticalAlertRepo: CriticalAlertRepository,
    private val chainedWorkRepo: ChainedWorkRepository,
    private val analyticsSyncRepo: AnalyticsSyncRepository,
    private val videoExportRepo: VideoExportRepository
) : WorkManagerRepository {

    // ── Global System Operations ─────────────────────────────────────────────

    override fun observeAllActiveWork(): Flow<List<WorkInfo>> {
        val query = WorkQuery.fromStates(
            listOf(
                WorkInfo.State.ENQUEUED,
                WorkInfo.State.RUNNING,
                WorkInfo.State.BLOCKED
            )
        )
        return workManager.getWorkInfosFlow(query)
    }

    override fun observeWorkByTag(tag: String): Flow<List<WorkInfo>> =
        workManager.getWorkInfosByTagFlow(tag)

    override fun cancelAllWork() {
        workManager.cancelAllWork()
    }

    override fun pruneWork() {
        workManager.pruneWork()
    }

    // ── Use Case 1: One-Time Work with Constraints ───────────────────────────

    override fun enqueueSyncLogs(
        requireNetwork: Boolean,
        requireCharging: Boolean,
        requireBatteryNotLow: Boolean
    ): UUID = syncLogsRepo.enqueueSyncLogs(
        requireNetwork,
        requireCharging,
        requireBatteryNotLow
    )

    override fun observeSyncLogsWork(id: UUID): Flow<WorkInfo?> =
        syncLogsRepo.observeWorkById(id)

    override fun cancelSyncLogsWork() =
        syncLogsRepo.cancelCurrentWork()

    // ── Use Case 2: Input / Output Data & Progress ───────────────────────────

    override fun enqueueImageCompression(imageUri: String, quality: Int): UUID =
        imageCompressRepo.enqueueImageCompression(imageUri, quality)

    override fun observeImageCompression(id: UUID): Flow<WorkInfo?> =
        imageCompressRepo.observeWorkById(id)

    override fun cancelImageCompression() =
        imageCompressRepo.cancelCurrentWork()

    // ── Use Case 3: Periodic Work ────────────────────────────────────────────

    override fun schedulePeriodicCleanup(
        repeatIntervalMinutes: Long,
        flexIntervalMinutes: Long,
        requireCharging: Boolean,
        requireBatteryNotLow: Boolean,
        policy: ExistingPeriodicWorkPolicy
    ): UUID = dailyCleanupRepo.schedulePeriodicCleanup(
        repeatIntervalMinutes,
        flexIntervalMinutes,
        requireCharging,
        requireBatteryNotLow,
        policy
    )

    override fun observePeriodicCleanup(): Flow<List<WorkInfo>> =
        dailyCleanupRepo.observePeriodicWork()

    override fun cancelPeriodicCleanup() =
        dailyCleanupRepo.cancelPeriodicCleanup()

    // ── Use Case 4: Expedited Work ───────────────────────────────────────────

    override fun dispatchCriticalAlert(
        title: String,
        message: String,
        severity: String,
        policy: OutOfQuotaPolicy
    ): UUID = criticalAlertRepo.dispatchCriticalAlert(title, message, severity, policy)

    override fun observeCriticalAlert(id: UUID): Flow<WorkInfo?> =
        criticalAlertRepo.observeAlertWorkById(id)

    override fun cancelCriticalAlert() =
        criticalAlertRepo.cancelCurrentAlert()

    // ── Use Case 5: Chained Pipeline DAG ─────────────────────────────────────

    override fun enqueuePipeline(simulateDownloadFailure: Boolean): PipelineWorkIds =
        chainedWorkRepo.enqueuePipeline(simulateDownloadFailure)

    override fun observePipelineNode(id: UUID): Flow<WorkInfo?> =
        chainedWorkRepo.observeWorkById(id)

    override fun cancelPipeline() =
        chainedWorkRepo.cancelPipeline()

    // ── Use Case 6: Unique Work & Backoff ────────────────────────────────────

    override fun enqueueUniqueAnalyticsSync(
        batchId: String,
        eventsCount: Int,
        policy: ExistingWorkPolicy,
        backoffPolicy: BackoffPolicy,
        simulateRetry: Boolean
    ): UUID = analyticsSyncRepo.enqueueUniqueAnalyticsSync(
        batchId,
        eventsCount,
        policy,
        backoffPolicy,
        simulateRetry
    )

    override fun observeUniqueAnalyticsWork(): Flow<List<WorkInfo>> =
        analyticsSyncRepo.observeUniqueAnalyticsWork()

    override fun cancelUniqueAnalyticsWork() =
        analyticsSyncRepo.cancelAnalyticsSync()

    // ── Use Case 7: Long-Running Foreground Work ─────────────────────────────

    override fun startVideoExport(title: String, resolution: String, format: String): UUID =
        videoExportRepo.startVideoExport(title, resolution, format)

    override fun observeVideoExport(id: UUID): Flow<WorkInfo?> =
        videoExportRepo.observeVideoExport(id)

    override fun cancelVideoExport() =
        videoExportRepo.cancelVideoExport()
}
