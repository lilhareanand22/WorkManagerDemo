package com.example.workmanagerdemo.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.workers.UserRepositorySyncWorker
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository layer for the Custom WorkerFactory & Dependency Injection use case.
 */
@Singleton
class UserSyncRepository @Inject constructor(
    private val workManager: WorkManager
) {
    private var currentWorkId: UUID? = null

    /**
     * Enqueues a [UserRepositorySyncWorker] request.
     */
    fun enqueueUserSync(): UUID {
        val request = OneTimeWorkRequestBuilder<UserRepositorySyncWorker>()
            .addTag(WorkerConstants.TAG_USER_SYNC)
            .build()

        currentWorkId = request.id

        workManager.enqueueUniqueWork(
            WorkerConstants.UNIQUE_USER_SYNC,
            ExistingWorkPolicy.REPLACE,
            request
        )

        return request.id
    }

    fun observeUserSync(): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(WorkerConstants.UNIQUE_USER_SYNC)

    fun cancelUserSync() {
        workManager.cancelUniqueWork(WorkerConstants.UNIQUE_USER_SYNC)
    }

    fun getCurrentWorkId(): UUID? = currentWorkId
}
