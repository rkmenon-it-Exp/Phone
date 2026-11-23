package com.example.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.callerid.CallerIdPermissionHelper
import com.example.data.preferences.CallerPhotoStyle
import com.example.data.preferences.DriveBackupFrequency
import com.example.data.preferences.IncomingAnswerStyle
import com.example.data.preferences.PulsePhonePreferences
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.CallAnswerEmerald
import com.example.ui.theme.CallFavoriteAmber
import com.example.ui.theme.Sim1OceanBlue
import com.example.ui.theme.Sim2DeepTeal
import com.example.ui.viewmodel.PulsePhoneViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SafTransferAction {
    EXPORT_SETTINGS_JSON,
    IMPORT_SETTINGS_JSON,
    EXPORT_LOGS_JSON,
    EXPORT_LOGS_CSV,
    IMPORT_LOGS,
    EXPORT_CONTACTS_JSON,
    EXPORT_CONTACTS_CSV,
    IMPORT_CONTACTS
}

@Composable
fun InCallSettingsContent(
    prefs: PulsePhonePreferences,
    viewModel: PulsePhoneViewModel,
    onPreviewCall: (incoming: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 28.dp)) {
        item { SettingsSectionHeader("Full-Screen OLED Caller ID") }
        item {
            SettingsSwitchRow(
                title = "Custom Full-Screen Caller ID",
                subtitle = "Render PulsePhone's #000000 OLED call screen (falls back to system UI when OFF)",
                checked = prefs.customFullScreenCallerId,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(customFullScreenCallerId = v) } }
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = { onPreviewCall(true) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preview_incoming_call_button")
                ) {
                    Icon(Icons.Filled.RingVolume, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Preview Incoming")
                }
                OutlinedButton(
                    onClick = { onPreviewCall(false) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preview_active_call_button")
                ) {
                    Icon(Icons.Filled.Call, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Preview Active")
                }
            }
        }

        item { SettingsSectionHeader("Caller Photo Style") }
        item {
            SettingsRadioGroup(
                options = CallerPhotoStyle.entries,
                selected = prefs.callerPhotoStyle,
                labelProvider = { it.label },
                onSelect = { style -> viewModel.updatePreferences { it.copy(callerPhotoStyle = style) } }
            )
        }

        item { SettingsSectionHeader("Incoming Call Answer Style") }
        item {
            SettingsRadioGroup(
                options = IncomingAnswerStyle.entries,
                selected = prefs.incomingAnswerStyle,
                labelProvider = { it.label },
                onSelect = { style -> viewModel.updatePreferences { it.copy(incomingAnswerStyle = style) } }
            )
        }

        item { SettingsSectionHeader("In-Call 2x4 Action Grid Customizer") }
        item {
            Text(
                text = "Toggle visibility and reorder the 8 active-call buttons.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        items(prefs.inCallActionOrder.size, key = { prefs.inCallActionOrder[it].name }) { idx ->
            val action = prefs.inCallActionOrder[idx]
            ReorderableToggleRow(
                label = action.label,
                checked = action in prefs.enabledInCallActions,
                canMoveUp = idx > 0,
                canMoveDown = idx < prefs.inCallActionOrder.lastIndex,
                onCheckedChange = { enabled -> viewModel.toggleInCallAction(action, enabled) },
                onMoveUp = { viewModel.moveInCallAction(action, moveUp = true) },
                onMoveDown = { viewModel.moveInCallAction(action, moveUp = false) }
            )
        }

        item { SettingsSectionHeader("Hardware Sensory Alerts") }
        item {
            SettingsSwitchRow(
                title = "Flashlight Alert on Incoming Call",
                subtitle = "Strobe rear camera torch while ringing",
                checked = prefs.flashlightAlertOnIncoming,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(flashlightAlertOnIncoming = v) } }
            )
        }
        item {
            SettingsSwitchRow(
                title = "Vibrate on Call Connect / Disconnect",
                subtitle = "Short tactile feedback when call connects or ends",
                checked = prefs.vibrateOnConnectDisconnect,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(vibrateOnConnectDisconnect = v) } }
            )
        }
    }
}

@Composable
fun SmartCallerIdSettingsContent(
    prefs: PulsePhonePreferences,
    viewModel: PulsePhoneViewModel,
    showWizard: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var notifAccessGranted by remember { mutableStateOf(CallerIdPermissionHelper.isNotificationListenerEnabled(context)) }
    var accessibilityGranted by remember { mutableStateOf(CallerIdPermissionHelper.isAccessibilityServiceEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifAccessGranted = CallerIdPermissionHelper.isNotificationListenerEnabled(context)
                accessibilityGranted = CallerIdPermissionHelper.isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 28.dp)) {
        item { SettingsSectionHeader("Master Control") }
        item {
            SettingsSwitchRow(
                title = "Find Name Caller (Smart Caller ID)",
                subtitle = "Automatically identify unsaved numbers via social notifications, optional social lookup & custom API",
                checked = prefs.findNameCallerEnabled,
                onCheckedChange = { enabled -> viewModel.toggleFindNameCaller(enabled) },
                testTag = "smart_caller_id_master_toggle"
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                FilledTonalButton(
                    onClick = { viewModel.openCallerIdWizard() },
                    modifier = Modifier.testTag("open_caller_id_wizard_button")
                ) {
                    Text("Open Permission Setup Wizard")
                }
            }
        }

        item { SettingsSectionHeader("Social Auto-Finder Sources") }
        item {
            SettingsSwitchRow(
                title = "Notification Access Capture",
                subtitle = if (notifAccessGranted) {
                    "Granted • Captures unsaved sender names & avatars from WhatsApp, Telegram & Messenger"
                } else {
                    "Notification Access not enabled yet — tap Setup below"
                },
                checked = prefs.notificationCaptureEnabled,
                enabled = prefs.findNameCallerEnabled,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(notificationCaptureEnabled = v) } }
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (notifAccessGranted) "Notification Listener Active" else "Permission Required",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (notifAccessGranted) CallAnswerEmerald else CallFavoriteAmber
                )
                OutlinedButton(
                    onClick = { CallerIdPermissionHelper.openNotificationListenerSettings(context) },
                    modifier = Modifier.testTag("grant_notification_access_button")
                ) {
                    Icon(Icons.Filled.NotificationsActive, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (notifAccessGranted) "Manage" else "Grant Access")
                }
            }
        }

        item {
            SettingsSwitchRow(
                title = "Accessibility Service Lookup (Optional)",
                subtitle = if (accessibilityGranted) {
                    "Granted • Enables on-demand public display name lookup in WhatsApp & Telegram"
                } else {
                    "Optional • Allow automated public profile lookups for unknown callers"
                },
                checked = prefs.accessibilitySocialLookupEnabled,
                enabled = prefs.findNameCallerEnabled,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(accessibilitySocialLookupEnabled = v) } }
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (accessibilityGranted) "Accessibility Service Active" else "Optional Permission Off",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (accessibilityGranted) CallAnswerEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { CallerIdPermissionHelper.openAccessibilitySettings(context) },
                    modifier = Modifier.testTag("grant_accessibility_button")
                ) {
                    Icon(Icons.Filled.AccessibilityNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (accessibilityGranted) "Manage" else "Configure")
                }
            }
        }

        item { SettingsSectionHeader("Custom Caller ID REST API") }
        item {
            SettingsSwitchRow(
                title = "Custom Caller ID API Fallback",
                subtitle = "Query your self-hosted or external REST endpoint (?number=...) if local social lookup finds nothing",
                checked = prefs.customCallerIdApiEnabled,
                enabled = prefs.findNameCallerEnabled,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(customCallerIdApiEnabled = v) } }
            )
        }
        if (prefs.customCallerIdApiEnabled) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = prefs.customCallerIdApiUrl,
                        onValueChange = { url -> viewModel.updatePreferences { it.copy(customCallerIdApiUrl = url) } },
                        label = { Text("REST API Endpoint URL (https://...)") },
                        placeholder = { Text("https://api.example.com/callerid") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_caller_id_url_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prefs.customCallerIdApiToken,
                        onValueChange = { tok -> viewModel.updatePreferences { it.copy(customCallerIdApiToken = tok) } },
                        label = { Text("Optional Bearer Token") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item { SettingsSectionHeader("Call Log & In-Call Indicators") }
        item {
            SettingsSwitchRow(
                title = "\"Not in Contacts\" Indicator",
                subtitle = "Show a subtle muted tag under unsaved numbers in Call Log & In-Call screen",
                checked = prefs.showNotInContactsIndicator,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(showNotInContactsIndicator = v) } }
            )
        }
        item {
            SettingsSwitchRow(
                title = "One-Tap Save to Google Contacts",
                subtitle = "Display a Save icon next to identified unknown callers to save Name, Number & Photo in one click",
                checked = prefs.showOneTapSaveToGoogleContacts,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(showOneTapSaveToGoogleContacts = v) } }
            )
        }
    }

    if (showWizard) {
        SmartCallerIdPermissionWizardDialog(
            notifGranted = notifAccessGranted,
            accessibilityGranted = accessibilityGranted,
            onOpenNotifSettings = { CallerIdPermissionHelper.openNotificationListenerSettings(context) },
            onOpenAccessibilitySettings = { CallerIdPermissionHelper.openAccessibilitySettings(context) },
            onDismiss = { viewModel.dismissCallerIdWizard() }
        )
    }
}

@Composable
private fun SmartCallerIdPermissionWizardDialog(
    notifGranted: Boolean,
    accessibilityGranted: Boolean,
    onOpenNotifSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Smart Caller ID Setup Wizard") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Grant the permissions below so PulsePhone can identify unknown callers automatically:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (notifGranted) Icons.Filled.CheckCircle else Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = if (notifGranted) CallAnswerEmerald else Sim1OceanBlue
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("1. Notification Access (Recommended)", style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Captures sender names & profile photos from WhatsApp, Telegram, and Messenger notifications for unsaved numbers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onOpenNotifSettings) {
                            Text(if (notifGranted) "Granted • Manage" else "Grant Notification Access")
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (accessibilityGranted) Icons.Filled.CheckCircle else Icons.Filled.AccessibilityNew,
                                contentDescription = null,
                                tint = if (accessibilityGranted) CallAnswerEmerald else Sim2DeepTeal
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("2. Accessibility Service (Optional)", style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Optional: Enables on-demand public display name extraction when inspecting unknown numbers in WhatsApp or Telegram.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onOpenAccessibilitySettings) {
                            Text(if (accessibilityGranted) "Enabled • Manage" else "Enable Accessibility")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun SyncBackupSettingsContent(
    prefs: PulsePhonePreferences,
    viewModel: PulsePhoneViewModel,
    onSignInGoogle: (Activity) -> Unit,
    onLaunchSafAction: (SafTransferAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 28.dp)) {
        item { SettingsSectionHeader("Google Account (One-Tap Credential Manager)") }
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (prefs.googleAccountEmail != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ContactAvatar(
                                name = prefs.googleAccountName ?: prefs.googleAccountEmail,
                                photoUri = prefs.googleAccountPhotoUrl,
                                size = 48.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prefs.googleAccountName ?: "Google Account",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = prefs.googleAccountEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { viewModel.signOutGoogle() }) {
                                Text("Sign Out")
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccountCircle, null, modifier = Modifier.size(40.dp), tint = Sim1OceanBlue)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sign in with Google", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Pick any device Gmail account with one tap for People Sync & Drive AppData Auto-Restore.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { activity?.let { onSignInGoogle(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_one_tap_sign_in_button")
                        ) {
                            Text("Sign in with Google")
                        }
                    }
                }
            }
        }

        item { SettingsSectionHeader("Google Contacts Two-Way Sync") }
        item {
            SettingsSwitchRow(
                title = "Google Contacts Two-Way Sync",
                subtitle = if (prefs.lastContactsSyncMillis > 0L) {
                    "Last synced: ${formatTimestamp(prefs.lastContactsSyncMillis)}"
                } else {
                    "Sync names, numbers, high-res photos & groups via Google People API"
                },
                checked = prefs.googleContactsTwoWaySync,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(googleContactsTwoWaySync = v) } }
            )
        }
        item {
            SettingsSwitchRow(
                title = "Wi-Fi Only Contacts Sync",
                subtitle = "Avoid cellular data usage during background sync",
                checked = prefs.googleContactsWifiOnly,
                enabled = prefs.googleContactsTwoWaySync,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(googleContactsWifiOnly = v) } }
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = { viewModel.syncGoogleContactsNow() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Sync, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sync Google Now")
                }
                OutlinedButton(
                    onClick = { viewModel.importFromDevice() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Import Device")
                }
            }
        }

        item { SettingsSectionHeader("Google Drive Auto-Backup (drive.appdata)") }
        item {
            SettingsSwitchRow(
                title = "Google Drive Auto-Backup",
                subtitle = if (prefs.lastDriveBackupMillis > 0L) {
                    "Last backup: ${formatTimestamp(prefs.lastDriveBackupMillis)}"
                } else {
                    "Silently back up unlimited Call Logs, Favorites & all DataStore Settings"
                },
                checked = prefs.driveAutoBackupEnabled,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(driveAutoBackupEnabled = v) } }
            )
        }
        item {
            SettingsDropdownCard(
                title = "Backup Frequency",
                selected = prefs.driveBackupFrequency,
                options = DriveBackupFrequency.entries,
                enabled = prefs.driveAutoBackupEnabled,
                labelProvider = { it.label },
                onSelect = { freq -> viewModel.updatePreferences { it.copy(driveBackupFrequency = freq) } }
            )
        }
        item {
            SettingsSwitchRow(
                title = "Wi-Fi Only Drive Backup",
                subtitle = "Run WorkManager cloud backups only on unmetered Wi-Fi",
                checked = prefs.driveBackupWifiOnly,
                enabled = prefs.driveAutoBackupEnabled,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(driveBackupWifiOnly = v) } }
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.backupToGoogleDriveNow() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("drive_backup_now_button")
                ) {
                    Icon(Icons.Filled.Backup, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Back up now")
                }
                OutlinedButton(
                    onClick = { viewModel.restoreFromGoogleDriveNow() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("drive_restore_now_button")
                ) {
                    Icon(Icons.Filled.CloudDownload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restore Drive")
                }
            }
        }

        item { SettingsSectionHeader("Local Import / Export (JSON & CSV)") }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("App Settings", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = { onLaunchSafAction(SafTransferAction.EXPORT_SETTINGS_JSON) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.UploadFile, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export JSON")
                    }
                    OutlinedButton(
                        onClick = { onLaunchSafAction(SafTransferAction.IMPORT_SETTINGS_JSON) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Import JSON") }
                }

                Spacer(Modifier.height(4.dp))
                Text("Unlimited Call Logs", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = { onLaunchSafAction(SafTransferAction.EXPORT_LOGS_JSON) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Export JSON") }
                    FilledTonalButton(
                        onClick = { onLaunchSafAction(SafTransferAction.EXPORT_LOGS_CSV) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Export CSV") }
                    OutlinedButton(
                        onClick = { onLaunchSafAction(SafTransferAction.IMPORT_LOGS) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Import") }
                }

                Spacer(Modifier.height(4.dp))
                Text("Contacts", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = { onLaunchSafAction(SafTransferAction.EXPORT_CONTACTS_JSON) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Export JSON") }
                    FilledTonalButton(
                        onClick = { onLaunchSafAction(SafTransferAction.EXPORT_CONTACTS_CSV) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Export CSV") }
                    OutlinedButton(
                        onClick = { onLaunchSafAction(SafTransferAction.IMPORT_CONTACTS) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Import") }
                }
            }
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    return SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))
}
