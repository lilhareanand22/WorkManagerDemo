package com.example.workmanagerdemo.ui.screens.foregroundwork

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Demo Screen: Long-Running Foreground Work (Sub-task 2.7 — VideoExportWorker).
 *
 * This screen will be fully implemented in Phase 2 (Sub-task 2.7) and Phase 4.
 * It will allow the user to:
 *   - Start a ~30-second simulated video export task.
 *   - Observe an ongoing foreground service notification with progress.
 *   - Cancel the work and observe the notification being dismissed.
 *   - See cooperative cancellation clean-up happening in onStopped().
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForegroundWorkScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎬 Long-Running Foreground Work") },
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
                Text(text = "🎬", style = MaterialTheme.typography.displayMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Long-Running Foreground Work", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Implementation coming in Phase 2 → Sub-task 2.7\nVideoExportWorker",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
