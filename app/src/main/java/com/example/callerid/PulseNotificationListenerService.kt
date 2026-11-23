package com.example.callerid

import android.app.Notification
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.PulsePhoneApplication
import com.example.data.local.CallerIdCacheEntity
import com.example.data.local.CallerIdSource
import com.example.data.repository.T9SearchEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Captures sender display names and profile avatars from incoming WhatsApp, Telegram,
 * and Messenger notifications for unsaved phone numbers when Find Name Caller is enabled.
 */
class PulseNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val supportedPackages = mapOf(
        "com.whatsapp" to CallerIdSource.WHATSAPP,
        "com.whatsapp.w4b" to CallerIdSource.WHATSAPP,
        "org.telegram.messenger" to CallerIdSource.TELEGRAM,
        "org.telegram.messenger.web" to CallerIdSource.TELEGRAM,
        "com.facebook.orca" to CallerIdSource.MESSENGER
    )

    private val phoneRegex = Regex("""\+?[0-9][0-9\-\s()]{6,18}[0-9]""")

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val source = supportedPackages[sbn.packageName] ?: return

        val app = applicationContext as? PulsePhoneApplication ?: return
        serviceScope.launch {
            val prefs = app.container.preferencesRepository.getSnapshot()
            if (!prefs.findNameCallerEnabled || !prefs.notificationCaptureEnabled) return@launch

            val extras = notification.extras ?: return@launch
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()

            // Try to extract messaging style Person name + phone number patterns
            val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
            val senderNameFromStyle = messagingStyle?.messages?.lastOrNull()?.person?.name?.toString()?.trim()

            // Pattern 1: WhatsApp unknown sender often formats title as "+1 555-0199" with "~John Doe" or vice-versa
            val rawCandidates = listOf(title, subText, senderNameFromStyle.orEmpty(), text)
            val detectedPhone = rawCandidates.firstNotNullOfOrNull { candidate ->
                phoneRegex.find(candidate)?.value?.trim()
            } ?: return@launch

            val normalized = T9SearchEngine.normalizePhoneNumber(detectedPhone)
            if (normalized.filter { it.isDigit() }.length < 7) return@launch

            // Skip if already saved in contacts
            if (app.container.database.contactDao().findByNormalizedNumber(normalized) != null) return@launch

            // Extract human-readable display name distinct from the raw phone number
            val candidateName = listOfNotNull(
                senderNameFromStyle?.removePrefix("~")?.trim(),
                title.removePrefix("~").trim(),
                subText.removePrefix("~").trim()
            ).firstOrNull { candidate ->
                candidate.isNotBlank() &&
                    !phoneRegex.matches(candidate) &&
                    T9SearchEngine.normalizePhoneNumber(candidate) != normalized
            } ?: return@launch

            val savedPhotoPath = extractAndSaveAvatar(notification, normalized)

            app.container.database.callerIdCacheDao().upsertEntry(
                CallerIdCacheEntity(
                    normalizedNumber = normalized,
                    rawNumber = detectedPhone,
                    displayName = candidateName,
                    photoUri = savedPhotoPath,
                    source = source.name,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun extractAndSaveAvatar(notification: Notification, normalizedKey: String): String? {
        return runCatching {
            val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val largeIcon: Icon? = notification.getLargeIcon()
                val drawable = largeIcon?.loadDrawable(this)
                (drawable as? BitmapDrawable)?.bitmap
            } else {
                @Suppress("DEPRECATION")
                notification.largeIcon
            }
            if (bitmap == null) return null

            val avatarDir = File(filesDir, "caller_avatars").apply { mkdirs() }
            val safeName = normalizedKey.filter { it.isDigit() }.takeLast(12).ifBlank { "unknown" }
            val file = File(avatarDir, "avatar_$safeName.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            file.toURI().toString()
        }.getOrNull()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
