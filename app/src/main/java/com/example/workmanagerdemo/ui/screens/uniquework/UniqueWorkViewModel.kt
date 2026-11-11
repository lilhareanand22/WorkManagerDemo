package com.example.workmanagerdemo.ui.screens.uniquework

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.repository.AnalyticsSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Unique Work & Conflict Policies screen.
 */
data class UniqueWorkUiState(
    val selectedPolicy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
    val selectedBackoffPolicy: BackoffPolicy = BackoffPolicy.EXPONENTIAL,
    val simulateRetry: Boolean = false,
    val batchCounter: Int = 1,
    val activeWorkList: List<WorkInfo> = emptyList(),
    val latestWorkInfo: WorkInfo? = null,
    val syncedBatchId: String? = null,
    val eventsSynced: Int = 0,
    val attemptCount: Int = 0,
    val logLines: List<String> = emptyList()
)

/**
 * ViewModel for [UniqueWorkScreen].
 *
 * Demonstrates:
 * - Comparing the 4 [ExistingWorkPolicy] conflict resolution strategies.
 * - Observing backoff recalculation on [Result.retry].
 * - Rapid fire stress testing to observe KEEP vs REPLACE vs APPEND.
 */
@HiltViewModel
class UniqueWorkViewModel @Inject constructor(
    private val repository: AnalyticsSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UniqueWorkUiState())
    val uiState: StateFlow<UniqueWorkUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeUniqueAnalyticsWork().collect { workInfoList ->
                handleWorkInfoList(workInfoList)
            }
        }
    }

    fun onPolicySelect(policy: ExistingWorkPolicy) {
        _uiState.update { it.copy(selectedPolicy = policy) }
    }

    fun onBackoffPolicySelect(policy: BackoffPolicy) {
        _uiState.update { it.copy(selectedBackoffPolicy = policy) }
    }

    fun onSimulateRetryToggle(enabled: Boolean) {
        _uiState.update { it.copy(simulateRetry = enabled) }
    }

    /** Enqueues a single unique work request. */
    fun enqueueWork() {
        val state = _uiState.value
        val batchId = "BATCH_${1000 + state.batchCounter}"

        val id = repository.enqueueUniqueAnalyticsSync(
            batchId = batchId,
            eventsCount = 280,
            policy = state.selectedPolicy,
            backoffPolicy = state.selectedBackoffPolicy,
            simulateRetry = state.simulateRetry
        )

        _uiState.update { it.copy(batchCounter = it.batchCounter + 1) }

        appendLog("🚀 Enqueued Unique Work [$batchId] with Policy: ${state.selectedPolicy.name}")
        appendLog("ID: ${id.toString().take(8)}…, Backoff=${state.selectedBackoffPolicy.name} (10s delay)")
    }

    /**
     * Rapid Fire Test:
     * Fires 2 rapid unique requests in a row to clearly demonstrate
     * how KEEP vs REPLACE vs APPEND handles the collision!
     */
    fun rapidFireEnqueueTest() {
        val state = _uiState.value
        val batchA = "RAPID_A_${1000 + state.batchCounter}"
        val batchB = "RAPID_B_${1001 + state.batchCounter}"

        appendLog("⚡ Triggering Rapid Double-Enqueue to test '${state.selectedPolicy.name}' conflict resolution…")

        // First enqueue
        repository.enqueueUniqueAnalyticsSync(
            batchId = batchA,
            eventsCount = 150,
            policy = state.selectedPolicy,
            backoffPolicy = state.selectedBackoffPolicy,
            simulateRetry = state.simulateRetry
        )
        appendLog("1️⃣ Sent $batchA (Policy: ${state.selectedPolicy.name})")

        // Immediate second enqueue with same unique work name!
        repository.enqueueUniqueAnalyticsSync(
            batchId = batchB,
            eventsCount = 350,
            policy = state.selectedPolicy,
            backoffPolicy = state.selectedBackoffPolicy,
            simulateRetry = state.simulateRetry
        )
        appendLog("2️⃣ Sent $batchB (Collision under ${state.selectedPolicy.name}!)")

        _uiState.update { it.copy(batchCounter = it.batchCounter + 2) }
    }

    /** Cancels the unique analytics work. */
    fun cancelWork() {
        repository.cancelAnalyticsSync()
        appendLog("⛔ Cancelled Unique Work")
    }

    /** Resets the screen. */
    fun reset() {
        _uiState.update {
            it.copy(
                activeWorkList = emptyList(),
                latestWorkInfo = null,
                syncedBatchId = null,
                eventsSynced = 0,
                attemptCount = 0,
                logLines = emptyList()
            )
        }
    }

    private fun handleWorkInfoList(list: List<WorkInfo>) {
        val latest = list.firstOrNull()

        val batchId = latest?.outputData?.getString(WorkerConstants.KEY_ANALYTICS_BATCH_ID)
        val events = latest?.outputData?.getInt(WorkerConstants.KEY_EVENTS_COUNT, 0) ?: 0
        val attempts = latest?.runAttemptCount ?: 0

        _uiState.update { state ->
            state.copy(
                activeWorkList = list,
                latestWorkInfo = latest,
                syncedBatchId = batchId ?: state.syncedBatchId,
                eventsSynced = if (events > 0) events else state.eventsSynced,
                attemptCount = attempts
            )
        }

        if (latest != null) {
            val progressMsg = latest.progress.getString(WorkerConstants.PROGRESS_MESSAGE)
            if (!progressMsg.isNullOrBlank()) {
                appendLog("▶️ ${latest.state.name} (Attempt #$attempts): $progressMsg")
            }
        }
    }

    private fun appendLog(line: String) {
        _uiState.update { it.copy(logLines = it.logLines + line) }
    }
}
