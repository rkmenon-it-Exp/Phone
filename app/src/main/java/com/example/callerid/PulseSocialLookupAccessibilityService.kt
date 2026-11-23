package com.example.callerid

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.PulsePhoneApplication
import com.example.data.local.CallerIdCacheEntity
import com.example.data.local.CallerIdSource
import com.example.data.repository.T9SearchEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Optional AccessibilityService for on-demand/background public display name extraction
 * in WhatsApp / Telegram when an unknown number is opened for lookup.
 * Strictly gated by user's explicit Accessibility toggle in Smart Caller ID settings.
 */
class PulseSocialLookupAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val phoneRegex = Regex("""\+?[0-9][0-9\-\s()]{6,18}[0-9]""")

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        val source = when (pkg) {
            "com.whatsapp", "com.whatsapp.w4b" -> CallerIdSource.WHATSAPP
            "org.telegram.messenger", "org.telegram.messenger.web" -> CallerIdSource.TELEGRAM
            "com.facebook.orca" -> CallerIdSource.MESSENGER
            else -> return
        }

        val root = rootInActiveWindow ?: return
        val texts = mutableListOf<String>()
        collectVisibleTexts(root, texts, maxNodes = 40)

        val app = applicationContext as? PulsePhoneApplication ?: return
        serviceScope.launch {
            val prefs = app.container.preferencesRepository.getSnapshot()
            if (!prefs.findNameCallerEnabled || !prefs.accessibilitySocialLookupEnabled) return@launch

            val pendingNumber = pendingLookupNumber
            val detectedPhone = pendingNumber ?: texts.firstNotNullOfOrNull { phoneRegex.find(it)?.value?.trim() }
                ?: return@launch
            val normalized = T9SearchEngine.normalizePhoneNumber(detectedPhone)
            if (normalized.filter { it.isDigit() }.length < 7) return@launch

            // Ignore if already in local contacts
            if (app.container.database.contactDao().findByNormalizedNumber(normalized) != null) return@launch

            // Look for WhatsApp/Telegram public push name ("~Name" or header name distinct from phone)
            val candidateName = texts
                .map { it.trim() }
                .firstOrNull { line ->
                    line.length in 2..48 &&
                        (line.startsWith("~") || (!phoneRegex.containsMatchIn(line) && !isGenericUiLabel(line)))
                }
                ?.removePrefix("~")
                ?.trim()
                ?: return@launch

            app.container.database.callerIdCacheDao().upsertEntry(
                CallerIdCacheEntity(
                    normalizedNumber = normalized,
                    rawNumber = detectedPhone,
                    displayName = candidateName,
                    photoUri = null,
                    source = source.name,
                    updatedAt = System.currentTimeMillis()
                )
            )
            if (pendingLookupNumber != null) {
                pendingLookupNumber = null
            }
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun collectVisibleTexts(node: AccessibilityNodeInfo?, out: MutableList<String>, maxNodes: Int) {
        if (node == null || out.size >= maxNodes) return
        node.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        for (i in 0 until node.childCount) {
            if (out.size >= maxNodes) break
            collectVisibleTexts(node.getChild(i), out, maxNodes)
        }
    }

    private fun isGenericUiLabel(text: String): Boolean {
        val lower = text.lowercase()
        return lower in setOf(
            "message", "call", "video", "search", "online", "typing...",
            "today", "yesterday", "whatsapp", "telegram", "messenger", "mute", "info"
        )
    }

    companion object {
        @Volatile
        var pendingLookupNumber: String? = null
    }
}
