package com.example.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.preferences.DriveBackupFrequency
import com.example.data.preferences.PulsePhonePreferences
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PulseWorkScheduler(private val context: Context) {

    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    fun syncSchedules(prefs: PulsePhonePreferences) {
        syncRetentionWork(prefs.autoDeleteOldCallLogs)
        syncDriveBackupWork(
            enabled = prefs.driveAutoBackupEnabled && !prefs.googleAccountEmail.isNullOrBlank(),
            frequency = prefs.driveBackupFrequency,
            wifiOnly = prefs.driveBackupWifiOnly
        )
    }

    private fun syncRetentionWork(enabled: Boolean) {
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_RETENTION)
            return
        }
        val request = PeriodicWorkRequestBuilder<CallLogRetentionWorker>(24L, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_RETENTION,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun syncDriveBackupWork(
        enabled: Boolean,
        frequency: DriveBackupFrequency,
        wifiOnly: Boolean
    ) {
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_DRIVE_BACKUP)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val initialDelayMillis = computeDelayUntilNextNightWindow()
        val request = PeriodicWorkRequestBuilder<DriveAutoBackupWorker>(
            frequency.intervalHours,
            TimeUnit.HOURS
        )
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_DRIVE_BACKUP,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun computeDelayUntilNextNightWindow(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(TimeUnit.MINUTES.toMillis(15))
    }

    companion object {
        private const val WORK_RETENTION = "pulsephone_call_log_retention"
        private const val WORK_DRIVE_BACKUP = "pulsephone_drive_auto_backup"
    }
}
