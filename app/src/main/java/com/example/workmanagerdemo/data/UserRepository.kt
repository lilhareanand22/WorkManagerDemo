package com.example.workmanagerdemo.data

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

data class UserProfile(
    val userId: String,
    val username: String,
    val email: String,
    val unreadNotifications: Int,
    val lastSyncEpoch: Long
)

data class SyncResult(
    val success: Boolean,
    val recordsUpdated: Int,
    val serverLatencyMs: Long
)

/**
 * Domain interface for user account and profile data operations.
 *
 * This dependency will be injected directly into [UserRepositorySyncWorker]
 * to demonstrate constructor injection via [androidx.hilt.work.HiltWorker].
 */
interface UserRepository {
    fun getUserProfile(): UserProfile
    suspend fun syncRemoteProfile(userId: String): SyncResult
    fun getLocalCacheCount(): Int
}

/**
 * Concrete implementation of [UserRepository].
 */
@Singleton
class UserRepositoryImpl @Inject constructor() : UserRepository {

    override fun getUserProfile(): UserProfile = UserProfile(
        userId = "USR_94821",
        username = "alex_developer",
        email = "alex.dev@internal.cloud",
        unreadNotifications = 4,
        lastSyncEpoch = System.currentTimeMillis() - 3600_000L
    )

    override suspend fun syncRemoteProfile(userId: String): SyncResult {
        // Simulate remote API call latency
        val start = System.currentTimeMillis()
        delay(900L)
        val latency = System.currentTimeMillis() - start

        return SyncResult(
            success = true,
            recordsUpdated = 12,
            serverLatencyMs = latency
        )
    }

    override fun getLocalCacheCount(): Int = 42
}
