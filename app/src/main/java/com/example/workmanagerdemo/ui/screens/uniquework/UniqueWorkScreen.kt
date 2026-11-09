package com.example.workmanagerdemo.ui.screens.uniquework

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Demo Screen: Unique Work & Conflict Policies (Sub-task 2.6 — AnalyticsSyncWorker).
 *
 * This screen will be fully implemented in Phase 2 (Sub-task 2.6) and Phase 4.
 * It will allow the user to:
 *   - Choose a conflict policy: KEEP, REPLACE, APPEND, or APPEND_OR_REPLACE.
 *   - Rapidly enqueue the same unique work name and observe different outcomes.
 *   - Demonstrate exponential backoff on retry with runAttemptCount tracking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniqueWorkScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔒 Unique Work & Policies") },
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
                Text(text = "🔒", style = MaterialTheme.typography.displayMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Unique Work & Conflict Policies", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Implementation coming in Phase 2 → Sub-task 2.6\nAnalyticsSyncWorker",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
