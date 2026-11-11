package com.example.workmanagerdemo.ui.screens.customfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.workmanagerdemo.WorkerConstants
import com.example.workmanagerdemo.data.UserProfile
import com.example.workmanagerdemo.data.UserRepository
import com.example.workmanagerdemo.repository.UserSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the Custom WorkerFactory & Hilt DI screen.
 */
data class CustomFactoryUiState(
    val userProfile: UserProfile? = null,
    val workInfo: WorkInfo? = null,
    val syncedUserId: String? = null,
    val recordsUpdated: Int = 0,
    val progressPercent: Int = 0,
    val progressMessage: String = "",
    val logLines: List<String> = emptyList()
)

/**
 * ViewModel for [CustomFactoryScreen].
 */
@HiltViewModel
class CustomFactoryViewModel @Inject constructor(
    private val repository: UserSyncRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomFactoryUiState())
    val uiState: StateFlow<CustomFactoryUiState> = _uiState.asStateFlow()

    init {
        // Load initial profile from injected repo
        _uiState.update { it.copy(userProfile = userRepository.getUserProfile()) }

        viewModelScope.launch {
            repository.observeUserSync().collect { list ->
                val latest = list.firstOrNull()
                handleWorkInfoUpdate(latest)
            }
        }
    }

    /** Triggers the Hilt-injected Worker. */
    fun startSync() {
        val id = repository.enqueueUserSync()
        _uiState.update {
            it.copy(
                progressPercent = 0,
                progressMessage = "HiltWorkerFactory resolving constructor dependencies…",
                syncedUserId = null,
                recordsUpdated = 0,
                logLines = listOf(
                    "⚙️ Enqueued UserRepositorySyncWorker (ID: ${id.toString().take(8)}…)",
                    "🛠️ WorkManager delegating instantiation to HiltWorkerFactory…",
                    "💉 Injected dependencies: UserRepository, WorkNotificationHelper"
                )
            )
        }
    }

    /** Cancels the sync work. */
    fun cancelSync() {
        repository.cancelUserSync()
        appendLog("⛔ Cancelled User Sync Worker")
    }

    /** Resets the screen. */
    fun reset() {
        _uiState.update {
            it.copy(
                workInfo = null,
                syncedUserId = null,
                recordsUpdated = 0,
                progressPercent = 0,
                progressMessage = "",
                logLines = emptyList()
            )
        }
    }

    private fun handleWorkInfoUpdate(workInfo: WorkInfo?) {
        if (workInfo == null) return

        val percent = workInfo.progress.getInt(WorkerConstants.PROGRESS_VALUE, 0)
        val msg = workInfo.progress.getString(WorkerConstants.PROGRESS_MESSAGE) ?: ""

        val syncedId = workInfo.outputData.getString(WorkerConstants.KEY_SYNCED_USER_ID)
        val records = workInfo.outputData.getInt(WorkerConstants.KEY_RECORDS_UPDATED, 0)

        _uiState.update { state ->
            state.copy(
                workInfo = workInfo,
                progressPercent = if (workInfo.state == WorkInfo.State.SUCCEEDED) 100 else percent,
                progressMessage = msg,
                syncedUserId = syncedId ?: state.syncedUserId,
                recordsUpdated = if (records > 0) records else state.recordsUpdated
            )
        }

        when (workInfo.state) {
            WorkInfo.State.ENQUEUED -> appendLog("⏳ ENQUEUED — Waiting for scheduler thread")
            WorkInfo.State.RUNNING -> {
                if (msg.isNotBlank()) appendLog("▶️ RUNNING ($percent%) — $msg")
            }
            WorkInfo.State.SUCCEEDED -> {
                appendLog("✅ SUCCEEDED — Cleanly synced $records records for user $syncedId via injected repo!")
            }
            WorkInfo.State.FAILED -> appendLog("❌ FAILED — Worker execution failed")
            WorkInfo.State.CANCELLED -> appendLog("⛔ CANCELLED — Worker stopped")
            WorkInfo.State.BLOCKED -> appendLog("🔒 BLOCKED")
        }
    }

    private fun appendLog(line: String) {
        _uiState.update { it.copy(logLines = it.logLines + line) }
    }
}
