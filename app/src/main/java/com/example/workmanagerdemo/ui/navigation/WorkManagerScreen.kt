package com.example.workmanagerdemo.ui.navigation

/**
 * Sealed class representing all navigation destinations in the app.
 *
 * Each object holds a [route] string used by the NavHost to identify screens.
 * Adding a new screen = adding a new object here + a composable in [WorkManagerNavHost].
 *
 * Screen → Worker mapping:
 * ┌─────────────────────────────────┬────────────────────────────────────┐
 * │ Screen                          │ Worker(s) demonstrated              │
 * ├─────────────────────────────────┼────────────────────────────────────┤
 * │ Dashboard                       │ (navigation hub, no worker)         │
 * │ OneTimeWorkScreen               │ SyncLogsWorker                      │
 * │ ProgressWorkScreen              │ ImageCompressWorker                 │
 * │ PeriodicWorkScreen              │ DailyCleanupWorker                  │
 * │ ExpeditedWorkScreen             │ CriticalAlertWorker                 │
 * │ ChainedWorkScreen               │ Download + Filter + CombineUpload   │
 * │ UniqueWorkScreen                │ AnalyticsSyncWorker                 │
 * │ ForegroundWorkScreen            │ VideoExportWorker                   │
 * └─────────────────────────────────┴────────────────────────────────────┘
 */
sealed class WorkManagerScreen(val route: String, val title: String, val emoji: String) {

    data object Dashboard : WorkManagerScreen(
        route = "dashboard",
        title = "WorkManager Demo",
        emoji = "🚀"
    )

    data object OneTimeWork : WorkManagerScreen(
        route = "one_time_work",
        title = "One-Time Work & Constraints",
        emoji = "⚙️"
    )

    data object ProgressWork : WorkManagerScreen(
        route = "progress_work",
        title = "Input / Output & Progress",
        emoji = "📊"
    )

    data object PeriodicWork : WorkManagerScreen(
        route = "periodic_work",
        title = "Periodic Work",
        emoji = "🔁"
    )

    data object ExpeditedWork : WorkManagerScreen(
        route = "expedited_work",
        title = "Expedited Work",
        emoji = "⚡"
    )

    data object ChainedWork : WorkManagerScreen(
        route = "chained_work",
        title = "Work Chaining (Pipeline)",
        emoji = "🔗"
    )

    data object UniqueWork : WorkManagerScreen(
        route = "unique_work",
        title = "Unique Work & Policies",
        emoji = "🔒"
    )

    data object ForegroundWork : WorkManagerScreen(
        route = "foreground_work",
        title = "Long-Running Foreground Work",
        emoji = "🎬"
    )

    data object InterviewQuestions : WorkManagerScreen(
        route = "interview_questions",
        title = "Top 20 Interview Questions & Answers",
        emoji = "🎓"
    )
}
