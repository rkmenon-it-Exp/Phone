package com.example.callerid

import com.example.data.local.CallerIdCacheDao
import com.example.data.local.CallerIdCacheEntity
import com.example.data.local.CallerIdSource
import com.example.data.local.ContactDao
import com.example.data.preferences.PreferencesRepository
import com.example.data.remote.CustomCallerIdApiClient
import com.example.data.repository.T9SearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ResolvedCallerIdentity(
    val displayName: String,
    val number: String,
    val photoUri: String?,
    val isSavedContact: Boolean,
    val source: CallerIdSource?
)

class SmartCallerIdResolver(
    private val preferencesRepository: PreferencesRepository,
    private val contactDao: ContactDao,
    private val callerIdCacheDao: CallerIdCacheDao,
    private val customApiClient: CustomCallerIdApiClient
) {
    suspend fun resolveNumber(rawNumber: String): ResolvedCallerIdentity = withContext(Dispatchers.IO) {
        val normalized = T9SearchEngine.normalizePhoneNumber(rawNumber)
        val matchKey = T9SearchEngine.numberMatchKey(rawNumber)

        // 1. Check saved local Room Contacts first
        val contact = contactDao.findByNormalizedNumber(normalized)
            ?: contactDao.getAllContactsOnce().firstOrNull { T9SearchEngine.numberMatchKey(it.primaryNumber) == matchKey }

        if (contact != null) {
            return@withContext ResolvedCallerIdentity(
                displayName = contact.displayName,
                number = rawNumber,
                photoUri = contact.photoUri,
                isSavedContact = true,
                source = null
            )
        }

        val prefs = preferencesRepository.getSnapshot()
        if (!prefs.findNameCallerEnabled) {
            return@withContext ResolvedCallerIdentity(
                displayName = rawNumber,
                number = rawNumber,
                photoUri = null,
                isSavedContact = false,
                source = null
            )
        }

        // 2. Check Social Auto-Finder local cache (WhatsApp / Telegram / Messenger / Accessibility)
        val cached = callerIdCacheDao.findByNumber(normalized)
        if (cached != null) {
            return@withContext ResolvedCallerIdentity(
                displayName = cached.displayName,
                number = rawNumber,
                photoUri = cached.photoUri,
                isSavedContact = false,
                source = runCatching { CallerIdSource.valueOf(cached.source) }.getOrNull()
            )
        }

        // 3. Fallback to Custom Caller ID REST API if enabled
        if (prefs.customCallerIdApiEnabled && prefs.customCallerIdApiUrl.isNotBlank()) {
            val apiResult = customApiClient.lookupNumber(
                baseUrl = prefs.customCallerIdApiUrl,
                apiToken = prefs.customCallerIdApiToken,
                normalizedNumber = normalized
            )
            if (apiResult != null) {
                callerIdCacheDao.upsertEntry(
                    CallerIdCacheEntity(
                        normalizedNumber = normalized,
                        rawNumber = rawNumber,
                        displayName = apiResult.displayName,
                        photoUri = apiResult.photoUrl,
                        source = CallerIdSource.CUSTOM_API.name,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                return@withContext ResolvedCallerIdentity(
                    displayName = apiResult.displayName,
                    number = rawNumber,
                    photoUri = apiResult.photoUrl,
                    isSavedContact = false,
                    source = CallerIdSource.CUSTOM_API
                )
            }
        }

        ResolvedCallerIdentity(
            displayName = rawNumber,
            number = rawNumber,
            photoUri = null,
            isSavedContact = false,
            source = null
        )
    }
}
