package com.example.workmanagerdemo.ui.screens.expeditedwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.repository.CriticalAlertRepository
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

/**
 * UI State for the Expedited Work screen.
 */
data class ExpeditedWorkUiState(
    val alertTitle: String = "Server Health Breach",
    val alertMessage: String = "CPU temperature exceeded 92°C on primary cluster node.",
    val selectedSeverity: String = "CRITICAL",
    val availableSeverities: List<String> = listOf("CRITICAL", "HIGH", "NORMAL"),
    val selectedPolicy: OutOfQuotaPolicy = OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST,
    val workId: UUID? = null,
    val workInfo: WorkInfo? = null,
    val latencyMs: Long? = null,
    val dispatchTimestamp: String? = null,
    val progressPercent: Int = 0,
    val progressMessage: String = "",
    val logLines: List<String> = emptyList()
)

/**
 * ViewModel for [ExpeditedWorkScreen].
 *
 * Demonstrates:
 * - Triggering expedited execution (`setExpedited`).
 * - Observing immediate state transitions from ENQUEUED to RUNNING.
 * - Comparing fallback policies (`RUN_AS_NON_EXPEDITED_WORK_REQUEST` vs `DROP_WORK_REQUEST`).
 */
@HiltViewModel
class ExpeditedWorkViewModel @Inject constructor(
    private val repository: CriticalAlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpeditedWorkUiState())
    val uiState: StateFlow<ExpeditedWorkUiState> = _uiState.asStateFlow()

    private val _currentWorkId = MutableStateFlow<UUID?>(null)

    init {
        viewModelScope.launch {
            _currentWorkId
                .flatMapLatest { id ->
                    if (id == null) flowOf(null)
                    else repository.observeAlertWorkById(id)
                }
                .collect { workInfo ->
                    handleWorkInfoUpdate(workInfo)
                }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(alertTitle = newTitle) }
    }

    fun onMessageChange(newMessage: String) {
        _uiState.update { it.copy(alertMessage = newMessage) }
    }

    fun onSeveritySelect(severity: String) {
        _uiState.update { it.copy(selectedSeverity = severity) }
    }

    fun onPolicySelect(policy: OutOfQuotaPolicy) {
        _uiState.update { it.copy(selectedPolicy = policy) }
    }

    /** Triggers the expedited critical alert task. */
    fun dispatchAlert() {
        val state = _uiState.value
        val id = repository.dispatchCriticalAlert(
            title = state.alertTitle,
            message = state.alertMessage,
            severity = state.selectedSeverity,
            policy = state.selectedPolicy
        )

        _currentWorkId.value = id
        _uiState.update {
            it.copy(
                workId = id,
                progressPercent = 0,
                progressMessage = "Dispatching expedited task…",
                latencyMs = null,
                dispatchTimestamp = null,
                logLines = listOf(
                    "⚡ Enqueued Expedited Work (ID: ${id.toString().take(8)}…)",
                    "Policy: ${state.selectedPolicy.name}, Severity: ${state.selectedSeverity}"
                )
            )
        }
    }

    /** Cancels the in-flight alert task. */
    fun cancelAlert() {
        repository.cancelCurrentAlert()
        appendLog("⛔ Cancellation signal dispatched")
    }

    /** Resets the screen. */
    fun reset() {
        _currentWorkId.value = null
        _uiState.update {
            it.copy(
                workId = null,
                workInfo = null,
                progressPercent = 0,
                progressMessage = "",
                latencyMs = null,
                dispatchTimestamp = null,
                logLines = emptyList()
            )
        }
    }

    private fun handleWorkInfoUpdate(workInfo: WorkInfo?) {
        if (workInfo == null) return

        val percent = workInfo.progress.getInt(WorkerConstants.PROGRESS_VALUE, 0)
        val msg = workInfo.progress.getString(WorkerConstants.PROGRESS_MESSAGE) ?: ""

        val latency = workInfo.outputData.getLong(WorkerConstants.KEY_EXECUTION_LATENCY_MS, 0L)
            .takeIf { it > 0L }

        val timestamp = workInfo.outputData.getString(WorkerConstants.KEY_DISPATCH_TIMESTAMP)

        _uiState.update { state ->
            state.copy(
                workInfo = workInfo,
                progressPercent = if (workInfo.state == WorkInfo.State.SUCCEEDED) 100 else percent,
                progressMessage = msg,
                latencyMs = latency ?: state.latencyMs,
                dispatchTimestamp = timestamp ?: state.dispatchTimestamp
            )
        }

        when (workInfo.state) {
            WorkInfo.State.ENQUEUED -> appendLog("⏳ ENQUEUED — Bypassing standard batching delay (Expedited).")
            WorkInfo.State.RUNNING -> {
                if (msg.isNotBlank()) appendLog("▶️ RUNNING ($percent%) — $msg")
            }
            WorkInfo.State.SUCCEEDED -> {
                appendLog("✅ SUCCEEDED — High-priority dispatch completed in ${latency ?: 0}ms.")
            }
            WorkInfo.State.FAILED -> {
                val failureMsg = workInfo.outputData.getString(WorkerConstants.PROGRESS_MESSAGE) ?: "Alert failed"
                appendLog("❌ FAILED — $failureMsg")
            }
            WorkInfo.State.CANCELLED -> appendLog("⛔ CANCELLED — Alert task cancelled.")
            WorkInfo.State.BLOCKED -> appendLog("🔒 BLOCKED")
        }
    }

    private fun appendLog(line: String) {
        _uiState.update { it.copy(logLines = it.logLines + line) }
    }
}
