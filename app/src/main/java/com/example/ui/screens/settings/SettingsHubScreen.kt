package com.example.ui.screens.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.preferences.PulsePhonePreferences
import com.example.telecom.PulseCallActivity
import com.example.ui.theme.CallAnswerEmerald
import com.example.ui.theme.CallFavoriteAmber
import com.example.ui.theme.CallMissedCrimson
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.Sim1OceanBlue
import com.example.ui.theme.Sim2DeepTeal
import com.example.ui.viewmodel.PulsePhoneViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(
    prefs: PulsePhonePreferences,
    viewModel: PulsePhoneViewModel,
    isDefaultDialer: Boolean,
    showCallerIdWizard: Boolean,
    onRequestDefaultDialer: () -> Unit,
    onSignInGoogle: (Activity) -> Unit,
    onLaunchSafAction: (SafTransferAction) -> Unit,
    onBack: () -> Unit
) {
    var activeSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    val context = LocalContext.current

    BackHandler {
        if (activeSection != null) {
            activeSection = null
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(activeSection?.title ?: "Settings & Customization")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (activeSection != null) activeSection = null else onBack()
                        },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        }
    ) { innerPadding ->
        val section = activeSection
        if (section == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                // Default Dialer Role Card
                item(key = "default_dialer_card") {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDefaultDialer) {
                                CallAnswerEmerald.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhoneAndroid,
                                contentDescription = null,
                                tint = if (isDefaultDialer) CallAnswerEmerald else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isDefaultDialer) "PulsePhone is Default Dialer" else "Set as Default Phone App",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (isDefaultDialer) {
                                        "Custom OLED InCallService & full call control active."
                                    } else {
                                        "Enable ROLE_DIALER to unlock custom OLED full-screen call handling."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!isDefaultDialer) {
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = onRequestDefaultDialer,
                                    modifier = Modifier.testTag("settings_set_default_dialer_button")
                                ) {
                                    Text("Enable")
                                }
                            }
                        }
                    }
                }

                // Preset Quick Switchers
                item(key = "presets_row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.applyRichPreset() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preset_rich_button")
                        ) {
                            Text("Rich True Phone")
                        }
                        OutlinedButton(
                            onClick = { viewModel.applyMinimalPreset() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preset_minimal_button")
                        ) {
                            Text("Minimalist")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                // 9 Categorized Customization Modules
                item(key = "cat_appearance") {
                    SettingsCategoryRow(
                        icon = Icons.Filled.Palette,
                        iconTint = IndigoPrimary,
                        title = SettingsSection.APPEARANCE.title,
                        subtitle = SettingsSection.APPEARANCE.subtitle,
                        badge = prefs.accentPalette.label.substringBefore(" "),
                        onClick = { activeSection = SettingsSection.APPEARANCE },
                        modifier = Modifier.testTag("settings_cat_appearance")
                    )
                }
                item(key = "cat_icon_styles") {
                    SettingsCategoryRow(
                        icon = Icons.Filled.AutoAwesome,
                        iconTint = Sim2DeepTeal,
                        title = SettingsSection.ICON_STYLES.title,
                        subtitle = SettingsSection.ICON_STYLES.subtitle,
                        badge = prefs.iconStyleGroup.label.substringBefore(" "),
                        onClick = { activeSection = SettingsSection.ICON_STYLES },
                        modifier = Modifier.testTag("settings_cat_icon_styles")
                    )
                }
                item(key = "cat_tabs") {
                    SettingsCategoryRow(
                        icon = Icons.Filled.Tab,
                        iconTint = Sim1OceanBlue,
                        title = SettingsSection.TABS.title,
                        subtitle = SettingsSection.TABS.subtitle,
                        badge = "${prefs.activeOrderedTabs.size}/5 ON",
                        onClick = { activeSection = SettingsSection.TABS },
                        modifier = Modifier.testTag("settings_cat_tabs")
                    )
                }
                item(key = "cat_dialpad") {
                    SettingsCategoryRow(
                        icon = Icons.Filled.Dialpad,
                        iconTint = Sim2DeepTeal,
                        title = SettingsSection.DIALPAD.title,
                        subtitle = SettingsSection.DIALPAD.subtitle,
                        badge = prefs.simCallingMode.label.substringBefore(" "),
                        onClick = { activeSection = SettingsSection.DIALPAD },
                        modifier = Modifier.testTag("settings_cat_dialpad")
                    )
                }
                item(key = "cat_calllog") {
                    SettingsCategoryRow(
                        icon = Icons.Filled.History,
                        iconTint = Sim1OceanBlue,
                        title = SettingsSection.CALL_LOG_DISPLAY.title,
                        subtitle = SettingsSection.CALL_LOG_DISPLAY.subtitle,
                        onClick = { activeSection = SettingsSection.CALL_LOG_DISPLAY },
                        modifier = Modifier.testTag("settings_cat_calllog")
                    )
                }
                item(key = "cat_retention") {
                    SettingsCategoryRow(
                        icon = Icons.Filled.AutoDelete,
                        iconTint = CallMissedCrimson,
                        title = SettingsSection.RETENTION.title,
                        subtitle = SettingsSection.RETENTION.subtitle,
                        badge = if (prefs.autoDeleteOldCallLogs) prefs.callLogRetentionPeriod.label else "Forever",
                        onClick = { activeSection = SettingsSection.RETENTION },
                        modifier = Modifier.testTag("settings_cat_retention")
                    )
                }
                item(key = "cat_favorites") {
                    SettingsCategoryRow(
                        icon = Icons.Filled.Star,
                        iconTint = CallFavoriteAmber,
                        title = SettingsSection.FAVORITES.title,
                        subtitle = SettingsSection.FAVORITES.subtitle,
                        onClick = { activeSection = SettingsSection.FAVORITES },
                        modifier = Modifier.testTag("settings_cat_favorites")
                    )
                }
                item(key = "cat_incall") {
                    SettingsCategoryRow(
                        icon = Icons.Filled.Call,
                        iconTint = CallAnswerEmerald,
                        title = SettingsSection.INCALL.title,
                        subtitle = SettingsSection.INCALL.subtitle,
                        badge = if (prefs.customFullScreenCallerId) "OLED ON" else "System",
                        onClick = { activeSection = SettingsSection.INCALL },
                        modifier = Modifier.testTag("settings_cat_incall")
                    )
                }
                item(key = "cat_smart_caller_id") {
                    SettingsCategoryRow(
                        icon = Icons.Filled.PersonSearch,
                        iconTint = Sim2DeepTeal,
                        title = SettingsSection.SMART_CALLER_ID.title,
                        subtitle = SettingsSection.SMART_CALLER_ID.subtitle,
                        badge = if (prefs.findNameCallerEnabled) "ON" else "OFF",
                        onClick = { activeSection = SettingsSection.SMART_CALLER_ID },
                        modifier = Modifier.testTag("settings_cat_smart_caller_id")
                    )
                }
                item(key = "cat_sync") {
                    SettingsCategoryRow(
                        icon = Icons.Filled.CloudSync,
                        iconTint = Sim1OceanBlue,
                        title = SettingsSection.SYNC_BACKUP.title,
                        subtitle = SettingsSection.SYNC_BACKUP.subtitle,
                        badge = if (prefs.googleAccountEmail != null) "Connected" else "Local",
                        onClick = { activeSection = SettingsSection.SYNC_BACKUP },
                        modifier = Modifier.testTag("settings_cat_sync")
                    )
                }
            }
        } else {
            val contentMod = Modifier
                .fillMaxSize()
                .padding(innerPadding)

            when (section) {
                SettingsSection.APPEARANCE -> ThemeGallerySettingsContent(prefs, viewModel, contentMod)
                SettingsSection.ICON_STYLES -> IconStylesSettingsContent(prefs, viewModel, contentMod)
                SettingsSection.TABS -> TabsSettingsContent(prefs, viewModel, contentMod)
                SettingsSection.DIALPAD -> DialpadSettingsContent(prefs, viewModel, contentMod)
                SettingsSection.CALL_LOG_DISPLAY -> CallLogDisplaySettingsContent(prefs, viewModel, contentMod)
                SettingsSection.RETENTION -> RetentionSettingsContent(prefs, viewModel, contentMod)
                SettingsSection.FAVORITES -> FavoritesSettingsContent(prefs, viewModel, contentMod)
                SettingsSection.INCALL -> InCallSettingsContent(
                    prefs = prefs,
                    viewModel = viewModel,
                    onPreviewCall = { incoming ->
                        viewModel.prepareInCallPreview(incoming)
                        context.startActivity(Intent(context, PulseCallActivity::class.java))
                    },
                    modifier = contentMod
                )
                SettingsSection.SMART_CALLER_ID -> SmartCallerIdSettingsContent(
                    prefs = prefs,
                    viewModel = viewModel,
                    showWizard = showCallerIdWizard,
                    modifier = contentMod
                )
                SettingsSection.SYNC_BACKUP -> SyncBackupSettingsContent(
                    prefs = prefs,
                    viewModel = viewModel,
                    onSignInGoogle = onSignInGoogle,
                    onLaunchSafAction = onLaunchSafAction,
                    modifier = contentMod
                )
            }
        }
    }
}
