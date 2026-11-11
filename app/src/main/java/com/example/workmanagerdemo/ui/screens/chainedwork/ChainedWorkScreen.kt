package com.example.workmanagerdemo.ui.screens.chainedwork

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
import androidx.work.WorkInfo
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.ui.theme.*

/**
 * Sub-task 2.5 — Demo Screen: Work Chaining & Pipelines.
 *
 * Demonstrates:
 * 1. Parallel tasks joining into a dependent worker DAG.
 * 2. Merging input data with [androidx.work.OverwritingInputMerger].
 * 3. Failure propagation: Downstream workers become FAILED if an upstream task fails.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChainedWorkScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChainedWorkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isPipelineActive = listOf(
        uiState.downloadWorkInfo?.state,
        uiState.filterWorkInfo?.state,
        uiState.combineWorkInfo?.state
    ).any { it == WorkInfo.State.ENQUEUED || it == WorkInfo.State.RUNNING || it == WorkInfo.State.BLOCKED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔗 Work Chaining (Pipeline)") },
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
            PipelineConceptCard()

            // ── 2. Visual DAG Graph Visualizer ────────────────────────────
            PipelineGraphVisualizer(
                downloadInfo = uiState.downloadWorkInfo,
                filterInfo = uiState.filterWorkInfo,
                combineInfo = uiState.combineWorkInfo
            )

            // ── 3. Configuration & Failure Propagation Tester ────────────
            PipelineConfigurationCard(
                simulateFailure = uiState.simulateDownloadFailure,
                onToggleFailure = viewModel::onSimulateFailureToggle,
                enabled = !isPipelineActive
            )

            // ── 4. Upload Result Card ─────────────────────────────────────
            AnimatedVisibility(visible = uiState.combineWorkInfo?.state == WorkInfo.State.SUCCEEDED) {
                UploadResultCard(
                    url = uiState.uploadUrl ?: "N/A",
                    records = uiState.recordsProcessed
                )
            }

            // ── 5. Action Buttons ─────────────────────────────────────────
            ActionButtons(
                isPipelineActive = isPipelineActive,
                isCompleted = uiState.combineWorkInfo?.state == WorkInfo.State.SUCCEEDED ||
                        uiState.combineWorkInfo?.state == WorkInfo.State.FAILED ||
                        uiState.downloadWorkInfo?.state == WorkInfo.State.FAILED,
                onStart = viewModel::startPipeline,
                onCancel = viewModel::cancelPipeline,
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
private fun PipelineConceptCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🔗 Work Chaining & DAG Pipelines",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            ConceptItem("Parallel Start (Fork)", "beginWith(listOf(A, B)) executes Node A and Node B simultaneously.")
            ConceptItem("Sequential Join (.then)", "then(C) creates a dependency: Node C waits until both A and B succeed.")
            ConceptItem("InputMerger", "Combines output Data from Node A and Node B into Node C's input Data map.")
            ConceptItem("Failure Propagation", "If Node A fails, Node C is marked FAILED and will NEVER execute!")
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
 * Visual Directed Acyclic Graph (DAG) displaying the 3 pipeline nodes.
 */
@Composable
private fun PipelineGraphVisualizer(
    downloadInfo: WorkInfo?,
    filterInfo: WorkInfo?,
    combineInfo: WorkInfo?
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📊 Live Pipeline DAG Graph",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(14.dp))

            // Parallel Top Row: Node A and Node B
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NodeCard(
                    title = "Node A: Download",
                    subtitle = "Raw Dataset (CSV)",
                    workInfo = downloadInfo,
                    modifier = Modifier.weight(1f)
                )

                NodeCard(
                    title = "Node B: Filter",
                    subtitle = "Sanitize & Validate",
                    workInfo = filterInfo,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Joining Arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Icon(Icons.Default.SouthEast, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "InputMerger (Join)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
                Icon(Icons.Default.SouthWest, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(8.dp))

            // Dependent Bottom Row: Node C
            NodeCard(
                title = "Node C: Combine & Upload",
                subtitle = "Merges A + B -> Cloud Storage",
                workInfo = combineInfo,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun NodeCard(
    title: String,
    subtitle: String,
    workInfo: WorkInfo?,
    modifier: Modifier = Modifier
) {
    val state = workInfo?.state
    val chipColor by animateColorAsState(
        targetValue = state?.toStateColor() ?: StateEnqueued,
        animationSpec = tween(300),
        label = "nodeColor"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // State Badge
            AssistChip(
                onClick = {},
                label = { Text(state?.name ?: "IDLE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(
                        state?.toStateIcon() ?: Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = chipColor.copy(alpha = 0.15f),
                    labelColor = chipColor,
                    leadingIconContentColor = chipColor
                )
            )

            // Progress Message if running
            val msg = workInfo?.progress?.getString(WorkerConstants.PROGRESS_MESSAGE)
                ?: workInfo?.outputData?.getString(WorkerConstants.PROGRESS_MESSAGE)
            if (!msg.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun PipelineConfigurationCard(
    simulateFailure: Boolean,
    onToggleFailure: (Boolean) -> Unit,
    enabled: Boolean
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🧪 Failure Propagation Simulation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Simulate Download Failure", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Forces Node A to fail. Observe how downstream Node C transitions to FAILED without ever executing!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(10.dp))
                Switch(checked = simulateFailure, onCheckedChange = onToggleFailure, enabled = enabled)
            }
        }
    }
}

@Composable
private fun UploadResultCard(url: String, records: Int) {
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
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = StateSucceeded)
                Text(
                    text = "🎉 Pipeline Succeeded!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Uploaded Target: $url", style = MaterialTheme.typography.bodySmall)
            Text("Sanitized Records: $records entries", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ActionButtons(
    isPipelineActive: Boolean,
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
            enabled = !isPipelineActive,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.AccountTree, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (isCompleted) "Re-run Pipeline" else "Start Pipeline")
        }

        if (isPipelineActive) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Cancel")
            }
        }

        if (isCompleted) {
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
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
                Text("Pipeline Event Log", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
