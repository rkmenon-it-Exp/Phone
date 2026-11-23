package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.PulsePhoneApplication

class DriveAutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? PulsePhoneApplication ?: return Result.success()
        val prefs = app.container.preferencesRepository.getSnapshot()
        if (!prefs.driveAutoBackupEnabled || prefs.googleAccountEmail.isNullOrBlank()) {
            return Result.success()
        }
        val outcome = app.container.googleCloudRepo.backupToGoogleDrive(respectWifiConstraint = true)
        return if (outcome.isSuccess) Result.success() else Result.retry()
    }
}
