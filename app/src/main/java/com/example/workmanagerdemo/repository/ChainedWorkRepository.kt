package com.example.workmanagerdemo.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OverwritingInputMerger
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.workers.CombineAndUploadWorker
import com.example.workmanagerdemo.workers.DownloadWorker
import com.example.workmanagerdemo.workers.FilterWorker
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encapsulates the IDs of the three nodes in the chained pipeline.
 */
data class PipelineWorkIds(
    val downloadId: UUID,
    val filterId: UUID,
    val combineId: UUID
)

/**
 * Repository layer for the Work Chaining & Pipelines use case.
 *
 * Demonstrates:
 * 1. **Parallel Fork & Join DAG**:
 *    `beginWith(listOf(A, B)).then(C).enqueue()`
 * 2. **InputMerger**:
 *    Uses [OverwritingInputMerger] so the downstream worker receives
 *    the merged outputs from both parallel parent workers.
 * 3. **Failure Propagation**:
 *    If Node A fails, Node C is cancelled by WorkManager without running.
 */
@Singleton
class ChainedWorkRepository @Inject constructor(
    private val workManager: WorkManager
) {
    private var currentPipelineIds: PipelineWorkIds? = null

    /**
     * Enqueues the 3-stage pipeline:
     *   [DownloadWorker] ┐
     *                    ├─► [CombineAndUploadWorker]
     *   [FilterWorker]   ┘
     *
     * @param simulateDownloadFailure If true, DownloadWorker returns Result.failure()
     *                                to demonstrate failure propagation.
     */
    fun enqueuePipeline(simulateDownloadFailure: Boolean = false): PipelineWorkIds {
        // Node A: DownloadWorker (Parallel Branch A)
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(WorkerConstants.KEY_SIMULATE_FAILURE to simulateDownloadFailure))
            .addTag(WorkerConstants.TAG_DOWNLOAD)
            .build()

        // Node B: FilterWorker (Parallel Branch B)
        val filterRequest = OneTimeWorkRequestBuilder<FilterWorker>()
            .addTag(WorkerConstants.TAG_FILTER)
            .build()

        // Node C: CombineAndUploadWorker (Sequential Dependent)
        // Configure OverwritingInputMerger to merge keys from both parent outputs
        val combineRequest = OneTimeWorkRequestBuilder<CombineAndUploadWorker>()
            .setInputMerger(OverwritingInputMerger::class.java)
            .addTag(WorkerConstants.TAG_COMBINE_UPLOAD)
            .build()

        val ids = PipelineWorkIds(
            downloadId = downloadRequest.id,
            filterId = filterRequest.id,
            combineId = combineRequest.id
        )
        currentPipelineIds = ids

        // ── Enqueue the Pipeline DAG with WorkContinuation ───────────
        workManager.beginUniqueWork(
            WorkerConstants.UNIQUE_CHAINED_PIPELINE,
            ExistingWorkPolicy.REPLACE,
            listOf(downloadRequest, filterRequest) // Parallel start
        )
            .then(combineRequest) // Dependent merge
            .enqueue()

        return ids
    }

    /** Observes individual node in the pipeline by ID. */
    fun observeWorkById(id: UUID): Flow<WorkInfo?> =
        workManager.getWorkInfoByIdFlow(id)

    /** Cancels the entire pipeline. */
    fun cancelPipeline() {
        workManager.cancelUniqueWork(WorkerConstants.UNIQUE_CHAINED_PIPELINE)
    }

    fun getCurrentPipelineIds(): PipelineWorkIds? = currentPipelineIds
}
