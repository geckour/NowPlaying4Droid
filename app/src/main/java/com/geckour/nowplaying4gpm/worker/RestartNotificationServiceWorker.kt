package com.geckour.nowplaying4gpm.worker

import android.content.Context
import android.content.Intent
import androidx.work.*
import com.geckour.nowplaying4gpm.service.NotificationService

class RestartNotificationServiceWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        context.startService(Intent(context, NotificationService::class.java))

        return Result.success()
    }

    companion object {
        private const val NAME = "RestartNotificationServiceWorker"

        fun start() {
            WorkManager.getInstance()
                .beginUniqueWork(
                    NAME,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<RestartNotificationServiceWorker>().build()
                ).enqueue()
        }
    }
}