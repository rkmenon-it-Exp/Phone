package com.example.data.repository

import com.example.data.local.CallDirection
import com.example.data.local.CallLogEntity
import com.example.data.local.CallerIdCacheEntity
import com.example.data.local.CallerIdSource
import com.example.data.local.ContactEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DisplayCallLogItem(
    val representativeId: Long,
    val groupedIds: List<Long>,
    val contactId: Long?,
    val displayName: String,
    val number: String,
    val normalizedNumber: String,
    val photoUri: String?,
    val direction: CallDirection,
    val timestamp: Long,
    val durationSec: Long,
    val simSlot: Int,
    val callCount: Int,
    val isSavedContact: Boolean,
    val callerIdSource: CallerIdSource?
)

data class T9SearchResult(
    val contactId: Long?,
    val displayName: String,
    val number: String,
    val normalizedNumber: String,
    val photoUri: String?,
    val isSavedContact: Boolean,
    val callerIdSource: CallerIdSource?,
    val matchedByName: Boolean
)

object T9SearchEngine {

    private val t9Map = mapOf(
        'A' to '2', 'B' to '2', 'C' to '2',
        'D' to '3', 'E' to '3', 'F' to '3',
        'G' to '4', 'H' to '4', 'I' to '4',
        'J' to '5', 'K' to '5', 'L' to '5',
        'M' to '6', 'N' to '6', 'O' to '6',
        'P' to '7', 'Q' to '7', 'R' to '7', 'S' to '7',
        'T' to '8', 'U' to '8', 'V' to '8',
        'W' to '9', 'X' to '9', 'Y' to '9', 'Z' to '9'
    )

    fun normalizePhoneNumber(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val sb = StringBuilder()
        trimmed.forEachIndexed { idx, c ->
            if (c.isDigit() || (idx == 0 && c == '+')) {
                sb.append(c)
            }
        }
        val cleaned = sb.toString()
        // Compare on last 10 digits when long international prefix varies, while keeping '+' normalized
        return cleaned
    }

    fun numberMatchKey(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    fun nameToT9Digits(name: String): String {
        val sb = StringBuilder(name.length)
        for (ch in name.uppercase(Locale.US)) {
            when {
                ch.isDigit() -> sb.append(ch)
                t9Map.containsKey(ch) -> sb.append(t9Map[ch])
                ch == ' ' -> sb.append(' ')
            }
        }
        return sb.toString()
    }

    fun search(
        query: String,
        contacts: List<ContactEntity>,
        callerIdEntries: Map<String, CallerIdCacheEntity>,
        t9Enabled: Boolean
    ): List<T9SearchResult> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return emptyList()
        val digitsOnly = cleanQuery.filter { it.isDigit() }
        val results = mutableListOf<T9SearchResult>()
        val seenKeys = mutableSetOf<String>()

        for (contact in contacts) {
            val numberDigits = contact.primaryNumber.filter { it.isDigit() }
            val numberMatch = digitsOnly.isNotEmpty() && numberDigits.contains(digitsOnly)
            val textMatch = contact.displayName.contains(cleanQuery, ignoreCase = true)
            val t9Match = if (t9Enabled && digitsOnly.isNotEmpty()) {
                val t9Name = nameToT9Digits(contact.displayName)
                val compactT9 = t9Name.replace(" ", "")
                val wordStartsMatch = t9Name.split(" ").any { it.startsWith(digitsOnly) }
                compactT9.contains(digitsOnly) || wordStartsMatch
            } else {
                false
            }

            if (numberMatch || textMatch || t9Match) {
                val key = numberMatchKey(contact.primaryNumber)
                seenKeys.add(key)
                results.add(
                    T9SearchResult(
                        contactId = contact.id,
                        displayName = contact.displayName,
                        number = contact.primaryNumber,
                        normalizedNumber = contact.normalizedNumber,
                        photoUri = contact.photoUri,
                        isSavedContact = true,
                        callerIdSource = null,
                        matchedByName = textMatch || t9Match
                    )
                )
            }
        }

        // Also match identified unknown callers from Smart Caller ID cache
        for ((_, entry) in callerIdEntries) {
            val key = numberMatchKey(entry.rawNumber)
            if (key in seenKeys) continue
            val numberDigits = entry.rawNumber.filter { it.isDigit() }
            val numberMatch = digitsOnly.isNotEmpty() && numberDigits.contains(digitsOnly)
            val t9Match = if (t9Enabled && digitsOnly.isNotEmpty()) {
                nameToT9Digits(entry.displayName).replace(" ", "").contains(digitsOnly)
            } else {
                false
            }
            if (numberMatch || t9Match) {
                val src = runCatching { CallerIdSource.valueOf(entry.source) }.getOrNull()
                results.add(
                    T9SearchResult(
                        contactId = null,
                        displayName = entry.displayName,
                        number = entry.rawNumber,
                        normalizedNumber = entry.normalizedNumber,
                        photoUri = entry.photoUri,
                        isSavedContact = false,
                        callerIdSource = src,
                        matchedByName = t9Match
                    )
                )
            }
        }

        return results.sortedByDescending { it.matchedByName }.take(40)
    }

    fun buildDisplayCallLogs(
        logs: List<CallLogEntity>,
        contacts: List<ContactEntity>,
        callerIdEntries: List<CallerIdCacheEntity>,
        groupConsecutive: Boolean,
        findNameCallerEnabled: Boolean
    ): List<DisplayCallLogItem> {
        if (logs.isEmpty()) return emptyList()

        val contactByKey = contacts.associateBy { numberMatchKey(it.primaryNumber) }
        val callerIdByKey = callerIdEntries.associateBy { numberMatchKey(it.rawNumber) }

        fun toSingleItem(log: CallLogEntity, ids: List<Long>, count: Int): DisplayCallLogItem {
            val key = numberMatchKey(log.number)
            val matchedContact = contactByKey[key]
            val matchedCallerId = if (matchedContact == null && findNameCallerEnabled) {
                callerIdByKey[key]
            } else {
                null
            }
            val direction = runCatching { CallDirection.valueOf(log.direction) }
                .getOrDefault(CallDirection.OUTGOING)
            val source = matchedCallerId?.let {
                runCatching { CallerIdSource.valueOf(it.source) }.getOrNull()
            }

            val resolvedName = matchedContact?.displayName
                ?: matchedCallerId?.displayName
                ?: log.cachedName?.takeIf { it.isNotBlank() && it != log.number }
                ?: log.number

            val resolvedPhoto = matchedContact?.photoUri
                ?: matchedCallerId?.photoUri
                ?: log.cachedPhotoUri

            return DisplayCallLogItem(
                representativeId = log.id,
                groupedIds = ids,
                contactId = matchedContact?.id ?: log.contactId,
                displayName = resolvedName,
                number = log.number,
                normalizedNumber = log.normalizedNumber,
                photoUri = resolvedPhoto,
                direction = direction,
                timestamp = log.timestamp,
                durationSec = log.durationSec,
                simSlot = log.simSlot.coerceIn(1, 2),
                callCount = count,
                isSavedContact = matchedContact != null,
                callerIdSource = source
            )
        }

        if (!groupConsecutive) {
            return logs.map { toSingleItem(it, listOf(it.id), 1) }
        }

        val grouped = mutableListOf<DisplayCallLogItem>()
        var idx = 0
        while (idx < logs.size) {
            val first = logs[idx]
            val firstKey = numberMatchKey(first.number)
            val ids = mutableListOf(first.id)
            var totalDuration = first.durationSec
            var next = idx + 1
            while (next < logs.size && numberMatchKey(logs[next].number) == firstKey) {
                ids.add(logs[next].id)
                totalDuration += logs[next].durationSec
                next++
            }
            grouped.add(
                toSingleItem(
                    log = first.copy(durationSec = first.durationSec.coerceAtLeast(0L)),
                    ids = ids,
                    count = ids.size
                )
            )
            idx = next
        }
        return grouped
    }

    fun formatRelativeTime(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diff = (nowMillis - timestampMillis).coerceAtLeast(0L)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7L -> "${days}d ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestampMillis))
        }
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0L) return "0s"
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hrs > 0 -> String.format(Locale.US, "%dh %02dm", hrs, mins)
            mins > 0 -> String.format(Locale.US, "%dm %02ds", mins, secs)
            else -> "${secs}s"
        }
    }
}
