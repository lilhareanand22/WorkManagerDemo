package com.example.workmanagerdemo.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workmanagerdemo.ui.navigation.WorkManagerScreen

/**
 * Main dashboard screen — the entry point of the app.
 *
 * Displays a scrollable list of demo cards, one per WorkManager use case.
 * Tapping a card navigates to the corresponding demo screen.
 *
 * This screen has no ViewModel or WorkManager interaction — it is purely
 * a navigation hub. All WorkManager logic lives in the individual demo screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateTo: (WorkManagerScreen) -> Unit
) {
    // All demo screens in display order
    val demoScreens = listOf(
        WorkManagerScreen.OneTimeWork,
        WorkManagerScreen.ProgressWork,
        WorkManagerScreen.PeriodicWork,
        WorkManagerScreen.ExpeditedWork,
        WorkManagerScreen.ChainedWork,
        WorkManagerScreen.UniqueWork,
        WorkManagerScreen.ForegroundWork
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🚀 WorkManager Demo",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Tap a card to explore a use case",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // ── Info banner ────────────────────────────────────────────
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "WorkManager guarantees background task execution " +
                                "even after process death or device reboot. Each card " +
                                "below demonstrates a core concept.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            items(demoScreens) { screen ->
                DemoCard(screen = screen, onClick = { onNavigateTo(screen) })
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * Individual demo use-case card shown on the Dashboard.
 *
 * Displays the screen's emoji icon, title, and a short description
 * derived from the screen's route for quick reference.
 */
@Composable
private fun DemoCard(
    screen: WorkManagerScreen,
    onClick: () -> Unit
) {
    val descriptions = mapOf(
        WorkManagerScreen.OneTimeWork.route    to "Run a one-time background task with network, battery & charging constraints.",
        WorkManagerScreen.ProgressWork.route   to "Pass input data to a worker, track live progress, and read output results.",
        WorkManagerScreen.PeriodicWork.route   to "Schedule repeating work with the 15-minute minimum interval and flex window.",
        WorkManagerScreen.ExpeditedWork.route  to "Execute high-priority tasks immediately, bypassing OS deferral (Android 12+).",
        WorkManagerScreen.ChainedWork.route    to "Build sequential and parallel work pipelines with InputMerger.",
        WorkManagerScreen.UniqueWork.route     to "Prevent duplicate tasks with KEEP, REPLACE, APPEND conflict policies.",
        WorkManagerScreen.ForegroundWork.route to "Run long (>10 min) tasks with a foreground service notification."
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Emoji icon badge
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = screen.emoji,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = screen.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = descriptions[screen.route] ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open ${screen.title}",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
