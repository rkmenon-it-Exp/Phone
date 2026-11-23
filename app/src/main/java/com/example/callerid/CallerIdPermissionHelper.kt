package com.example.callerid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

object CallerIdPermissionHelper {

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val target = ComponentName(context, PulseNotificationListenerService::class.java).flattenToString()
        val shortTarget = ComponentName(context, PulseNotificationListenerService::class.java).flattenToShortString()
        return enabledListeners.split(":").any { entry ->
            entry.equals(target, ignoreCase = true) ||
                entry.equals(shortTarget, ignoreCase = true) ||
                entry.contains(context.packageName)
        }
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val target = ComponentName(context, PulseSocialLookupAccessibilityService::class.java).flattenToString()
        val shortTarget = ComponentName(context, PulseSocialLookupAccessibilityService::class.java).flattenToShortString()
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val component = splitter.next()
            if (component.equals(target, ignoreCase = true) || component.equals(shortTarget, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
