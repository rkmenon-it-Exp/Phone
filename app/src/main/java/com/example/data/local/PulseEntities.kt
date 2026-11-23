package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    VOICEMAIL
}

enum class CallerIdSource(val label: String) {
    WHATSAPP("WhatsApp"),
    TELEGRAM("Telegram"),
    MESSENGER("Messenger"),
    SOCIAL_LOOKUP("Social Lookup"),
    CUSTOM_API("Caller ID API")
}

@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["normalizedNumber"]),
        Index(value = ["systemContactId"]),
        Index(value = ["googleResourceName"])
    ]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val systemContactId: Long? = null,
    val googleResourceName: String? = null,
    val displayName: String,
    val primaryNumber: String,
    val normalizedNumber: String,
    val secondaryNumber: String? = null,
    val email: String? = null,
    val photoUri: String? = null,
    val isFavorite: Boolean = false,
    val groupName: String = "General",
    val timesContacted: Int = 0,
    val lastContactedAt: Long = 0L,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "contact_groups",
    indices = [Index(value = ["name"], unique = true)]
)
data class ContactGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val googleResourceName: String? = null,
    val name: String,
    val colorHex: Long = 0xFF1A73E8
)

@Entity(
    tableName = "call_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["normalizedNumber"]),
        Index(value = ["systemCallId"], unique = true)
    ]
)
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val systemCallId: Long? = null,
    val contactId: Long? = null,
    val cachedName: String? = null,
    val cachedPhotoUri: String? = null,
    val number: String,
    val normalizedNumber: String,
    val direction: String = CallDirection.OUTGOING.name,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSec: Long = 0L,
    val simSlot: Int = 1,
    val isRead: Boolean = true
)

@Entity(tableName = "caller_id_cache")
data class CallerIdCacheEntity(
    @PrimaryKey val normalizedNumber: String,
    val rawNumber: String,
    val displayName: String,
    val photoUri: String? = null,
    val source: String = CallerIdSource.WHATSAPP.name,
    val updatedAt: Long = System.currentTimeMillis()
)
