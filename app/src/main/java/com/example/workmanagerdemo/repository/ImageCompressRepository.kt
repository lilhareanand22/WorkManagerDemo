package com.example.workmanagerdemo.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.workers.ImageCompressWorker
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for the Image Compression WorkManager use case.
 *
 * Responsibilities:
 * 1. Packages input parameters into [androidx.work.Data] using [workDataOf].
 * 2. Enqueues [ImageCompressWorker] using [WorkManager.enqueueUniqueWork] with [ExistingWorkPolicy.REPLACE].
 * 3. Exposes reactive streams ([Flow]) of [WorkInfo] for tracking state, progress, and output data.
 * 4. Provides cancellation controls by ID or tag.
 */
@Singleton
class ImageCompressRepository @Inject constructor(
    private val workManager: WorkManager
) {
    private var currentWorkId: UUID? = null

    /**
     * Enqueues an image compression task with the given parameters.
     *
     * @param imageUri Path or URI of the image to compress.
     * @param quality Compression quality level (1 to 100).
     * @return The unique [UUID] of the scheduled WorkRequest.
     */
    fun enqueueImageCompression(imageUri: String, quality: Int): UUID {
        // Construct the input Data payload.
        // WorkManager enforces a 10KB maximum limit for Data objects.
        val inputData = workDataOf(
            WorkerConstants.KEY_IMAGE_URI to imageUri,
            WorkerConstants.KEY_COMPRESS_QUALITY to quality
        )

        val workRequest = OneTimeWorkRequestBuilder<ImageCompressWorker>()
            .setInputData(inputData)
            .addTag(WorkerConstants.TAG_IMAGE_COMPRESS)
            .build()

        currentWorkId = workRequest.id

        // Enqueue as unique work to prevent overlapping redundant compression tasks
        workManager.enqueueUniqueWork(
            WorkerConstants.UNIQUE_IMAGE_COMPRESS,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        return workRequest.id
    }

    /**
     * Observes real-time [WorkInfo] updates for a specific task.
     * Emits state changes, [WorkInfo.progress] updates, and final [WorkInfo.outputData].
     */
    fun observeWorkById(id: UUID): Flow<WorkInfo?> =
        workManager.getWorkInfoByIdFlow(id)

    /**
     * Cancels the currently tracked compression request.
     */
    fun cancelCurrentWork() {
        currentWorkId?.let { workManager.cancelWorkById(it) }
    }

    /**
     * Cancels all tasks tagged with [WorkerConstants.TAG_IMAGE_COMPRESS].
     */
    fun cancelAllCompressionWork() {
        workManager.cancelAllWorkByTag(WorkerConstants.TAG_IMAGE_COMPRESS)
    }

    fun getCurrentWorkId(): UUID? = currentWorkId
}
