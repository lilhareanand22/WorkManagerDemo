package com.example.workmanagerdemo.ui.screens.interview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class InterviewItem(
    val id: Int,
    val question: String,
    val answer: String,
    val category: String
)

/**
 * Top 20 WorkManager Interview Questions Screen.
 *
 * Provides an interactive in-app study browser with:
 * - Keyword search filter
 * - Category filter chips
 * - Expandable cards with detailed answers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewQuestionsScreen(
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var expandedQuestionId by remember { mutableStateOf<Int?>(1) } // Open Q1 by default

    val allQuestions = remember { getInterviewQuestionsList() }
    val categories = listOf("All", "Architecture", "Data & Progress", "Quotas & Execution", "Doze & Lifecycle")

    val filteredQuestions = remember(searchQuery, selectedCategory) {
        allQuestions.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    item.question.contains(searchQuery, ignoreCase = true) ||
                    item.answer.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎓 Top 20 Interview Q&A") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search questions (e.g. Doze, 10KB, Expedited…)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Category Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Showing ${filteredQuestions.size} of ${allQuestions.size} questions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Questions List
            items(filteredQuestions, key = { it.id }) { item ->
                val isExpanded = expandedQuestionId == item.id
                QuestionCard(
                    item = item,
                    isExpanded = isExpanded,
                    onToggle = {
                        expandedQuestionId = if (isExpanded) null else item.id
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun QuestionCard(
    item: InterviewItem,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "Q${item.id}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = item.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = item.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun getInterviewQuestionsList(): List<InterviewItem> = listOf(
    InterviewItem(
        id = 1,
        category = "Architecture",
        question = "What is WorkManager and when should you choose it over Coroutines, AlarmManager, or Foreground Services?",
        answer = "WorkManager is designed for guaranteed, deferrable, persistent background work that must execute even if the app process is killed or the device is rebooted. Coroutines are for in-memory, immediate UI-bound work. AlarmManager is strictly for exact time-sensitive alarms (clock/calendar). Foreground Services are for immediate, user-perceivable tasks (active music playback, navigation)."
    ),
    InterviewItem(
        id = 2,
        category = "Architecture",
        question = "How does WorkManager ensure persistence across process death and device reboots?",
        answer = "WorkManager uses an internal Room SQLite database (androidx.work.workdb) as its single source of truth. Every request is committed to disk before scheduling. On API 23+, jobs are registered with JobScheduler using JobInfo.setPersisted(true). When the OS reboots or constraints match, the OS awakens WorkManager's SystemJobService, which reads the pending job from SQLite and executes it."
    ),
    InterviewItem(
        id = 3,
        category = "Architecture",
        question = "What is the difference between Worker, CoroutineWorker, ListenableWorker, and RxWorker?",
        answer = "ListenableWorker is the base non-blocking class returning a ListenableFuture. Worker is a synchronous blocking implementation running on an Executor thread pool. CoroutineWorker is the recommended Kotlin class with a suspending doWork() running on Dispatchers.Default. RxWorker provides ReactiveX Single interop."
    ),
    InterviewItem(
        id = 4,
        category = "Doze & Lifecycle",
        question = "Why is the minimum periodic interval 15 minutes, and what is the flex interval?",
        answer = "Android enforces a 15-minute minimum (MIN_PERIODIC_INTERVAL_MILLIS) to prevent battery drain. Values < 15m are automatically clamped. The flex interval (min 5m) defines a window at the end of each period during which the OS can batch the work with other system alarms to conserve CPU wakeups."
    ),
    InterviewItem(
        id = 5,
        category = "Quotas & Execution",
        question = "What is Expedited Work and why was it introduced in Android 12?",
        answer = "Starting in Android 12 (API 31), apps cannot launch foreground services from the background. Expedited Work (setExpedited) was introduced to replace background FGS for short, high-priority tasks. It executes immediately without deferral, governed by App Standby Bucket quotas."
    ),
    InterviewItem(
        id = 6,
        category = "Data & Progress",
        question = "What is the 10KB Data limit in WorkManager and how do you handle larger payloads?",
        answer = "Data objects are serialized into the internal Room database and passed across Binder IPC. To prevent SQLite CursorWindow and 1MB global Binder transaction buffer overruns, WorkManager clamps Data payloads to 10KB. Large files, bitmaps, or lists must be saved to disk (cacheDir or Room DB), passing only the file URI/path in Data."
    ),
    InterviewItem(
        id = 7,
        category = "Architecture",
        question = "How does Work Chaining work and what happens when an upstream worker fails?",
        answer = "Work is chained as a Directed Acyclic Graph (DAG) using beginWith().then(). Dependent workers start in the BLOCKED state. If any upstream worker returns Result.failure(), all downstream dependent workers are immediately marked FAILED and will NEVER execute (Failure Propagation)."
    ),
    InterviewItem(
        id = 8,
        category = "Architecture",
        question = "Explain the 4 ExistingWorkPolicy options for Unique Work.",
        answer = "KEEP: If work exists, keep it and discard the new request. REPLACE: Cancel active work and run the new request immediately. APPEND: Chain the new work to run after the active work finishes. APPEND_OR_REPLACE: Appends to active work, but if active work failed/cancelled, starts freshly."
    ),
    InterviewItem(
        id = 9,
        category = "Architecture",
        question = "How does WorkManager handle Worker cancellation and cooperative cancellation?",
        answer = "WorkManager cancels the underlying coroutine Job. Suspending points (delay, Room queries) throw CancellationException. Developers must also periodically inspect isStopped during CPU loops. onStopped() is invoked as a cleanup hook for releasing native resources."
    ),
    InterviewItem(
        id = 10,
        category = "Architecture",
        question = "How do Backoff Criteria and Retry work in WorkManager?",
        answer = "When doWork() returns Result.retry(), WorkManager reschedules with BackoffPolicy.EXPONENTIAL (delay doubles: 10s, 20s, 40s) or LINEAR (delay increases linearly: 10s, 20s, 30s). Minimum backoff is 10s; maximum is 5 hours. Developers should inspect runAttemptCount to cap retries."
    ),
    InterviewItem(
        id = 11,
        category = "Architecture",
        question = "How do you implement Hilt Dependency Injection in WorkManager?",
        answer = "1. Remove WorkManagerInitializer from AndroidManifest.xml. 2. Implement Configuration.Provider in Application class and inject HiltWorkerFactory. 3. Annotate Worker with @HiltWorker and use @AssistedInject constructor(@Assisted context: Context, @Assisted params: WorkerParameters, val repo: MyRepo)."
    ),
    InterviewItem(
        id = 12,
        category = "Data & Progress",
        question = "How does an app observe WorkManager state in Jetpack Compose?",
        answer = "WorkManager exposes Flow observables: getWorkInfoByIdFlow(), getWorkInfosByTagFlow(), getWorkInfosForUniqueWorkFlow(). In Compose, collect it as state using collectAsStateWithLifecycle(), which automatically pauses collection when the app goes into the background."
    ),
    InterviewItem(
        id = 13,
        category = "Doze & Lifecycle",
        question = "How does WorkManager handle Doze mode and App Standby?",
        answer = "WorkManager respects Doze mode. Deferrable tasks are paused and batched into Doze maintenance windows. If network constraints are set, Doze blocks network access, causing WorkManager to pause the worker until the device exits Doze or enters a maintenance window."
    ),
    InterviewItem(
        id = 14,
        category = "Data & Progress",
        question = "How does WorkManager track real-time progress via setProgress() without polling?",
        answer = "Calling setProgress() inserts a row into the WorkProgress table in Room. Room's internal InvalidationTracker detects table mutation and triggers a reload on the Flow backing getWorkInfoByIdFlow(). Progress is transient and deleted once the worker terminates."
    ),
    InterviewItem(
        id = 15,
        category = "Architecture",
        question = "Why can't PeriodicWorkRequest be chained?",
        answer = "PeriodicWorkRequests represent infinite cycles (ENQUEUED <-> RUNNING) without a terminal SUCCEEDED state. Calling beginWith().then() with a periodic request throws an IllegalArgumentException. If periodic chaining is needed, run a Periodic worker that programmatically enqueues a OneTimeWork chain inside doWork()."
    ),
    InterviewItem(
        id = 16,
        category = "Architecture",
        question = "How do you test WorkManager synchronously in unit and instrumentation tests?",
        answer = "Use androidx.work:work-testing. Initialize WorkManagerTestInitHelper.initializeTestWorkManager(context, config), retrieve the TestDriver, enqueue the request, and synchronously trigger execution using testDriver.setAllConstraintsMet(id) or setPeriodDelayMet(id)."
    ),
    InterviewItem(
        id = 17,
        category = "Doze & Lifecycle",
        question = "What happens when an app is Force Stopped from Android Settings?",
        answer = "The Linux kernel marks the app package as FLAG_STOPPED. All scheduled JobScheduler jobs and alarms are cleared. No broadcasts (BOOT_COMPLETED) are delivered. WorkManager is completely disabled until the user explicitly taps the app icon to reopen it."
    ),
    InterviewItem(
        id = 18,
        category = "Data & Progress",
        question = "What is InputMerger and what types exist?",
        answer = "When parallel workers join into a downstream worker, InputMerger combines their output Data maps. OverwritingInputMerger (default) overwrites duplicate keys with the last completed value. ArrayCreatingInputMerger merges duplicate keys into typed arrays (e.g. Array<String>)."
    ),
    InterviewItem(
        id = 19,
        category = "Quotas & Execution",
        question = "How do you implement Long-Running workers exceeding the 10-minute limit?",
        answer = "Call setForeground(foregroundInfo) inside the CoroutineWorker. WorkManager delegates the task to internal SystemForegroundService and displays an ongoing notification. This removes the 10-minute background timeout."
    ),
    InterviewItem(
        id = 20,
        category = "Doze & Lifecycle",
        question = "What are the most common anti-patterns with WorkManager?",
        answer = "1. Using WorkManager for immediate UI data fetching (use Coroutines + viewModelScope instead). 2. Storing large payloads/bitmaps in Data (>10KB). 3. Expecting periodic work < 15 minutes. 4. Forgetting cooperative cancellation checks (isStopped). 5. Missing getForegroundInfo() fallback on expedited workers on API < 31."
    )
)
