package com.example.workmanagerdemo.ui.screens.periodicwork

import androidx.compose.animation.AnimatedContent
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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkInfo
import com.example.workmanagerdemo.ui.theme.*

/**
 * Sub-task 2.3 — Demo Screen: Periodic Work & Flex Windows.
 *
 * Demonstrates:
 * 1. The 15-Minute Minimum Repeat Interval rule.
 * 2. Flex Interval execution window concept.
 * 3. Comparing [ExistingPeriodicWorkPolicy] (UPDATE vs KEEP vs CANCEL_AND_REENQUEUE).
 * 4. The perpetual lifecycle of PeriodicWorkRequest: ENQUEUED ↔ RUNNING.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodicWorkScreen(
    onNavigateBack: () -> Unit,
    viewModel: PeriodicWorkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWorkActive = uiState.workInfo?.state == WorkInfo.State.ENQUEUED ||
            uiState.workInfo?.state == WorkInfo.State.RUNNING

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔁 Periodic Work") },
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
            PeriodicConceptCard()

            // ── 2. Visual Flex Window Diagram ─────────────────────────────
            FlexWindowVisualizer(
                repeatInterval = uiState.repeatIntervalMinutes,
                flexInterval = uiState.flexIntervalMinutes
            )

            // ── 3. Configuration Card ─────────────────────────────────────
            PeriodicConfigurationCard(
                repeatMinutes = uiState.repeatIntervalMinutes,
                flexMinutes = uiState.flexIntervalMinutes,
                requireCharging = uiState.requireCharging,
                requireBatteryNotLow = uiState.requireBatteryNotLow,
                selectedPolicy = uiState.selectedPolicy,
                onRepeatChange = viewModel::onRepeatIntervalChange,
                onFlexChange = viewModel::onFlexIntervalChange,
                onChargingToggle = viewModel::onChargingToggle,
                onBatteryToggle = viewModel::onBatteryToggle,
                onPolicySelect = viewModel::onPolicySelect
            )

            // ── 4. Live Periodic Status Card ──────────────────────────────
            PeriodicStatusCard(
                workInfo = uiState.workInfo,
                totalRuns = uiState.totalRunsCount
            )

            // ── 5. Cleanup Execution Stats ────────────────────────────────
            if (uiState.lastExecutionTimestamp != null) {
                CleanupStatsCard(
                    filesCount = uiState.filesDeletedCount,
                    bytes = uiState.bytesReclaimed,
                    timestamp = uiState.lastExecutionTimestamp ?: "N/A"
                )
            }

            // ── 6. Action Buttons ─────────────────────────────────────────
            ActionButtons(
                isWorkActive = isWorkActive,
                onSchedule = viewModel::schedulePeriodicWork,
                onCancel = viewModel::cancelPeriodicWork
            )

            // ── 7. Log Console ────────────────────────────────────────────
            if (uiState.logLines.isNotEmpty()) {
                LogPanel(lines = uiState.logLines)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PeriodicConceptCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "💡 Key Periodic Concepts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            ConceptItem("15-Min Minimum Interval", "OS limits repeat interval to >= 15 min. Smaller values are clamped automatically.")
            ConceptItem("Flex Window", "Defines when work can run at the end of a period (e.g., last 5 min of every 15 min).")
            ConceptItem("No Direct Chaining", "PeriodicWorkRequest cannot use .then(). Use OneTime chaining inside the worker if needed.")
            ConceptItem("Perpetual Lifecycle", "Transitions from ENQUEUED -> RUNNING -> ENQUEUED (reschedules itself indefinitely).")
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

/**
 * Visual timeline showing the difference between the idle interval
 * and the flex window where execution actually happens.
 */
@Composable
private fun FlexWindowVisualizer(repeatInterval: Long, flexInterval: Long) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⏱️ Flex Window Timeline Visualizer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "The OS batches task execution inside the green Flex Window to conserve battery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            val idleMinutes = repeatInterval - flexInterval

            // Timeline bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                // Idle section
                Box(
                    modifier = Modifier
                        .weight(idleMinutes.toFloat())
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Idle (${idleMinutes}m)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Flex window section
                Box(
                    modifier = Modifier
                        .weight(flexInterval.toFloat())
                        .fillMaxHeight()
                        .background(StateSucceeded.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Flex Window (${flexInterval}m)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StateSucceeded
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Minute 0", style = MaterialTheme.typography.labelSmall)
                Text("Minute $idleMinutes", style = MaterialTheme.typography.labelSmall)
                Text("Minute $repeatInterval", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodicConfigurationCard(
    repeatMinutes: Long,
    flexMinutes: Long,
    requireCharging: Boolean,
    requireBatteryNotLow: Boolean,
    selectedPolicy: ExistingPeriodicWorkPolicy,
    onRepeatChange: (Long) -> Unit,
    onFlexChange: (Long) -> Unit,
    onChargingToggle: (Boolean) -> Unit,
    onBatteryToggle: (Boolean) -> Unit,
    onPolicySelect: (ExistingPeriodicWorkPolicy) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚙️ Periodic Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            // Repeat interval selector
            Text("Repeat Interval (Minimum 15 min):", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15L, 30L, 60L).forEach { mins ->
                    FilterChip(
                        selected = repeatMinutes == mins,
                        onClick = { onRepeatChange(mins) },
                        label = { Text("${mins}m") }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Flex window selector
            Text("Flex Window (Minimum 5 min):", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5L, 10L, 15L).forEach { mins ->
                    FilterChip(
                        selected = flexMinutes == mins,
                        onClick = { onFlexChange(mins) },
                        label = { Text("${mins}m") }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Policy selection
            Text("ExistingPeriodicWorkPolicy:", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExistingPeriodicWorkPolicy.entries.forEach { policy ->
                    FilterChip(
                        selected = selectedPolicy == policy,
                        onClick = { onPolicySelect(policy) },
                        label = { Text(policy.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Constraints
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Requires Device Charging", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = requireCharging, onCheckedChange = onChargingToggle)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Battery Not Low (>20%)", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = requireBatteryNotLow, onCheckedChange = onBatteryToggle)
            }
        }
    }
}

@Composable
private fun PeriodicStatusCard(workInfo: WorkInfo?, totalRuns: Int) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📡 WorkManager State",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))

            if (workInfo == null) {
                Text(
                    text = "No periodic cleanup scheduled. Configure parameters above and tap Schedule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val chipColor by animateColorAsState(
                    targetValue = workInfo.state.toStateColor(),
                    animationSpec = tween(300),
                    label = "chipColor"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedContent(targetState = workInfo.state, label = "stateBadge") { state ->
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

                    if (workInfo.state == WorkInfo.State.RUNNING) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                MetaRow("Task ID", workInfo.id.toString().take(13) + "…")
                MetaRow("Observed Cycles", "$totalRuns runs")
                MetaRow("Next Action", if (workInfo.state == WorkInfo.State.ENQUEUED) "Waiting for flex window / constraints" else "Executing cleanup")
            }
        }
    }
}

@Composable
private fun CleanupStatsCard(filesCount: Int, bytes: Long, timestamp: String) {
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
                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = StateSucceeded)
                Text(
                    text = "🧹 Last Cleanup Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            MetaRow("Files Removed", "$filesCount temporary files")
            MetaRow("Storage Reclaimed", "${bytes / 1024} KB")
            MetaRow("Executed At", timestamp)
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
private fun ActionButtons(
    isWorkActive: Boolean,
    onSchedule: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onSchedule,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Schedule, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (isWorkActive) "Update Schedule" else "Schedule Work")
        }

        if (isWorkActive) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Cancel")
            }
        }
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
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Periodic Event Log", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
