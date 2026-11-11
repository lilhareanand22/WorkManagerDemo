package com.example.workmanagerdemo.data

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.workmanagerdemo.WorkManagerDemoApp
import com.example.workmanagerdemo.WorkerConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dedicated helper service for posting and updating work status notifications.
 *
 * Injected as a secondary dependency into [com.example.workmanagerdemo.workers.UserRepositorySyncWorker]
 * to demonstrate multi-dependency injection into Workers via Hilt.
 */
@Singleton
class WorkNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun postSyncNotification(title: String, message: String) {
        val notification: Notification = NotificationCompat.Builder(
            context,
            WorkManagerDemoApp.CHANNEL_ID_GENERAL_WORK
        )
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(WorkerConstants.NOTIFICATION_ID_GENERAL_WORK, notification)
    }

    fun dismissNotification() {
        notificationManager.cancel(WorkerConstants.NOTIFICATION_ID_GENERAL_WORK)
    }
}
