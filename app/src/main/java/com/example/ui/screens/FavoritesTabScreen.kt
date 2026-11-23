package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ContactEntity
import com.example.data.preferences.FavoritesDisplayMode
import com.example.data.preferences.PulsePhonePreferences
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.CallAnswerEmerald
import com.example.ui.theme.CallFavoriteAmber

@Composable
fun FavoritesTabScreen(
    prefs: PulsePhonePreferences,
    favorites: List<ContactEntity>,
    frequentlyContacted: List<ContactEntity>,
    onCallContact: (ContactEntity) -> Unit,
    onOpenContactDetails: (ContactEntity) -> Unit,
    onToggleFavorite: (ContactEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val onTapFavorite: (ContactEntity) -> Unit = { contact ->
        if (prefs.favoriteOneTapDirectCall) {
            onCallContact(contact)
        } else {
            onOpenContactDetails(contact)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (favorites.isEmpty()) {
            item(key = "empty_favorites") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.StarOutline,
                        contentDescription = null,
                        tint = CallFavoriteAmber,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("No Favorites Yet", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Star contacts from the Contacts tab to pin them here in a 3-column, 4-column, or compact list layout.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item(key = "favorites_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FAVORITES (${favorites.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (prefs.favoriteOneTapDirectCall) "Tap to Call" else "Tap for Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (prefs.favoritesDisplayMode) {
                FavoritesDisplayMode.GRID_3, FavoritesDisplayMode.GRID_4 -> {
                    val cols = if (prefs.favoritesDisplayMode == FavoritesDisplayMode.GRID_3) 3 else 4
                    val rows = favorites.chunked(cols)
                    items(rows, key = { row -> row.joinToString("_") { it.id.toString() } }) { rowContacts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowContacts.forEach { contact ->
                                FavoriteGridCard(
                                    contact = contact,
                                    compact = cols == 4,
                                    onTap = { onTapFavorite(contact) },
                                    onInfoClick = { onOpenContactDetails(contact) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("favorite_card_${contact.id}")
                                )
                            }
                            repeat(cols - rowContacts.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                FavoritesDisplayMode.COMPACT_LIST -> {
                    items(favorites, key = { "fav_list_${it.id}" }) { contact ->
                        FavoriteCompactRow(
                            contact = contact,
                            onTap = { onTapFavorite(contact) },
                            onCall = { onCallContact(contact) },
                            onToggleStar = { onToggleFavorite(contact) }
                        )
                    }
                }
            }
        }

        // Toggleable "Frequently contacted" bottom section
        if (prefs.showFrequentlyContacted && frequentlyContacted.isNotEmpty()) {
            item(key = "frequent_header") {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "FREQUENTLY CONTACTED",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            items(frequentlyContacted, key = { "freq_${it.id}" }) { contact ->
                FavoriteCompactRow(
                    contact = contact,
                    onTap = { onTapFavorite(contact) },
                    onCall = { onCallContact(contact) },
                    onToggleStar = { onToggleFavorite(contact) }
                )
            }
        }
    }
}

@Composable
private fun FavoriteGridCard(
    contact: ContactEntity,
    compact: Boolean,
    onTap: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        modifier = modifier.clickable(onClick = onTap)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 6.dp else 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ContactAvatar(
                name = contact.displayName,
                photoUri = contact.photoUri,
                size = if (compact) 46.dp else 58.dp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = contact.displayName,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (!compact) {
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Contact details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteCompactRow(
    contact: ContactEntity,
    onTap: () -> Unit,
    onCall: () -> Unit,
    onToggleStar: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(name = contact.displayName, photoUri = contact.photoUri, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = contact.primaryNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleStar) {
                Icon(
                    imageVector = if (contact.isFavorite) Icons.Filled.Star else Icons.Filled.StarOutline,
                    contentDescription = "Toggle favorite",
                    tint = if (contact.isFavorite) CallFavoriteAmber else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onCall) {
                Icon(Icons.Filled.Call, contentDescription = "Call ${contact.displayName}", tint = CallAnswerEmerald)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
    }
}
