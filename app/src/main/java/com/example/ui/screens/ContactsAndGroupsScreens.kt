package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CallDirection
import com.example.data.local.ContactEntity
import com.example.data.local.ContactGroupEntity
import com.example.data.preferences.PulsePhonePreferences
import com.example.data.repository.DisplayCallLogItem
import com.example.ui.components.CallLogItemRow
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.CallAnswerEmerald
import com.example.ui.theme.CallFavoriteAmber
import com.example.ui.viewmodel.RecentsFilter

@Composable
fun ContactsTabScreen(
    contacts: List<ContactEntity>,
    onCallContact: (ContactEntity) -> Unit,
    onOpenContact: (ContactEntity) -> Unit,
    onToggleFavorite: (ContactEntity) -> Unit,
    onAddNewContact: () -> Unit,
    onImportFromDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filtered = remember(contacts, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) contacts else contacts.filter {
            it.displayName.contains(q, ignoreCase = true) ||
                it.primaryNumber.contains(q, ignoreCase = true) ||
                it.groupName.contains(q, ignoreCase = true)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search ${contacts.size} contacts...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search contacts") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("contacts_search_input")
            )

            if (filtered.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Contacts,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("No Contacts Found", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Import contacts from your device, sync with Google Contacts, or create one directly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(onClick = onImportFromDevice) {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Import Device Contacts")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filtered, key = { it.id }) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenContact(contact) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactAvatar(name = contact.displayName, photoUri = contact.photoUri, size = 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${contact.primaryNumber} • ${contact.groupName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onToggleFavorite(contact) }) {
                                Icon(
                                    imageVector = if (contact.isFavorite) Icons.Filled.Star else Icons.Filled.StarOutline,
                                    contentDescription = "Favorite ${contact.displayName}",
                                    tint = if (contact.isFavorite) CallFavoriteAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onCallContact(contact) }) {
                                Icon(Icons.Filled.Call, contentDescription = "Call ${contact.displayName}", tint = CallAnswerEmerald)
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 72.dp)
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddNewContact,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("contacts_add_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add new contact")
        }
    }
}

@Composable
fun GroupsTabScreen(
    contacts: List<ContactEntity>,
    groups: List<ContactGroupEntity>,
    onAddGroup: (String) -> Unit,
    onCallContact: (ContactEntity) -> Unit,
    onOpenContact: (ContactEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val allGroupNames = remember(contacts, groups) {
        (listOf("All", "General", "Google", "Work", "Family") +
            groups.map { it.name } +
            contacts.map { it.groupName }).distinct()
    }
    var selectedGroup by rememberSaveable { mutableStateOf("All") }
    var showAddGroupDialog by rememberSaveable { mutableStateOf(false) }
    var newGroupName by rememberSaveable { mutableStateOf("") }

    val members = remember(contacts, selectedGroup) {
        if (selectedGroup == "All") contacts else contacts.filter {
            it.groupName.equals(selectedGroup, ignoreCase = true)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allGroupNames.forEach { groupName ->
                    val count = if (groupName == "All") contacts.size else contacts.count {
                        it.groupName.equals(groupName, ignoreCase = true)
                    }
                    FilterChip(
                        selected = selectedGroup == groupName,
                        onClick = { selectedGroup = groupName },
                        label = { Text("$groupName ($count)") }
                    )
                }
            }

            if (members.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(10.dp))
                    Text("No contacts in \"$selectedGroup\"", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Assign contacts to groups when creating or editing them, or sync Google Contact Groups.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(members, key = { it.id }) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenContact(contact) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactAvatar(name = contact.displayName, photoUri = contact.photoUri, size = 42.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.displayName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = contact.primaryNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onCallContact(contact) }) {
                                Icon(Icons.Filled.Call, contentDescription = "Call ${contact.displayName}", tint = CallAnswerEmerald)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddGroupDialog = true },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("groups_add_fab")
        ) {
            Icon(Icons.Filled.GroupAdd, contentDescription = "Create group")
        }
    }

    if (showAddGroupDialog) {
        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text("New Contact Group") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            onAddGroup(newGroupName.trim())
                            selectedGroup = newGroupName.trim()
                            newGroupName = ""
                            showAddGroupDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddGroupDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun RecentsTabScreen(
    prefs: PulsePhonePreferences,
    callLogs: List<DisplayCallLogItem>,
    totalRawLogsCount: Int,
    isDualSimAvailable: Boolean,
    onCallLogRowClick: (DisplayCallLogItem) -> Unit,
    onCallLogRowLongClick: (DisplayCallLogItem) -> Unit,
    onQuickCallLogItem: (DisplayCallLogItem) -> Unit,
    onOneTapSaveContact: (String, String, String?) -> Unit,
    onLookupUnknown: (String) -> Unit,
    onDeleteCallLog: (DisplayCallLogItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by rememberSaveable { mutableStateOf(RecentsFilter.ALL) }

    LaunchedEffect(isDualSimAvailable) {
        if (!isDualSimAvailable && (filter == RecentsFilter.SIM_1 || filter == RecentsFilter.SIM_2)) {
            filter = RecentsFilter.ALL
        }
    }

    val visibleFilters = remember(isDualSimAvailable) {
        if (isDualSimAvailable) {
            RecentsFilter.entries
        } else {
            RecentsFilter.entries.filter { it != RecentsFilter.SIM_1 && it != RecentsFilter.SIM_2 }
        }
    }

    val filteredLogs = remember(callLogs, filter) {
        when (filter) {
            RecentsFilter.ALL -> callLogs
            RecentsFilter.MISSED -> callLogs.filter { it.direction == CallDirection.MISSED || it.direction == CallDirection.REJECTED }
            RecentsFilter.INCOMING -> callLogs.filter { it.direction == CallDirection.INCOMING }
            RecentsFilter.OUTGOING -> callLogs.filter { it.direction == CallDirection.OUTGOING }
            RecentsFilter.SIM_1 -> callLogs.filter { it.simSlot == 1 }
            RecentsFilter.SIM_2 -> callLogs.filter { it.simSlot == 2 }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            visibleFilters.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label) }
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Unlimited Archive: $totalRawLogsCount calls stored • Retention: ${
                    if (prefs.autoDeleteOldCallLogs) prefs.callLogRetentionPeriod.label else "Forever"
                }",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        if (filteredLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No calls matching \"${filter.label}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredLogs, key = { "recent_${it.representativeId}" }) { item ->
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
}
