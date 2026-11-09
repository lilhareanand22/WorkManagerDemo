package com.example.workmanagerdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.workmanagerdemo.ui.theme.WorkManagerDemoTheme
import com.example.workmanagerdemo.ui.navigation.WorkManagerNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-Activity entry point for the WorkManager Demo app.
 *
 * Responsibilities:
 * - Enable edge-to-edge rendering via [enableEdgeToEdge].
 * - Set the Compose content root with [WorkManagerDemoTheme].
 * - Host [WorkManagerNavHost] which provides the full navigation graph
 *   (Dashboard → individual demo screens).
 *
 * This Activity is annotated with [@AndroidEntryPoint] so that Hilt can
 * inject dependencies into this Activity and any ViewModel associated with it.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkManagerDemoTheme {
                WorkManagerNavHost()
            }
        }
    }
}
