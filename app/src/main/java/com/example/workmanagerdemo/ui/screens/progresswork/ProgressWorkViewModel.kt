package com.example.workmanagerdemo.ui.screens.progresswork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.repository.ImageCompressRepository
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
 * UI State for the Input / Output & Progress demo screen.
 */
data class ProgressWorkUiState(
    val selectedQuality: Int = 75,
    val selectedImageName: String = "nature_mountain_4k.jpg",
    val availableImages: List<String> = listOf(
        "nature_mountain_4k.jpg",
        "city_night_panorama.jpg",
        "portrait_dslr_raw.jpg"
    ),
    val workId: UUID? = null,
    val workInfo: WorkInfo? = null,
    val progressPercent: Int = 0,
    val progressMessage: String = "",
    val outputUri: String? = null,
    val originalSizeBytes: Long? = null,
    val outputSizeBytes: Long? = null,
    val compressionRatio: String? = null,
    val logLines: List<String> = emptyList()
)

/**
 * ViewModel managing state for [ProgressWorkScreen].
 *
 * Demonstrates:
 * - Setting input parameters (quality, image URI) passed to [ImageCompressWorker].
 * - Observing reactive progress emissions from [WorkInfo.progress].
 * - Parsing result data from [WorkInfo.outputData] upon [WorkInfo.State.SUCCEEDED].
 */
@HiltViewModel
class ProgressWorkViewModel @Inject constructor(
    private val repository: ImageCompressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressWorkUiState())
    val uiState: StateFlow<ProgressWorkUiState> = _uiState.asStateFlow()

    private val _currentWorkId = MutableStateFlow<UUID?>(null)

    init {
        // Observe WorkInfo for the active request reactively
        viewModelScope.launch {
            _currentWorkId
                .flatMapLatest { id ->
                    if (id == null) flowOf(null)
                    else repository.observeWorkById(id)
                }
                .collect { workInfo ->
                    handleWorkInfoUpdate(workInfo)
                }
        }
    }

    /** Updates the selected compression quality (10 to 100). */
    fun onQualityChange(quality: Int) {
        _uiState.update { it.copy(selectedQuality = quality) }
    }

    /** Selects a sample image to compress. */
    fun onImageSelect(imageName: String) {
        _uiState.update { it.copy(selectedImageName = imageName) }
    }

    /** Enqueues the image compression work request. */
    fun startCompression() {
        val state = _uiState.value
        val fakeUri = "file:///storage/emulated/0/DCIM/${state.selectedImageName}"

        val id = repository.enqueueImageCompression(
            imageUri = fakeUri,
            quality = state.selectedQuality
        )

        _currentWorkId.value = id
        _uiState.update {
            it.copy(
                workId = id,
                progressPercent = 0,
                progressMessage = "Starting…",
                outputUri = null,
                originalSizeBytes = null,
                outputSizeBytes = null,
                compressionRatio = null,
                logLines = listOf("Worker enqueued with Quality=${state.selectedQuality}%, File=${state.selectedImageName}")
            )
        }
    }

    /** Cancels the ongoing compression task. */
    fun cancelCompression() {
        repository.cancelCurrentWork()
        appendLog("⛔ Cancellation signal sent to Worker")
    }

    /** Resets the screen state for a new test run. */
    fun reset() {
        _currentWorkId.value = null
        _uiState.update {
            it.copy(
                workId = null,
                workInfo = null,
                progressPercent = 0,
                progressMessage = "",
                outputUri = null,
                originalSizeBytes = null,
                outputSizeBytes = null,
                compressionRatio = null,
                logLines = emptyList()
            )
        }
    }

    private fun handleWorkInfoUpdate(workInfo: WorkInfo?) {
        if (workInfo == null) return

        // 1. Read ephemeral progress data set via setProgress()
        val percent = workInfo.progress.getInt(WorkerConstants.PROGRESS_VALUE, 0)
        val progressMsg = workInfo.progress.getString(WorkerConstants.PROGRESS_MESSAGE) ?: ""

        // 2. Read final output data returned via Result.success()
        val outputUri = workInfo.outputData.getString(WorkerConstants.KEY_OUTPUT_URI)
        val originalSize = workInfo.outputData.getLong(WorkerConstants.KEY_ORIGINAL_SIZE_BYTES, 0L)
            .takeIf { it > 0L }
        val outputSize = workInfo.outputData.getLong(WorkerConstants.KEY_OUTPUT_SIZE_BYTES, 0L)
            .takeIf { it > 0L }
        val ratio = workInfo.outputData.getString(WorkerConstants.KEY_COMPRESSION_RATIO)

        _uiState.update { state ->
            state.copy(
                workInfo = workInfo,
                progressPercent = if (workInfo.state == WorkInfo.State.SUCCEEDED) 100 else percent,
                progressMessage = progressMsg,
                outputUri = outputUri ?: state.outputUri,
                originalSizeBytes = originalSize ?: state.originalSizeBytes,
                outputSizeBytes = outputSize ?: state.outputSizeBytes,
                compressionRatio = ratio ?: state.compressionRatio
            )
        }

        // Add log entries based on state transitions
        when (workInfo.state) {
            WorkInfo.State.ENQUEUED -> appendLog("⏳ State: ENQUEUED")
            WorkInfo.State.RUNNING -> {
                if (progressMsg.isNotBlank()) {
                    appendLog("▶️ $percent% - $progressMsg")
                }
            }
            WorkInfo.State.SUCCEEDED -> {
                appendLog("✅ State: SUCCEEDED - Output saved to $outputUri")
            }
            WorkInfo.State.FAILED -> {
                val failureReason = workInfo.outputData.getString(WorkerConstants.PROGRESS_MESSAGE)
                    ?: "Unknown error"
                appendLog("❌ State: FAILED - $failureReason")
            }
            WorkInfo.State.CANCELLED -> appendLog("⛔ State: CANCELLED")
            WorkInfo.State.BLOCKED -> appendLog("🔒 State: BLOCKED")
        }
    }

    private fun appendLog(message: String) {
        _uiState.update { it.copy(logLines = it.logLines + message) }
    }
}
