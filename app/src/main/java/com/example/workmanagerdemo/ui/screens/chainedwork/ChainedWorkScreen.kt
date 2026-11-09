package com.example.workmanagerdemo.ui.screens.chainedwork

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Demo Screen: Work Chaining — Sequential & Parallel Pipelines (Sub-task 2.5).
 *
 * This screen will be fully implemented in Phase 2 (Sub-task 2.5) and Phase 4.
 * It will allow the user to:
 *   - Trigger a 3-stage pipeline: DownloadWorker + FilterWorker → CombineAndUploadWorker.
 *   - Visualise parallel branch execution and their merge point.
 *   - Demonstrate failure propagation: failing Download blocks CombineAndUpload.
 *   - Show ArrayCreatingInputMerger combining outputs from both parallel branches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChainedWorkScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔗 Work Chaining (Pipeline)") },
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
                Text(text = "🔗", style = MaterialTheme.typography.displayMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Work Chaining (Pipeline)", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Implementation coming in Phase 2 → Sub-task 2.5\nDownloadWorker + FilterWorker → CombineAndUploadWorker",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
