package com.example.workmanagerdemo

import android.content.Context
import androidx.work.WorkerParameters
import com.example.workmanagerdemo.data.UserProfile
import com.example.workmanagerdemo.data.UserRepository
import com.example.workmanagerdemo.data.WorkNotificationHelper
import com.example.workmanagerdemo.workers.UserRepositorySyncWorker
import com.example.workmanagerdemo.workers.factory.CustomAppWorkerFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.UUID

/**
 * Phase 6 ─ Unit Tests for Custom WorkerFactory & Dependency Injection
 * ====================================================================
 * Demonstrates testing [androidx.work.WorkerFactory] logic in isolation on the JVM
 * without needing an Android emulator.
 */
class CustomWorkerFactoryTest {

    private lateinit var mockUserRepository: UserRepository
    private lateinit var mockNotificationHelper: WorkNotificationHelper
    private lateinit var factory: CustomAppWorkerFactory

    private lateinit var mockContext: Context
    private lateinit var mockWorkerParameters: WorkerParameters

    @Before
    fun setUp() {
        mockUserRepository = object : UserRepository {
            override fun getUserProfile(): UserProfile = UserProfile(
                userId = "TEST_USER_001",
                username = "unit_tester",
                email = "test@internal.cloud",
                unreadNotifications = 0,
                lastSyncEpoch = 1731300000L
            )

            override suspend fun syncRemoteProfile(userId: String) =
                com.example.workmanagerdemo.data.SyncResult(true, 5, 120L)

            override fun getLocalCacheCount(): Int = 10
        }

        mockNotificationHelper = mock(WorkNotificationHelper::class.java)
        mockContext = mock(Context::class.java)
        mockWorkerParameters = mock(WorkerParameters::class.java)

        factory = CustomAppWorkerFactory(
            userRepository = mockUserRepository,
            notificationHelper = mockNotificationHelper
        )
    }

    /**
     * Verifies that when WorkManager requests [UserRepositorySyncWorker],
     * the custom factory creates it and injects the domain dependencies.
     */
    @Test
    fun createWorker_whenTargetIsUserRepositorySyncWorker_returnsInstantiatedWorker() {
        val worker = factory.createWorker(
            appContext = mockContext,
            workerClassName = UserRepositorySyncWorker::class.java.name,
            workerParameters = mockWorkerParameters
        )

        assertNotNull("Factory must instantiate UserRepositorySyncWorker", worker)
        assertTrue(
            "Worker must be an instance of UserRepositorySyncWorker",
            worker is UserRepositorySyncWorker
        )
    }

    /**
     * Verifies that when an unknown worker is requested, the factory returns null
     * so WorkManager falls back to the default reflection factory.
     */
    @Test
    fun createWorker_whenTargetIsUnknownWorker_returnsNull() {
        val worker = factory.createWorker(
            appContext = mockContext,
            workerClassName = "com.example.nonexistent.UnknownWorker",
            workerParameters = mockWorkerParameters
        )

        assertNull("Factory must return null for unhandled worker classes to allow fallback", worker)
    }

    @Test
    fun userRepository_returnsValidMockData() {
        val profile = mockUserRepository.getUserProfile()
        assertEquals("TEST_USER_001", profile.userId)
        assertEquals("unit_tester", profile.username)
        assertEquals(10, mockUserRepository.getLocalCacheCount())
    }
}
