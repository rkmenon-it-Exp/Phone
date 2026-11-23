package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CallDirection
import com.example.data.local.CallLogEntity
import com.example.data.local.ContactEntity
import com.example.data.preferences.PulsePhonePreferences
import com.example.data.repository.DisplayCallLogItem
import com.example.data.repository.T9SearchEngine
import com.example.ui.theme.CallAnswerEmerald
import com.example.ui.theme.CallMissedCrimson
import com.example.ui.theme.Sim1OceanBlue
import com.example.ui.theme.Sim2DeepTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallLogLongPressMenuDialog(
    item: DisplayCallLogItem,
    isDualSimAvailable: Boolean,
    sim1Label: String,
    sim2Label: String,
    prefs: PulsePhonePreferences,
    onCallWithSim: (Int) -> Unit,
    onOpenPreview: () -> Unit,
    onOpenDetails: () -> Unit,
    onOpenHistory: () -> Unit,
    onEditBeforeCall: () -> Unit,
    onOneTapSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val primarySimSlot = if (isDualSimAvailable) item.simSlot.coerceIn(1, 2) else 1
    val otherSimSlot = if (primarySimSlot == 1) 2 else 1
    val primarySimName = if (primarySimSlot == 1) sim1Label else sim2Label
    val otherSimName = if (otherSimSlot == 1) sim1Label else sim2Label

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContactAvatar(name = item.displayName, photoUri = item.photoUri, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.number,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ContextMenuRow(
                    icon = Icons.Filled.Call,
                    tint = if (isDualSimAvailable) {
                        if (primarySimSlot == 1) Sim1OceanBlue else Sim2DeepTeal
                    } else {
                        CallAnswerEmerald
                    },
                    title = if (isDualSimAvailable) "Call Back ($primarySimName)" else "Call Back",
                    subtitle = if (isDualSimAvailable) "Last-used SIM for this number" else null,
                    testTag = "long_press_call_primary_sim",
                    onClick = {
                        onDismiss()
                        onCallWithSim(primarySimSlot)
                    }
                )

                // Dual-SIM Other-SIM option: shown ONLY when Dual-SIM is installed (or preview enabled)
                if (isDualSimAvailable) {
                    ContextMenuRow(
                        icon = Icons.Filled.SimCard,
                        tint = if (otherSimSlot == 1) Sim1OceanBlue else Sim2DeepTeal,
                        title = "Call with $otherSimName",
                        subtitle = "Switch to secondary SIM for this call",
                        testTag = "long_press_call_other_sim",
                        onClick = {
                            onDismiss()
                            onCallWithSim(otherSimSlot)
                        }
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )

                ContextMenuRow(
                    icon = Icons.Filled.Person,
                    tint = MaterialTheme.colorScheme.primary,
                    title = "User / Contact Preview",
                    testTag = "long_press_user_preview",
                    onClick = {
                        onDismiss()
                        onOpenPreview()
                    }
                )
                ContextMenuRow(
                    icon = Icons.Filled.Info,
                    tint = MaterialTheme.colorScheme.secondary,
                    title = "Call Details",
                    testTag = "long_press_call_details",
                    onClick = {
                        onDismiss()
                        onOpenDetails()
                    }
                )
                ContextMenuRow(
                    icon = Icons.Filled.History,
                    tint = MaterialTheme.colorScheme.tertiary,
                    title = "Full Call History",
                    testTag = "long_press_call_history",
                    onClick = {
                        onDismiss()
                        onOpenHistory()
                    }
                )
                ContextMenuRow(
                    icon = Icons.Filled.Dialpad,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "Edit Number in Dialpad",
                    testTag = "long_press_edit_dialpad",
                    onClick = {
                        onDismiss()
                        onEditBeforeCall()
                    }
                )

                if (!item.isSavedContact && prefs.showOneTapSaveToGoogleContacts) {
                    ContextMenuRow(
                        icon = Icons.Filled.PersonAdd,
                        tint = Sim2DeepTeal,
                        title = "Save to Google Contacts",
                        testTag = "long_press_save_contact",
                        onClick = {
                            onDismiss()
                            onOneTapSave()
                        }
                    )
                }

                ContextMenuRow(
                    icon = Icons.Filled.DeleteOutline,
                    tint = CallMissedCrimson,
                    title = "Delete from Call Log",
                    testTag = "long_press_delete_log",
                    onClick = {
                        onDismiss()
                        onDelete()
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun ContextMenuRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String? = null,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CallLogUserPreviewDialog(
    item: DisplayCallLogItem,
    matchedContact: ContactEntity?,
    isDualSimAvailable: Boolean,
    sim1Label: String,
    sim2Label: String,
    onCallWithSim: (Int) -> Unit,
    onEditOrSaveContact: () -> Unit,
    onOpenDetails: () -> Unit,
    onOpenHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val lastSim = if (isDualSimAvailable) item.simSlot.coerceIn(1, 2) else 1
    val otherSim = if (lastSim == 1) 2 else 1
    val lastSimName = if (lastSim == 1) sim1Label else sim2Label
    val otherSimName = if (otherSim == 1) sim1Label else sim2Label

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ContactAvatar(
                    name = matchedContact?.displayName ?: item.displayName,
                    photoUri = matchedContact?.photoUri ?: item.photoUri,
                    size = 74.dp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = matchedContact?.displayName ?: item.displayName,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.number,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = when {
                            matchedContact != null -> "${matchedContact.groupName} • Saved Contact"
                            item.callerIdSource != null -> "Not in contacts • via ${item.callerIdSource.label}"
                            else -> "Not in contacts"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (!matchedContact?.email.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = matchedContact?.email.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Primary callback button (uses last-used SIM)
                Button(
                    onClick = {
                        onDismiss()
                        onCallWithSim(lastSim)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDualSimAvailable && lastSim == 2) Sim2DeepTeal else CallAnswerEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("preview_call_primary_button")
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isDualSimAvailable) "Call ($lastSimName)" else "Call Now")
                }

                // Dual-SIM secondary option only when Dual-SIM is available
                if (isDualSimAvailable) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onCallWithSim(otherSim)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("preview_call_other_sim_button")
                    ) {
                        Icon(
                            Icons.Filled.SimCard,
                            contentDescription = null,
                            tint = if (otherSim == 1) Sim1OceanBlue else Sim2DeepTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Call with $otherSimName")
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenHistory()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("History", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenDetails()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Details", maxLines = 1)
                    }
                }

                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = {
                        onDismiss()
                        onEditOrSaveContact()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (matchedContact != null) Icons.Filled.Edit else Icons.Filled.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (matchedContact != null) "Edit Contact" else "Add to Contacts")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CallLogDetailsDialog(
    item: DisplayCallLogItem,
    isDualSimAvailable: Boolean,
    sim1Label: String,
    sim2Label: String,
    onCallWithSim: (Int) -> Unit,
    onOpenHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val exactTimestamp = remember(item.timestamp) {
        SimpleDateFormat("EEE, MMM d, yyyy • h:mm:ss a", Locale.getDefault()).format(Date(item.timestamp))
    }
    val directionLabel = when (item.direction) {
        CallDirection.INCOMING -> "Incoming Call"
        CallDirection.OUTGOING -> "Outgoing Call"
        CallDirection.MISSED -> "Missed Call"
        CallDirection.REJECTED -> "Rejected Call"
        CallDirection.VOICEMAIL -> "Voicemail"
    }
    val lastSim = if (isDualSimAvailable) item.simSlot.coerceIn(1, 2) else 1
    val simName = if (lastSim == 1) sim1Label else sim2Label

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Call Details",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ContactAvatar(name = item.displayName, photoUri = item.photoUri, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(item.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            item.number,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                DetailInfoRow("Call Type", directionLabel)
                DetailInfoRow("Date & Time", exactTimestamp)
                DetailInfoRow(
                    "Duration",
                    if (item.durationSec > 0L) T9SearchEngine.formatDuration(item.durationSec) else "Not answered / 0s"
                )
                if (isDualSimAvailable) {
                    DetailInfoRow("SIM Used", "$simName (SIM $lastSim)")
                }
                if (item.callCount > 1) {
                    DetailInfoRow("Grouped Entries", "${item.callCount} consecutive calls")
                }
                if (!item.isSavedContact) {
                    DetailInfoRow(
                        "Contact Status",
                        item.callerIdSource?.let { "Not in contacts • via ${it.label}" } ?: "Not in contacts"
                    )
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            onCallWithSim(lastSim)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CallAnswerEmerald, contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isDualSimAvailable) "Call ($simName)" else "Call Back", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenHistory()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("All Logs", maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CallLogHistoryDialog(
    item: DisplayCallLogItem,
    rawCallLogs: List<CallLogEntity>,
    isDualSimAvailable: Boolean,
    sim1Label: String,
    sim2Label: String,
    onCallWithSim: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val targetKey = remember(item.number) { T9SearchEngine.numberMatchKey(item.number) }
    val matchingEntries = remember(rawCallLogs, targetKey) {
        rawCallLogs
            .filter { T9SearchEngine.numberMatchKey(it.number) == targetKey }
            .sortedByDescending { it.timestamp }
    }
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }
    val primarySim = if (isDualSimAvailable) (matchingEntries.firstOrNull()?.simSlot ?: item.simSlot).coerceIn(1, 2) else 1
    val primarySimLabel = if (primarySim == 1) sim1Label else sim2Label

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "${item.displayName} • History",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.number} (${matchingEntries.size} ${if (matchingEntries.size == 1) "call" else "calls"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (matchingEntries.isEmpty()) {
                    Text(
                        text = "No archived call entries found for this number.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(matchingEntries, key = { it.id }) { entry ->
                            val direction = runCatching { CallDirection.valueOf(entry.direction) }
                                .getOrDefault(CallDirection.OUTGOING)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CallDirectionIcon(direction = direction)
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = dateFormatter.format(Date(entry.timestamp)),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                        )
                                        Text(
                                            text = buildString {
                                                append(direction.name.lowercase().replaceFirstChar { it.uppercase() })
                                                if (entry.durationSec > 0L) {
                                                    append(" • ${T9SearchEngine.formatDuration(entry.durationSec)}")
                                                }
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isDualSimAvailable) {
                                        SimSlotBadge(simSlot = entry.simSlot)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onCallWithSim(primarySim)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CallAnswerEmerald, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("history_dialog_call_button")
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isDualSimAvailable) "Call Back ($primarySimLabel)" else "Call Back")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
