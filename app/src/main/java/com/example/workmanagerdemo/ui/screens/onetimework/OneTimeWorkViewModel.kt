package com.example.workmanagerdemo.ui.screens.onetimework

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.workmanagerdemo.repository.SyncLogsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ── UI State data class ──────────────────────────────────────────────────────

/**
 * Immutable snapshot of everything the [OneTimeWorkScreen] needs to render.
 *
 * Using a single data class as the UI state is an MVI / Unidirectional
 * Data Flow (UDF) pattern.  The screen reads this one object and never
 * holds mutable state of its own (except for Compose's remember { } for
 * purely local animation state).
 *
 * @param requireNetwork        Whether the network constraint toggle is ON.
 * @param requireCharging       Whether the charging constraint toggle is ON.
 * @param requireBatteryNotLow  Whether the battery-not-low toggle is ON.
 * @param workId                UUID of the currently tracked work request (nullable).
 * @param workInfo              Latest [WorkInfo] from WorkManager (nullable = not started yet).
 * @param logLines              In-screen log lines for step-by-step feedback.
 */
data class OneTimeWorkUiState(
    val requireNetwork: Boolean        = true,
    val requireCharging: Boolean       = false,
    val requireBatteryNotLow: Boolean  = false,
    val workId: UUID?                  = null,
    val workInfo: WorkInfo?            = null,
    val logLines: List<String>         = emptyList()
)

// ── ViewModel ────────────────────────────────────────────────────────────────

/**
 * ViewModel for the One-Time Work & Constraints demo screen.
 *
 * ── Why @HiltViewModel? ──────────────────────────────────────────────────────
 * @HiltViewModel tells Hilt to generate the factory needed to supply
 * [SyncLogsRepository] into this ViewModel's constructor at runtime.
 * We get the ViewModel via hiltViewModel() in the Composable, which
 * internally uses the generated factory.
 *
 * ── Why extend ViewModel? ────────────────────────────────────────────────────
 * [ViewModel] survives configuration changes (rotation, fold/unfold).
 * The coroutine [viewModelScope] is cancelled automatically when the
 * ViewModel is cleared (user navigates away permanently).
 *
 * ── State management with StateFlow ─────────────────────────────────────────
 * [_uiState] is a [MutableStateFlow] only the ViewModel can mutate.
 * [uiState] is the read-only [StateFlow] the UI collects.
 * This follows the "single source of truth" principle.
 *
 * ── WorkInfo observation chain ───────────────────────────────────────────────
 * WorkManager exposes Flows.  We use flatMapLatest on the work ID so that
 * whenever the user enqueues a NEW request, we immediately switch to
 * observing the new ID's Flow without collecting the old one.
 */
@HiltViewModel
class OneTimeWorkViewModel @Inject constructor(
    private val repository: SyncLogsRepository
) : ViewModel() {

    // Backing mutable state — only the ViewModel writes here
    private val _uiState = MutableStateFlow(OneTimeWorkUiState())

    // Exposed read-only state — the Composable observes this
    val uiState: StateFlow<OneTimeWorkUiState> = _uiState.asStateFlow()

    // Tracks the UUID of the current request so we can switch observations
    private val _currentWorkId = MutableStateFlow<UUID?>(null)

    init {
        // ── Reactive observation chain ───────────────────────────────────
        // flatMapLatest: when _currentWorkId changes (new work enqueued),
        // the previous Flow is cancelled and we subscribe to the new one.
        // This prevents stale WorkInfo leaking into the UI.
        viewModelScope.launch {
            _currentWorkId
                .flatMapLatest { id ->
                    // If no work has been started yet, emit nothing.
                    if (id == null) flowOf(null)
                    else repository.observeWorkById(id)
                }
                .collect { workInfo ->
                    _uiState.update { it.copy(workInfo = workInfo) }
                    // Append a log line whenever the state changes
                    if (workInfo != null) appendLog(workInfo)
                }
        }
    }

    // ── Constraint toggles ───────────────────────────────────────────────────

    /** User toggled the Network constraint switch. */
    fun onNetworkToggle(enabled: Boolean) =
        _uiState.update { it.copy(requireNetwork = enabled) }

    /** User toggled the Charging constraint switch. */
    fun onChargingToggle(enabled: Boolean) =
        _uiState.update { it.copy(requireCharging = enabled) }

    /** User toggled the Battery Not Low constraint switch. */
    fun onBatteryToggle(enabled: Boolean) =
        _uiState.update { it.copy(requireBatteryNotLow = enabled) }

    // ── Work actions ─────────────────────────────────────────────────────────

    /**
     * Builds and enqueues a [com.example.workmanagerdemo.workers.SyncLogsWorker]
     * with the constraints currently selected in the UI.
     *
     * Flow:
     *   1. Repository builds and enqueues the WorkRequest → returns UUID.
     *   2. We store the UUID in _currentWorkId.
     *   3. The flatMapLatest chain above automatically switches to observing
     *      the new UUID's WorkInfo Flow.
     *   4. Each WorkInfo emission triggers appendLog() and a state update.
     */
    fun enqueueWork() {
        val state = _uiState.value
        val id = repository.enqueueSyncLogs(
            requireNetwork       = state.requireNetwork,
            requireCharging      = state.requireCharging,
            requireBatteryNotLow = state.requireBatteryNotLow
        )
        _currentWorkId.value = id
        _uiState.update { it.copy(workId = id, logLines = emptyList()) }
        appendLog("Work enqueued — ID: ${id.toString().take(8)}…")
        appendLog("Constraints: Network=${ state.requireNetwork }, " +
                "Charging=${ state.requireCharging }, " +
                "BatteryNotLow=${ state.requireBatteryNotLow }")
    }

    /**
     * Cancels the currently running / enqueued work by its UUID.
     *
     * WorkManager propagates cancellation to the Worker's coroutine.
     * The Worker's state transitions to CANCELLED in the Room DB,
     * which the observation Flow picks up and delivers to the UI.
     */
    fun cancelWork() {
        repository.cancelCurrentWork()
        appendLog("⛔ Cancellation requested…")
    }

    /**
     * Resets the UI to its initial state so the user can start a fresh demo.
     * Does NOT cancel any in-flight work — call cancelWork() first if needed.
     */
    fun resetState() {
        _currentWorkId.value = null
        _uiState.update {
            it.copy(workId = null, workInfo = null, logLines = emptyList())
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Translates a [WorkInfo] state change into a human-readable log line
     * and appends it to [OneTimeWorkUiState.logLines].
     */
    private fun appendLog(workInfo: WorkInfo) {
        val line = when (workInfo.state) {
            WorkInfo.State.ENQUEUED   ->
                "⏳ ENQUEUED — Waiting for constraints to be met…"
            WorkInfo.State.RUNNING    ->
                "▶️ RUNNING — Worker is executing…"
            WorkInfo.State.SUCCEEDED  -> {
                val msg = workInfo.outputData
                    .getString(com.example.workmanagerdemo.WorkerConstants.PROGRESS_MESSAGE)
                    ?: "No output message"
                "✅ SUCCEEDED — $msg"
            }
            WorkInfo.State.FAILED     -> {
                val msg = workInfo.outputData
                    .getString(com.example.workmanagerdemo.WorkerConstants.PROGRESS_MESSAGE)
                    ?: "Unknown failure"
                "❌ FAILED — $msg"
            }
            WorkInfo.State.CANCELLED  -> "⛔ CANCELLED — Work was stopped."
            WorkInfo.State.BLOCKED    -> "🔒 BLOCKED — Waiting for prerequisite work."
        }
        appendLog(line)
    }

    /** Appends a plain string line to the in-screen log panel. */
    private fun appendLog(line: String) {
        _uiState.update { it.copy(logLines = it.logLines + line) }
    }
}
