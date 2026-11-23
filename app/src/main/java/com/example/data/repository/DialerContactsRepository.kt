package com.example.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.example.callerid.ResolvedCallerIdentity
import com.example.callerid.SmartCallerIdResolver
import com.example.data.local.CallDirection
import com.example.data.local.CallLogDao
import com.example.data.local.CallLogEntity
import com.example.data.local.CallerIdCacheDao
import com.example.data.local.CallerIdCacheEntity
import com.example.data.local.ContactDao
import com.example.data.local.ContactEntity
import com.example.data.local.ContactGroupEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.remote.GoogleAuthAndCloudRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class SimHardwareInfo(
    val activeSimCount: Int = 1,
    val isDualSimAvailable: Boolean = false,
    val sim1Label: String = "SIM 1",
    val sim2Label: String = "SIM 2"
)

class DialerContactsRepository(
    private val context: Context,
    private val contactDao: ContactDao,
    private val callLogDao: CallLogDao,
    private val callerIdCacheDao: CallerIdCacheDao,
    private val deviceImporter: DeviceContactsCallLogImporter,
    private val googleCloudRepo: GoogleAuthAndCloudRepository,
    private val preferencesRepository: PreferencesRepository,
    private val callerIdResolver: SmartCallerIdResolver
) {
    val allContacts: Flow<List<ContactEntity>> = contactDao.observeAllContacts()
    val favoriteContacts: Flow<List<ContactEntity>> = contactDao.observeFavoriteContacts()
    val frequentlyContacted: Flow<List<ContactEntity>> = contactDao.observeFrequentlyContacted()
    val allGroups: Flow<List<ContactGroupEntity>> = contactDao.observeAllGroups()
    val allCallLogs: Flow<List<CallLogEntity>> = callLogDao.observeAllCallLogs()
    val totalCallLogCount: Flow<Int> = callLogDao.observeCallLogCount()
    val callerIdCacheEntries: Flow<List<CallerIdCacheEntity>> = callerIdCacheDao.observeAllEntries()

    @SuppressLint("MissingPermission")
    fun detectSimHardware(): SimHardwareInfo {
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPhoneState) {
            return SimHardwareInfo(activeSimCount = 1, isDualSimAvailable = false)
        }

        // 1. Primary check: active SIM subscriptions via SubscriptionManager
        val subList = runCatching {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            sm?.activeSubscriptionInfoList.orEmpty()
        }.getOrDefault(emptyList())

        if (subList.isNotEmpty()) {
            val sorted = subList.sortedBy { it.simSlotIndex }
            val sim1Name = sorted.getOrNull(0)?.displayName?.toString()?.takeIf { it.isNotBlank() } ?: "SIM 1"
            val sim2Name = sorted.getOrNull(1)?.displayName?.toString()?.takeIf { it.isNotBlank() } ?: "SIM 2"
            return SimHardwareInfo(
                activeSimCount = sorted.size,
                isDualSimAvailable = sorted.size >= 2,
                sim1Label = sim1Name,
                sim2Label = sim2Name
            )
        }

        // 2. Fallback check: TelecomManager call-capable phone accounts
        val accounts = runCatching {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            tm?.callCapablePhoneAccounts.orEmpty()
        }.getOrDefault(emptyList())

        return SimHardwareInfo(
            activeSimCount = accounts.size.coerceAtLeast(1),
            isDualSimAvailable = accounts.size >= 2,
            sim1Label = "SIM 1",
            sim2Label = "SIM 2"
        )
    }

    suspend fun findLastUsedSimSlot(rawNumber: String): Int? = withContext(Dispatchers.IO) {
        val normalized = T9SearchEngine.normalizePhoneNumber(rawNumber)
        if (normalized.isBlank()) return@withContext null
        callLogDao.getLastSimSlotForNumber(normalized)
            ?: callLogDao.getAllCallLogsOnce()
                .firstOrNull { T9SearchEngine.numberMatchKey(it.number) == T9SearchEngine.numberMatchKey(rawNumber) }
                ?.simSlot
    }

    fun isDefaultDialer(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } else {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecom?.defaultDialerPackage == context.packageName
        }
    }

    fun createDefaultDialerRequestIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
            ) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            } else {
                null
            }
        } else {
            @Suppress("DEPRECATION")
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
        }
    }

    suspend fun importFromDevice(): DeviceImportSummary = deviceImporter.importAllFromDevice()

    suspend fun upsertContact(contact: ContactEntity): Long = withContext(Dispatchers.IO) {
        val normalized = T9SearchEngine.normalizePhoneNumber(contact.primaryNumber)
        val savedId = contactDao.upsertContact(
            contact.copy(
                normalizedNumber = normalized,
                updatedAt = System.currentTimeMillis()
            )
        )
        // Ensure group exists
        if (contact.groupName.isNotBlank()) {
            contactDao.insertGroup(ContactGroupEntity(name = contact.groupName))
        }
        savedId
    }

    suspend fun toggleFavorite(contactId: Long, isFavorite: Boolean) {
        contactDao.setFavorite(contactId, isFavorite)
    }

    suspend fun deleteContact(contactId: Long) {
        contactDao.deleteContactById(contactId)
    }

    suspend fun addGroup(name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty()) {
            contactDao.insertGroup(ContactGroupEntity(name = clean))
        }
    }

    suspend fun deleteGroup(group: ContactGroupEntity) {
        contactDao.deleteGroup(group)
    }

    suspend fun recordCallLog(
        number: String,
        direction: CallDirection,
        durationSec: Long,
        simSlot: Int,
        cachedName: String? = null,
        cachedPhotoUri: String? = null
    ) = withContext(Dispatchers.IO) {
        val normalized = T9SearchEngine.normalizePhoneNumber(number)
        val contact = contactDao.findByNormalizedNumber(normalized)
        if (contact != null) {
            contactDao.incrementContacted(contact.id)
        }
        callLogDao.insertCallLog(
            CallLogEntity(
                contactId = contact?.id,
                cachedName = contact?.displayName ?: cachedName,
                cachedPhotoUri = contact?.photoUri ?: cachedPhotoUri,
                number = number,
                normalizedNumber = normalized,
                direction = direction.name,
                timestamp = System.currentTimeMillis(),
                durationSec = durationSec,
                simSlot = simSlot.coerceIn(1, 2)
            )
        )
    }

    suspend fun deleteCallLogGroup(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            callLogDao.deleteCallLogsByIds(ids)
        }
    }

    suspend fun resolveCallerIdentity(number: String): ResolvedCallerIdentity =
        callerIdResolver.resolveNumber(number)

    /**
     * One-Tap Save to Google Contacts + local Room Contacts + System ContactsContract.
     */
    suspend fun oneTapSaveToGoogleContacts(
        displayName: String,
        phoneNumber: String,
        photoUri: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = displayName.trim().ifBlank { phoneNumber }
            val normalized = T9SearchEngine.normalizePhoneNumber(phoneNumber)
            val prefs = preferencesRepository.getSnapshot()

            // 1. Push to Google Contacts via People API when signed in
            val googleResourceName = if (prefs.googleAccountEmail != null) {
                googleCloudRepo.createGoogleContact(cleanName, phoneNumber).getOrNull()
            } else {
                null
            }

            // 2. Write to Android system ContactsContract (linked to signed-in Google account if available)
            deviceImporter.insertIntoSystemContacts(
                displayName = cleanName,
                phoneNumber = phoneNumber,
                accountEmail = prefs.googleAccountEmail
            )

            // 3. Persist immediately in local Room Contacts and remove from unsaved caller ID cache
            val existing = contactDao.findByNormalizedNumber(normalized)
            contactDao.upsertContact(
                ContactEntity(
                    id = existing?.id ?: 0L,
                    systemContactId = existing?.systemContactId,
                    googleResourceName = googleResourceName ?: existing?.googleResourceName,
                    displayName = cleanName,
                    primaryNumber = phoneNumber,
                    normalizedNumber = normalized,
                    photoUri = photoUri ?: existing?.photoUri,
                    groupName = if (googleResourceName != null) "Google" else "General",
                    updatedAt = System.currentTimeMillis()
                )
            )
            callerIdCacheDao.deleteByNumber(normalized)

            if (googleResourceName != null) {
                "Saved $cleanName to Google Contacts."
            } else {
                "Saved $cleanName to Contacts."
            }
        }
    }

    /**
     * Places an outgoing phone call honoring SIM slot selection via TelecomManager when permitted,
     * with clean fallback to ACTION_DIAL if CALL_PHONE permission has not been granted yet.
     */
    @SuppressLint("MissingPermission")
    suspend fun placeOutgoingCall(rawNumber: String, simSlot: Int): Result<String> = withContext(Dispatchers.Main) {
        val number = rawNumber.trim()
        if (number.isBlank()) return@withContext Result.failure(IllegalArgumentException("Enter a phone number first."))

        // Archive immediately in unlimited local Room Call Log
        val identity = callerIdResolver.resolveNumber(number)
        recordCallLog(
            number = number,
            direction = CallDirection.OUTGOING,
            durationSec = 0L,
            simSlot = simSlot,
            cachedName = identity.displayName.takeIf { it != number },
            cachedPhotoUri = identity.photoUri
        )

        val uri = Uri.fromParts("tel", number, null)
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        runCatching {
            if (hasCallPermission) {
                val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                val extras = Bundle()
                val simHandle = resolveSimAccountHandle(telecom, simSlot)
                if (simHandle != null) {
                    extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, simHandle)
                }
                if (telecom != null) {
                    telecom.placeCall(uri, extras)
                    "Calling $number on SIM $simSlot"
                } else {
                    val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(callIntent)
                    "Calling $number on SIM $simSlot"
                }
            } else {
                val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                "Opened dialer for $number (grant Call permission for direct calling)."
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun resolveSimAccountHandle(telecom: TelecomManager?, simSlot: Int): PhoneAccountHandle? {
        if (telecom == null) return null
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPhoneState) return null
        return runCatching {
            val accounts = telecom.callCapablePhoneAccounts
            if (accounts.isEmpty()) null else accounts.getOrNull((simSlot - 1).coerceAtLeast(0)) ?: accounts.first()
        }.getOrNull()
    }
}
