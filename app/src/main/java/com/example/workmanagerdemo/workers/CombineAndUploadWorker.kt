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

/**
 * Sub-task 2.5 ─ CombineAndUploadWorker (Pipeline Converging Dependent)
 * ====================================================================
 * This worker depends on BOTH [DownloadWorker] and [FilterWorker].
 * It does NOT execute until BOTH upstream tasks complete with [Result.success].
 *
 * Demonstrates:
 * 1. **Input Merging**:
 *    Receives outputs merged by [androidx.work.ArrayCreatingInputMerger] or [androidx.work.OverwritingInputMerger]:
 *    - [WorkerConstants.KEY_DOWNLOADED_FILE_URI] from [DownloadWorker]
 *    - [WorkerConstants.KEY_FILTERED_DATA] and [WorkerConstants.KEY_RECORDS_PROCESSED] from [FilterWorker]
 * 2. **Failure Propagation**:
 *    If either [DownloadWorker] or [FilterWorker] returns [Result.failure],
 *    this worker transitions from `BLOCKED` directly to `FAILED` and is NEVER executed.
 * 3. Final cloud upload execution.
 */
@HiltWorker
class CombineAndUploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "CombineAndUploadWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "CombineAndUploadWorker started after upstream join — ID=$id")

        // ── 1. Read Merged Inputs From Both Parent Workers ─────────────
        val downloadedFileUri = inputData.getString(WorkerConstants.KEY_DOWNLOADED_FILE_URI)
        val filteredData = inputData.getString(WorkerConstants.KEY_FILTERED_DATA)
        val recordsCount = inputData.getInt(WorkerConstants.KEY_RECORDS_PROCESSED, 0)

        Log.d(TAG, "Inputs merged: File=$downloadedFileUri, Filter=$filteredData, Records=$recordsCount")

        // Validate upstream dependencies succeeded
        if (downloadedFileUri.isNullOrBlank() || filteredData.isNullOrBlank()) {
            val errorMsg = "Missing merged input data from upstream tasks!"
            Log.e(TAG, errorMsg)
            return Result.failure(workDataOf(WorkerConstants.PROGRESS_MESSAGE to errorMsg))
        }

        return try {
            if (isStopped) return Result.failure()

            // ── 2. Combine and Transform Data ───────────────────────────
            reportProgress(25, "Combining raw dataset with sanitized filters…")
            delay(1_000L)

            if (isStopped) return Result.failure()
            reportProgress(60, "Packaging payload for Cloud Gateway…")
            delay(1_000L)

            // ── 3. Simulate Remote Upload ───────────────────────────────
            if (isStopped) return Result.failure()
            reportProgress(90, "Uploading batch to https://cloud-storage.internal/…")
            delay(1_200L)

            val uploadUrl = "https://cloud-storage.internal/datasets/batch_${System.currentTimeMillis()}.json"
            Log.d(TAG, "Upload completed successfully to $uploadUrl")

            val outputData = workDataOf(
                WorkerConstants.KEY_UPLOAD_URL to uploadUrl,
                WorkerConstants.KEY_RECORDS_PROCESSED to recordsCount,
                WorkerConstants.PROGRESS_MESSAGE to "Successfully merged and uploaded $recordsCount records"
            )

            Result.success(outputData)

        } catch (e: Exception) {
            Log.e(TAG, "CombineAndUploadWorker failed: ${e.message}", e)
            Result.failure(
                workDataOf(WorkerConstants.PROGRESS_MESSAGE to "Upload error: ${e.message}")
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
