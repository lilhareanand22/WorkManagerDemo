package com.example.workmanagerdemo

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.example.workmanagerdemo.workers.CombineAndUploadWorker
import com.example.workmanagerdemo.workers.DailyCleanupWorker
import com.example.workmanagerdemo.workers.DownloadWorker
import com.example.workmanagerdemo.workers.ImageCompressWorker
import com.example.workmanagerdemo.workers.SyncLogsWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Phase 6 ─ WorkManager Synchronous Integration Tests
 * ====================================================
 * Demonstrates how to test WorkManager code synchronously using [WorkManagerTestInitHelper]
 * and [androidx.work.testing.TestDriver].
 *
 * Key Testing Techniques Demonstrated:
 * 1. Replacing background thread pools with [SynchronousExecutor].
 * 2. Simulating hardware constraints (WiFi, Battery, Charging) via [TestDriver.setAllConstraintsMet].
 * 3. Simulating time delays and flex intervals via [TestDriver.setPeriodDelayMet].
 * 4. Verifying input/output data contracts and failure propagation without flakiness.
 */
@RunWith(AndroidJUnit4::class)
class WorkManagerIntegrationTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Configure synchronous execution for deterministic testing
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
    }

    /**
     * TEST 1: Constraints Testing
     * Verifies that work with unmet constraints remains ENQUEUED,
     * and only executes to SUCCEEDED after [TestDriver.setAllConstraintsMet] is called.
     */
    @Test
    fun testOneTimeWorkWithConstraints_whenConstraintsMet_succeeds() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(true)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncLogsWorker>()
            .setConstraints(constraints)
            .build()

        // Enqueue the constrained work
        workManager.enqueue(request).result.get()

        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)
        assertNotNull("TestDriver must be available", testDriver)

        // 1. Initial State: Work must be ENQUEUED (constraints not yet met)
        var workInfo = workManager.getWorkInfoById(request.id).get()
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)

        // 2. Simulate hardware constraints being satisfied
        testDriver?.setAllConstraintsMet(request.id)

        // 3. Work executed synchronously to completion
        workInfo = workManager.getWorkInfoById(request.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)
    }

    /**
     * TEST 2: Input / Output Data Contract
     * Verifies that ImageCompressWorker reads input parameters and returns
     * output size metadata compliant with the 10KB Data limit.
     */
    @Test
    fun testImageCompressWorker_readsInput_outputsValidFileUri() {
        val inputData = workDataOf(
            WorkerConstants.KEY_IMAGE_URI to "file:///data/media/photo_sample.jpg",
            WorkerConstants.KEY_COMPRESS_QUALITY to 85
        )

        val request = OneTimeWorkRequestBuilder<ImageCompressWorker>()
            .setInputData(inputData)
            .build()

        workManager.enqueue(request).result.get()

        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

        // Assert output data contract
        val outputUri = workInfo.outputData.getString(WorkerConstants.KEY_OUTPUT_URI)
        val outputSize = workInfo.outputData.getLong(WorkerConstants.KEY_OUTPUT_SIZE_BYTES, 0L)
        val compressionRatio = workInfo.outputData.getString(WorkerConstants.KEY_COMPRESSION_RATIO)

        assertNotNull("Output URI must not be null", outputUri)
        assertTrue("Output size must be greater than zero", outputSize > 0)
        assertNotNull("Compression ratio must not be null", compressionRatio)
    }

    /**
     * TEST 3: Periodic Work & Self-Rescheduling
     * Verifies that when a PeriodicWorkRequest finishes, WorkManager reschedules
     * it back to ENQUEUED rather than terminating in SUCCEEDED.
     */
    @Test
    fun testPeriodicWork_whenPeriodDelayMet_reschedulesToEnqueued() {
        val periodicRequest = PeriodicWorkRequestBuilder<DailyCleanupWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        ).build()

        workManager.enqueue(periodicRequest).result.get()

        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)
        assertNotNull(testDriver)

        // Simulate the 15-minute period delay elapsing
        testDriver?.setPeriodDelayMet(periodicRequest.id)

        val workInfo = workManager.getWorkInfoById(periodicRequest.id).get()

        // Critical Assertion: Periodic work must remain in ENQUEUED, ready for next period!
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
    }

    /**
     * TEST 4: Failure Propagation in Chained Pipelines
     * Verifies that if an upstream parent worker fails, downstream dependent workers
     * immediately transition to FAILED without executing.
     */
    @Test
    fun testChainedPipeline_whenParentFails_downstreamIsMarkedFailed() {
        // Upstream worker with simulated failure ON
        val downloadFailedRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(WorkerConstants.KEY_SIMULATE_FAILURE to true))
            .build()

        // Downstream worker that depends on download
        val combineRequest = OneTimeWorkRequestBuilder<CombineAndUploadWorker>()
            .build()

        // Chain: Download (FAILS) -> Combine
        workManager.beginWith(downloadFailedRequest)
            .then(combineRequest)
            .enqueue()
            .result
            .get()

        val parentInfo = workManager.getWorkInfoById(downloadFailedRequest.id).get()
        val childInfo = workManager.getWorkInfoById(combineRequest.id).get()

        // Parent must be FAILED
        assertEquals(WorkInfo.State.FAILED, parentInfo.state)

        // Downstream dependent must be FAILED via Failure Propagation!
        assertEquals(WorkInfo.State.FAILED, childInfo.state)
    }
}
