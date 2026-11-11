package com.example.workmanagerdemo.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.workmanagerdemo.repository.WorkManagerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the Dashboard screen.
 *
 * Observes system-wide WorkManager tasks using [WorkManagerRepository]
 * and provides global administration controls (Cancel All, Prune Database).
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: WorkManagerRepository
) : ViewModel() {

    /**
     * Emits the list of all currently active work across all screens.
     */
    val activeWorkList: StateFlow<List<WorkInfo>> = repository.observeAllActiveWork()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Emits the count of currently active background tasks.
     */
    val activeCount: StateFlow<Int> = activeWorkList
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    /**
     * Cancels all scheduled, enqueued, and running background jobs.
     */
    fun cancelAllWork() {
        repository.cancelAllWork()
    }

    /**
     * Prunes terminal work records from WorkManager's Room SQLite database.
     */
    fun pruneDatabase() {
        repository.pruneWork()
    }
}
