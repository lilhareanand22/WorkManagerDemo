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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Sub-task 2.1 ─ SyncLogsWorker
 * ============================================================
 * A [CoroutineWorker] that simulates serialising in-memory logs
 * and uploading them to a remote server.
 *
 * ── Why CoroutineWorker? ─────────────────────────────────────
 * [CoroutineWorker] exposes a `suspend` function [doWork], which
 * lets us call suspending APIs (network, database, delay) without
 * blocking a thread.  WorkManager automatically runs [doWork] on
 * [kotlinx.coroutines.Dispatchers.Default] by default; we can
 * switch dispatchers inside the function when needed.
 *
 * ── Why @HiltWorker? ─────────────────────────────────────────
 * WorkManager normally creates Workers using reflection, calling
 * the two-argument constructor (Context, WorkerParameters).
 * With @HiltWorker + @AssistedInject we tell Hilt to build the
 * Worker, which allows us to inject real dependencies (a
 * repository, a logger, a network client) through additional
 * constructor parameters.  The two @Assisted parameters are still
 * supplied by WorkManager at runtime.
 *
 * ── Cooperative Cancellation ─────────────────────────────────
 * WorkManager can cancel a running Worker at any time (user
 * request, constraint lost, app kill).  For a CoroutineWorker the
 * underlying coroutine Job is cancelled, so any `delay()` or
 * suspend-call will throw CancellationException automatically.
 * We additionally check [isStopped] before each step as an
 * explicit safety net for non-suspending loops.
 *
 * ── Tagging ──────────────────────────────────────────────────
 * We add [WorkerConstants.TAG_SYNC_LOGS] in the enqueue call
 * (see [OneTimeWorkViewModel]) which lets us:
 *   • Observe: workManager.getWorkInfosByTagFlow(TAG_SYNC_LOGS)
 *   • Cancel all matching: workManager.cancelAllWorkByTag(TAG_SYNC_LOGS)
 */
@HiltWorker
class SyncLogsWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters
    // Additional injected dependencies (e.g. a LogRepository) would go here.
    // e.g. private val logRepository: LogRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncLogsWorker"

        // Simulated log entries — replace with real data source in production.
        private val FAKE_LOG_ENTRIES = listOf(
            "INFO  [2026-11-09 07:00:01] App launched",
            "DEBUG [2026-11-09 07:00:02] User session restored",
            "INFO  [2026-11-09 07:05:14] Feature flag fetch complete",
            "WARN  [2026-11-09 07:12:33] Slow network detected (RTT 850ms)",
            "ERROR [2026-11-09 07:15:01] Image load failed – retrying",
            "INFO  [2026-11-09 07:15:03] Image load succeeded on retry",
            "INFO  [2026-11-09 07:30:00] Background sync triggered"
        )
    }

    /**
     * The core work method — called by WorkManager on a background thread.
     *
     * Workflow:
     *   Step 1 — Collect logs from the in-memory buffer.
     *   Step 2 — Serialise logs to JSON / plain text.
     *   Step 3 — Upload serialised payload to the server.
     *
     * Each step uses [delay] to simulate real async I/O, and checks
     * [isStopped] so cancellation is honoured between steps.
     *
     * @return
     *   [Result.success] ─ All steps completed; WorkManager marks
     *                       the request as SUCCEEDED and clears it
     *                       from the internal database.
     *   [Result.failure]  ─ Unrecoverable error; downstream chained
     *                       workers (if any) will also be FAILED.
     *   [Result.retry]    ─ Transient error; WorkManager reschedules
     *                       with backoff (not used here, but shown
     *                       in Sub-task 2.6 AnalyticsSyncWorker).
     */
    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork() started — runAttemptCount=$runAttemptCount")

        return try {

            // ── STEP 1: Collect logs ─────────────────────────────────────
            // isStopped returns true as soon as WorkManager cancels this
            // Worker.  Checking it before each step prevents wasted CPU
            // when the user has already cancelled the work.
            if (isStopped) return Result.failure(buildFailureData("Cancelled before Step 1"))

            Log.d(TAG, "Step 1/3 — Collecting ${FAKE_LOG_ENTRIES.size} log entries…")
            delay(2_000L)   // Simulate reading from a local DB / buffer

            val collectedLogs = FAKE_LOG_ENTRIES.toList()
            Log.d(TAG, "Step 1/3 ✓ — Collected ${collectedLogs.size} entries")

            // ── STEP 2: Serialise ────────────────────────────────────────
            if (isStopped) return Result.failure(buildFailureData("Cancelled before Step 2"))

            Log.d(TAG, "Step 2/3 — Serialising logs…")
            delay(2_000L)   // Simulate JSON serialisation

            // Build a lightweight plain-text payload (never store a large
            // payload in WorkManager Data — 10 KB limit!  Store the file
            // path instead for real payloads.)
            val payloadSummary = "Logs serialised: ${collectedLogs.size} entries, " +
                    "size ≈ ${collectedLogs.sumOf { it.length }} chars"
            Log.d(TAG, "Step 2/3 ✓ — $payloadSummary")

            // ── STEP 3: Upload ───────────────────────────────────────────
            if (isStopped) return Result.failure(buildFailureData("Cancelled before Step 3"))

            Log.d(TAG, "Step 3/3 — Uploading to server…")
            delay(2_000L)   // Simulate network I/O

            Log.d(TAG, "Step 3/3 ✓ — Upload complete")

            // ── SUCCESS ──────────────────────────────────────────────────
            // Output Data is stored in WorkManager's Room database and can
            // be read by the UI via workInfo.outputData, or consumed by a
            // downstream chained worker.
            // IMPORTANT: Output Data is also capped at 10 KB.
            val outputData = workDataOf(
                WorkerConstants.PROGRESS_MESSAGE to "All ${ collectedLogs.size } logs synced successfully.",
                "timestamp" to System.currentTimeMillis()
            )
            Log.d(TAG, "doWork() SUCCEEDED")
            Result.success(outputData)

        } catch (e: CancellationException) {
            Log.d(TAG, "doWork() CANCELLED — rethrowing CancellationException")
            throw e
        } catch (e: Exception) {
            // For all other exceptions we return failure so WorkManager marks the work FAILED.
            Log.e(TAG, "doWork() FAILED: ${e.message}", e)
            Result.failure(buildFailureData(e.message ?: "Unknown error"))
        } finally {
            // ── CLEANUP ON COMPLETION OR CANCELLATION ──────────────────────
            // Note: In CoroutineWorker, onStopped() is 'final' and cannot be overridden.
            // WorkManager cancels the coroutine Job when stopping a CoroutineWorker.
            // The `finally` block runs whether doWork() finishes normally OR is cancelled.
            if (isStopped) {
                Log.d(TAG, "Worker stopped/cancelled — releasing resources in finally block")
                // e.g. closeNetworkSocket(), cancelHttpCall(), releaseResources()
            }
        }
    }

    /** Builds a [androidx.work.Data] object for failure output. */
    private fun buildFailureData(reason: String) = workDataOf(
        WorkerConstants.PROGRESS_MESSAGE to "Failed: $reason"
    )
}
