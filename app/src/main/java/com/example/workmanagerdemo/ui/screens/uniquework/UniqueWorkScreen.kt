package com.example.workmanagerdemo.ui.screens.uniquework

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.ui.theme.*

/**
 * Sub-task 2.6 — Demo Screen: Unique Work & Conflict Policies.
 *
 * Demonstrates:
 * 1. [ExistingWorkPolicy] conflict resolution strategies: KEEP, REPLACE, APPEND, APPEND_OR_REPLACE.
 * 2. Rapid double-enqueue collision test to witness policy execution.
 * 3. Backoff Criteria on [androidx.work.ListenableWorker.Result.retry].
 * 4. Tracking [androidx.work.WorkInfo.getRunAttemptCount].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniqueWorkScreen(
    onNavigateBack: () -> Unit,
    viewModel: UniqueWorkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWorkActive = uiState.latestWorkInfo?.state == WorkInfo.State.ENQUEUED ||
            uiState.latestWorkInfo?.state == WorkInfo.State.RUNNING

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔒 Unique Work & Policies") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. Concept Card ───────────────────────────────────────────
            UniqueConceptCard()

            // ── 2. Policy & Backoff Configuration Card ────────────────────
            ConfigurationCard(
                selectedPolicy = uiState.selectedPolicy,
                selectedBackoff = uiState.selectedBackoffPolicy,
                simulateRetry = uiState.simulateRetry,
                onPolicySelect = viewModel::onPolicySelect,
                onBackoffSelect = viewModel::onBackoffPolicySelect,
                onSimulateRetryToggle = viewModel::onSimulateRetryToggle
            )

            // ── 3. Actions & Collision Tester Card ────────────────────────
            ActionsCard(
                isWorkActive = isWorkActive,
                onEnqueueSingle = viewModel::enqueueWork,
                onRapidDoubleEnqueue = viewModel::rapidFireEnqueueTest,
                onCancel = viewModel::cancelWork,
                onReset = viewModel::reset
            )

            // ── 4. Live Status & Queue Card ───────────────────────────────
            UniqueStatusCard(
                latestWorkInfo = uiState.latestWorkInfo,
                queueCount = uiState.activeWorkList.size,
                attemptCount = uiState.attemptCount
            )

            // ── 5. Success Card ───────────────────────────────────────────
            AnimatedVisibility(visible = uiState.latestWorkInfo?.state == WorkInfo.State.SUCCEEDED) {
                SyncResultCard(
                    batchId = uiState.syncedBatchId ?: "N/A",
                    eventsCount = uiState.eventsSynced,
                    attempts = uiState.attemptCount
                )
            }

            // ── 6. Log Console ────────────────────────────────────────────
            if (uiState.logLines.isNotEmpty()) {
                LogPanel(lines = uiState.logLines)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun UniqueConceptCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🔒 Unique Work Conflict Policies",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            ConceptItem("KEEP", "If work is running/enqueued, keeps existing and ignores the new request.")
            ConceptItem("REPLACE", "Cancels the active/queued work and enqueues the new request immediately.")
            ConceptItem("APPEND", "Chains the new request to execute after the active request completes.")
            ConceptItem("APPEND_OR_REPLACE", "Appends to active work, but if active work failed/cancelled, starts fresh.")
            ConceptItem("Exponential Backoff", "Delay doubles each retry: 10s, 20s, 40s (max 5 hours).")
        }
    }
}

@Composable
private fun ConceptItem(title: String, desc: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigurationCard(
    selectedPolicy: ExistingWorkPolicy,
    selectedBackoff: BackoffPolicy,
    simulateRetry: Boolean,
    onPolicySelect: (ExistingWorkPolicy) -> Unit,
    onBackoffSelect: (BackoffPolicy) -> Unit,
    onSimulateRetryToggle: (Boolean) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚙️ Conflict & Retry Setup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            // ExistingWorkPolicy selector
            Text("Select ExistingWorkPolicy:", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExistingWorkPolicy.entries.forEach { policy ->
                    FilterChip(
                        selected = selectedPolicy == policy,
                        onClick = { onPolicySelect(policy) },
                        label = { Text(policy.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // BackoffPolicy selector
            Text("Retry Backoff Policy (10s Initial Delay):", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BackoffPolicy.entries.forEach { backoff ->
                    FilterChip(
                        selected = selectedBackoff == backoff,
                        onClick = { onBackoffSelect(backoff) },
                        label = { Text(backoff.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Retry simulation switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Simulate Transient Failure (HTTP 503)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Forces worker to return Result.retry() to demonstrate backoff progression.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Switch(checked = simulateRetry, onCheckedChange = onSimulateRetryToggle)
            }
        }
    }
}

@Composable
private fun ActionsCard(
    isWorkActive: Boolean,
    onEnqueueSingle: () -> Unit,
    onRapidDoubleEnqueue: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🚀 Execution & Conflict Testing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onEnqueueSingle,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Enqueue Single")
                }

                // Rapid Collision Tester!
                Button(
                    onClick = onRapidDoubleEnqueue,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("⚡ Test Collision")
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isWorkActive) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Cancel Unique Work")
                    }
                }

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun UniqueStatusCard(
    latestWorkInfo: WorkInfo?,
    queueCount: Int,
    attemptCount: Int
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📡 WorkManager Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))

            if (latestWorkInfo == null) {
                Text(
                    text = "No unique analytics work scheduled yet. Choose a policy above and tap Enqueue or Test Collision.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val chipColor by animateColorAsState(
                    targetValue = latestWorkInfo.state.toStateColor(),
                    animationSpec = tween(300),
                    label = "chipColor"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedContent(targetState = latestWorkInfo.state, label = "stateBadge") { state ->
                        AssistChip(
                            onClick = {},
                            label = { Text(state.name, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(state.toStateIcon(), contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = chipColor.copy(alpha = 0.15f),
                                labelColor = chipColor,
                                leadingIconContentColor = chipColor
                            )
                        )
                    }

                    if (attemptCount > 0) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Attempt #$attemptCount", fontWeight = FontWeight.Bold) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                MetaRow("Task ID", latestWorkInfo.id.toString().take(13) + "…")
                MetaRow("Queue Chain Length", "$queueCount work requests")
                MetaRow("Run Attempt Count", "$attemptCount retries")
            }
        }
    }
}

@Composable
private fun SyncResultCard(batchId: String, eventsCount: Int, attempts: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StateSucceeded)
                Text("🎉 Analytics Sync Complete!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            MetaRow("Batch ID", batchId)
            MetaRow("Events Synced", "$eventsCount events")
            MetaRow("Resolved On Attempt", "#$attempts")
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LogPanel(lines: List<String>) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Unique Work Event Log", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    lines.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun WorkInfo.State.toStateColor(): Color = when (this) {
    WorkInfo.State.ENQUEUED -> StateEnqueued
    WorkInfo.State.RUNNING -> StateRunning
    WorkInfo.State.SUCCEEDED -> StateSucceeded
    WorkInfo.State.FAILED -> StateFailed
    WorkInfo.State.CANCELLED -> StateCancelled
    WorkInfo.State.BLOCKED -> StateBlocked
}

private fun WorkInfo.State.toStateIcon() = when (this) {
    WorkInfo.State.ENQUEUED -> Icons.Default.HourglassEmpty
    WorkInfo.State.RUNNING -> Icons.Default.PlayArrow
    WorkInfo.State.SUCCEEDED -> Icons.Default.CheckCircle
    WorkInfo.State.FAILED -> Icons.Default.Error
    WorkInfo.State.CANCELLED -> Icons.Default.Cancel
    WorkInfo.State.BLOCKED -> Icons.Default.Lock
}
