package com.example.workmanagerdemo.workers.factory

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.workmanagerdemo.data.UserRepository
import com.example.workmanagerdemo.data.WorkNotificationHelper
import com.example.workmanagerdemo.workers.UserRepositorySyncWorker
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 5 ─ CustomAppWorkerFactory (Manual WorkerFactory Demonstration)
 * ====================================================================
 * Demonstrates how a custom [WorkerFactory] works under the hood:
 *
 * 1. **Mechanism**:
 *    WorkManager calls [createWorker] whenever a task is ready to execute,
 *    passing the class name as a string (from `WorkSpec.worker_class_name`).
 *
 * 2. **Factory Resolution**:
 *    - If `workerClassName` matches [UserRepositorySyncWorker], this factory
 *      manually invokes its constructor, supplying [userRepository] and [notificationHelper].
 *    - If no match, returning `null` tells WorkManager to fall back to the next
 *      registered factory or the default reflection-based factory.
 *
 * Note: While [androidx.hilt.work.HiltWorkerFactory] automates this process
 * via generated assisted factories, understanding [WorkerFactory] is a staple
 * senior Android interview topic.
 */
@Singleton
class CustomAppWorkerFactory @Inject constructor(
    private val userRepository: UserRepository,
    private val notificationHelper: WorkNotificationHelper
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            UserRepositorySyncWorker::class.java.name -> {
                UserRepositorySyncWorker(
                    context = appContext,
                    workerParams = workerParameters,
                    userRepository = userRepository,
                    notificationHelper = notificationHelper
                )
            }
            // Return null to delegate to the default factory
            else -> null
        }
    }
}
