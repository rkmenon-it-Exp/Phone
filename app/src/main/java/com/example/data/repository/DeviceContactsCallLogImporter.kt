package com.example.data.repository

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.local.CallDirection
import com.example.data.local.CallLogDao
import com.example.data.local.CallLogEntity
import com.example.data.local.ContactDao
import com.example.data.local.ContactEntity
import com.example.data.local.ContactGroupEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceImportSummary(
    val importedContacts: Int,
    val importedCallLogs: Int,
    val importedGroups: Int
)

class DeviceContactsCallLogImporter(
    private val context: Context,
    private val contactDao: ContactDao,
    private val callLogDao: CallLogDao
) {
    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun hasWriteContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun hasCallLogPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    suspend fun importAllFromDevice(): DeviceImportSummary = withContext(Dispatchers.IO) {
        val groupsCount = if (hasContactsPermission()) importGroups() else 0
        val contactsCount = if (hasContactsPermission()) importContacts() else 0
        val logsCount = if (hasCallLogPermission()) importCallLogs() else 0
        DeviceImportSummary(
            importedContacts = contactsCount,
            importedCallLogs = logsCount,
            importedGroups = groupsCount
        )
    }

    private suspend fun importGroups(): Int {
        var count = 0
        val projection = arrayOf(
            ContactsContract.Groups._ID,
            ContactsContract.Groups.TITLE
        )
        runCatching {
            context.contentResolver.query(
                ContactsContract.Groups.CONTENT_URI,
                projection,
                "${ContactsContract.Groups.DELETED} = 0",
                null,
                null
            )?.use { cursor ->
                val titleIdx = cursor.getColumnIndex(ContactsContract.Groups.TITLE)
                while (cursor.moveToNext()) {
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx)?.trim() else null
                    if (!title.isNullOrBlank() && !title.startsWith("System Group")) {
                        val row = contactDao.insertGroup(ContactGroupEntity(name = title))
                        if (row > 0) count++
                    }
                }
            }
        }
        return count
    }

    private suspend fun importContacts(): Int {
        var imported = 0
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED,
            ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED,
            ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED
        )
        val seenContactIds = mutableSetOf<Long>()
        val batch = mutableListOf<ContactEntity>()

        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val starIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                val timesIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED)
                val lastIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED)

                while (cursor.moveToNext()) {
                    val systemId = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                    if (!seenContactIds.add(systemId)) continue
                    val rawNumber = if (numIdx >= 0) cursor.getString(numIdx)?.trim().orEmpty() else ""
                    if (rawNumber.isBlank()) continue
                    val name = (if (nameIdx >= 0) cursor.getString(nameIdx) else null)?.takeIf { it.isNotBlank() } ?: rawNumber
                    val photo = if (photoIdx >= 0) cursor.getString(photoIdx) else null
                    val starred = if (starIdx >= 0) cursor.getInt(starIdx) == 1 else false
                    val times = if (timesIdx >= 0) cursor.getInt(timesIdx) else 0
                    val lastAt = if (lastIdx >= 0) cursor.getLong(lastIdx) else 0L
                    val normalized = T9SearchEngine.normalizePhoneNumber(rawNumber)

                    val existing = contactDao.findBySystemId(systemId) ?: contactDao.findByNormalizedNumber(normalized)
                    val entity = ContactEntity(
                        id = existing?.id ?: 0L,
                        systemContactId = systemId,
                        googleResourceName = existing?.googleResourceName,
                        displayName = name,
                        primaryNumber = rawNumber,
                        normalizedNumber = normalized,
                        secondaryNumber = existing?.secondaryNumber,
                        email = existing?.email,
                        photoUri = photo ?: existing?.photoUri,
                        isFavorite = starred || (existing?.isFavorite == true),
                        groupName = existing?.groupName ?: "General",
                        timesContacted = maxOf(times, existing?.timesContacted ?: 0),
                        lastContactedAt = maxOf(lastAt, existing?.lastContactedAt ?: 0L),
                        notes = existing?.notes.orEmpty(),
                        updatedAt = System.currentTimeMillis()
                    )
                    batch.add(entity)
                    imported++
                }
            }
        }
        if (batch.isNotEmpty()) {
            contactDao.upsertAll(batch)
        }
        return imported
    }

    private suspend fun importCallLogs(): Int {
        val batch = mutableListOf<CallLogEntity>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.CACHED_PHOTO_URI,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )
        runCatching {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val photoIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_PHOTO_URI)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
                val accIdx = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

                while (cursor.moveToNext()) {
                    val sysId = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                    val number = if (numIdx >= 0) cursor.getString(numIdx)?.trim().orEmpty() else ""
                    if (number.isBlank()) continue
                    val cachedName = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val cachedPhoto = if (photoIdx >= 0) cursor.getString(photoIdx) else null
                    val rawType = if (typeIdx >= 0) cursor.getInt(typeIdx) else CallLog.Calls.OUTGOING_TYPE
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    val duration = if (durIdx >= 0) cursor.getLong(durIdx) else 0L
                    val accountId = if (accIdx >= 0) cursor.getString(accIdx).orEmpty() else ""

                    val direction = when (rawType) {
                        CallLog.Calls.INCOMING_TYPE -> CallDirection.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallDirection.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
                        CallLog.Calls.REJECTED_TYPE -> CallDirection.REJECTED
                        CallLog.Calls.VOICEMAIL_TYPE -> CallDirection.VOICEMAIL
                        else -> CallDirection.OUTGOING
                    }
                    val simSlot = if (accountId.contains("2") || accountId.endsWith("1")) 2 else 1

                    batch.add(
                        CallLogEntity(
                            systemCallId = sysId,
                            cachedName = cachedName,
                            cachedPhotoUri = cachedPhoto,
                            number = number,
                            normalizedNumber = T9SearchEngine.normalizePhoneNumber(number),
                            direction = direction.name,
                            timestamp = date,
                            durationSec = duration,
                            simSlot = simSlot
                        )
                    )
                }
            }
        }
        if (batch.isNotEmpty()) {
            callLogDao.insertCallLogs(batch)
        }
        return batch.size
    }

    suspend fun insertIntoSystemContacts(
        displayName: String,
        phoneNumber: String,
        accountEmail: String?
    ): Boolean = withContext(Dispatchers.IO) {
        if (!hasWriteContactsPermission()) return@withContext false
        runCatching {
            val ops = ArrayList<ContentProviderOperation>()
            val rawBuilder = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            if (!accountEmail.isNullOrBlank()) {
                rawBuilder.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, "com.google")
                rawBuilder.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountEmail)
            } else {
                rawBuilder.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                rawBuilder.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            }
            ops.add(rawBuilder.build())

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .build()
            )
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                    .build()
            )
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        }.getOrDefault(false)
    }
}
