package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.CallLogDao
import com.example.data.local.CallLogEntity
import com.example.data.local.ContactDao
import com.example.data.local.ContactEntity
import com.example.data.preferences.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LocalTransferManager(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val contactDao: ContactDao,
    private val callLogDao: CallLogDao
) {
    suspend fun exportSettingsJson(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = preferencesRepository.getSnapshot()
            val payload = JSONObject().apply {
                put("type", "pulsephone_settings")
                put("version", 1)
                put("exportedAt", System.currentTimeMillis())
                put("settings", preferencesRepository.toJson(prefs))
            }
            writeTextToUri(uri, payload.toString(2))
            "Exported all PulsePhone settings to JSON."
        }
    }

    suspend fun importSettingsJson(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = readTextFromUri(uri)
            val root = JSONObject(raw)
            val settingsObj = root.optJSONObject("settings") ?: root
            preferencesRepository.importFromJson(settingsObj)
            "Restored PulsePhone settings from JSON."
        }
    }

    suspend fun exportCallLogsJson(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val logs = callLogDao.getAllCallLogsOnce()
            val arr = JSONArray()
            logs.forEach { log ->
                arr.put(
                    JSONObject().apply {
                        put("number", log.number)
                        put("cachedName", log.cachedName ?: "")
                        put("direction", log.direction)
                        put("timestamp", log.timestamp)
                        put("durationSec", log.durationSec)
                        put("simSlot", log.simSlot)
                    }
                )
            }
            val payload = JSONObject().apply {
                put("type", "pulsephone_call_logs")
                put("count", logs.size)
                put("exportedAt", System.currentTimeMillis())
                put("callLogs", arr)
            }
            writeTextToUri(uri, payload.toString(2))
            "Exported ${logs.size} call logs to JSON."
        }
    }

    suspend fun exportCallLogsCsv(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val logs = callLogDao.getAllCallLogsOnce()
            val sb = StringBuilder()
            sb.appendLine("number,name,direction,timestamp,duration_sec,sim_slot")
            logs.forEach { log ->
                sb.appendLine(
                    listOf(
                        escapeCsv(log.number),
                        escapeCsv(log.cachedName.orEmpty()),
                        escapeCsv(log.direction),
                        log.timestamp.toString(),
                        log.durationSec.toString(),
                        log.simSlot.toString()
                    ).joinToString(",")
                )
            }
            writeTextToUri(uri, sb.toString())
            "Exported ${logs.size} call logs to CSV."
        }
    }

    suspend fun importCallLogs(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = readTextFromUri(uri).trim()
            val imported = mutableListOf<CallLogEntity>()
            if (raw.startsWith("{") || raw.startsWith("[")) {
                val arr = if (raw.startsWith("[")) JSONArray(raw) else JSONObject(raw).getJSONArray("callLogs")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val number = obj.optString("number").trim()
                    if (number.isBlank()) continue
                    imported.add(
                        CallLogEntity(
                            number = number,
                            normalizedNumber = T9SearchEngine.normalizePhoneNumber(number),
                            cachedName = obj.optString("cachedName").takeIf { it.isNotBlank() },
                            direction = obj.optString("direction", "OUTGOING"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            durationSec = obj.optLong("durationSec", 0L),
                            simSlot = obj.optInt("simSlot", 1).coerceIn(1, 2)
                        )
                    )
                }
            } else {
                val lines = raw.lines().filter { it.isNotBlank() }
                lines.drop(1).forEach { line ->
                    val cols = parseCsvLine(line)
                    if (cols.size >= 4) {
                        val number = cols[0].trim()
                        if (number.isNotBlank()) {
                            imported.add(
                                CallLogEntity(
                                    number = number,
                                    normalizedNumber = T9SearchEngine.normalizePhoneNumber(number),
                                    cachedName = cols.getOrNull(1)?.takeIf { it.isNotBlank() },
                                    direction = cols.getOrNull(2)?.ifBlank { "OUTGOING" } ?: "OUTGOING",
                                    timestamp = cols.getOrNull(3)?.toLongOrNull() ?: System.currentTimeMillis(),
                                    durationSec = cols.getOrNull(4)?.toLongOrNull() ?: 0L,
                                    simSlot = cols.getOrNull(5)?.toIntOrNull()?.coerceIn(1, 2) ?: 1
                                )
                            )
                        }
                    }
                }
            }
            if (imported.isNotEmpty()) callLogDao.insertCallLogs(imported)
            "Imported ${imported.size} call logs."
        }
    }

    suspend fun exportContactsJson(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val contacts = contactDao.getAllContactsOnce()
            val arr = JSONArray()
            contacts.forEach { c ->
                arr.put(
                    JSONObject().apply {
                        put("displayName", c.displayName)
                        put("primaryNumber", c.primaryNumber)
                        put("secondaryNumber", c.secondaryNumber ?: "")
                        put("email", c.email ?: "")
                        put("isFavorite", c.isFavorite)
                        put("groupName", c.groupName)
                        put("notes", c.notes)
                    }
                )
            }
            val payload = JSONObject().apply {
                put("type", "pulsephone_contacts")
                put("count", contacts.size)
                put("exportedAt", System.currentTimeMillis())
                put("contacts", arr)
            }
            writeTextToUri(uri, payload.toString(2))
            "Exported ${contacts.size} contacts to JSON."
        }
    }

    suspend fun exportContactsCsv(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val contacts = contactDao.getAllContactsOnce()
            val sb = StringBuilder()
            sb.appendLine("display_name,primary_number,secondary_number,email,is_favorite,group_name,notes")
            contacts.forEach { c ->
                sb.appendLine(
                    listOf(
                        escapeCsv(c.displayName),
                        escapeCsv(c.primaryNumber),
                        escapeCsv(c.secondaryNumber.orEmpty()),
                        escapeCsv(c.email.orEmpty()),
                        c.isFavorite.toString(),
                        escapeCsv(c.groupName),
                        escapeCsv(c.notes)
                    ).joinToString(",")
                )
            }
            writeTextToUri(uri, sb.toString())
            "Exported ${contacts.size} contacts to CSV."
        }
    }

    suspend fun importContacts(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = readTextFromUri(uri).trim()
            val imported = mutableListOf<ContactEntity>()
            if (raw.startsWith("{") || raw.startsWith("[")) {
                val arr = if (raw.startsWith("[")) JSONArray(raw) else JSONObject(raw).getJSONArray("contacts")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("displayName").trim()
                    val number = obj.optString("primaryNumber").trim()
                    if (number.isBlank()) continue
                    imported.add(
                        ContactEntity(
                            displayName = name.ifBlank { number },
                            primaryNumber = number,
                            normalizedNumber = T9SearchEngine.normalizePhoneNumber(number),
                            secondaryNumber = obj.optString("secondaryNumber").takeIf { it.isNotBlank() },
                            email = obj.optString("email").takeIf { it.isNotBlank() },
                            isFavorite = obj.optBoolean("isFavorite", false),
                            groupName = obj.optString("groupName", "General").ifBlank { "General" },
                            notes = obj.optString("notes", "")
                        )
                    )
                }
            } else {
                val lines = raw.lines().filter { it.isNotBlank() }
                lines.drop(1).forEach { line ->
                    val cols = parseCsvLine(line)
                    if (cols.size >= 2) {
                        val number = cols[1].trim()
                        if (number.isNotBlank()) {
                            imported.add(
                                ContactEntity(
                                    displayName = cols[0].trim().ifBlank { number },
                                    primaryNumber = number,
                                    normalizedNumber = T9SearchEngine.normalizePhoneNumber(number),
                                    secondaryNumber = cols.getOrNull(2)?.takeIf { it.isNotBlank() },
                                    email = cols.getOrNull(3)?.takeIf { it.isNotBlank() },
                                    isFavorite = cols.getOrNull(4)?.toBooleanStrictOrNull() ?: false,
                                    groupName = cols.getOrNull(5)?.ifBlank { "General" } ?: "General",
                                    notes = cols.getOrNull(6).orEmpty()
                                )
                            )
                        }
                    }
                }
            }
            if (imported.isNotEmpty()) contactDao.upsertAll(imported)
            "Imported ${imported.size} contacts."
        }
    }

    private fun writeTextToUri(uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
            ?: error("Unable to open destination file.")
    }

    private fun readTextFromUri(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Unable to read selected file.")
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
