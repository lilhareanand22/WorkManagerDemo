package com.example.workmanagerdemo.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.data.UserRepository
import com.example.workmanagerdemo.data.WorkNotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

/**
 * Phase 5 ─ UserRepositorySyncWorker
 * ============================================================
 * Demonstrates **Constructor Dependency Injection** in WorkManager:
 *
 * 1. **The Reflection Problem**:
 *    By default, WorkManager's `DefaultWorkerFactory` uses reflection to call
 *    `Worker(Context, WorkerParameters)`. It has NO ability to inject
 *    business repositories, DAOs, or network clients.
 *
 * 2. **Hilt Assisted Injection**:
 *    Using [@HiltWorker] and [@AssistedInject], Dagger/Hilt generates an assisted
 *    factory. At runtime:
 *    - WorkManager supplies the [@Assisted] arguments: [context] and [workerParams].
 *    - Hilt supplies the domain dependencies: [userRepository] and [notificationHelper].
 *
 * 3. **Manual Initialization Setup**:
 *    Works because `WorkManagerDemoApp` implements [androidx.work.Configuration.Provider]
 *    and supplies `HiltWorkerFactory` to WorkManager.
 */
@HiltWorker
class UserRepositorySyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val userRepository: UserRepository,
    private val notificationHelper: WorkNotificationHelper
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "UserSyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "UserRepositorySyncWorker running — Injected dependencies resolved cleanly by Hilt!")

        return try {
            if (isStopped) return Result.failure()

            // ── STEP 1: Use Injected UserRepository ──────────────────────
            reportProgress(20, "Querying local user profile via injected UserRepository…")
            val profile = userRepository.getUserProfile()
            Log.d(TAG, "Injected repo returned profile for: ${profile.username} (${profile.userId})")
            delay(800L)

            // ── STEP 2: Sync with Remote Cloud API ───────────────────────
            if (isStopped) return Result.failure()
            reportProgress(50, "Syncing profile data with remote cloud endpoints…")
            val syncResult = userRepository.syncRemoteProfile(profile.userId)
            delay(1_000L)

            // ── STEP 3: Use Injected NotificationHelper ──────────────────
            if (isStopped) return Result.failure()
            reportProgress(85, "Posting sync confirmation via injected WorkNotificationHelper…")
            notificationHelper.postSyncNotification(
                title = "Profile Synced",
                message = "Updated ${syncResult.recordsUpdated} account records for ${profile.username}"
            )
            delay(500L)

            // ── SUCCESS: Return Synced Data ──────────────────────────────
            val outputData = workDataOf(
                WorkerConstants.KEY_SYNCED_USER_ID to profile.userId,
                WorkerConstants.KEY_RECORDS_UPDATED to syncResult.recordsUpdated,
                WorkerConstants.PROGRESS_MESSAGE to "Synced ${syncResult.recordsUpdated} records for ${profile.username} via Injected UserRepository"
            )

            Log.d(TAG, "UserRepositorySyncWorker completed successfully")
            Result.success(outputData)

        } catch (e: Exception) {
            Log.e(TAG, "UserRepositorySyncWorker failed: ${e.message}", e)
            Result.failure(
                workDataOf(WorkerConstants.PROGRESS_MESSAGE to "Sync failed: ${e.message}")
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
