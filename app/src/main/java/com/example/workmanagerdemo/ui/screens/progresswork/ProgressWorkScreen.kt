package com.example.workmanagerdemo.ui.screens.progresswork

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Demo Screen: Input / Output Data & Live Progress (Sub-task 2.2 — ImageCompressWorker).
 *
 * This screen will be fully implemented in Phase 2 (Sub-task 2.2) and Phase 4.
 * It will allow the user to:
 *   - Set a compression quality slider (10–100).
 *   - Enqueue [ImageCompressWorker] with an input URI and quality parameter.
 *   - Watch a live LinearProgressIndicator bound to setProgress() updates.
 *   - Display the final output: compressed file size and status from Result.success().
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressWorkScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Input / Output & Progress") },
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
                Text(text = "📊", style = MaterialTheme.typography.displayMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Input / Output & Live Progress", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Implementation coming in Phase 2 → Sub-task 2.2\nImageCompressWorker",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
