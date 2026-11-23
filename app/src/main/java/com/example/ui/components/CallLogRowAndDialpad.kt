package com.example.ui.components

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CallDirection
import com.example.data.preferences.PulsePhonePreferences
import com.example.data.preferences.SimCallingMode
import com.example.data.repository.DisplayCallLogItem
import com.example.data.repository.T9SearchEngine
import com.example.ui.theme.CallAnswerEmerald
import com.example.ui.theme.CallMissedCrimson
import com.example.ui.theme.Sim1OceanBlue
import com.example.ui.theme.Sim2DeepTeal

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CallLogItemRow(
    item: DisplayCallLogItem,
    prefs: PulsePhonePreferences,
    onRowClick: () -> Unit,
    onRowLongClick: () -> Unit = {},
    onQuickCall: () -> Unit,
    onOneTapSave: () -> Unit,
    onLookupUnknown: () -> Unit,
    onDelete: () -> Unit,
    isDualSimAvailable: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isMissed = item.direction == CallDirection.MISSED || item.direction == CallDirection.REJECTED

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onRowClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRowLongClick()
                    }
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (prefs.showLogAvatar) {
                ContactAvatar(
                    name = item.displayName,
                    photoUri = item.photoUri,
                    size = 44.dp
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isMissed) CallMissedCrimson else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (prefs.showCallCountBadge && item.callCount > 1) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "(${item.callCount})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Subtle, muted "Not in contacts" tag under unsaved numbers (+ Caller ID source if matched)
                if (!item.isSavedContact && prefs.showNotInContactsIndicator) {
                    val tag = buildString {
                        append("Not in contacts")
                        item.callerIdSource?.let { append(" • via ${it.label}") }
                    }
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (prefs.showCallDirectionArrow) {
                        CallDirectionIcon(direction = item.direction)
                    }
                    if (prefs.showSimBadge && isDualSimAvailable) {
                        SimSlotBadge(simSlot = item.simSlot)
                    }
                    if (item.displayName != item.number) {
                        Text(
                            text = item.number,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (prefs.showCallDuration && item.durationSec > 0L) {
                        Text(
                            text = "• ${T9SearchEngine.formatDuration(item.durationSec)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (prefs.showRelativeTime) {
                        Text(
                            text = "• ${T9SearchEngine.formatRelativeTime(item.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // One-Tap Save to Google Contacts for unsaved / identified unknown callers
            if (!item.isSavedContact && prefs.showOneTapSaveToGoogleContacts) {
                IconButton(
                    onClick = onOneTapSave,
                    modifier = Modifier.testTag("call_log_save_contact_${item.representativeId}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = "Save to Google Contacts",
                        tint = Sim2DeepTeal
                    )
                }
            } else if (!item.isSavedContact && prefs.findNameCallerEnabled && item.callerIdSource == null) {
                IconButton(
                    onClick = onLookupUnknown,
                    modifier = Modifier.testTag("call_log_lookup_${item.representativeId}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Find caller name",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (prefs.showQuickCallIcon) {
                IconButton(
                    onClick = onQuickCall,
                    modifier = Modifier.testTag("call_log_quick_call_${item.representativeId}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Quick call ${item.displayName}",
                        tint = CallAnswerEmerald
                    )
                }
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete call log",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = if (prefs.showLogAvatar) 72.dp else 16.dp)
        )
    }
}

private data class DialKeySpec(val digit: Char, val letters: String, val dtmfTone: Int)

private val DialpadRows = listOf(
    listOf(
        DialKeySpec('1', "∞", ToneGenerator.TONE_DTMF_1),
        DialKeySpec('2', "ABC", ToneGenerator.TONE_DTMF_2),
        DialKeySpec('3', "DEF", ToneGenerator.TONE_DTMF_3)
    ),
    listOf(
        DialKeySpec('4', "GHI", ToneGenerator.TONE_DTMF_4),
        DialKeySpec('5', "JKL", ToneGenerator.TONE_DTMF_5),
        DialKeySpec('6', "MNO", ToneGenerator.TONE_DTMF_6)
    ),
    listOf(
        DialKeySpec('7', "PQRS", ToneGenerator.TONE_DTMF_7),
        DialKeySpec('8', "TUV", ToneGenerator.TONE_DTMF_8),
        DialKeySpec('9', "WXYZ", ToneGenerator.TONE_DTMF_9)
    ),
    listOf(
        DialKeySpec('*', ",", ToneGenerator.TONE_DTMF_S),
        DialKeySpec('0', "+", ToneGenerator.TONE_DTMF_0),
        DialKeySpec('#', "", ToneGenerator.TONE_DTMF_P)
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomizableT9Dialpad(
    query: String,
    prefs: PulsePhonePreferences,
    isDualSimAvailable: Boolean = false,
    sim1Label: String = "SIM 1",
    sim2Label: String = "SIM 2",
    onAppendChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onCallWithSim: (Int?) -> Unit,
    onCollapseDialpad: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val toneGenerator = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 65) }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { toneGenerator?.release() } }
    }

    val keyHeight = (54f * prefs.dialpadHeightScale * prefs.dialpadButtonSizeScale).coerceIn(48f, 76f).dp
    val digitFontSize = (24f * prefs.dialpadFontScale).coerceIn(18f, 34f).sp
    val letterFontSize = (10.5f * prefs.dialpadFontScale).coerceIn(9f, 15f).sp
    val rowSpacing = (6f * prefs.dialpadHeightScale).coerceIn(2f, 12.dp.value).dp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Number Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapseDialpad,
                    modifier = Modifier.testTag("dialpad_collapse_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardHide,
                        contentDescription = "Hide dialpad",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = query.ifEmpty { if (prefs.t9SearchEnabled) "Type number or T9 name" else "Enter number" },
                        style = if (query.isEmpty()) {
                            MaterialTheme.typography.bodyMedium
                        } else {
                            MaterialTheme.typography.headlineMedium.copy(fontSize = (26f * prefs.dialpadFontScale).coerceIn(20f, 34f).sp)
                        },
                        color = if (query.isEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("dialpad_number_display")
                    )
                }

                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = {
                                if (prefs.dialpadVibrationEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onBackspace()
                            },
                            onLongClick = {
                                if (prefs.dialpadVibrationEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onClear()
                            }
                        )
                        .testTag("dialpad_backspace_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace (long press to clear)",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 4x3 T9 Keypad Grid (Styled dynamically by prefs.numericButtonStyle)
            val numStyle = prefs.numericButtonStyle
            val cs = MaterialTheme.colorScheme
            val vibrantKeyPalette = listOf(
                cs.primary,
                cs.secondary,
                Color(0xFF7C3AED),
                Color(0xFFD97706)
            )
            Column(verticalArrangement = Arrangement.spacedBy(rowSpacing)) {
                DialpadRows.forEachIndexed { rowIdx, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEachIndexed { colIdx, key ->
                            val keyShape = when (numStyle) {
                                com.example.data.preferences.IconDesignStyle.NEON_OUTLINE -> RoundedCornerShape(26.dp)
                                com.example.data.preferences.IconDesignStyle.MINIMAL -> RoundedCornerShape(12.dp)
                                com.example.data.preferences.IconDesignStyle.STEALTH_BLACK -> RoundedCornerShape(20.dp)
                                else -> RoundedCornerShape(18.dp)
                            }
                            val keyAccent = vibrantKeyPalette[(rowIdx + colIdx) % vibrantKeyPalette.size]
                            val keyContainerColor = when (numStyle) {
                                com.example.data.preferences.IconDesignStyle.GLASS_TRANSPARENCY -> cs.primary.copy(alpha = 0.11f)
                                com.example.data.preferences.IconDesignStyle.STEALTH_BLACK -> Color(0xFF0A0D12)
                                com.example.data.preferences.IconDesignStyle.MINIMAL -> Color.Transparent
                                com.example.data.preferences.IconDesignStyle.VIBRANT_COLOR -> keyAccent.copy(alpha = 0.15f)
                                com.example.data.preferences.IconDesignStyle.NEON_OUTLINE -> cs.surface.copy(alpha = 0.5f)
                                com.example.data.preferences.IconDesignStyle.SOFT_MATERIAL -> cs.surfaceVariant.copy(alpha = 0.65f)
                            }
                            val keyBorder = when (numStyle) {
                                com.example.data.preferences.IconDesignStyle.GLASS_TRANSPARENCY ->
                                    androidx.compose.foundation.BorderStroke(1.dp, cs.primary.copy(alpha = 0.34f))
                                com.example.data.preferences.IconDesignStyle.STEALTH_BLACK ->
                                    androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                                com.example.data.preferences.IconDesignStyle.MINIMAL -> null
                                com.example.data.preferences.IconDesignStyle.VIBRANT_COLOR ->
                                    androidx.compose.foundation.BorderStroke(1.dp, keyAccent.copy(alpha = 0.35f))
                                com.example.data.preferences.IconDesignStyle.NEON_OUTLINE ->
                                    androidx.compose.foundation.BorderStroke(1.4.dp, cs.primary.copy(alpha = 0.78f))
                                com.example.data.preferences.IconDesignStyle.SOFT_MATERIAL -> null
                            }
                            val digitTextColor = when (numStyle) {
                                com.example.data.preferences.IconDesignStyle.STEALTH_BLACK -> Color.White
                                com.example.data.preferences.IconDesignStyle.VIBRANT_COLOR -> keyAccent
                                com.example.data.preferences.IconDesignStyle.NEON_OUTLINE -> cs.primary
                                else -> cs.onSurface
                            }
                            val letterTextColor = when (numStyle) {
                                com.example.data.preferences.IconDesignStyle.STEALTH_BLACK -> Color.White.copy(alpha = 0.62f)
                                com.example.data.preferences.IconDesignStyle.VIBRANT_COLOR -> keyAccent.copy(alpha = 0.78f)
                                else -> cs.onSurfaceVariant
                            }

                            Surface(
                                shape = keyShape,
                                color = keyContainerColor,
                                border = keyBorder,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(keyHeight)
                                    .clip(keyShape)
                                    .combinedClickable(
                                        onClick = {
                                            if (prefs.dialpadVibrationEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            if (prefs.dialpadKeypressTonesEnabled) {
                                                runCatching { toneGenerator?.startTone(key.dtmfTone, 90) }
                                            }
                                            onAppendChar(key.digit)
                                        },
                                        onLongClick = {
                                            if (key.digit == '0') {
                                                if (prefs.dialpadVibrationEnabled) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                                onAppendChar('+')
                                            }
                                        }
                                    )
                                    .testTag("dialpad_key_${key.digit}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = key.digit.toString(),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontSize = digitFontSize,
                                            lineHeight = digitFontSize
                                        ),
                                        color = digitTextColor
                                    )
                                    if (key.letters.isNotEmpty()) {
                                        Text(
                                            text = key.letters,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = letterFontSize,
                                                letterSpacing = 1.sp
                                            ),
                                            color = letterTextColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // SIM Calling Action Bar: auto-hides Dual-SIM selection on single-SIM devices
            val simStyle = prefs.simButtonStyle
            if (!isDualSimAvailable) {
                StyledSimCallButton(
                    label = "Call",
                    baseColor = CallAnswerEmerald,
                    style = simStyle,
                    onClick = { onCallWithSim(1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialpad_call_single_hardware_sim_button")
                )
            } else {
                when (prefs.simCallingMode) {
                    SimCallingMode.DUAL_SIM_SPLIT -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StyledSimCallButton(
                                label = sim1Label,
                                baseColor = Sim1OceanBlue,
                                style = simStyle,
                                onClick = { onCallWithSim(1) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dialpad_call_sim1_button")
                            )
                            StyledSimCallButton(
                                label = sim2Label,
                                baseColor = Sim2DeepTeal,
                                style = simStyle,
                                onClick = { onCallWithSim(2) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dialpad_call_sim2_button")
                            )
                        }
                    }

                    SimCallingMode.SINGLE_DEFAULT_SIM -> {
                        val simColor = if (prefs.defaultSimSlot == 1) Sim1OceanBlue else Sim2DeepTeal
                        val preferredLabel = if (prefs.defaultSimSlot == 1) sim1Label else sim2Label
                        StyledSimCallButton(
                            label = if (prefs.rememberLastUsedSim) "Call (Smart SIM)" else "Call ($preferredLabel)",
                            baseColor = simColor,
                            style = simStyle,
                            onClick = { onCallWithSim(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialpad_call_single_sim_button")
                        )
                    }

                    SimCallingMode.ASK_EVERY_TIME -> {
                        StyledSimCallButton(
                            label = "Call • Choose SIM",
                            baseColor = CallAnswerEmerald,
                            style = simStyle,
                            onClick = { onCallWithSim(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialpad_call_ask_sim_button")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StyledSimCallButton(
    label: String,
    baseColor: Color,
    style: com.example.data.preferences.IconDesignStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when (style) {
        com.example.data.preferences.IconDesignStyle.GLASS_TRANSPARENCY -> baseColor.copy(alpha = 0.22f)
        com.example.data.preferences.IconDesignStyle.STEALTH_BLACK -> Color(0xFF0B0E14)
        com.example.data.preferences.IconDesignStyle.MINIMAL -> Color.Transparent
        com.example.data.preferences.IconDesignStyle.VIBRANT_COLOR -> baseColor
        com.example.data.preferences.IconDesignStyle.NEON_OUTLINE -> baseColor.copy(alpha = 0.12f)
        com.example.data.preferences.IconDesignStyle.SOFT_MATERIAL -> baseColor
    }
    val contentColor = when (style) {
        com.example.data.preferences.IconDesignStyle.GLASS_TRANSPARENCY -> baseColor
        com.example.data.preferences.IconDesignStyle.STEALTH_BLACK -> Color.White
        com.example.data.preferences.IconDesignStyle.MINIMAL -> baseColor
        com.example.data.preferences.IconDesignStyle.VIBRANT_COLOR -> Color.White
        com.example.data.preferences.IconDesignStyle.NEON_OUTLINE -> baseColor
        com.example.data.preferences.IconDesignStyle.SOFT_MATERIAL -> Color.White
    }
    val borderStroke = when (style) {
        com.example.data.preferences.IconDesignStyle.GLASS_TRANSPARENCY ->
            androidx.compose.foundation.BorderStroke(1.2.dp, baseColor.copy(alpha = 0.65f))
        com.example.data.preferences.IconDesignStyle.STEALTH_BLACK ->
            androidx.compose.foundation.BorderStroke(1.2.dp, baseColor.copy(alpha = 0.70f))
        com.example.data.preferences.IconDesignStyle.MINIMAL ->
            androidx.compose.foundation.BorderStroke(1.dp, baseColor.copy(alpha = 0.45f))
        com.example.data.preferences.IconDesignStyle.NEON_OUTLINE ->
            androidx.compose.foundation.BorderStroke(1.8.dp, baseColor)
        else -> null
    }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = borderStroke,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.height(50.dp)
    ) {
        Icon(Icons.Filled.Call, contentDescription = label, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
    }
}
