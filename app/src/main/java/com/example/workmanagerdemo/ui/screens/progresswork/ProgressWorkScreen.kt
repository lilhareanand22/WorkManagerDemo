package com.example.workmanagerdemo.ui.screens.progresswork

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
import com.example.workmanagerdemo.ui.theme.*

/**
 * Sub-task 2.2 — Demo Screen: Input / Output Data & Live Progress.
 *
 * Demonstrates:
 * 1. Passing input parameters via [androidx.work.WorkRequest.Builder.setInputData].
 * 2. Real-time progress updates via [androidx.work.CoroutineWorker.setProgress].
 * 3. Reactive UI collection via [androidx.work.WorkInfo.getProgress].
 * 4. Receiving and displaying final output data from [androidx.work.WorkInfo.getOutputData].
 * 5. Architectural compliance with the 10KB Data payload limit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressWorkScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProgressWorkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWorkActive = uiState.workInfo?.state == WorkInfo.State.ENQUEUED ||
            uiState.workInfo?.state == WorkInfo.State.RUNNING

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Input/Output & Progress") },
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
            ConceptCard()

            // ── 2. Configuration & Input Data Card ────────────────────────
            InputConfigurationCard(
                selectedImage = uiState.selectedImageName,
                availableImages = uiState.availableImages,
                quality = uiState.selectedQuality,
                onImageSelect = viewModel::onImageSelect,
                onQualityChange = viewModel::onQualityChange,
                enabled = !isWorkActive
            )

            // ── 3. Live Progress & Status Card ────────────────────────────
            ProgressStatusCard(
                workInfo = uiState.workInfo,
                progressPercent = uiState.progressPercent,
                progressMessage = uiState.progressMessage
            )

            // ── 4. Output Results Card (Visible upon completion) ──────────
            AnimatedVisibility(visible = uiState.workInfo?.state == WorkInfo.State.SUCCEEDED) {
                OutputResultCard(
                    outputUri = uiState.outputUri ?: "N/A",
                    originalSize = uiState.originalSizeBytes ?: 0L,
                    compressedSize = uiState.outputSizeBytes ?: 0L,
                    compressionRatio = uiState.compressionRatio ?: "N/A"
                )
            }

            // ── 5. Action Buttons ─────────────────────────────────────────
            ActionButtons(
                workState = uiState.workInfo?.state,
                onStart = viewModel::startCompression,
                onCancel = viewModel::cancelCompression,
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
private fun ConceptCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "💡 Key Concepts Demonstrated",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            ConceptItem("Input Data", "Passed to WorkRequest via setInputData(workDataOf(...)). Read via inputData.")
            ConceptItem("setProgress()", "Writes ephemeral progress to Room. Observed reactively via workInfo.progress.")
            ConceptItem("Output Data", "Returned on completion via Result.success(workDataOf(...)). Stored in Room.")
            ConceptItem("10KB Limit Pattern", "Never store bitmaps in Data! Save to cacheDir and pass back only file paths.")
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
private fun InputConfigurationCard(
    selectedImage: String,
    availableImages: List<String>,
    quality: Int,
    onImageSelect: (String) -> Unit,
    onQualityChange: (Int) -> Unit,
    enabled: Boolean
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📥 Input Parameters (Data Contract)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "These values are packaged into androidx.work.Data and passed to the Worker.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            // Image selection chips
            Text("Select Source Image:", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableImages.forEach { name ->
                    val isSelected = name == selectedImage
                    FilterChip(
                        selected = isSelected,
                        onClick = { onImageSelect(name) },
                        label = { Text(name.substringBefore("_"), style = MaterialTheme.typography.labelSmall) },
                        enabled = enabled,
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Compression quality slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Compression Quality:", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "$quality%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = quality.toFloat(),
                onValueChange = { onQualityChange(it.toInt()) },
                valueRange = 10f..100f,
                steps = 8,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun ProgressStatusCard(
    workInfo: WorkInfo?,
    progressPercent: Int,
    progressMessage: String
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📈 Live Execution & Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            if (workInfo == null) {
                Text(
                    text = "No active compression task. Configure parameters above and tap Start Compression.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // State Badge
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
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Smooth animated progress bar
                val animatedProgress by animateFloatAsState(
                    targetValue = progressPercent / 100f,
                    animationSpec = tween(durationMillis = 350),
                    label = "linearProgress"
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )

                Spacer(Modifier.height(8.dp))

                // Current stage description from setProgress()
                Text(
                    text = progressMessage.ifBlank {
                        if (workInfo.state == WorkInfo.State.ENQUEUED) "Queued in WorkDatabase…"
                        else "Processing…"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OutputResultCard(
    outputUri: String,
    originalSize: Long,
    compressedSize: Long,
    compressionRatio: String
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
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StateSucceeded
                )
                Text(
                    text = "🎉 Output Data (Result.success)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            ResultRow("Output File Path", outputUri)
            ResultRow("Original Size", "${originalSize / 1024} KB")
            ResultRow("Compressed Size", "${compressedSize / 1024} KB")
            ResultRow("Saved", compressionRatio)
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun ActionButtons(
    workState: WorkInfo.State?,
    onStart: () -> Unit,
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
            onClick = onStart,
            enabled = !isActive,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Compress, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (isTerminal) "Re-run Compression" else "Start Compression")
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
                Text(
                    text = "Reactive Event Log",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
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
