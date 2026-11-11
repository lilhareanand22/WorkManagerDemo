package com.example.workmanagerdemo.ui.screens.expeditedwork

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import com.example.workmanagerdemo.ui.theme.*

/**
 * Sub-task 2.4 — Demo Screen: Expedited Work & Out-of-Quota Policies.
 *
 * Demonstrates:
 * 1. Immediate task dispatch via [androidx.work.WorkRequest.Builder.setExpedited].
 * 2. Android 12+ (API 31+) background execution policy compliance.
 * 3. Mandatory `getForegroundInfo()` fallback for Android 11 and lower.
 * 4. Comparing [OutOfQuotaPolicy] options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpeditedWorkScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExpeditedWorkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWorkActive = uiState.workInfo?.state == WorkInfo.State.ENQUEUED ||
            uiState.workInfo?.state == WorkInfo.State.RUNNING

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚡ Expedited Work") },
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
            ExpeditedConceptCard()

            // ── 2. Configuration & Alert Payload Card ─────────────────────
            AlertConfigurationCard(
                title = uiState.alertTitle,
                message = uiState.alertMessage,
                severity = uiState.selectedSeverity,
                availableSeverities = uiState.availableSeverities,
                selectedPolicy = uiState.selectedPolicy,
                onTitleChange = viewModel::onTitleChange,
                onMessageChange = viewModel::onMessageChange,
                onSeveritySelect = viewModel::onSeveritySelect,
                onPolicySelect = viewModel::onPolicySelect,
                enabled = !isWorkActive
            )

            // ── 3. Live Expedited Execution Status ────────────────────────
            ExpeditedStatusCard(
                workInfo = uiState.workInfo,
                progressPercent = uiState.progressPercent,
                progressMessage = uiState.progressMessage
            )

            // ── 4. Dispatch Metrics Card ──────────────────────────────────
            AnimatedVisibility(visible = uiState.workInfo?.state == WorkInfo.State.SUCCEEDED) {
                DispatchMetricsCard(
                    latency = uiState.latencyMs ?: 0L,
                    timestamp = uiState.dispatchTimestamp ?: "N/A",
                    severity = uiState.selectedSeverity
                )
            }

            // ── 5. Action Buttons ─────────────────────────────────────────
            ActionButtons(
                workState = uiState.workInfo?.state,
                onDispatch = viewModel::dispatchAlert,
                onCancel = viewModel::cancelAlert,
                onReset = viewModel::reset
            )

            // ── 6. Log Console ────────────────────────────────────────────
            if (uiState.logLines.isNotEmpty()) {
                LogPanel(lines = uiState.logLines)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ExpeditedConceptCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚡ What is Expedited Work?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            ConceptItem("Immediate Execution", "Runs immediately, bypassing the OS's normal power-saving batching delays.")
            ConceptItem("Android 12+ FGS Alternative", "Replaces background foreground services which are blocked starting in API 31.")
            ConceptItem("Mandatory getForegroundInfo()", "On Android 11 and lower, WorkManager runs a Foreground Service. Omitting this crashes on older APIs!")
            ConceptItem("OutOfQuotaPolicy", "Handles cases where App Standby Bucket execution quota is depleted.")
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
private fun AlertConfigurationCard(
    title: String,
    message: String,
    severity: String,
    availableSeverities: List<String>,
    selectedPolicy: OutOfQuotaPolicy,
    onTitleChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSeveritySelect: (String) -> Unit,
    onPolicySelect: (OutOfQuotaPolicy) -> Unit,
    enabled: Boolean
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🚨 Critical Alert Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Alert Title") },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                label = { Text("Detailed Description") },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                maxLines = 3
            )

            Spacer(Modifier.height(12.dp))

            Text("Severity Level:", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availableSeverities.forEach { level ->
                    FilterChip(
                        selected = severity == level,
                        onClick = { onSeveritySelect(level) },
                        label = { Text(level, style = MaterialTheme.typography.labelSmall) },
                        enabled = enabled
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("OutOfQuotaPolicy:", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutOfQuotaPolicy.entries.forEach { policy ->
                    FilterChip(
                        selected = selectedPolicy == policy,
                        onClick = { onPolicySelect(policy) },
                        label = { Text(policy.name, style = MaterialTheme.typography.labelSmall) },
                        enabled = enabled
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpeditedStatusCard(
    workInfo: WorkInfo?,
    progressPercent: Int,
    progressMessage: String
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📡 Execution Engine Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            if (workInfo == null) {
                Text(
                    text = "No expedited task dispatched. Tap 'Dispatch Expedited Alert' to trigger immediate execution.",
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

                    AssistChip(
                        onClick = {},
                        label = { Text("⚡ EXPEDITED", fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }

                Spacer(Modifier.height(12.dp))

                val animatedProgress by animateFloatAsState(
                    targetValue = progressPercent / 100f,
                    animationSpec = tween(300),
                    label = "expeditedProgress"
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = progressMessage.ifBlank { "Processing alert…" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DispatchMetricsCard(latency: Long, timestamp: String, severity: String) {
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
                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "⚡ Expedited Dispatch Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            MetricRow("Execution Latency", "$latency ms (Near Zero Deferral)")
            MetricRow("Dispatch Timestamp", timestamp)
            MetricRow("Priority Rating", severity)
            MetricRow("Background Mode", "Bypassed JobScheduler Batch Window")
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
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
    workState: WorkInfo.State?,
    onDispatch: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit
) {
    val isActive = workState == WorkInfo.State.ENQUEUED || workState == WorkInfo.State.RUNNING
    val isTerminal = workState == WorkInfo.State.SUCCEEDED ||
            workState == WorkInfo.State.FAILED ||
            workState == WorkInfo.State.CANCELLED

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onDispatch,
            enabled = !isActive,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.FlashOn, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (isTerminal) "Re-dispatch" else "Dispatch Alert")
        }

        if (isActive) {
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

        if (isTerminal) {
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
                Text("Expedited Event Log", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
