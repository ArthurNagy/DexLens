package com.arthurnagy.dexlens.core.data.content

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arthurnagy.dexlens.core.data.R
import com.arthurnagy.dexlens.core.domain.DomainError
import com.github.michaelbull.result.getError
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import timber.log.Timber

/** Runs the download as a foreground task so it survives leaving the app. A lost connection retries later. */
@HiltWorker
internal class ContentPackWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val downloader: ContentPackDownloader,
    ) : CoroutineWorker(context, params) {
        @OptIn(FlowPreview::class)
        override suspend fun doWork(): Result {
            val promoted = promoteToForeground()
            val progress = MutableStateFlow(ContentProgress(0, 0))
            val failure = coroutineScope {
                val reporter = launch { progress.sample(PROGRESS_INTERVAL_MS).collectLatest { publish(it, promoted) } }
                val outcome = downloader.download(progress).getError()
                reporter.cancel()
                outcome
            }
            publish(progress.value, promoted)
            return when (failure) {
                null -> Result.success()
                is DomainError.Network -> Result.retry()
                else -> Result.failure()
            }
        }

        override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(ContentProgress(0, 0))

        /** Returns false when Android refuses a foreground start from the background: the work still runs, without a notification. */
        private suspend fun promoteToForeground(): Boolean = try {
            setForeground(getForegroundInfo())
            true
        } catch (e: IllegalStateException) {
            Timber.w(e, "could not start the download in the foreground")
            false
        }

        private suspend fun publish(
            progress: ContentProgress,
            notify: Boolean,
        ) {
            setProgress(workDataOf(KEY_COMPLETED to progress.completed, KEY_TOTAL to progress.total))
            if (notify) notificationManager().notify(NOTIFICATION_ID, notification(progress))
        }

        private fun notificationManager() = applicationContext.getSystemService(NotificationManager::class.java)

        private fun foregroundInfo(progress: ContentProgress) =
            ForegroundInfo(NOTIFICATION_ID, notification(progress), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

        private fun notification(progress: ContentProgress): Notification {
            notificationManager().createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.core_data_download_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
            return NotificationCompat
                .Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(applicationContext.getString(R.string.core_data_download_title))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(progress.total, progress.completed, progress.total == 0)
                .build()
        }

        companion object {
            const val KEY_COMPLETED = "completed"
            const val KEY_TOTAL = "total"
            private const val CHANNEL_ID = "content-download"
            private const val NOTIFICATION_ID = 7301
            private const val PROGRESS_INTERVAL_MS = 500L
        }
    }
