package com.example.workmanagerdemo

/**
 * Central registry of all constants used across Workers and the UI.
 *
 * Keeping all keys, tags, and IDs in one place prevents typos, makes
 * refactoring safe, and serves as self-documenting API surface for
 * the input/output data contracts of each Worker.
 */
object WorkerConstants {

    // ── Worker Tags ──────────────────────────────────────────────────────────
    // Tags allow bulk cancellation (cancelAllWorkByTag) and grouped observation
    // (getWorkInfosByTagFlow). Every worker should be tagged for debuggability.

    const val TAG_SYNC_LOGS          = "tag_sync_logs"
    const val TAG_IMAGE_COMPRESS     = "tag_image_compress"
    const val TAG_DAILY_CLEANUP      = "tag_daily_cleanup"
    const val TAG_CRITICAL_ALERT     = "tag_critical_alert"
    const val TAG_DOWNLOAD           = "tag_download"
    const val TAG_FILTER             = "tag_filter"
    const val TAG_COMBINE_UPLOAD     = "tag_combine_upload"
    const val TAG_ANALYTICS_SYNC     = "tag_analytics_sync"
    const val TAG_VIDEO_EXPORT       = "tag_video_export"
    const val TAG_USER_SYNC          = "tag_user_sync"

    // ── Unique Work Names ────────────────────────────────────────────────────
    // Used with enqueueUniqueWork / enqueueUniquePeriodicWork to prevent
    // duplicate scheduling of the same logical background job.

    const val UNIQUE_WORK_ANALYTICS_SYNC  = "unique_analytics_sync"
    const val UNIQUE_PERIODIC_CLEANUP     = "unique_periodic_cleanup"
    const val UNIQUE_IMAGE_COMPRESS       = "unique_image_compress"
    const val UNIQUE_CHAINED_PIPELINE     = "unique_chained_pipeline"
    const val UNIQUE_VIDEO_EXPORT         = "unique_video_export"
    const val UNIQUE_USER_SYNC            = "unique_user_sync"

    // ── Input / Output Data Keys ─────────────────────────────────────────────
    // These string keys form the Data contract between enqueuing code and
    // Worker implementations. Keep them namespaced to avoid collisions when
    // multiple workers share output keys in a chained pipeline.

    // ImageCompressWorker input keys
    const val KEY_IMAGE_URI           = "key_image_uri"
    const val KEY_COMPRESS_QUALITY    = "key_compress_quality"
    const val KEY_ORIGINAL_SIZE_BYTES = "key_original_size_bytes"

    // ImageCompressWorker output keys
    const val KEY_OUTPUT_URI          = "key_output_uri"
    const val KEY_OUTPUT_SIZE_BYTES   = "key_output_size_bytes"
    const val KEY_COMPRESSION_RATIO   = "key_compression_ratio"

    // DailyCleanupWorker output keys
    const val KEY_FILES_DELETED_COUNT = "key_files_deleted_count"
    const val KEY_BYTES_RECLAIMED     = "key_bytes_reclaimed"
    const val KEY_CLEANUP_TIMESTAMP   = "key_cleanup_timestamp"

    // DownloadWorker output key (passed into CombineAndUploadWorker)
    const val KEY_DOWNLOADED_FILE_URI = "key_downloaded_file_uri"
    const val KEY_SIMULATE_FAILURE    = "key_simulate_failure"

    // FilterWorker output key (passed into CombineAndUploadWorker)
    const val KEY_FILTERED_DATA       = "key_filtered_data"
    const val KEY_RECORDS_PROCESSED   = "key_records_processed"

    // CombineAndUploadWorker output key
    const val KEY_UPLOAD_URL          = "key_upload_url"

    // AnalyticsSyncWorker input keys
    const val KEY_ANALYTICS_BATCH_ID  = "key_analytics_batch_id"
    const val KEY_EVENTS_COUNT        = "key_events_count"
    const val KEY_ATTEMPT_COUNT       = "key_attempt_count"
    const val KEY_RETRY_BACKOFF_ACTIVE = "key_retry_backoff_active"

    // VideoExportWorker input keys
    const val KEY_VIDEO_TITLE         = "key_video_title"
    const val KEY_EXPORT_FORMAT       = "key_export_format"
    const val KEY_EXPORT_QUALITY      = "key_export_quality"
    const val KEY_OUTPUT_DURATION     = "key_output_duration"

    // CriticalAlertWorker input/output keys
    const val KEY_ALERT_TITLE         = "key_alert_title"
    const val KEY_ALERT_MESSAGE       = "key_alert_message"
    const val KEY_ALERT_SEVERITY      = "key_alert_severity"
    const val KEY_DISPATCH_TIMESTAMP  = "key_dispatch_timestamp"
    const val KEY_EXECUTION_LATENCY_MS = "key_execution_latency_ms"

    // UserRepositorySyncWorker keys
    const val KEY_SYNCED_USER_ID      = "key_synced_user_id"
    const val KEY_RECORDS_UPDATED     = "key_records_updated"

    // ── Progress Keys ────────────────────────────────────────────────────────
    // Used with setProgress() / setProgressAsync() inside running Workers,
    // and read via workInfo.progress in the Compose UI.

    const val PROGRESS_VALUE          = "progress_value"      // Int 0–100
    const val PROGRESS_MESSAGE        = "progress_message"    // Human-readable status

    // ── Notification IDs ─────────────────────────────────────────────────────
    // Must be unique integers per active notification. Notification IDs are
    // scoped per app, so they cannot conflict with system notifications.

    const val NOTIFICATION_ID_VIDEO_EXPORT    = 1001
    const val NOTIFICATION_ID_GENERAL_WORK   = 1002
    const val NOTIFICATION_ID_EXPEDITED_ALERT = 1003

    // ── Default Values ───────────────────────────────────────────────────────

    const val DEFAULT_COMPRESS_QUALITY   = 80       // JPEG quality 0–100
    const val DEFAULT_EXPORT_FORMAT      = "mp4"
    const val DEFAULT_EXPORT_QUALITY     = "720p"
}
