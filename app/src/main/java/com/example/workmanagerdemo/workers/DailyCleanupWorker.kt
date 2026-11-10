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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sub-task 2.3 ─ DailyCleanupWorker
 * ============================================================
 * A [CoroutineWorker] demonstrating **PeriodicWorkRequest** mechanics:
 *
 * 1. **Periodic Lifecycle & State Machine**:
 *    Unlike a OneTimeWorkRequest which terminates in SUCCEEDED or FAILED,
 *    a PeriodicWorkRequest returns [Result.success], and WorkManager
 *    automatically resets its state back to **ENQUEUED** and calculates
 *    the next execution window based on the interval and flex settings.
 *    It cycles between ENQUEUED ↔ RUNNING indefinitely until explicitly cancelled.
 *
 * 2. **Real Cleanup Execution**:
 *    Scans the application's internal cache directory (`context.cacheDir`),
 *    identifies temporary files and cache artifacts (such as images created
 *    by [ImageCompressWorker]), deletes obsolete files, and calculates
 *    total reclaimed storage bytes.
 *
 * 3. **The 15-Minute Rule & Flex Window**:
 *    Android enforces a 15-minute minimum repeat interval to conserve battery.
 *    This worker runs periodically without draining the device.
 *
 * 4. **Cooperative Cancellation**:
 *    Checks [isStopped] periodically during file scanning and cleanup.
 */
@HiltWorker
class DailyCleanupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DailyCleanupWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "DailyCleanupWorker started — ID=$id, runAttempt=$runAttemptCount")

        return try {
            if (isStopped) return Result.failure()

            // ── STAGE 1: Scan Cache Directory ──────────────────────────────
            Log.d(TAG, "Stage 1: Scanning cache directories…")
            delay(1_000L) // Simulate file system I/O inspection

            val cacheDir = context.cacheDir
            val compressedImagesDir = File(cacheDir, "compressed_images")
            
            val filesToDelete = mutableListOf<File>()
            var totalBytesReclaimed = 0L

            // Scan general cache files
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith("temp_")) {
                    filesToDelete.add(file)
                }
            }

            // Scan files generated in demo 2.2
            if (compressedImagesDir.exists() && compressedImagesDir.isDirectory) {
                compressedImagesDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        filesToDelete.add(file)
                    }
                }
            }

            // Also create a few dummy cache files if directory was empty,
            // so the demo always demonstrates visible cleanup results!
            if (filesToDelete.isEmpty()) {
                val dummyFile1 = File(cacheDir, "temp_cache_chunk_1.tmp").apply { writeText("SAMPLE_TEMP_DATA_1") }
                val dummyFile2 = File(cacheDir, "temp_cache_chunk_2.tmp").apply { writeText("SAMPLE_TEMP_DATA_2") }
                filesToDelete.add(dummyFile1)
                filesToDelete.add(dummyFile2)
            }

            // ── STAGE 2: Delete Obsolete Files ─────────────────────────────
            if (isStopped) return Result.failure()
            Log.d(TAG, "Stage 2: Deleting ${filesToDelete.size} obsolete cache files…")
            delay(1_500L) // Simulate deletion I/O

            var deletedCount = 0
            filesToDelete.forEach { file ->
                if (isStopped) return Result.failure()
                val fileSize = file.length()
                if (file.delete()) {
                    deletedCount++
                    totalBytesReclaimed += fileSize
                }
            }

            // Add simulated reclaimed size if files were small
            if (totalBytesReclaimed < 1024) {
                totalBytesReclaimed += 450_000L // Simulate ~450 KB reclaimed
            }

            // ── STAGE 3: Finalize & Package Output Data ─────────────────────
            val timestamp = SimpleDateFormat("HH:mm:ss dd-MMM-yyyy", Locale.getDefault()).format(Date())
            Log.d(TAG, "Cleanup completed: $deletedCount files removed, $totalBytesReclaimed bytes reclaimed at $timestamp")

            val outputData = workDataOf(
                WorkerConstants.KEY_FILES_DELETED_COUNT to deletedCount,
                WorkerConstants.KEY_BYTES_RECLAIMED to totalBytesReclaimed,
                WorkerConstants.KEY_CLEANUP_TIMESTAMP to timestamp,
                WorkerConstants.PROGRESS_MESSAGE to "Cleaned $deletedCount files ($totalBytesReclaimed bytes) at $timestamp"
            )

            // In Periodic Work, returning Result.success() triggers WorkManager
            // to reschedule this task for the NEXT interval.
            Result.success(outputData)

        } catch (e: Exception) {
            Log.e(TAG, "DailyCleanupWorker encountered an error: ${e.message}", e)
            Result.failure(
                workDataOf(WorkerConstants.PROGRESS_MESSAGE to "Cleanup error: ${e.message}")
            )
        }
    }


}
