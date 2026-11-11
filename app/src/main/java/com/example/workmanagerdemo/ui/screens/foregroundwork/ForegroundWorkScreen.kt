package com.example.workmanagerdemo.ui.screens.foregroundwork

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
import androidx.work.WorkInfo
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.ui.theme.*

/**
 * Sub-task 2.7 — Demo Screen: Long-Running Foreground Work.
 *
 * Demonstrates:
 * 1. Bypassing the 10-minute background execution timeout using [androidx.work.CoroutineWorker.setForeground].
 * 2. Ongoing system notification with live progress bar.
 * 3. System-level cancellation via [androidx.work.WorkManager.createCancelPendingIntent].
 * 4. Cooperative cleanup in [androidx.work.ListenableWorker.onStopped].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForegroundWorkScreen(
    onNavigateBack: () -> Unit,
    viewModel: ForegroundWorkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWorkActive = uiState.workInfo?.state == WorkInfo.State.ENQUEUED ||
            uiState.workInfo?.state == WorkInfo.State.RUNNING

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎬 Long-Running Foreground Work") },
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
            ForegroundConceptCard()

            // ── 2. Video Configuration Card ───────────────────────────────
            VideoConfigCard(
                title = uiState.videoTitle,
                resolution = uiState.selectedResolution,
                availableResolutions = uiState.availableResolutions,
                format = uiState.selectedFormat,
                availableFormats = uiState.availableFormats,
                onTitleChange = viewModel::onTitleChange,
                onResolutionSelect = viewModel::onResolutionSelect,
                onFormatSelect = viewModel::onFormatSelect,
                enabled = !isWorkActive
            )

            // ── 3. Live Progress & Notification Simulator ─────────────────
            TranscodingProgressCard(
                workInfo = uiState.workInfo,
                progressPercent = uiState.progressPercent,
                stageMessage = uiState.progressMessage
            )

            // ── 4. Export Result Card ─────────────────────────────────────
            AnimatedVisibility(visible = uiState.workInfo?.state == WorkInfo.State.SUCCEEDED) {
                ExportResultCard(
                    uri = uiState.outputUri ?: "N/A",
                    duration = uiState.outputDuration ?: "N/A",
                    sizeBytes = uiState.outputSizeBytes ?: 0L,
                    resolution = uiState.selectedResolution
                )
            }

            // ── 5. Action Buttons ─────────────────────────────────────────
            ActionButtons(
                isWorkActive = isWorkActive,
                isCompleted = uiState.workInfo?.state == WorkInfo.State.SUCCEEDED ||
                        uiState.workInfo?.state == WorkInfo.State.FAILED ||
                        uiState.workInfo?.state == WorkInfo.State.CANCELLED,
                onStart = viewModel::startExport,
                onCancel = viewModel::cancelExport,
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
private fun ForegroundConceptCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🎬 Foreground Workers & 10-Minute Limits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            ConceptItem("No 10-Min Timeout", "Normal workers get killed after ~10 min. setForeground() lifts this restriction.")
            ConceptItem("SystemForegroundService", "WorkManager handles foreground service lifecycle & notification display.")
            ConceptItem("createCancelPendingIntent()", "Generates an OS PendingIntent attached to the notification 'Cancel' button.")
            ConceptItem("onStopped() Cleanup", "If user cancels, onStopped() deletes partial video fragments immediately.")
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

@Composable
private fun VideoConfigCard(
    title: String,
    resolution: String,
    availableResolutions: List<String>,
    format: String,
    availableFormats: List<String>,
    onTitleChange: (String) -> Unit,
    onResolutionSelect: (String) -> Unit,
    onFormatSelect: (String) -> Unit,
    enabled: Boolean
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🎞️ Video Transcoder Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Project Title") },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            Text("Resolution:", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availableResolutions.forEach { res ->
                    FilterChip(
                        selected = resolution == res,
                        onClick = { onResolutionSelect(res) },
                        label = { Text(res) },
                        enabled = enabled
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Container Format:", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availableFormats.forEach { fmt ->
                    FilterChip(
                        selected = format == fmt,
                        onClick = { onFormatSelect(fmt) },
                        label = { Text(fmt.uppercase()) },
                        enabled = enabled
                    )
                }
            }
        }
    }
}

@Composable
private fun TranscodingProgressCard(
    workInfo: WorkInfo?,
    progressPercent: Int,
    stageMessage: String
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 Live Transcoding Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            if (workInfo == null) {
                Text(
                    text = "No export active. Tap 'Start Video Export' to launch foreground processing.",
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
                        label = { Text("🛡️ FOREGROUND SERVICE", fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(Modifier.height(12.dp))

                val animatedProgress by animateFloatAsState(
                    targetValue = progressPercent / 100f,
                    animationSpec = tween(300),
                    label = "transcodeProgress"
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stageMessage.ifBlank { "Transcoding in progress…" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$progressPercent%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Notification drawer banner callout
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "A live progress notification with a 'Cancel' action is active in the Android system drawer.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportResultCard(
    uri: String,
    duration: String,
    sizeBytes: Long,
    resolution: String
) {
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
                Icon(Icons.Default.Movie, contentDescription = null, tint = StateSucceeded)
                Text(
                    text = "🎉 Video Transcode Complete!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            MetaRow("Output File", uri)
            MetaRow("Duration", duration)
            MetaRow("Resolution", resolution)
            MetaRow("File Size", "${sizeBytes / (1024 * 1024)} MB")
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
    isCompleted: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onStart,
            enabled = !isWorkActive,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (isCompleted) "Re-export Video" else "Start Video Export")
        }

        if (isWorkActive) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Cancel Export")
            }
        }

        if (isCompleted) {
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
                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Foreground Event Log", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
