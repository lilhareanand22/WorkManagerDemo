package com.example.workmanagerdemo.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.workers.VideoExportWorker
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository layer for the Long-Running Foreground Worker (Video Export).
 *
 * Demonstrates:
 * 1. Enqueuing tasks that promote themselves to an Android Foreground Service.
 * 2. Observing real-time progress and output data.
 * 3. Enforcing uniqueness with [ExistingWorkPolicy.REPLACE] so only one video renders at a time.
 */
@Singleton
class VideoExportRepository @Inject constructor(
    private val workManager: WorkManager
) {
    private var currentWorkId: UUID? = null

    /**
     * Enqueues a video export task.
     */
    fun startVideoExport(
        title: String,
        resolution: String = "1080p",
        format: String = "mp4"
    ): UUID {
        val inputData = workDataOf(
            WorkerConstants.KEY_VIDEO_TITLE to title,
            WorkerConstants.KEY_EXPORT_QUALITY to resolution,
            WorkerConstants.KEY_EXPORT_FORMAT to format
        )

        val workRequest = OneTimeWorkRequestBuilder<VideoExportWorker>()
            .setInputData(inputData)
            .addTag(WorkerConstants.TAG_VIDEO_EXPORT)
            .build()

        currentWorkId = workRequest.id

        workManager.enqueueUniqueWork(
            WorkerConstants.UNIQUE_VIDEO_EXPORT,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        return workRequest.id
    }

    fun observeVideoExport(id: UUID): Flow<WorkInfo?> =
        workManager.getWorkInfoByIdFlow(id)

    fun cancelVideoExport() {
        currentWorkId?.let { workManager.cancelWorkById(it) }
    }

    fun getCurrentWorkId(): UUID? = currentWorkId
}
