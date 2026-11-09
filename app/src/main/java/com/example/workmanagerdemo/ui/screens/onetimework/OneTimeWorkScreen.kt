package com.example.workmanagerdemo.ui.screens.onetimework

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
import androidx.work.WorkInfo
import com.example.workmanagerdemo.ui.theme.*

/**
 * Sub-task 2.1 — Full demo screen for One-Time Work & Constraints.
 *
 * This screen lets the user:
 *   1. Toggle three constraint switches (Network, Charging, Battery Not Low).
 *   2. Enqueue a SyncLogsWorker one-time work request.
 *   3. Watch a live status chip animate through ENQUEUED → RUNNING → SUCCEEDED.
 *   4. Cancel mid-execution and observe the CANCELLED state.
 *   5. Read the in-screen log panel showing every WorkInfo state change.
 *
 * ── Compose + ViewModel wiring ───────────────────────────────────────────────
 * hiltViewModel() retrieves a Hilt-injected [OneTimeWorkViewModel] scoped
 * to this NavBackStackEntry, so it survives recomposition but is cleared
 * when the user pops back to the Dashboard.
 *
 * collectAsStateWithLifecycle() collects the StateFlow safely — it pauses
 * collection when the app goes to the background (Lifecycle.State.STARTED)
 * to avoid unnecessary processing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneTimeWorkScreen(
    onNavigateBack: () -> Unit,
    viewModel: OneTimeWorkViewModel = hiltViewModel()
) {
    // Collect the entire UI state as a Compose State object.
    // Every time the ViewModel emits a new OneTimeWorkUiState, the
    // Composables that read `uiState` are automatically recomposed.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ One-Time Work & Constraints") },
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

            // ── 1. Concept explanation card ──────────────────────────────
            ConceptCard()

            // ── 2. Constraint toggles ────────────────────────────────────
            ConstraintCard(
                requireNetwork       = uiState.requireNetwork,
                requireCharging      = uiState.requireCharging,
                requireBatteryNotLow = uiState.requireBatteryNotLow,
                onNetworkToggle      = viewModel::onNetworkToggle,
                onChargingToggle     = viewModel::onChargingToggle,
                onBatteryToggle      = viewModel::onBatteryToggle,
                // Disable toggles while work is running/enqueued
                enabled = uiState.workInfo?.state.let {
                    it == null ||
                    it == WorkInfo.State.SUCCEEDED ||
                    it == WorkInfo.State.FAILED ||
                    it == WorkInfo.State.CANCELLED
                }
            )

            // ── 3. Live status chip ──────────────────────────────────────
            WorkStatusCard(workInfo = uiState.workInfo)

            // ── 4. Action buttons ────────────────────────────────────────
            ActionButtons(
                workState  = uiState.workInfo?.state,
                onEnqueue  = viewModel::enqueueWork,
                onCancel   = viewModel::cancelWork,
                onReset    = viewModel::resetState
            )

            // ── 5. Live log panel ────────────────────────────────────────
            if (uiState.logLines.isNotEmpty()) {
                LogPanel(lines = uiState.logLines)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Sub-composables ──────────────────────────────────────────────────────────

/**
 * Explains the WorkManager concepts demonstrated on this screen.
 * Cards like this make the demo self-documenting.
 */
@Composable
private fun ConceptCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📖 What this demo shows",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            ConceptItem("OneTimeWorkRequest", "Runs exactly once. Persists across process death.")
            ConceptItem("Constraints", "Work stays ENQUEUED until all constraints are satisfied.")
            ConceptItem("Tags", "TAG_SYNC_LOGS lets us cancel / observe a group of work.")
            ConceptItem("CoroutineWorker", "doWork() is a suspend fun — works with delay, Room, Retrofit.")
            ConceptItem("isStopped", "Checked between steps for cooperative cancellation.")
            ConceptItem("onStopped()", "Called by WorkManager when the Worker is cancelled.")
        }
    }
}

@Composable
private fun ConceptItem(title: String, description: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Card with three constraint toggle switches.
 *
 * Each switch maps to a constraint on the WorkRequest.
 * When the user enqueues work:
 *  • Network ON  → setRequiredNetworkType(CONNECTED)
 *  • Charging ON → setRequiresCharging(true)
 *  • Battery ON  → setRequiresBatteryNotLow(true)
 */
@Composable
private fun ConstraintCard(
    requireNetwork: Boolean,
    requireCharging: Boolean,
    requireBatteryNotLow: Boolean,
    onNetworkToggle: (Boolean) -> Unit,
    onChargingToggle: (Boolean) -> Unit,
    onBatteryToggle: (Boolean) -> Unit,
    enabled: Boolean
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🔧 WorkRequest Constraints",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Active constraints delay execution until all conditions are met.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            ConstraintToggleRow(
                icon        = Icons.Default.Wifi,
                label       = "Requires Network",
                sublabel    = "NetworkType.CONNECTED",
                checked     = requireNetwork,
                onToggle    = onNetworkToggle,
                enabled     = enabled
            )
            ConstraintToggleRow(
                icon        = Icons.Default.BatteryChargingFull,
                label       = "Requires Charging",
                sublabel    = "setRequiresCharging(true)",
                checked     = requireCharging,
                onToggle    = onChargingToggle,
                enabled     = enabled
            )
            ConstraintToggleRow(
                icon        = Icons.Default.BatteryStd,
                label       = "Battery Not Low",
                sublabel    = "setRequiresBatteryNotLow(true)",
                checked     = requireBatteryNotLow,
                onToggle    = onBatteryToggle,
                enabled     = enabled
            )
        }
    }
}

@Composable
private fun ConstraintToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sublabel: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(sublabel, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace)
        }
        Switch(
            checked  = checked,
            onCheckedChange = onToggle,
            enabled  = enabled
        )
    }
}

/**
 * Animated status card that shows the current WorkInfo state.
 *
 * WorkInfo.State values:
 *   ENQUEUED  — Stored in DB, waiting for constraints.
 *   RUNNING   — Actively executing in the Worker.
 *   SUCCEEDED — doWork() returned Result.success().
 *   FAILED    — doWork() returned Result.failure().
 *   CANCELLED — cancelWorkById() / cancelAllWorkByTag() was called.
 *   BLOCKED   — This request is waiting for a prerequisite in a chain.
 */
@Composable
private fun WorkStatusCard(workInfo: WorkInfo?) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📡 Live Work Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            if (workInfo == null) {
                Text(
                    text = "No work enqueued yet. Configure constraints above and tap Enqueue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // ── State chip ───────────────────────────────────────────
                val chipColor by animateColorAsState(
                    targetValue = workInfo.state.toColor(),
                    animationSpec = tween(durationMillis = 400),
                    label = "chipColor"
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedContent(
                        targetState = workInfo.state,
                        label = "stateChip"
                    ) { state ->
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = state.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = state.toIcon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = chipColor.copy(alpha = 0.15f),
                                labelColor     = chipColor,
                                leadingIconContentColor = chipColor
                            )
                        )
                    }

                    // Show a spinner while running
                    if (workInfo.state == WorkInfo.State.RUNNING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Metadata row ─────────────────────────────────────────
                WorkInfoMetaRow("Run attempts", workInfo.runAttemptCount.toString())
                WorkInfoMetaRow("Tags", workInfo.tags.joinToString(", "))
                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    val msg = workInfo.outputData
                        .getString(com.example.workmanagerdemo.WorkerConstants.PROGRESS_MESSAGE)
                    if (msg != null) WorkInfoMetaRow("Output", msg)
                }
                if (workInfo.state == WorkInfo.State.FAILED) {
                    val msg = workInfo.outputData
                        .getString(com.example.workmanagerdemo.WorkerConstants.PROGRESS_MESSAGE)
                    if (msg != null) WorkInfoMetaRow("Failure reason", msg)
                }
            }
        }
    }
}

@Composable
private fun WorkInfoMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

/**
 * Enqueue / Cancel / Reset action buttons.
 *
 * Button visibility rules:
 *   • "Enqueue" — always visible; label changes to "Re-Enqueue" post terminal state.
 *   • "Cancel"  — only visible while work is ENQUEUED or RUNNING.
 *   • "Reset"   — only visible in terminal states (SUCCEEDED / FAILED / CANCELLED).
 */
@Composable
private fun ActionButtons(
    workState: WorkInfo.State?,
    onEnqueue: () -> Unit,
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
        // Enqueue button
        Button(
            onClick  = onEnqueue,
            enabled  = !isActive,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (isTerminal) "Re-Enqueue" else "Enqueue Work")
        }

        // Cancel button (only shown when work is active)
        if (isActive) {
            OutlinedButton(
                onClick  = onCancel,
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Cancel")
            }
        }

        // Reset button (only shown after terminal state)
        if (isTerminal) {
            OutlinedButton(
                onClick  = onReset,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Reset")
            }
        }
    }
}

/**
 * Monospace log panel showing in-screen state transition events.
 *
 * Updated by [OneTimeWorkViewModel.appendLog] on every WorkInfo emission.
 * Uses a Box with verticalScroll so long logs remain readable without
 * pushing other UI off-screen.
 */
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
                    text = "Event Log",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
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

// ── Extension helpers ────────────────────────────────────────────────────────

/** Maps each WorkInfo.State to a semantic colour from our theme. */
private fun WorkInfo.State.toColor(): Color = when (this) {
    WorkInfo.State.ENQUEUED  -> StateEnqueued
    WorkInfo.State.RUNNING   -> StateRunning
    WorkInfo.State.SUCCEEDED -> StateSucceeded
    WorkInfo.State.FAILED    -> StateFailed
    WorkInfo.State.CANCELLED -> StateCancelled
    WorkInfo.State.BLOCKED   -> StateBlocked
}

/** Maps each WorkInfo.State to a Material icon. */
private fun WorkInfo.State.toIcon() = when (this) {
    WorkInfo.State.ENQUEUED  -> Icons.Default.HourglassEmpty
    WorkInfo.State.RUNNING   -> Icons.Default.PlayArrow
    WorkInfo.State.SUCCEEDED -> Icons.Default.CheckCircle
    WorkInfo.State.FAILED    -> Icons.Default.Error
    WorkInfo.State.CANCELLED -> Icons.Default.Cancel
    WorkInfo.State.BLOCKED   -> Icons.Default.Lock
}
