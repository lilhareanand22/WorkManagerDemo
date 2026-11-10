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
import kotlin.math.roundToInt

/**
 * Sub-task 2.2 ─ ImageCompressWorker
 * ============================================================
 * A [CoroutineWorker] demonstrating three critical WorkManager concepts:
 *
 * 1. **Input Data Handling**:
 *    Reads input parameters passed via [androidx.work.WorkRequest.Builder.setInputData].
 *    Validates required keys (e.g., image URI and compression quality factor).
 *
 * 2. **Real-time Progress Reporting via [setProgress]**:
 *    Reports incremental progress percentage (0% → 100%) and stage descriptions.
 *    Under the hood, [setProgress] writes to WorkManager's Room `WorkProgress` table.
 *    Room's InvalidationTracker triggers emissions on `getWorkInfoByIdFlow()`, which
 *    updates the Jetpack Compose UI reactively without polling.
 *
 * 3. **The 10KB Data Limit Pattern & Output Data**:
 *    WorkManager enforces a strict 10KB limit on [androidx.work.Data] payloads to
 *    prevent SQLite CursorWindow and IPC Binder transaction buffer overflows.
 *    We NEVER pass raw byte arrays or bitmaps in [androidx.work.Data].
 *    Instead, the worker writes the compressed file to disk ([Context.getCacheDir])
 *    and returns only the file path, metadata, and final sizes via [Result.success].
 *
 * 4. **Cooperative Cancellation**:
 *    Regularly checks [isStopped] so when the user taps "Cancel", background
 *    processing terminates promptly without leaking CPU cycles.
 */
@HiltWorker
class ImageCompressWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ImageCompressWorker"

        // Simulated processing stages for realistic progress demonstration
        private const val STAGE_INIT = 10
        private const val STAGE_DECODE = 30
        private const val STAGE_TRANSFORM = 60
        private const val STAGE_COMPRESS = 85
        private const val STAGE_FINALIZE = 100
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork() started — ID=$id, attempt=$runAttemptCount")

        // ── STEP 1: Parse & Validate Input Data ─────────────────────────────
        val imageUri = inputData.getString(WorkerConstants.KEY_IMAGE_URI)
        val quality = inputData.getInt(
            WorkerConstants.KEY_COMPRESS_QUALITY,
            WorkerConstants.DEFAULT_COMPRESS_QUALITY
        )

        Log.d(TAG, "Input params: URI=$imageUri, Quality=$quality%")

        // Input data validation
        if (imageUri.isNullOrBlank()) {
            val errorData = workDataOf(
                WorkerConstants.PROGRESS_MESSAGE to "Failure: Missing or invalid Image URI"
            )
            Log.e(TAG, "Validation error: Image URI is missing")
            return Result.failure(errorData)
        }

        return try {
            // ── STAGE 1: Initialize & Read Metadata (0% -> 10%) ─────────────
            if (isStopped) return cancelledResult()
            reportProgress(STAGE_INIT, "Reading image headers & metadata…")
            delay(1_000L)

            // Simulated original image attributes
            val originalSizeBytes = 3_450_000L // ~3.45 MB
            val imageName = imageUri.substringAfterLast("/").ifEmpty { "sample_photo.jpg" }

            // ── STAGE 2: Downsample & Decode Bitmap (10% -> 30%) ────────────
            if (isStopped) return cancelledResult()
            reportProgress(STAGE_DECODE, "Decoding bitmap in memory…")
            delay(1_200L)

            // ── STAGE 3: Apply Compression Algorithm (30% -> 60%) ───────────
            if (isStopped) return cancelledResult()
            reportProgress(50, "Applying DCT quantization (Quality: $quality%)…")
            delay(1_500L)

            if (isStopped) return cancelledResult()
            reportProgress(STAGE_TRANSFORM, "Encoding optimized Huffman tables…")
            delay(1_200L)

            // ── STAGE 4: Write to Cache Directory (60% -> 85%) ──────────────
            // Architectural Best Practice: Always write output files to local storage
            // (e.g. cacheDir) and return only the URI/path in WorkManager Data.
            if (isStopped) return cancelledResult()
            reportProgress(STAGE_COMPRESS, "Writing compressed file to disk cache…")
            delay(1_200L)

            val outputDir = File(context.cacheDir, "compressed_images").apply {
                if (!exists()) mkdirs()
            }
            val outputFile = File(outputDir, "compressed_${System.currentTimeMillis()}_$imageName")

            // Simulate compression ratio based on quality parameter
            // Higher quality = larger size, Lower quality = smaller size
            val compressionRatioMultiplier = (quality / 100.0) * 0.45 + 0.15
            val compressedSizeBytes = (originalSizeBytes * compressionRatioMultiplier).roundToInt().toLong()

            // Create placeholder file on disk to represent real output
            outputFile.writeText("SIMULATED_COMPRESSED_IMAGE_DATA_QUALITY_$quality")

            // ── STAGE 5: Finalize & Package Output (85% -> 100%) ─────────────
            if (isStopped) return cancelledResult()
            reportProgress(STAGE_FINALIZE, "Compression complete!")
            delay(500L)

            val reductionPercent = (((originalSizeBytes - compressedSizeBytes).toDouble() / originalSizeBytes) * 100).roundToInt()

            Log.d(TAG, "Compression succeeded: $originalSizeBytes -> $compressedSizeBytes bytes ($reductionPercent% reduction)")

            // ── SUCCESS: Return Output Data ─────────────────────────────────
            // Output Data is stored in Room and can be retrieved by observing WorkInfo.outputData
            val outputData = workDataOf(
                WorkerConstants.KEY_OUTPUT_URI to outputFile.absolutePath,
                WorkerConstants.KEY_ORIGINAL_SIZE_BYTES to originalSizeBytes,
                WorkerConstants.KEY_OUTPUT_SIZE_BYTES to compressedSizeBytes,
                WorkerConstants.KEY_COMPRESSION_RATIO to "$reductionPercent% reduction",
                WorkerConstants.PROGRESS_MESSAGE to "Successfully compressed to ${compressedSizeBytes / 1024} KB"
            )

            Result.success(outputData)

        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image: ${e.message}", e)
            Result.failure(
                workDataOf(
                    WorkerConstants.PROGRESS_MESSAGE to "Compression failed: ${e.message}"
                )
            )
        }
    }

    /**
     * Publishes real-time progress using [setProgress].
     *
     * This writes to the `WorkProgress` table in SQLite, which causes
     * [androidx.work.WorkManager.getWorkInfoByIdFlow] to emit updated [androidx.work.WorkInfo].
     */
    private suspend fun reportProgress(percent: Int, message: String) {
        val progressData = workDataOf(
            WorkerConstants.PROGRESS_VALUE to percent,
            WorkerConstants.PROGRESS_MESSAGE to message
        )
        setProgress(progressData)
        Log.d(TAG, "Progress update: $percent% - $message")
    }

    /** Helper for graceful early exit upon cancellation */
    private fun cancelledResult(): Result {
        Log.d(TAG, "Worker stopped/cancelled by user or system")
        return Result.failure(
            workDataOf(WorkerConstants.PROGRESS_MESSAGE to "Compression cancelled")
        )
    }


}
