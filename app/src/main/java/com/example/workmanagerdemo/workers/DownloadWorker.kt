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
import java.io.File

/**
 * Sub-task 2.5 ─ DownloadWorker (Parallel Branch A in the Work Pipeline)
 * =====================================================================
 * Simulates downloading a raw dataset from a remote server.
 * Runs in parallel with [FilterWorker].
 *
 * Demonstrates:
 * 1. Generating output data intended for a downstream chained child ([CombineAndUploadWorker]).
 * 2. Simulating failure to demonstrate **WorkManager Failure Propagation**:
 *    When [DownloadWorker] fails, WorkManager immediately cancels downstream
 *    dependents and marks them as `FAILED` without running them!
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DownloadWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "DownloadWorker started — ID=$id")

        val shouldSimulateFailure = inputData.getBoolean(WorkerConstants.KEY_SIMULATE_FAILURE, false)

        return try {
            if (isStopped) return Result.failure()

            reportProgress(20, "Establishing connection to data server…")
            delay(800L)

            if (isStopped) return Result.failure()
            reportProgress(60, "Downloading raw chunks (1.2 MB / 2.4 MB)…")
            delay(1_000L)

            // Test Failure Propagation:
            if (shouldSimulateFailure) {
                Log.e(TAG, "Simulating network timeout failure in DownloadWorker!")
                val failureData = workDataOf(
                    WorkerConstants.PROGRESS_MESSAGE to "Simulated Network Timeout in DownloadWorker!"
                )
                return Result.failure(failureData)
            }

            if (isStopped) return Result.failure()
            reportProgress(90, "Verifying checksum SHA-256…")
            delay(600L)

            // Write sample downloaded file to cache
            val downloadedFile = File(context.cacheDir, "raw_dataset_${System.currentTimeMillis()}.csv")
            downloadedFile.writeText("id,timestamp,value\n1,1731300000,98.2\n2,1731300060,94.5")

            val outputData = workDataOf(
                WorkerConstants.KEY_DOWNLOADED_FILE_URI to downloadedFile.absolutePath,
                WorkerConstants.PROGRESS_MESSAGE to "Downloaded 2.4 MB raw dataset successfully"
            )

            Log.d(TAG, "DownloadWorker succeeded — file=${downloadedFile.name}")
            Result.success(outputData)

        } catch (e: Exception) {
            Log.e(TAG, "DownloadWorker failed: ${e.message}", e)
            Result.failure(
                workDataOf(WorkerConstants.PROGRESS_MESSAGE to "Download error: ${e.message}")
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
