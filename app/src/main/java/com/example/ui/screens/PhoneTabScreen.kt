package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.preferences.PulsePhonePreferences
import com.example.data.repository.DisplayCallLogItem
import com.example.data.repository.T9SearchResult
import com.example.ui.components.CallLogItemRow
import com.example.ui.components.ContactAvatar
import com.example.ui.components.CustomizableT9Dialpad
import com.example.ui.theme.CallAnswerEmerald
import com.example.ui.theme.Sim2DeepTeal

@Composable
fun PhoneTabScreen(
    prefs: PulsePhonePreferences,
    callLogs: List<DisplayCallLogItem>,
    dialedQuery: String,
    t9Results: List<T9SearchResult>,
    isDualSimAvailable: Boolean,
    sim1Label: String,
    sim2Label: String,
    onAppendChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onCallNumber: (String, Int?) -> Unit,
    onCallLogRowClick: (DisplayCallLogItem) -> Unit,
    onCallLogRowLongClick: (DisplayCallLogItem) -> Unit,
    onQuickCallLogItem: (DisplayCallLogItem) -> Unit,
    onOpenContactByNumber: (String, String) -> Unit,
    onOneTapSaveContact: (String, String, String?) -> Unit,
    onLookupUnknown: (String) -> Unit,
    onDeleteCallLog: (DisplayCallLogItem) -> Unit,
    onImportFromDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dialpadExpanded by rememberSaveable { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top area: T9 search matches when typing, else Unlimited Call Log
            Box(modifier = Modifier.weight(1f)) {
                if (dialedQuery.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        // Direct create contact / call typed number row
                        item(key = "direct_typed_actions") {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable { onOpenContactByNumber("", dialedQuery) }
                                    .testTag("t9_save_typed_number_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PersonAdd,
                                        contentDescription = "Create new contact",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "Add \"$dialedQuery\" to Contacts",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (t9Results.isEmpty()) {
                            item(key = "no_t9_match") {
                                Text(
                                    text = if (prefs.t9SearchEnabled) {
                                        "No T9 contact matches for \"$dialedQuery\""
                                    } else {
                                        "T9 Smart Search is OFF in Settings"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            items(t9Results, key = { "${it.contactId ?: -1}_${it.normalizedNumber}" }) { match ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onCallNumber(match.number, null) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ContactAvatar(name = match.displayName, photoUri = match.photoUri, size = 40.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = match.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildString {
                                                append(match.number)
                                                if (!match.isSavedContact && prefs.showNotInContactsIndicator) {
                                                    append(" • Not in contacts")
                                                    match.callerIdSource?.let { append(" (${it.label})") }
                                                }
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!match.isSavedContact && prefs.showOneTapSaveToGoogleContacts) {
                                        IconButton(onClick = { onOneTapSaveContact(match.displayName, match.number, match.photoUri) }) {
                                            Icon(Icons.Filled.PersonAdd, "Save to Google Contacts", tint = Sim2DeepTeal)
                                        }
                                    }
                                    IconButton(onClick = { onCallNumber(match.number, null) }) {
                                        Icon(Icons.Filled.Call, "Call ${match.displayName}", tint = CallAnswerEmerald)
                                    }
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 68.dp)
                                )
                            }
                        }
                    }
                } else if (callLogs.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Unlimited Call Archive Ready",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Every call placed or imported is preserved in Room beyond Android's 500-call system limit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        FilledTonalButton(
                            onClick = onImportFromDevice,
                            modifier = Modifier.testTag("empty_logs_import_button")
                        ) {
                            Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Import Device Call Logs & Contacts")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = if (dialpadExpanded) 8.dp else 88.dp)
                    ) {
                        items(callLogs, key = { it.representativeId }) { item ->
                            CallLogItemRow(
                                item = item,
                                prefs = prefs,
                                isDualSimAvailable = isDualSimAvailable,
                                onRowClick = { onCallLogRowClick(item) },
                                onRowLongClick = { onCallLogRowLongClick(item) },
                                onQuickCall = { onQuickCallLogItem(item) },
                                onOneTapSave = { onOneTapSaveContact(item.displayName, item.number, item.photoUri) },
                                onLookupUnknown = { onLookupUnknown(item.number) },
                                onDelete = { onDeleteCallLog(item) }
                            )
                        }
                    }
                }
            }

            // Collapsible Granular T9 Dialpad
            AnimatedVisibility(
                visible = dialpadExpanded,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                CustomizableT9Dialpad(
                    query = dialedQuery,
                    prefs = prefs,
                    isDualSimAvailable = isDualSimAvailable,
                    sim1Label = sim1Label,
                    sim2Label = sim2Label,
                    onAppendChar = onAppendChar,
                    onBackspace = onBackspace,
                    onClear = onClear,
                    onCallWithSim = { explicitSim ->
                        if (dialedQuery.isNotBlank()) {
                            onCallNumber(dialedQuery, explicitSim)
                        } else if (callLogs.isNotEmpty()) {
                            // Classic dialer redial last number into input when pressing Call on empty field
                            callLogs.first().number.forEach { onAppendChar(it) }
                        }
                    },
                    onCollapseDialpad = { dialpadExpanded = false }
                )
            }
        }

        if (!dialpadExpanded) {
            FloatingActionButton(
                onClick = { dialpadExpanded = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("phone_show_dialpad_fab")
            ) {
                Icon(Icons.Filled.Dialpad, contentDescription = "Show dialpad")
            }
        }
    }
}
