package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.PulsePhoneApplication
import java.util.concurrent.TimeUnit

class CallLogRetentionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? PulsePhoneApplication ?: return Result.success()
        val prefs = app.container.preferencesRepository.getSnapshot()
        if (!prefs.autoDeleteOldCallLogs) {
            // Keep call logs forever when toggle is OFF
            return Result.success()
        }
        val retentionMillis = TimeUnit.DAYS.toMillis(prefs.callLogRetentionPeriod.days)
        val cutoff = System.currentTimeMillis() - retentionMillis
        app.container.database.callLogDao().deleteLogsOlderThan(cutoff)
        return Result.success()
    }
}
