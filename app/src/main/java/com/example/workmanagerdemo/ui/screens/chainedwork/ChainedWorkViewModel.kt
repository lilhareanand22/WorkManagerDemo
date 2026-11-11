package com.example.workmanagerdemo.ui.screens.chainedwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.repository.ChainedWorkRepository
import com.example.workmanagerdemo.repository.PipelineWorkIds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Chained Work Pipeline screen.
 */
data class ChainedWorkUiState(
    val simulateDownloadFailure: Boolean = false,
    val pipelineIds: PipelineWorkIds? = null,
    val downloadWorkInfo: WorkInfo? = null,
    val filterWorkInfo: WorkInfo? = null,
    val combineWorkInfo: WorkInfo? = null,
    val uploadUrl: String? = null,
    val recordsProcessed: Int = 0,
    val logLines: List<String> = emptyList()
)

/**
 * ViewModel for [ChainedWorkScreen].
 *
 * Demonstrates:
 * - Managing multi-node WorkContinuation state.
 * - Observing simultaneous parallel tasks and dependent joins.
 * - Visualizing failure propagation.
 */
@HiltViewModel
class ChainedWorkViewModel @Inject constructor(
    private val repository: ChainedWorkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChainedWorkUiState())
    val uiState: StateFlow<ChainedWorkUiState> = _uiState.asStateFlow()

    fun onSimulateFailureToggle(enabled: Boolean) {
        _uiState.update { it.copy(simulateDownloadFailure = enabled) }
    }

    /** Triggers the 3-node work pipeline DAG. */
    fun startPipeline() {
        val simulateFailure = _uiState.value.simulateDownloadFailure
        val ids = repository.enqueuePipeline(simulateDownloadFailure = simulateFailure)

        _uiState.update {
            it.copy(
                pipelineIds = ids,
                downloadWorkInfo = null,
                filterWorkInfo = null,
                combineWorkInfo = null,
                uploadUrl = null,
                recordsProcessed = 0,
                logLines = listOf(
                    "🔗 Enqueued Pipeline DAG with beginWith([Download, Filter]).then(CombineAndUpload)",
                    if (simulateFailure) "⚠️ Warning: 'Simulate Failure' is ON — DownloadWorker will fail!"
                    else "✅ Clean execution mode active"
                )
            )
        }

        observePipelineNodes(ids)
    }

    /** Cancels the running pipeline. */
    fun cancelPipeline() {
        repository.cancelPipeline()
        appendLog("⛔ Cancelled Chained Pipeline")
    }

    /** Resets the screen. */
    fun reset() {
        _uiState.update {
            it.copy(
                pipelineIds = null,
                downloadWorkInfo = null,
                filterWorkInfo = null,
                combineWorkInfo = null,
                uploadUrl = null,
                recordsProcessed = 0,
                logLines = emptyList()
            )
        }
    }

    private fun observePipelineNodes(ids: PipelineWorkIds) {
        // Observe Node A (Download)
        viewModelScope.launch {
            repository.observeWorkById(ids.downloadId).collect { workInfo ->
                if (workInfo != null) {
                    val prev = _uiState.value.downloadWorkInfo?.state
                    _uiState.update { it.copy(downloadWorkInfo = workInfo) }
                    if (prev != workInfo.state) {
                        appendLog("Node A (Download): ${workInfo.state.name}")
                    }
                }
            }
        }

        // Observe Node B (Filter)
        viewModelScope.launch {
            repository.observeWorkById(ids.filterId).collect { workInfo ->
                if (workInfo != null) {
                    val prev = _uiState.value.filterWorkInfo?.state
                    _uiState.update { it.copy(filterWorkInfo = workInfo) }
                    if (prev != workInfo.state) {
                        appendLog("Node B (Filter): ${workInfo.state.name}")
                    }
                }
            }
        }

        // Observe Node C (Combine & Upload)
        viewModelScope.launch {
            repository.observeWorkById(ids.combineId).collect { workInfo ->
                if (workInfo != null) {
                    val prev = _uiState.value.combineWorkInfo?.state
                    val url = workInfo.outputData.getString(WorkerConstants.KEY_UPLOAD_URL)
                    val records = workInfo.outputData.getInt(WorkerConstants.KEY_RECORDS_PROCESSED, 0)

                    _uiState.update {
                        it.copy(
                            combineWorkInfo = workInfo,
                            uploadUrl = url ?: it.uploadUrl,
                            recordsProcessed = if (records > 0) records else it.recordsProcessed
                        )
                    }

                    if (prev != workInfo.state) {
                        appendLog("Node C (Combine & Upload): ${workInfo.state.name}")
                        if (workInfo.state == WorkInfo.State.FAILED) {
                            appendLog("❌ Downstream Node C was marked FAILED due to upstream failure propagation!")
                        } else if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                            appendLog("🎉 Pipeline completed! Uploaded to $url")
                        }
                    }
                }
            }
        }
    }

    private fun appendLog(line: String) {
        _uiState.update { it.copy(logLines = it.logLines + line) }
    }
}
