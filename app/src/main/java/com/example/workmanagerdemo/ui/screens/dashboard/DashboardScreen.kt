package com.example.workmanagerdemo.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.ui.navigation.WorkManagerScreen
import com.example.workmanagerdemo.ui.theme.StateRunning
import com.example.workmanagerdemo.ui.theme.StateSucceeded

/**
 * Enhanced Dashboard Screen (Phase 4).
 *
 * Features:
 * 1. Live system-wide active task monitor.
 * 2. Real-time background job count badge.
 * 3. Global "Cancel All Work" and "Prune DB" administration controls.
 * 4. Active state indicators on individual demo cards.
 * 5. Dedicated "Top 20 Interview Q&A" featured study hub.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateTo: (WorkManagerScreen) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val activeList by viewModel.activeWorkList.collectAsStateWithLifecycle()
    val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()

    val demoScreens = listOf(
        WorkManagerScreen.OneTimeWork,
        WorkManagerScreen.ProgressWork,
        WorkManagerScreen.PeriodicWork,
        WorkManagerScreen.ExpeditedWork,
        WorkManagerScreen.ChainedWork,
        WorkManagerScreen.UniqueWork,
        WorkManagerScreen.ForegroundWork
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🚀 WorkManager Demo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Jetpack Background Architecture",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. Live Active Work Monitor Banner ────────────────────────
            item {
                ActiveWorkMonitorBanner(
                    activeCount = activeCount,
                    onCancelAll = viewModel::cancelAllWork,
                    onPrune = viewModel::pruneDatabase
                )
            }

            // ── 2. Featured Card: Top 20 Interview Q&A ───────────────────
            item {
                FeaturedInterviewCard(
                    onClick = { onNavigateTo(WorkManagerScreen.InterviewQuestions) }
                )
            }

            // ── 3. Section Header ─────────────────────────────────────────
            item {
                Text(
                    text = "🛠️ Practical WorkManager Use Cases",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // ── 4. Use Case Cards ─────────────────────────────────────────
            items(demoScreens) { screen ->
                val matchingTag = screen.toWorkerTag()
                val isCardActive = activeList.any { workInfo ->
                    matchingTag != null && workInfo.tags.contains(matchingTag)
                }

                DemoCard(
                    screen = screen,
                    isActive = isCardActive,
                    onClick = { onNavigateTo(screen) }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

/**
 * System-wide live monitor banner showing active jobs count.
 */
@Composable
private fun ActiveWorkMonitorBanner(
    activeCount: Int,
    onCancelAll: () -> Unit,
    onPrune: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (activeCount > 0)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (activeCount > 0) Icons.Default.Sync else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (activeCount > 0) StateRunning else StateSucceeded
                    )
                    Text(
                        text = if (activeCount > 0) "$activeCount Active Background Jobs" else "All Background Jobs Idle",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (activeCount > 0) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = StateRunning
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (activeCount > 0)
                    "Tasks are actively executing or queued in WorkDatabase across schedulers."
                else "No active tasks in Room SQLite. Completed tasks can be pruned to save space.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (activeCount > 0) {
                    OutlinedButton(
                        onClick = onCancelAll,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel All Work", style = MaterialTheme.typography.labelSmall)
                    }
                }

                FilledTonalButton(
                    onClick = onPrune,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Prune Finished Tasks", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * Featured Card linking directly to the in-app Top 20 Interview Q&A screen.
 */
@Composable
private fun FeaturedInterviewCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🎓", style = MaterialTheme.typography.headlineSmall)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Top 20 Interview Q&A",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "NEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Searchable in-app technical guide covering internal architecture, Doze, quotas & failure policies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open Interview Guide",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Individual demo use-case card shown on the Dashboard.
 */
@Composable
private fun DemoCard(
    screen: WorkManagerScreen,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val descriptions = mapOf(
        WorkManagerScreen.OneTimeWork.route to "One-time execution with Network, Charging, and Battery constraints.",
        WorkManagerScreen.ProgressWork.route to "Data packaging, 10KB limit pattern, and live setProgress() reporting.",
        WorkManagerScreen.PeriodicWork.route to "15-minute minimum repeat interval rule, flex windows & self-rescheduling.",
        WorkManagerScreen.ExpeditedWork.route to "Immediate priority execution, OutOfQuotaPolicy, and API < 31 fallback.",
        WorkManagerScreen.ChainedWork.route to "Parallel DAG fork & join with OverwritingInputMerger & failure propagation.",
        WorkManagerScreen.UniqueWork.route to "Conflict policies (KEEP, REPLACE, APPEND) & exponential backoff retry math.",
        WorkManagerScreen.ForegroundWork.route to "Bypassing 10-minute timeout via setForeground() & notification cancel action."
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = screen.emoji, style = MaterialTheme.typography.headlineSmall)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Live active indicator chip
                    if (isActive) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StateRunning.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StateRunning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = descriptions[screen.route] ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open ${screen.title}",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun WorkManagerScreen.toWorkerTag(): String? = when (this) {
    WorkManagerScreen.OneTimeWork -> WorkerConstants.TAG_SYNC_LOGS
    WorkManagerScreen.ProgressWork -> WorkerConstants.TAG_IMAGE_COMPRESS
    WorkManagerScreen.PeriodicWork -> WorkerConstants.TAG_DAILY_CLEANUP
    WorkManagerScreen.ExpeditedWork -> WorkerConstants.TAG_CRITICAL_ALERT
    WorkManagerScreen.ChainedWork -> WorkerConstants.TAG_COMBINE_UPLOAD
    WorkManagerScreen.UniqueWork -> WorkerConstants.TAG_ANALYTICS_SYNC
    WorkManagerScreen.ForegroundWork -> WorkerConstants.TAG_VIDEO_EXPORT
    else -> null
}
