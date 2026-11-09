package com.example.workmanagerdemo.ui.screens.periodicwork

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Demo Screen: Periodic Work (Sub-task 2.3 — DailyCleanupWorker).
 *
 * This screen will be fully implemented in Phase 2 (Sub-task 2.3) and Phase 4.
 * It will allow the user to:
 *   - Enqueue a PeriodicWorkRequest with a 15-minute repeat interval.
 *   - Visualise the flex window concept (e.g., execute between minute 10–15).
 *   - Observe next scheduled execution info and work state.
 *   - Cancel the periodic work and restart it with enqueueUniquePeriodicWork.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodicWorkScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔁 Periodic Work") },
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
                Text(text = "🔁", style = MaterialTheme.typography.displayMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Periodic Work", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Implementation coming in Phase 2 → Sub-task 2.3\nDailyCleanupWorker",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
