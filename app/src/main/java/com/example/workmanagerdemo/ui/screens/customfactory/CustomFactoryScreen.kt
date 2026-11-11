package com.example.workmanagerdemo.ui.screens.customfactory

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
 * Phase 5 ─ Custom WorkerFactory & Hilt DI Screen.
 *
 * Demonstrates:
 * 1. How WorkManager overcomes reflection limitations.
 * 2. Constructor dependency injection via [@HiltWorker] and [@AssistedInject].
 * 3. Manual initialization via [androidx.work.Configuration.Provider] in Application.
 * 4. Resolving multiple domain dependencies inside Workers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFactoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: CustomFactoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWorkActive = uiState.workInfo?.state == WorkInfo.State.ENQUEUED ||
            uiState.workInfo?.state == WorkInfo.State.RUNNING

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💉 WorkerFactory & Hilt DI") },
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
            FactoryConceptCard()

            // ── 2. Injected Dependencies Inspector ────────────────────────
            InjectedDependenciesCard(
                username = uiState.userProfile?.username ?: "N/A",
                email = uiState.userProfile?.email ?: "N/A",
                userId = uiState.userProfile?.userId ?: "N/A"
            )

            // ── 3. Live Execution Status Card ─────────────────────────────
            ExecutionStatusCard(
                workInfo = uiState.workInfo,
                progressPercent = uiState.progressPercent,
                stageMessage = uiState.progressMessage
            )

            // ── 4. Success Result Card ────────────────────────────────────
            AnimatedVisibility(visible = uiState.workInfo?.state == WorkInfo.State.SUCCEEDED) {
                SyncSuccessCard(
                    userId = uiState.syncedUserId ?: "N/A",
                    recordsUpdated = uiState.recordsUpdated
                )
            }

            // ── 5. Action Buttons ─────────────────────────────────────────
            ActionButtons(
                isWorkActive = isWorkActive,
                isCompleted = uiState.workInfo?.state == WorkInfo.State.SUCCEEDED ||
                        uiState.workInfo?.state == WorkInfo.State.FAILED ||
                        uiState.workInfo?.state == WorkInfo.State.CANCELLED,
                onStart = viewModel::startSync,
                onCancel = viewModel::cancelSync,
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
private fun FactoryConceptCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "💉 The WorkerFactory & DI Problem",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            ConceptItem("Reflection Limit", "Default WorkManager calls Worker(Context, WorkerParameters). It cannot inject Repositories!")
            ConceptItem("@HiltWorker + @AssistedInject", "Hilt splits the constructor: Context & WorkerParams from WorkManager; Repositories from Dagger graph.")
            ConceptItem("Configuration.Provider", "Implemented in Application class to supply HiltWorkerFactory to WorkManager.")
            ConceptItem("Manifest Initialization Removed", "tools:node=\"remove\" on WorkManagerInitializer disables default auto-init.")
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
private fun InjectedDependenciesCard(username: String, email: String, userId: String) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Constructor Dependencies Injected by Hilt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("1. UserRepository (Domain Service)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("• Target User ID: $userId", style = MaterialTheme.typography.bodySmall)
                    Text("• Username: $username ($email)", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("2. WorkNotificationHelper (App Service)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("• Posts system notifications upon sync completion", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ExecutionStatusCard(
    workInfo: WorkInfo?,
    progressPercent: Int,
    stageMessage: String
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📡 Hilt Worker Execution Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))

            if (workInfo == null) {
                Text(
                    text = "Tap 'Run Injected Worker' to verify Hilt constructor injection in real time.",
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
                        label = { Text("HiltWorkerFactory", fontWeight = FontWeight.Bold) },
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
                    label = "factoryProgress"
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
                    text = stageMessage.ifBlank { "Processing…" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SyncSuccessCard(userId: String, recordsUpdated: Int) {
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
                Text(
                    text = "🎉 Constructor DI Succeeded!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Synced User ID: $userId", style = MaterialTheme.typography.bodySmall)
            Text("Records Updated: $recordsUpdated accounts", style = MaterialTheme.typography.bodySmall)
            Text("Notification: Posted via Injected WorkNotificationHelper", style = MaterialTheme.typography.bodySmall)
        }
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
            Text(if (isCompleted) "Re-run Injected Worker" else "Run Injected Worker")
        }

        if (isWorkActive) {
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
                Text("DI Execution Log", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
