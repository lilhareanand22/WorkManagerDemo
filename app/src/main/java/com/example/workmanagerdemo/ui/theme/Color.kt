package com.example.workmanagerdemo.ui.theme

import androidx.compose.ui.graphics.Color

// ── WorkManagerDemo Brand Palette ──────────────────────────────────────────────
// Material You colour roles mapped to a teal/indigo engineering-inspired palette.

val WorkPrimary        = Color(0xFF1565C0)   // Deep blue — represents persistent reliability
val WorkOnPrimary      = Color(0xFFFFFFFF)
val WorkPrimaryContainer    = Color(0xFFD3E4FF)
val WorkOnPrimaryContainer  = Color(0xFF001B3D)

val WorkSecondary      = Color(0xFF00796B)   // Teal — represents progress & async processing
val WorkOnSecondary    = Color(0xFFFFFFFF)
val WorkSecondaryContainer  = Color(0xFFB2DFDB)
val WorkOnSecondaryContainer = Color(0xFF00201C)

val WorkTertiary       = Color(0xFF7B1FA2)   // Purple — represents expedited / high-priority
val WorkOnTertiary     = Color(0xFFFFFFFF)
val WorkTertiaryContainer   = Color(0xFFEAD6FF)
val WorkOnTertiaryContainer = Color(0xFF280048)

val WorkError          = Color(0xFFB3261E)
val WorkOnError        = Color(0xFFFFFFFF)
val WorkErrorContainer = Color(0xFFF9DEDC)
val WorkOnErrorContainer    = Color(0xFF410002)

val WorkBackground     = Color(0xFFFEFBFF)
val WorkOnBackground   = Color(0xFF1B1B1F)
val WorkSurface        = Color(0xFFFEFBFF)
val WorkOnSurface      = Color(0xFF1B1B1F)
val WorkSurfaceVariant = Color(0xFFE1E2EC)
val WorkOnSurfaceVariant    = Color(0xFF44464F)
val WorkOutline        = Color(0xFF757780)

// ── Status Colours (for WorkInfo.State chips in the UI) ────────────────────────
val StateEnqueued  = Color(0xFF9E9E9E)   // Grey   — waiting to run
val StateRunning   = Color(0xFF1565C0)   // Blue   — actively executing
val StateSucceeded = Color(0xFF2E7D32)   // Green  — completed successfully
val StateFailed    = Color(0xFFB3261E)   // Red    — terminal failure
val StateCancelled = Color(0xFFE65100)   // Orange — explicitly cancelled
val StateBlocked   = Color(0xFF6A1B9A)   // Purple — blocked by prerequisite
