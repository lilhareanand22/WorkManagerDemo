package com.example.workmanagerdemo.ui.screens.periodicwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkInfo
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.repository.DailyCleanupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Periodic Work Screen.
 */
data class PeriodicWorkUiState(
    val repeatIntervalMinutes: Long = 15L,
    val flexIntervalMinutes: Long = 5L,
    val requireCharging: Boolean = false,
    val requireBatteryNotLow: Boolean = true,
    val selectedPolicy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE,
    val workInfo: WorkInfo? = null,
    val lastExecutionTimestamp: String? = null,
    val filesDeletedCount: Int = 0,
    val bytesReclaimed: Long = 0L,
    val totalRunsCount: Int = 0,
    val logLines: List<String> = emptyList()
)

/**
 * ViewModel managing state for [PeriodicWorkScreen].
 *
 * Demonstrates:
 * - Setting up [androidx.work.PeriodicWorkRequest] with 15-minute repeat and 5-minute flex windows.
 * - Comparing [ExistingPeriodicWorkPolicy] options (UPDATE vs KEEP vs CANCEL_AND_REENQUEUE).
 * - Reactively observing the recurring cycle: ENQUEUED ↔ RUNNING.
 */
@HiltViewModel
class PeriodicWorkViewModel @Inject constructor(
    private val repository: DailyCleanupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeriodicWorkUiState())
    val uiState: StateFlow<PeriodicWorkUiState> = _uiState.asStateFlow()

    init {
        // Observe periodic work state reactively
        viewModelScope.launch {
            repository.observePeriodicWork().collect { workInfoList ->
                val latestWorkInfo = workInfoList.firstOrNull()
                handleWorkInfoUpdate(latestWorkInfo)
            }
        }
    }

    fun onRepeatIntervalChange(minutes: Long) {
        val clampedRepeat = maxOf(minutes, 15L)
        _uiState.update { it.copy(repeatIntervalMinutes = clampedRepeat) }
    }

    fun onFlexIntervalChange(minutes: Long) {
        val clampedFlex = maxOf(minutes, 5L)
        _uiState.update { it.copy(flexIntervalMinutes = clampedFlex) }
    }

    fun onChargingToggle(enabled: Boolean) {
        _uiState.update { it.copy(requireCharging = enabled) }
    }

    fun onBatteryToggle(enabled: Boolean) {
        _uiState.update { it.copy(requireBatteryNotLow = enabled) }
    }

    fun onPolicySelect(policy: ExistingPeriodicWorkPolicy) {
        _uiState.update { it.copy(selectedPolicy = policy) }
    }

    /** Schedules or updates the unique periodic work. */
    fun schedulePeriodicWork() {
        val state = _uiState.value
        val id = repository.schedulePeriodicCleanup(
            repeatIntervalMinutes = state.repeatIntervalMinutes,
            flexIntervalMinutes = state.flexIntervalMinutes,
            requireCharging = state.requireCharging,
            requireBatteryNotLow = state.requireBatteryNotLow,
            policy = state.selectedPolicy
        )

        appendLog("Scheduled Periodic Work (ID: ${id.toString().take(8)}…)")
        appendLog("Config: Repeat=${state.repeatIntervalMinutes}m, Flex=${state.flexIntervalMinutes}m, Policy=${state.selectedPolicy.name}")
    }

    /** Cancels the active periodic cleanup work. */
    fun cancelPeriodicWork() {
        repository.cancelPeriodicCleanup()
        appendLog("⛔ Cancelled Unique Periodic Work")
    }

    private fun handleWorkInfoUpdate(workInfo: WorkInfo?) {
        if (workInfo == null) {
            _uiState.update { it.copy(workInfo = null) }
            return
        }

        // Read output data if available
        val deleted = workInfo.outputData.getInt(WorkerConstants.KEY_FILES_DELETED_COUNT, 0)
        val bytes = workInfo.outputData.getLong(WorkerConstants.KEY_BYTES_RECLAIMED, 0L)
        val timestamp = workInfo.outputData.getString(WorkerConstants.KEY_CLEANUP_TIMESTAMP)

        val previousState = _uiState.value.workInfo?.state
        val currentState = workInfo.state

        _uiState.update { state ->
            state.copy(
                workInfo = workInfo,
                lastExecutionTimestamp = timestamp ?: state.lastExecutionTimestamp,
                filesDeletedCount = if (deleted > 0) deleted else state.filesDeletedCount,
                bytesReclaimed = if (bytes > 0L) bytes else state.bytesReclaimed,
                totalRunsCount = if (previousState == WorkInfo.State.RUNNING && currentState == WorkInfo.State.ENQUEUED) {
                    state.totalRunsCount + 1
                } else state.totalRunsCount
            )
        }

        // State log feedback
        if (previousState != currentState) {
            when (currentState) {
                WorkInfo.State.ENQUEUED -> {
                    if (previousState == WorkInfo.State.RUNNING) {
                        appendLog("🔄 Periodic cycle complete! WorkManager re-scheduled work for the next interval (state=ENQUEUED).")
                    } else {
                        appendLog("⏳ Periodic Work is ENQUEUED (waiting for interval / flex window).")
                    }
                }
                WorkInfo.State.RUNNING -> {
                    appendLog("▶️ Periodic Worker is RUNNING cleanup execution…")
                }
                WorkInfo.State.CANCELLED -> {
                    appendLog("⛔ Periodic Work has been CANCELLED.")
                }
                WorkInfo.State.FAILED -> {
                    appendLog("❌ Periodic Work FAILED.")
                }
                else -> {}
            }
        }
    }

    private fun appendLog(msg: String) {
        _uiState.update { it.copy(logLines = it.logLines + msg) }
    }
}
