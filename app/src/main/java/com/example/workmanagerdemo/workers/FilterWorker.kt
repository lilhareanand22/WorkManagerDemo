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
 * Sub-task 2.5 ─ FilterWorker (Parallel Branch B in the Work Pipeline)
 * ===================================================================
 * Runs concurrently with [DownloadWorker].
 * Simulates in-memory filtering, schema verification, and deduplication rules.
 *
 * Its output [WorkerConstants.KEY_FILTERED_DATA] and [WorkerConstants.KEY_RECORDS_PROCESSED]
 * will be merged with [DownloadWorker]'s output by [androidx.work.ArrayCreatingInputMerger]
 * and passed into [CombineAndUploadWorker].
 */
@HiltWorker
class FilterWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "FilterWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "FilterWorker started in parallel — ID=$id")

        return try {
            if (isStopped) return Result.failure()

            reportProgress(30, "Compiling business validation rules…")
            delay(900L)

            if (isStopped) return Result.failure()
            reportProgress(70, "Scanning in-memory schema & removing duplicates…")
            delay(1_100L)

            val processedRecords = 1_420
            val filteredRules = "Sanitized 1,420 records; removed 38 null entries & duplicates"

            val outputData = workDataOf(
                WorkerConstants.KEY_FILTERED_DATA to filteredRules,
                WorkerConstants.KEY_RECORDS_PROCESSED to processedRecords,
                WorkerConstants.PROGRESS_MESSAGE to "Filter completed: $processedRecords records ready"
            )

            Log.d(TAG, "FilterWorker succeeded — processed $processedRecords records")
            Result.success(outputData)

        } catch (e: Exception) {
            Log.e(TAG, "FilterWorker failed: ${e.message}", e)
            Result.failure(
                workDataOf(WorkerConstants.PROGRESS_MESSAGE to "Filter error: ${e.message}")
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
