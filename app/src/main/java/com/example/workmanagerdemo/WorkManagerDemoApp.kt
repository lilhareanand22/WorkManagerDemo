package com.example.workmanagerdemo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory

/**
 * Custom Application class for WorkManager Demo.
 *
 * Key responsibilities:
 *
 * 1. **Hilt Entry Point** (`@HiltAndroidApp`): Triggers Hilt's code generation
 *    and sets up the application-level Hilt component.
 *
 * 2. **WorkManager Manual Initialization** (implements [Configuration.Provider]):
 *    Since we removed `WorkManagerInitializer` from AndroidManifest.xml, we must
 *    provide the [Configuration] ourselves here. This allows us to inject a
 *    custom [HiltWorkerFactory] so that our Workers can receive dependencies
 *    (repositories, DAOs, etc.) via constructor injection.
 *
 * 3. **Notification Channels**: Android 8.0 (API 26+) requires notification channels
 *    to be created before posting any notification. We create all channels needed
 *    by our Workers upfront here in [onCreate].
 */
@HiltAndroidApp
class WorkManagerDemoApp : Application(), Configuration.Provider {

    /**
     * Hilt injects [HiltWorkerFactory] which knows how to construct every
     * `@HiltWorker`-annotated Worker with its required dependencies.
     */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    /**
     * Provides the [Configuration] used to initialize WorkManager manually.
     *
     * - [Configuration.Builder.setWorkerFactory]: Uses Hilt's worker factory so
     *   Workers receive injected dependencies (Phase 5).
     * - [Configuration.Builder.setMinimumLoggingLevel]: Enables verbose WorkManager
     *   logs in Debug builds to help trace scheduling decisions in Logcat.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    /**
     * Creates all notification channels used by this app's Workers.
     *
     * Channels must be created before any Worker calls [setForeground] or
     * the NotificationManager posts a notification. Creating an existing
     * channel is a no-op, so it is safe to call this repeatedly.
     *
     * Channel IDs are centralised in [WorkerConstants] to avoid string duplication.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            // ── Channel: Long-running foreground work (VideoExportWorker) ───────
            val foregroundChannel = NotificationChannel(
                CHANNEL_ID_FOREGROUND_WORK,
                "Background Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress for long-running background tasks " +
                        "(e.g., file export, video processing)."
            }

            // ── Channel: General work status notifications ────────────────────
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL_WORK,
                "Work Status",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about completed or failed background work."
            }

            notificationManager.createNotificationChannels(
                listOf(foregroundChannel, generalChannel)
            )
        }
    }

    companion object {
        /** Channel used by VideoExportWorker's foreground service notification. */
        const val CHANNEL_ID_FOREGROUND_WORK = "channel_foreground_work"

        /** Channel used for general work completion/failure status updates. */
        const val CHANNEL_ID_GENERAL_WORK = "channel_general_work"
    }
}
