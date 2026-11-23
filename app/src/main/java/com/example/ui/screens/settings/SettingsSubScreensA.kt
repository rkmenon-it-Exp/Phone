package com.example.ui.screens.settings

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
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.AccentPalette
import com.example.data.preferences.CallLogRetentionPeriod
import com.example.data.preferences.CallLogTapAction
import com.example.data.preferences.FavoritesDisplayMode
import com.example.data.preferences.PulsePhonePreferences
import com.example.data.preferences.SimCallingMode
import com.example.data.preferences.ThemeMode
import com.example.ui.viewmodel.PulsePhoneViewModel
import kotlin.math.roundToInt

@Composable
fun AppearanceSettingsContent(
    prefs: PulsePhonePreferences,
    viewModel: PulsePhoneViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 28.dp)) {
        item { SettingsSectionHeader("Theme Posture") }
        item {
            SettingsRadioGroup(
                options = ThemeMode.entries,
                selected = prefs.themeMode,
                labelProvider = { it.label },
                subtitleProvider = {
                    when (it) {
                        ThemeMode.SYSTEM -> "Follows Android light/dark system theme"
                        ThemeMode.LIGHT -> "Crisp Daylight light background"
                        ThemeMode.DARK -> "Deep charcoal low-glare dark mode"
                        ThemeMode.OLED_BLACK -> "True #000000 pitch black for AMOLED power savings"
                    }
                },
                onSelect = { mode -> viewModel.updatePreferences { it.copy(themeMode = mode) } }
            )
        }
        item { SettingsSectionHeader("Color System") }
        item {
            SettingsSwitchRow(
                title = "Material You Dynamic Color",
                subtitle = "Derive accent colors from your wallpaper (Android 12+)",
                checked = prefs.useDynamicColor,
                onCheckedChange = { enabled -> viewModel.updatePreferences { it.copy(useDynamicColor = enabled) } }
            )
        }
        item { SettingsSectionHeader("Accent Palette") }
        item {
            SettingsRadioGroup(
                options = AccentPalette.entries,
                selected = prefs.accentPalette,
                labelProvider = { it.label },
                onSelect = { pal -> viewModel.updatePreferences { it.copy(accentPalette = pal) } }
            )
        }
    }
}

@Composable
fun TabsSettingsContent(
    prefs: PulsePhonePreferences,
    viewModel: PulsePhoneViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 28.dp)) {
        item { SettingsSectionHeader("Active Tabs & Display Order") }
        item {
            Text(
                text = "Toggle individual tabs ON/OFF and reorder them with Up/Down arrows. At least one tab remains active.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        items(prefs.orderedTabs.size, key = { prefs.orderedTabs[it].name }) { index ->
            val tab = prefs.orderedTabs[index]
            ReorderableToggleRow(
                label = tab.label,
                checked = tab in prefs.enabledTabs,
                canMoveUp = index > 0,
                canMoveDown = index < prefs.orderedTabs.lastIndex,
                onCheckedChange = { enabled -> viewModel.toggleTabEnabled(tab, enabled) },
                onMoveUp = { viewModel.moveTabOrder(tab, moveUp = true) },
                onMoveDown = { viewModel.moveTabOrder(tab, moveUp = false) }
            )
        }
        item { SettingsSectionHeader("Default Startup Tab") }
        item {
            SettingsDropdownCard(
                title = "Open app on tab",
                selected = prefs.effectiveStartupTab,
                options = prefs.activeOrderedTabs,
                labelProvider = { it.label },
                onSelect = { tab -> viewModel.updatePreferences { it.copy(defaultStartupTab = tab) } }
            )
        }
    }
}

@Composable
fun DialpadSettingsContent(
    prefs: PulsePhonePreferences,
    viewModel: PulsePhoneViewModel,
    modifier: Modifier = Modifier
) {
    val simHardware by viewModel.simHardwareInfo.collectAsStateWithLifecycle()
    val effectiveDualSim = simHardware.isDualSimAvailable || prefs.forceShowDualSimPreview

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 28.dp)) {
        item { SettingsSectionHeader("Smart Search & Sensory Feedback") }
        item {
            SettingsSwitchRow(
                title = "T9 Smart Search",
                subtitle = "Predict contact names & numbers as you type digits",
                checked = prefs.t9SearchEnabled,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(t9SearchEnabled = v) } }
            )
        }
        item {
            SettingsSwitchRow(
                title = "Dialpad Vibration / Haptic Feedback",
                subtitle = "Tactile pulse on keypad presses",
                checked = prefs.dialpadVibrationEnabled,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(dialpadVibrationEnabled = v) } }
            )
        }
        item {
            SettingsSwitchRow(
                title = "Keypress DTMF Tones",
                subtitle = "Play classic keypad audio tones while dialing",
                checked = prefs.dialpadKeypressTonesEnabled,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(dialpadKeypressTonesEnabled = v) } }
            )
        }

        item { SettingsSectionHeader("Dialpad Geometry & Typography") }
        item {
            SettingsSliderRow(
                title = "Dialpad Height",
                value = prefs.dialpadHeightScale,
                valueRange = 0.75f..1.30f,
                valueLabel = "${(prefs.dialpadHeightScale * 100).roundToInt()}%",
                onValueChange = { v -> viewModel.updatePreferences { it.copy(dialpadHeightScale = v) } }
            )
        }
        item {
            SettingsSliderRow(
                title = "Button Size",
                value = prefs.dialpadButtonSizeScale,
                valueRange = 0.80f..1.25f,
                valueLabel = "${(prefs.dialpadButtonSizeScale * 100).roundToInt()}%",
                onValueChange = { v -> viewModel.updatePreferences { it.copy(dialpadButtonSizeScale = v) } }
            )
        }
        item {
            SettingsSliderRow(
                title = "Keypad Font Scale",
                value = prefs.dialpadFontScale,
                valueRange = 0.80f..1.35f,
                valueLabel = "${(prefs.dialpadFontScale * 100).roundToInt()}%",
                onValueChange = { v -> viewModel.updatePreferences { it.copy(dialpadFontScale = v) } }
            )
        }

        item { SettingsSectionHeader("Smart Dual-SIM Detection & Memory") }
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (effectiveDualSim) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.SimCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (simHardware.isDualSimAvailable) {
                                "Hardware Detected: Dual SIM (${simHardware.sim1Label} & ${simHardware.sim2Label})"
                            } else {
                                "Hardware Detected: Single SIM (SIM 2 auto-hidden)"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (effectiveDualSim) {
                                "Dual-SIM dial buttons and Long-Press Other-SIM menu are active."
                            } else {
                                "SIM selection UI and other-SIM options stay automatically hidden on single-SIM devices."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            SettingsSwitchRow(
                title = "Remember Last-Used SIM for Redial & Callback",
                subtitle = "Automatically dial back each contact using the same SIM used in their recent call log",
                checked = prefs.rememberLastUsedSim,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(rememberLastUsedSim = v) } },
                testTag = "settings_remember_last_sim_toggle"
            )
        }
        item {
            SettingsSwitchRow(
                title = "Force Show Dual-SIM UI (Preview)",
                subtitle = "Show Dual-SIM controls even when only 1 SIM card is installed",
                checked = prefs.forceShowDualSimPreview,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(forceShowDualSimPreview = v) } },
                testTag = "settings_force_dual_sim_preview_toggle"
            )
        }

        if (effectiveDualSim) {
            item { SettingsSectionHeader("SIM Calling Mode") }
            item {
                SettingsRadioGroup(
                    options = SimCallingMode.entries,
                    selected = prefs.simCallingMode,
                    labelProvider = { it.label },
                    subtitleProvider = { it.subtitle },
                    onSelect = { mode -> viewModel.updatePreferences { it.copy(simCallingMode = mode) } }
                )
            }
            if (prefs.simCallingMode == SimCallingMode.SINGLE_DEFAULT_SIM) {
                item {
                    SettingsDropdownCard(
                        title = "Preferred Default SIM",
                        selected = prefs.defaultSimSlot,
                        options = listOf(1, 2),
                        labelProvider = { if (it == 1) simHardware.sim1Label else simHardware.sim2Label },
                        onSelect = { slot -> viewModel.updatePreferences { it.copy(defaultSimSlot = slot) } }
                    )
                }
            }
        }

        item { SettingsSectionHeader("Auto-Redial") }
        item {
            SettingsSwitchRow(
                title = "Enable Auto-Redial",
                subtitle = "Automatically retry unanswered calls on disconnect",
                checked = prefs.autoRedialEnabled,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(autoRedialEnabled = v) } }
            )
        }
        if (prefs.autoRedialEnabled) {
            item {
                SettingsIntSliderRow(
                    title = "Redial Attempts",
                    value = prefs.autoRedialAttempts,
                    range = 1..20,
                    suffix = "times",
                    onValueChange = { count -> viewModel.updatePreferences { it.copy(autoRedialAttempts = count) } }
                )
            }
            item {
                SettingsIntSliderRow(
                    title = "Interval Between Redials",
                    value = prefs.autoRedialIntervalSeconds,
                    range = 5..60,
                    suffix = "sec",
                    onValueChange = { sec -> viewModel.updatePreferences { it.copy(autoRedialIntervalSeconds = sec) } }
                )
            }
        }
    }
}

@Composable
fun CallLogDisplaySettingsContent(
    prefs: PulsePhonePreferences,
    viewModel: PulsePhoneViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 28.dp)) {
        item { SettingsSectionHeader("Call Log Row Tap Action") }
        item {
            Text(
                text = "Choose what happens when you tap a recent call log entry. Long-pressing any row always opens the full context & Dual-SIM menu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        item {
            SettingsRadioGroup(
                options = CallLogTapAction.entries,
                selected = prefs.callLogTapAction,
                labelProvider = { it.label },
                subtitleProvider = { it.subtitle },
                onSelect = { action -> viewModel.updatePreferences { it.copy(callLogTapAction = action) } }
            )
        }

        item { SettingsSectionHeader("Individual Row Elements") }
        item {
            SettingsSwitchRow(
                title = "Contact Avatar",
                subtitle = "Show circular photo or initials monogram",
                checked = prefs.showLogAvatar,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(showLogAvatar = v) } }
            )
        }
        item {
            SettingsSwitchRow(
                title = "Call Count Badge",
                subtitle = "Show grouped call count next to name (e.g., (34))",
                checked = prefs.showCallCountBadge,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(showCallCountBadge = v) } }
            )
        }
        item {
            SettingsSwitchRow(
                title = "Call Direction Arrow",
                subtitle = "Incoming, outgoing, missed, and rejected icons",
                checked = prefs.showCallDirectionArrow,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(showCallDirectionArrow = v) } }
            )
        }
        item {
            SettingsSwitchRow(
                title = "SIM Badge (1 / 2)",
                subtitle = "Display color-coded SIM slot indicator",
                checked = prefs.showSimBadge,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(showSimBadge = v) } }
            )
        }
        item {
            SettingsSwitchRow(
                title = "Call Duration",
                subtitle = "Display elapsed talk time on each entry",
                checked = prefs.showCallDuration,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(showCallDuration = v) } }
            )
        }
        item {
            SettingsSwitchRow(
                title = "Relative Time",
                subtitle = "Show concise timestamps like 2h ago or Yesterday",
                checked = prefs.showRelativeTime,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(showRelativeTime = v) } }
            )
        }
        item {
            SettingsSwitchRow(
                title = "Quick-Call Icon",
                subtitle = "Show one-tap green call button on the right edge",
                checked = prefs.showQuickCallIcon,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(showQuickCallIcon = v) } }
            )
        }
        item { SettingsSectionHeader("Grouping Behavior") }
        item {
            SettingsSwitchRow(
                title = "Group Consecutive Calls",
                subtitle = "Merge repeated calls from the same contact into one expandable row",
                checked = prefs.groupConsecutiveCalls,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(groupConsecutiveCalls = v) } }
            )
        }
    }
}

@Composable
fun RetentionSettingsContent(
    prefs: PulsePhonePreferences,
    viewModel: PulsePhoneViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 28.dp)) {
        item { SettingsSectionHeader("Unlimited Room Archive & Auto-Delete") }
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "PulsePhone archives every call in a local Room Database, bypassing Android's 500-call system limit. By default, auto-delete is OFF so your history is kept forever.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
        item {
            SettingsSwitchRow(
                title = "Auto-delete old call logs",
                subtitle = if (prefs.autoDeleteOldCallLogs) {
                    "WorkManager automatically removes logs older than ${prefs.callLogRetentionPeriod.label}"
                } else {
                    "OFF — Keep call logs forever (Never delete)"
                },
                checked = prefs.autoDeleteOldCallLogs,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(autoDeleteOldCallLogs = v) } },
                testTag = "retention_master_toggle"
            )
        }
        item {
            Spacer(Modifier.height(6.dp))
            SettingsDropdownCard(
                title = "Delete call logs older than",
                selected = prefs.callLogRetentionPeriod,
                options = CallLogRetentionPeriod.entries,
                enabled = prefs.autoDeleteOldCallLogs,
                labelProvider = { it.label },
                onSelect = { period -> viewModel.updatePreferences { it.copy(callLogRetentionPeriod = period) } }
            )
        }
    }
}

@Composable
fun FavoritesSettingsContent(
    prefs: PulsePhonePreferences,
    viewModel: PulsePhoneViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 28.dp)) {
        item { SettingsSectionHeader("Display Mode") }
        item {
            SettingsRadioGroup(
                options = FavoritesDisplayMode.entries,
                selected = prefs.favoritesDisplayMode,
                labelProvider = { it.label },
                onSelect = { mode -> viewModel.updatePreferences { it.copy(favoritesDisplayMode = mode) } }
            )
        }
        item { SettingsSectionHeader("Sections & Tap Action") }
        item {
            SettingsSwitchRow(
                title = "Show \"Frequently contacted\" Section",
                subtitle = "Display top-called people below pinned favorites",
                checked = prefs.showFrequentlyContacted,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(showFrequentlyContacted = v) } }
            )
        }
        item {
            SettingsSwitchRow(
                title = "One-Tap Direct Call",
                subtitle = if (prefs.favoriteOneTapDirectCall) {
                    "Tapping a favorite places a call immediately"
                } else {
                    "Tapping a favorite opens Contact Details"
                },
                checked = prefs.favoriteOneTapDirectCall,
                onCheckedChange = { v -> viewModel.updatePreferences { it.copy(favoriteOneTapDirectCall = v) } }
            )
        }
    }
}
