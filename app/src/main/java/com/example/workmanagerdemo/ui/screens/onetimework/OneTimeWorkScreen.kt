package com.example.workmanagerdemo.ui.screens.onetimework

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Demo Screen: One-Time Work & Constraints (Sub-task 2.1 — SyncLogsWorker).
 *
 * This screen will be fully implemented in Phase 2 (Sub-task 2.1) and Phase 4.
 * It will allow the user to:
 *   - Toggle constraints: NetworkType, Requires Charging, Battery Not Low.
 *   - Enqueue a [SyncLogsWorker] one-time work request.
 *   - Observe live WorkInfo state transitions: ENQUEUED → RUNNING → SUCCEEDED / FAILED.
 *   - Cancel the work and observe the CANCELLED state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneTimeWorkScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ One-Time Work & Constraints") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⚙️",
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "One-Time Work & Constraints",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Implementation coming in Phase 2 → Sub-task 2.1\nSyncLogsWorker",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
