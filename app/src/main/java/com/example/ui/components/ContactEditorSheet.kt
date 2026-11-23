package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ContactEntity
import com.example.ui.theme.CallAnswerEmerald
import com.example.ui.theme.CallFavoriteAmber
import com.example.ui.theme.CallMissedCrimson

@Composable
fun ContactEditorDialog(
    initialContact: ContactEntity?,
    prefillName: String = "",
    prefillNumber: String = "",
    onSave: (ContactEntity) -> Unit,
    onDelete: ((Long) -> Unit)? = null,
    onCall: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var displayName by remember(initialContact, prefillName) {
        mutableStateOf(initialContact?.displayName ?: prefillName)
    }
    var primaryNumber by remember(initialContact, prefillNumber) {
        mutableStateOf(initialContact?.primaryNumber ?: prefillNumber)
    }
    var secondaryNumber by remember(initialContact) {
        mutableStateOf(initialContact?.secondaryNumber.orEmpty())
    }
    var email by remember(initialContact) {
        mutableStateOf(initialContact?.email.orEmpty())
    }
    var groupName by remember(initialContact) {
        mutableStateOf(initialContact?.groupName ?: "General")
    }
    var notes by remember(initialContact) {
        mutableStateOf(initialContact?.notes.orEmpty())
    }
    var isFavorite by remember(initialContact) {
        mutableStateOf(initialContact?.isFavorite ?: false)
    }
    var photoUri by remember(initialContact) {
        mutableStateOf(initialContact?.photoUri)
    }

    // Play-policy-compliant zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (initialContact != null) "Contact Details" else "New Contact")
                Row {
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarOutline,
                            contentDescription = "Toggle favorite",
                            tint = if (isFavorite) CallFavoriteAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (initialContact != null && primaryNumber.isNotBlank()) {
                        IconButton(onClick = { onCall(primaryNumber) }) {
                            Icon(Icons.Filled.Call, contentDescription = "Call contact", tint = CallAnswerEmerald)
                        }
                    }
                    if (initialContact != null && onDelete != null) {
                        IconButton(onClick = {
                            onDelete(initialContact.id)
                            onDismiss()
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete contact", tint = CallMissedCrimson)
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ContactAvatar(
                    name = displayName.ifBlank { primaryNumber.ifBlank { "?" } },
                    photoUri = photoUri,
                    size = 76.dp,
                    modifier = Modifier
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("contact_editor_photo_picker")
                )
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pick Contact Photo")
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_name_input")
                )
                OutlinedTextField(
                    value = primaryNumber,
                    onValueChange = { primaryNumber = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_number_input")
                )
                OutlinedTextField(
                    value = secondaryNumber,
                    onValueChange = { secondaryNumber = it },
                    label = { Text("Secondary Number (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanNumber = primaryNumber.trim()
                    if (cleanNumber.isNotEmpty()) {
                        onSave(
                            ContactEntity(
                                id = initialContact?.id ?: 0L,
                                systemContactId = initialContact?.systemContactId,
                                googleResourceName = initialContact?.googleResourceName,
                                displayName = displayName.trim().ifBlank { cleanNumber },
                                primaryNumber = cleanNumber,
                                normalizedNumber = cleanNumber,
                                secondaryNumber = secondaryNumber.trim().takeIf { it.isNotBlank() },
                                email = email.trim().takeIf { it.isNotBlank() },
                                photoUri = photoUri,
                                isFavorite = isFavorite,
                                groupName = groupName.trim().ifBlank { "General" },
                                timesContacted = initialContact?.timesContacted ?: 0,
                                lastContactedAt = initialContact?.lastContactedAt ?: 0L,
                                notes = notes.trim()
                            )
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("contact_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
