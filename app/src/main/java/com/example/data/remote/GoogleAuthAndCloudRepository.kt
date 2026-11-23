package com.example.data.remote

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.BuildConfig
import com.example.data.local.CallLogDao
import com.example.data.local.CallLogEntity
import com.example.data.local.ContactDao
import com.example.data.local.ContactEntity
import com.example.data.local.ContactGroupEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.remote.CustomCallerIdApiClient.Companion.toJsonRequestBody
import com.example.data.repository.T9SearchEngine
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class GoogleSignInOutcome(
    val email: String,
    val displayName: String,
    val photoUrl: String?,
    val pendingAuthIntent: PendingIntent?,
    val detectedBackup: DriveBackupMetadata?
)

data class DriveBackupMetadata(
    val fileId: String,
    val fileName: String,
    val sizeBytes: Long,
    val modifiedTimeIso: String,
    val modifiedTimestampMillis: Long
)

class GoogleAuthAndCloudRepository(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val contactDao: ContactDao,
    private val callLogDao: CallLogDao,
    private val peopleApi: GooglePeopleApi,
    private val driveApi: GoogleDriveApi
) {
    private val requiredScopes = listOf(
        Scope("https://www.googleapis.com/auth/contacts"),
        Scope("https://www.googleapis.com/auth/drive.appdata")
    )

    @Volatile
    private var cachedAccessToken: String? = null

    fun isWebClientIdConfigured(): Boolean {
        val id = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        return id.isNotBlank() && !id.equals("YOUR_GOOGLE_WEB_CLIENT_ID", ignoreCase = true)
    }

    fun isOnWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * One-Tap Google Sign-In via Android Credential Manager.
     * Displays device Gmail accounts for immediate sign-in without password entry.
     */
    suspend fun signInWithGoogle(activity: Activity): Result<GoogleSignInOutcome> = withContext(Dispatchers.Main) {
        if (!isWebClientIdConfigured()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Set GOOGLE_WEB_CLIENT_ID in the AI Studio Secrets panel to enable Google Sign-In, People Sync & Drive Backup."
                )
            )
        }
        runCatching {
            val credentialManager = CredentialManager.create(activity)
            val googleOption = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID.trim())
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()

            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                val email = googleCred.id
                val displayName = googleCred.displayName ?: email.substringBefore("@")
                val photoUrl = googleCred.profilePictureUri?.toString()

                preferencesRepository.update {
                    it.copy(
                        googleAccountEmail = email,
                        googleAccountName = displayName,
                        googleAccountPhotoUrl = photoUrl
                    )
                }

                // Request OAuth2 access token for People API & Drive appDataFolder
                val authResult = authorizeGoogleApis()
                val detected = if (authResult.first != null) {
                    cachedAccessToken = authResult.first
                    checkExistingDriveBackup().getOrNull()
                } else {
                    null
                }

                GoogleSignInOutcome(
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    pendingAuthIntent = authResult.second,
                    detectedBackup = detected
                )
            } else {
                error("Unexpected credential type returned by Credential Manager.")
            }
        }
    }

    suspend fun authorizeGoogleApis(): Pair<String?, PendingIntent?> = withContext(Dispatchers.IO) {
        runCatching {
            val authReq = AuthorizationRequest.builder()
                .setRequestedScopes(requiredScopes)
                .build()
            val result = Identity.getAuthorizationClient(context).authorize(authReq).awaitTask()
            if (result.hasResolution()) {
                null to result.pendingIntent
            } else {
                cachedAccessToken = result.accessToken
                result.accessToken to null
            }
        }.getOrElse { null to null }
    }

    fun onAuthorizationResult(accessToken: String?) {
        if (!accessToken.isNullOrBlank()) {
            cachedAccessToken = accessToken
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            cachedAccessToken = null
            runCatching {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            }
            preferencesRepository.update {
                it.copy(
                    googleAccountEmail = null,
                    googleAccountName = null,
                    googleAccountPhotoUrl = null
                )
            }
        }
    }

    private suspend fun getValidBearerToken(): String? {
        cachedAccessToken?.let { return "Bearer $it" }
        val (token, _) = authorizeGoogleApis()
        return token?.let { "Bearer $it" }
    }

    /**
     * Two-way Google Contacts Sync via Google People API v1.
     */
    suspend fun syncGoogleContacts(forceEvenOnCellular: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = preferencesRepository.getSnapshot()
            if (!prefs.googleContactsTwoWaySync && !forceEvenOnCellular) {
                return@runCatching "Google Contacts sync is turned OFF in Settings."
            }
            if (prefs.googleContactsWifiOnly && !forceEvenOnCellular && !isOnWifi()) {
                return@runCatching "Waiting for Wi-Fi (Wi-Fi Only Sync enabled)."
            }
            val bearer = getValidBearerToken()
                ?: error("Sign in and authorize Google Contacts access first.")

            // 1. Pull Contact Groups
            val groupsResp = peopleApi.getContactGroups(bearer)
            if (groupsResp.isSuccessful) {
                val groupsJson = JSONObject(groupsResp.body()?.string().orEmpty())
                val arr = groupsJson.optJSONArray("contactGroups") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val g = arr.getJSONObject(i)
                    val name = g.optString("formattedName").ifBlank { g.optString("name") }
                    val resName = g.optString("resourceName").takeIf { it.isNotBlank() }
                    if (name.isNotBlank() && g.optString("groupType") == "USER_CONTACT_GROUP") {
                        contactDao.insertGroup(ContactGroupEntity(googleResourceName = resName, name = name))
                    }
                }
            }

            // 2. Pull Connections (Names, Numbers, High-Res Photos)
            val connResp = peopleApi.getConnections(bearer)
            if (!connResp.isSuccessful) {
                error("Google People API sync failed (HTTP ${connResp.code()}).")
            }
            val root = JSONObject(connResp.body()?.string().orEmpty())
            val connections = root.optJSONArray("connections") ?: JSONArray()
            val remoteNumbers = mutableSetOf<String>()
            var pulled = 0

            for (i in 0 until connections.length()) {
                val person = connections.getJSONObject(i)
                val resourceName = person.optString("resourceName").takeIf { it.isNotBlank() }
                val phones = person.optJSONArray("phoneNumbers") ?: continue
                if (phones.length() == 0) continue
                val primaryNum = phones.getJSONObject(0).optString("value").trim()
                if (primaryNum.isBlank()) continue
                val secondaryNum = if (phones.length() > 1) phones.getJSONObject(1).optString("value").trim() else null

                val names = person.optJSONArray("names")
                val displayName = if (names != null && names.length() > 0) {
                    names.getJSONObject(0).optString("displayName").ifBlank { primaryNum }
                } else {
                    primaryNum
                }
                val photos = person.optJSONArray("photos")
                val photoUrl = if (photos != null && photos.length() > 0) {
                    photos.getJSONObject(0).optString("url").takeIf { it.isNotBlank() }
                } else {
                    null
                }
                val emails = person.optJSONArray("emailAddresses")
                val email = if (emails != null && emails.length() > 0) {
                    emails.getJSONObject(0).optString("value").takeIf { it.isNotBlank() }
                } else {
                    null
                }

                val normalized = T9SearchEngine.normalizePhoneNumber(primaryNum)
                remoteNumbers.add(T9SearchEngine.numberMatchKey(primaryNum))

                val existing = (resourceName?.let { contactDao.findByGoogleResourceName(it) })
                    ?: contactDao.findByNormalizedNumber(normalized)

                contactDao.upsertContact(
                    ContactEntity(
                        id = existing?.id ?: 0L,
                        systemContactId = existing?.systemContactId,
                        googleResourceName = resourceName ?: existing?.googleResourceName,
                        displayName = displayName,
                        primaryNumber = primaryNum,
                        normalizedNumber = normalized,
                        secondaryNumber = secondaryNum ?: existing?.secondaryNumber,
                        email = email ?: existing?.email,
                        photoUri = photoUrl ?: existing?.photoUri,
                        isFavorite = existing?.isFavorite ?: false,
                        groupName = existing?.groupName ?: "Google",
                        timesContacted = existing?.timesContacted ?: 0,
                        lastContactedAt = existing?.lastContactedAt ?: 0L,
                        notes = existing?.notes.orEmpty(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                pulled++
            }

            // 3. Push local-only contacts back to Google People API (Two-way)
            var pushed = 0
            val locals = contactDao.getAllContactsOnce().filter {
                it.googleResourceName == null && T9SearchEngine.numberMatchKey(it.primaryNumber) !in remoteNumbers
            }
            for (local in locals.take(25)) {
                val createdResource = pushContactToGoogleInternal(bearer, local.displayName, local.primaryNumber)
                if (createdResource != null) {
                    contactDao.updateContact(local.copy(googleResourceName = createdResource))
                    pushed++
                }
            }

            preferencesRepository.update { it.copy(lastContactsSyncMillis = System.currentTimeMillis()) }
            "Synced $pulled Google contacts (pushed $pushed local)."
        }
    }

    /**
     * One-Tap Save to Google Contacts via People API.
     */
    suspend fun createGoogleContact(
        displayName: String,
        phoneNumber: String
    ): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            val bearer = getValidBearerToken() ?: return@runCatching null
            pushContactToGoogleInternal(bearer, displayName, phoneNumber)
        }
    }

    private suspend fun pushContactToGoogleInternal(
        bearer: String,
        displayName: String,
        phoneNumber: String
    ): String? {
        val bodyJson = JSONObject().apply {
            put("names", JSONArray().put(JSONObject().put("givenName", displayName)))
            put("phoneNumbers", JSONArray().put(JSONObject().put("value", phoneNumber).put("type", "mobile")))
        }
        val resp = peopleApi.createContact(bearer, bodyJson.toString().toJsonRequestBody())
        if (!resp.isSuccessful) return null
        val respJson = JSONObject(resp.body()?.string().orEmpty())
        return respJson.optString("resourceName").takeIf { it.isNotBlank() }
    }

    /**
     * Detect existing PulsePhone backup inside Google Drive appDataFolder upon Sign-In.
     */
    suspend fun checkExistingDriveBackup(): Result<DriveBackupMetadata?> = withContext(Dispatchers.IO) {
        runCatching {
            val bearer = getValidBearerToken() ?: return@runCatching null
            val resp = driveApi.listAppDataFiles(bearer)
            if (!resp.isSuccessful) return@runCatching null
            val root = JSONObject(resp.body()?.string().orEmpty())
            val files = root.optJSONArray("files") ?: return@runCatching null
            if (files.length() == 0) return@runCatching null
            val file = files.getJSONObject(0)
            val modifiedIso = file.optString("modifiedTime")
            DriveBackupMetadata(
                fileId = file.getString("id"),
                fileName = file.optString("name", "pulsephone_backup.json"),
                sizeBytes = file.optString("size").toLongOrNull() ?: 0L,
                modifiedTimeIso = modifiedIso,
                modifiedTimestampMillis = parseIsoTimestamp(modifiedIso)
            )
        }
    }

    /**
     * Back up Unlimited Call Logs, Favorites, Contacts, and all DataStore Settings to Google Drive appDataFolder.
     */
    suspend fun backupToGoogleDrive(respectWifiConstraint: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = preferencesRepository.getSnapshot()
            if (respectWifiConstraint && prefs.driveBackupWifiOnly && !isOnWifi()) {
                return@runCatching "Deferred: waiting for Wi-Fi connection."
            }
            val bearer = getValidBearerToken()
                ?: error("Sign in with Google and grant Drive AppData permission first.")

            val logs = callLogDao.getAllCallLogsOnce()
            val contacts = contactDao.getAllContactsOnce()
            val payload = JSONObject().apply {
                put("app", "PulsePhone")
                put("schemaVersion", 1)
                put("backupTimestamp", System.currentTimeMillis())
                put("settings", preferencesRepository.toJson(prefs))
                put("callLogs", JSONArray().apply {
                    logs.forEach { log ->
                        put(
                            JSONObject().apply {
                                put("number", log.number)
                                put("cachedName", log.cachedName ?: "")
                                put("cachedPhotoUri", log.cachedPhotoUri ?: "")
                                put("direction", log.direction)
                                put("timestamp", log.timestamp)
                                put("durationSec", log.durationSec)
                                put("simSlot", log.simSlot)
                            }
                        )
                    }
                })
                put("favorites", JSONArray().apply {
                    contacts.filter { it.isFavorite }.forEach { fav ->
                        put(
                            JSONObject().apply {
                                put("displayName", fav.displayName)
                                put("primaryNumber", fav.primaryNumber)
                                put("groupName", fav.groupName)
                                put("photoUri", fav.photoUri ?: "")
                            }
                        )
                    }
                })
            }.toString()

            val existing = checkExistingDriveBackup().getOrNull()
            val response = if (existing != null) {
                driveApi.updateMedia(bearer, existing.fileId, payload.toJsonRequestBody())
            } else {
                val boundary = "pulsephone_boundary_${System.currentTimeMillis()}"
                val metadata = JSONObject().apply {
                    put("name", "pulsephone_backup.json")
                    put("parents", JSONArray().put("appDataFolder"))
                }.toString()
                val multipart = buildString {
                    append("--$boundary\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(metadata)
                    append("\r\n--$boundary\r\n")
                    append("Content-Type: application/json\r\n\r\n")
                    append(payload)
                    append("\r\n--$boundary--")
                }
                driveApi.uploadMultipart(
                    bearer,
                    multipart.toRequestBody("multipart/related; boundary=$boundary".toMediaType())
                )
            }

            if (!response.isSuccessful) {
                error("Google Drive backup failed (HTTP ${response.code()}).")
            }

            preferencesRepository.update { it.copy(lastDriveBackupMillis = System.currentTimeMillis()) }
            "Backed up ${logs.size} call logs, ${contacts.count { it.isFavorite }} favorites & all settings to Google Drive."
        }
    }

    /**
     * Restore Call Logs, Favorites, and DataStore Settings from Google Drive appDataFolder.
     */
    suspend fun restoreFromGoogleDrive(fileId: String? = null): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bearer = getValidBearerToken()
                ?: error("Sign in with Google to restore from Drive.")
            val targetId = fileId ?: checkExistingDriveBackup().getOrNull()?.fileId
                ?: error("No PulsePhone backup found in Google Drive.")

            val resp = driveApi.downloadFile(bearer, targetId)
            if (!resp.isSuccessful) {
                error("Failed to download Drive backup (HTTP ${resp.code()}).")
            }
            val root = JSONObject(resp.body()?.string().orEmpty())

            root.optJSONObject("settings")?.let { settingsJson ->
                preferencesRepository.importFromJson(settingsJson)
            }

            val restoredLogs = mutableListOf<CallLogEntity>()
            val logsArr = root.optJSONArray("callLogs") ?: JSONArray()
            for (i in 0 until logsArr.length()) {
                val obj = logsArr.getJSONObject(i)
                val number = obj.optString("number").trim()
                if (number.isBlank()) continue
                restoredLogs.add(
                    CallLogEntity(
                        number = number,
                        normalizedNumber = T9SearchEngine.normalizePhoneNumber(number),
                        cachedName = obj.optString("cachedName").takeIf { it.isNotBlank() },
                        cachedPhotoUri = obj.optString("cachedPhotoUri").takeIf { it.isNotBlank() },
                        direction = obj.optString("direction", "OUTGOING"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        durationSec = obj.optLong("durationSec", 0L),
                        simSlot = obj.optInt("simSlot", 1).coerceIn(1, 2)
                    )
                )
            }
            if (restoredLogs.isNotEmpty()) {
                callLogDao.insertCallLogs(restoredLogs)
            }

            val favArr = root.optJSONArray("favorites") ?: JSONArray()
            var restoredFavs = 0
            for (i in 0 until favArr.length()) {
                val obj = favArr.getJSONObject(i)
                val number = obj.optString("primaryNumber").trim()
                if (number.isBlank()) continue
                val normalized = T9SearchEngine.normalizePhoneNumber(number)
                val existing = contactDao.findByNormalizedNumber(normalized)
                if (existing != null) {
                    contactDao.setFavorite(existing.id, true)
                } else {
                    contactDao.upsertContact(
                        ContactEntity(
                            displayName = obj.optString("displayName").ifBlank { number },
                            primaryNumber = number,
                            normalizedNumber = normalized,
                            photoUri = obj.optString("photoUri").takeIf { it.isNotBlank() },
                            isFavorite = true,
                            groupName = obj.optString("groupName", "General").ifBlank { "General" }
                        )
                    )
                }
                restoredFavs++
            }

            "Restored ${restoredLogs.size} call logs, $restoredFavs favorites & all settings from Google Drive."
        }
    }

    private fun parseIsoTimestamp(iso: String): Long {
        if (iso.isBlank()) return System.currentTimeMillis()
        return runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(iso)?.time ?: System.currentTimeMillis()
        }.getOrDefault(System.currentTimeMillis())
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exc ->
        if (cont.isActive) cont.resumeWithException(exc)
    }
    addOnCanceledListener {
        cont.cancel()
    }
}
