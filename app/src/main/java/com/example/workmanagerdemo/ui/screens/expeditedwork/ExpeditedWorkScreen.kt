package com.example.workmanagerdemo.ui.screens.expeditedwork

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Demo Screen: Expedited Work (Sub-task 2.4 — CriticalAlertWorker).
 *
 * This screen will be fully implemented in Phase 2 (Sub-task 2.4) and Phase 4.
 * It will allow the user to:
 *   - Trigger an expedited one-time work request that bypasses OS deferral.
 *   - Demonstrate the OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST fallback.
 *   - Show getForegroundInfo() in action on API < 31 devices.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpeditedWorkScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚡ Expedited Work") },
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
                Text(text = "⚡", style = MaterialTheme.typography.displayMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Expedited Work", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Implementation coming in Phase 2 → Sub-task 2.4\nCriticalAlertWorker",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
