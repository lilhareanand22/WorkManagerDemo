package com.example.workmanagerdemo.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module: provides app-scoped dependencies.
 *
 * [WorkManager.getInstance] returns the singleton WorkManager instance
 * that was initialized by our [WorkManagerDemoApp] using the custom
 * [Configuration] with [HiltWorkerFactory]. Injecting it here means
 * ViewModels and the Repository never need to hold a [Context] reference
 * — they simply receive a ready-to-use [WorkManager] object.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the app-scoped [WorkManager] singleton.
     *
     * Usage in ViewModel / Repository constructor:
     * ```kotlin
     * class WorkManagerRepository @Inject constructor(
     *     private val workManager: WorkManager
     * )
     * ```
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)
}
