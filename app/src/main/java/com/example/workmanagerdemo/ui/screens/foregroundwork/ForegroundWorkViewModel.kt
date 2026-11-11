package com.example.workmanagerdemo.ui.screens.foregroundwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.repository.VideoExportRepository
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
 * UI State for Long-Running Foreground Work screen.
 */
data class ForegroundWorkUiState(
    val videoTitle: String = "Cinematic_Tokyo_Walk",
    val selectedResolution: String = "1080p",
    val availableResolutions: List<String> = listOf("720p", "1080p", "4K"),
    val selectedFormat: String = "mp4",
    val availableFormats: List<String> = listOf("mp4", "mkv"),
    val workId: UUID? = null,
    val workInfo: WorkInfo? = null,
    val progressPercent: Int = 0,
    val progressMessage: String = "",
    val outputUri: String? = null,
    val outputDuration: String? = null,
    val outputSizeBytes: Long? = null,
    val logLines: List<String> = emptyList()
)

/**
 * ViewModel for [ForegroundWorkScreen].
 *
 * Demonstrates:
 * - Long-running tasks promoted to a Foreground Service.
 * - Observing dual updates (in-app progress & notification status).
 * - System-level cancellation handling.
 */
@HiltViewModel
class ForegroundWorkViewModel @Inject constructor(
    private val repository: VideoExportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForegroundWorkUiState())
    val uiState: StateFlow<ForegroundWorkUiState> = _uiState.asStateFlow()

    private val _currentWorkId = MutableStateFlow<UUID?>(null)

    init {
        viewModelScope.launch {
            _currentWorkId
                .flatMapLatest { id ->
                    if (id == null) flowOf(null)
                    else repository.observeVideoExport(id)
                }
                .collect { workInfo ->
                    handleWorkInfoUpdate(workInfo)
                }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(videoTitle = newTitle) }
    }

    fun onResolutionSelect(resolution: String) {
        _uiState.update { it.copy(selectedResolution = resolution) }
    }

    fun onFormatSelect(format: String) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    /** Starts the video export task. */
    fun startExport() {
        val state = _uiState.value
        val id = repository.startVideoExport(
            title = state.videoTitle,
            resolution = state.selectedResolution,
            format = state.selectedFormat
        )

        _currentWorkId.value = id
        _uiState.update {
            it.copy(
                workId = id,
                progressPercent = 0,
                progressMessage = "Promoting to Foreground Service…",
                outputUri = null,
                outputDuration = null,
                outputSizeBytes = null,
                logLines = listOf(
                    "🎬 Started Video Export (ID: ${id.toString().take(8)}…)",
                    "Config: ${state.selectedResolution} | ${state.selectedFormat.uppercase()} | Title: ${state.videoTitle}",
                    "🔔 Check the Android notification drawer for the live progress bar!"
                )
            )
        }
    }

    /** Cancels the video export. */
    fun cancelExport() {
        repository.cancelVideoExport()
        appendLog("⛔ Cancel signal sent to Foreground Worker")
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
                outputUri = null,
                outputDuration = null,
                outputSizeBytes = null,
                logLines = emptyList()
            )
        }
    }

    private fun handleWorkInfoUpdate(workInfo: WorkInfo?) {
        if (workInfo == null) return

        val percent = workInfo.progress.getInt(WorkerConstants.PROGRESS_VALUE, 0)
        val msg = workInfo.progress.getString(WorkerConstants.PROGRESS_MESSAGE) ?: ""

        val uri = workInfo.outputData.getString(WorkerConstants.KEY_OUTPUT_URI)
        val duration = workInfo.outputData.getString(WorkerConstants.KEY_OUTPUT_DURATION)
        val size = workInfo.outputData.getLong(WorkerConstants.KEY_OUTPUT_SIZE_BYTES, 0L).takeIf { it > 0L }

        _uiState.update { state ->
            state.copy(
                workInfo = workInfo,
                progressPercent = if (workInfo.state == WorkInfo.State.SUCCEEDED) 100 else percent,
                progressMessage = msg,
                outputUri = uri ?: state.outputUri,
                outputDuration = duration ?: state.outputDuration,
                outputSizeBytes = size ?: state.outputSizeBytes
            )
        }

        when (workInfo.state) {
            WorkInfo.State.ENQUEUED -> appendLog("⏳ ENQUEUED — Preparing transcode engine…")
            WorkInfo.State.RUNNING -> {
                if (msg.isNotBlank()) appendLog("▶️ $percent% — $msg")
            }
            WorkInfo.State.SUCCEEDED -> {
                appendLog("✅ SUCCEEDED — Video exported cleanly to $uri")
            }
            WorkInfo.State.FAILED -> {
                appendLog("❌ FAILED — Video export terminated unexpectedly.")
            }
            WorkInfo.State.CANCELLED -> {
                appendLog("⛔ CANCELLED — Worker stopped & temporary partial files cleaned up via onStopped().")
            }
            WorkInfo.State.BLOCKED -> appendLog("🔒 BLOCKED")
        }
    }

    private fun appendLog(line: String) {
        _uiState.update { it.copy(logLines = it.logLines + line) }
    }
}
