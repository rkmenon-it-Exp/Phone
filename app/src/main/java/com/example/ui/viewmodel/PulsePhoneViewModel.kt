package com.example.ui.viewmodel

import android.app.Activity
import android.app.PendingIntent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.CallDirection
import com.example.data.local.CallLogEntity
import com.example.data.local.CallerIdSource
import com.example.data.local.ContactEntity
import com.example.data.local.ContactGroupEntity
import com.example.data.preferences.AppTab
import com.example.data.preferences.InCallActionButton
import com.example.data.preferences.PulsePhonePreferences
import com.example.data.preferences.SimCallingMode
import com.example.data.remote.DriveBackupMetadata
import com.example.data.repository.DisplayCallLogItem
import com.example.data.repository.SimHardwareInfo
import com.example.data.repository.T9SearchEngine
import com.example.data.repository.T9SearchResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RecentsFilter(val label: String) {
    ALL("All"),
    MISSED("Missed"),
    INCOMING("Incoming"),
    OUTGOING("Outgoing"),
    SIM_1("SIM 1"),
    SIM_2("SIM 2")
}

class PulsePhoneViewModel(private val container: AppContainer) : ViewModel() {

    val preferences: StateFlow<PulsePhonePreferences> = container.preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PulsePhonePreferences())

    val allContacts: StateFlow<List<ContactEntity>> = container.dialerContactsRepository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteContacts: StateFlow<List<ContactEntity>> = container.dialerContactsRepository.favoriteContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val frequentlyContacted: StateFlow<List<ContactEntity>> = container.dialerContactsRepository.frequentlyContacted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGroups: StateFlow<List<ContactGroupEntity>> = container.dialerContactsRepository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCallLogCount: StateFlow<Int> = container.dialerContactsRepository.totalCallLogCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val rawCallLogs: StateFlow<List<CallLogEntity>> = container.dialerContactsRepository.allCallLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _simHardwareInfo = MutableStateFlow(container.dialerContactsRepository.detectSimHardware())
    val simHardwareInfo: StateFlow<SimHardwareInfo> = _simHardwareInfo.asStateFlow()

    fun refreshSimHardware() {
        _simHardwareInfo.value = container.dialerContactsRepository.detectSimHardware()
    }

    fun isDualSimEffective(prefs: PulsePhonePreferences = preferences.value): Boolean {
        return _simHardwareInfo.value.isDualSimAvailable || prefs.forceShowDualSimPreview
    }

    val displayCallLogs: StateFlow<List<DisplayCallLogItem>> = combine(
        container.dialerContactsRepository.allCallLogs,
        container.dialerContactsRepository.allContacts,
        container.dialerContactsRepository.callerIdCacheEntries,
        container.preferencesRepository.preferencesFlow
    ) { logs, contacts, callerIdEntries, prefs ->
        T9SearchEngine.buildDisplayCallLogs(
            logs = logs,
            contacts = contacts,
            callerIdEntries = callerIdEntries,
            groupConsecutive = prefs.groupConsecutiveCalls,
            findNameCallerEnabled = prefs.findNameCallerEnabled
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dialedQuery = MutableStateFlow("")
    val dialedQuery: StateFlow<String> = _dialedQuery.asStateFlow()

    val t9Results: StateFlow<List<T9SearchResult>> = combine(
        _dialedQuery,
        container.dialerContactsRepository.allContacts,
        container.dialerContactsRepository.callerIdCacheEntries,
        container.preferencesRepository.preferencesFlow
    ) { query, contacts, callerIds, prefs ->
        T9SearchEngine.search(
            query = query,
            contacts = contacts,
            callerIdEntries = if (prefs.findNameCallerEnabled) {
                callerIds.associateBy { it.normalizedNumber }
            } else {
                emptyMap()
            },
            t9Enabled = prefs.t9SearchEnabled
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pendingSimChoiceNumber = MutableStateFlow<String?>(null)
    val pendingSimChoiceNumber: StateFlow<String?> = _pendingSimChoiceNumber.asStateFlow()

    private val _detectedDriveBackup = MutableStateFlow<DriveBackupMetadata?>(null)
    val detectedDriveBackup: StateFlow<DriveBackupMetadata?> = _detectedDriveBackup.asStateFlow()

    private val _pendingGoogleAuthIntent = MutableStateFlow<PendingIntent?>(null)
    val pendingGoogleAuthIntent: StateFlow<PendingIntent?> = _pendingGoogleAuthIntent.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _showCallerIdWizard = MutableStateFlow(false)
    val showCallerIdWizard: StateFlow<Boolean> = _showCallerIdWizard.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val isDefaultDialer: Boolean
        get() = container.dialerContactsRepository.isDefaultDialer()

    fun createDefaultDialerIntent() = container.dialerContactsRepository.createDefaultDialerRequestIntent()

    fun appendDialChar(ch: Char) {
        _dialedQuery.value += ch
    }

    fun backspaceDial() {
        val cur = _dialedQuery.value
        if (cur.isNotEmpty()) _dialedQuery.value = cur.dropLast(1)
    }

    fun clearDial() {
        _dialedQuery.value = ""
    }

    fun setDialQuery(number: String) {
        _dialedQuery.value = number
    }

    fun dismissSimPicker() {
        _pendingSimChoiceNumber.value = null
    }

    fun initiateCallRespectingSimMode(number: String, explicitSimSlot: Int? = null) {
        val clean = number.trim()
        if (clean.isEmpty()) return
        val prefs = preferences.value
        val dualSim = isDualSimEffective(prefs)

        // Single-SIM device: never show SIM picker or dial SIM 2
        if (!dualSim) {
            placeCallNow(clean, 1)
            return
        }

        // Explicit SIM button press (e.g. SIM 1 / SIM 2 split button or Long-Press Other SIM)
        if (explicitSimSlot != null) {
            placeCallNow(clean, explicitSimSlot.coerceIn(1, 2))
            return
        }

        viewModelScope.launch {
            // Smart Last-Used SIM memory: reuse the SIM slot from the latest call log with this number
            if (prefs.rememberLastUsedSim) {
                val lastUsedSim = container.dialerContactsRepository.findLastUsedSimSlot(clean)
                if (lastUsedSim != null) {
                    placeCallNow(clean, lastUsedSim.coerceIn(1, 2))
                    return@launch
                }
            }
            when (prefs.simCallingMode) {
                SimCallingMode.ASK_EVERY_TIME -> _pendingSimChoiceNumber.value = clean
                SimCallingMode.SINGLE_DEFAULT_SIM -> placeCallNow(clean, prefs.defaultSimSlot)
                SimCallingMode.DUAL_SIM_SPLIT -> placeCallNow(clean, prefs.defaultSimSlot)
            }
        }
    }

    /**
     * Quick-callback from a recent call log item: calls back on the exact SIM used in that log
     * when Dual-SIM is available and rememberLastUsedSim is ON; falls back to SIM 1 on single-SIM devices.
     */
    fun callBackFromRecentLog(item: DisplayCallLogItem) {
        val prefs = preferences.value
        if (!isDualSimEffective(prefs)) {
            placeCallNow(item.number, 1)
            return
        }
        if (prefs.rememberLastUsedSim) {
            placeCallNow(item.number, item.simSlot.coerceIn(1, 2))
        } else {
            when (prefs.simCallingMode) {
                SimCallingMode.ASK_EVERY_TIME -> _pendingSimChoiceNumber.value = item.number
                SimCallingMode.SINGLE_DEFAULT_SIM,
                SimCallingMode.DUAL_SIM_SPLIT -> placeCallNow(item.number, prefs.defaultSimSlot)
            }
        }
    }

    fun placeCallNow(number: String, simSlot: Int) {
        _pendingSimChoiceNumber.value = null
        viewModelScope.launch {
            val result = container.dialerContactsRepository.placeOutgoingCall(number, simSlot)
            result.onSuccess { msg -> _messages.tryEmit(msg) }
                .onFailure { err -> _messages.tryEmit(err.message ?: "Unable to place call.") }
        }
    }

    fun importFromDevice() {
        viewModelScope.launch {
            _isBusy.value = true
            val summary = container.dialerContactsRepository.importFromDevice()
            _isBusy.value = false
            _messages.tryEmit(
                "Synced ${summary.importedContacts} contacts, ${summary.importedCallLogs} call logs & ${summary.importedGroups} groups from device."
            )
        }
    }

    fun saveContact(contact: ContactEntity) {
        viewModelScope.launch {
            container.dialerContactsRepository.upsertContact(contact)
            _messages.tryEmit("Saved ${contact.displayName}.")
        }
    }

    fun deleteContact(contactId: Long) {
        viewModelScope.launch {
            container.dialerContactsRepository.deleteContact(contactId)
            _messages.tryEmit("Contact deleted.")
        }
    }

    fun toggleFavorite(contactId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            container.dialerContactsRepository.toggleFavorite(contactId, isFavorite)
        }
    }

    fun addGroup(name: String) {
        viewModelScope.launch {
            container.dialerContactsRepository.addGroup(name)
            _messages.tryEmit("Group \"$name\" created.")
        }
    }

    fun deleteGroup(group: ContactGroupEntity) {
        viewModelScope.launch {
            container.dialerContactsRepository.deleteGroup(group)
        }
    }

    fun deleteCallLogItem(item: DisplayCallLogItem) {
        viewModelScope.launch {
            container.dialerContactsRepository.deleteCallLogGroup(item.groupedIds)
            _messages.tryEmit("Removed from call history.")
        }
    }

    fun oneTapSaveToGoogleContacts(displayName: String, number: String, photoUri: String?) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = container.dialerContactsRepository.oneTapSaveToGoogleContacts(
                displayName = displayName,
                phoneNumber = number,
                photoUri = photoUri
            )
            _isBusy.value = false
            result.onSuccess { _messages.tryEmit(it) }
                .onFailure { _messages.tryEmit(it.message ?: "Save failed.") }
        }
    }

    fun lookupUnknownNumberNow(number: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val resolved = container.dialerContactsRepository.resolveCallerIdentity(number)
            _isBusy.value = false
            val msg = if (resolved.source != null) {
                "Identified ${resolved.displayName} via ${resolved.source.label}."
            } else if (resolved.isSavedContact) {
                "Already saved as ${resolved.displayName}."
            } else {
                "No public social/API match found yet for $number."
            }
            _messages.tryEmit(msg)
        }
    }

    // --- Granular Preferences Mutations ---

    fun updatePreferences(transform: (PulsePhonePreferences) -> PulsePhonePreferences) {
        viewModelScope.launch {
            container.preferencesRepository.update(transform)
            val snapshot = container.preferencesRepository.getSnapshot()
            container.workScheduler.syncSchedules(snapshot)
        }
    }

    fun toggleFindNameCaller(enabled: Boolean) {
        viewModelScope.launch {
            val current = container.preferencesRepository.getSnapshot()
            if (enabled && !current.callerIdWizardSeen) {
                _showCallerIdWizard.value = true
            }
            container.preferencesRepository.update {
                it.copy(
                    findNameCallerEnabled = enabled,
                    callerIdWizardSeen = if (enabled) true else it.callerIdWizardSeen
                )
            }
        }
    }

    fun openCallerIdWizard() {
        _showCallerIdWizard.value = true
    }

    fun dismissCallerIdWizard() {
        _showCallerIdWizard.value = false
    }

    fun toggleTabEnabled(tab: AppTab, enabled: Boolean) {
        updatePreferences { cur ->
            val nextSet = if (enabled) cur.enabledTabs + tab else cur.enabledTabs - tab
            // Always keep at least one tab enabled
            val safeSet = nextSet.ifEmpty { setOf(tab) }
            val nextStartup = if (cur.defaultStartupTab in safeSet) cur.defaultStartupTab else safeSet.first()
            cur.copy(enabledTabs = safeSet, defaultStartupTab = nextStartup)
        }
    }

    fun moveTabOrder(tab: AppTab, moveUp: Boolean) {
        updatePreferences { cur ->
            val list = cur.orderedTabs.toMutableList()
            val idx = list.indexOf(tab)
            val targetIdx = if (moveUp) idx - 1 else idx + 1
            if (idx >= 0 && targetIdx in list.indices) {
                val item = list.removeAt(idx)
                list.add(targetIdx, item)
            }
            cur.copy(orderedTabs = list)
        }
    }

    fun toggleInCallAction(action: InCallActionButton, enabled: Boolean) {
        updatePreferences { cur ->
            val next = if (enabled) cur.enabledInCallActions + action else cur.enabledInCallActions - action
            cur.copy(enabledInCallActions = next)
        }
    }

    fun moveInCallAction(action: InCallActionButton, moveUp: Boolean) {
        updatePreferences { cur ->
            val list = cur.inCallActionOrder.toMutableList()
            val idx = list.indexOf(action)
            val targetIdx = if (moveUp) idx - 1 else idx + 1
            if (idx >= 0 && targetIdx in list.indices) {
                val item = list.removeAt(idx)
                list.add(targetIdx, item)
            }
            cur.copy(inCallActionOrder = list)
        }
    }

    fun applyIconStyleGroup(style: com.example.data.preferences.IconDesignStyle) {
        updatePreferences { cur ->
            cur.copy(
                iconStyleGroup = style,
                numericButtonStyle = style,
                simButtonStyle = style,
                bottomTabIconStyle = style
            )
        }
        _messages.tryEmit("Applied ${style.label} icon pack to all buttons & tabs.")
    }

    fun applyPreinstalledThemePreset(preset: com.example.data.preferences.PreinstalledThemePreset) {
        updatePreferences { cur ->
            cur.copy(
                themeMode = preset.themeMode,
                useDynamicColor = false,
                accentPalette = preset.accentPalette,
                iconStyleGroup = preset.iconStyle,
                numericButtonStyle = preset.iconStyle,
                simButtonStyle = preset.iconStyle,
                bottomTabIconStyle = preset.iconStyle
            )
        }
        _messages.tryEmit("Applied ${preset.title} theme.")
    }

    fun applyRichPreset() {
        viewModelScope.launch {
            container.preferencesRepository.applyRichTruePhonePreset()
            container.workScheduler.syncSchedules(container.preferencesRepository.getSnapshot())
            _messages.tryEmit("Applied Rich True Phone preset.")
        }
    }

    fun applyMinimalPreset() {
        viewModelScope.launch {
            container.preferencesRepository.applyMinimalPreset()
            container.workScheduler.syncSchedules(container.preferencesRepository.getSnapshot())
            _messages.tryEmit("Applied Minimalist preset.")
        }
    }

    // --- Google Sign-In, Sync, Drive Backup & Auto-Restore ---

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = container.googleCloudRepo.signInWithGoogle(activity)
            _isBusy.value = false
            result.onSuccess { outcome ->
                _messages.tryEmit("Signed in as ${outcome.email}")
                if (outcome.pendingAuthIntent != null) {
                    _pendingGoogleAuthIntent.value = outcome.pendingAuthIntent
                }
                if (outcome.detectedBackup != null) {
                    _detectedDriveBackup.value = outcome.detectedBackup
                }
                container.workScheduler.syncSchedules(container.preferencesRepository.getSnapshot())
            }.onFailure { err ->
                _messages.tryEmit(err.message ?: "Google Sign-In cancelled.")
            }
        }
    }

    fun clearPendingGoogleAuthIntent() {
        _pendingGoogleAuthIntent.value = null
    }

    fun onGoogleAuthorizationGranted(accessToken: String?) {
        container.googleCloudRepo.onAuthorizationResult(accessToken)
        viewModelScope.launch {
            val backup = container.googleCloudRepo.checkExistingDriveBackup().getOrNull()
            if (backup != null) {
                _detectedDriveBackup.value = backup
            }
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            container.googleCloudRepo.signOut()
            container.workScheduler.syncSchedules(container.preferencesRepository.getSnapshot())
            _messages.tryEmit("Signed out of Google account.")
        }
    }

    fun syncGoogleContactsNow() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = container.googleCloudRepo.syncGoogleContacts(forceEvenOnCellular = true)
            _isBusy.value = false
            res.onSuccess { _messages.tryEmit(it) }
                .onFailure { _messages.tryEmit(it.message ?: "Google Contacts sync failed.") }
        }
    }

    fun backupToGoogleDriveNow() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = container.googleCloudRepo.backupToGoogleDrive(respectWifiConstraint = false)
            _isBusy.value = false
            res.onSuccess { _messages.tryEmit(it) }
                .onFailure { _messages.tryEmit(it.message ?: "Google Drive backup failed.") }
        }
    }

    fun restoreFromGoogleDriveNow(fileId: String? = null) {
        _detectedDriveBackup.value = null
        viewModelScope.launch {
            _isBusy.value = true
            val res = container.googleCloudRepo.restoreFromGoogleDrive(fileId)
            _isBusy.value = false
            res.onSuccess { _messages.tryEmit(it) }
                .onFailure { _messages.tryEmit(it.message ?: "Google Drive restore failed.") }
        }
    }

    fun dismissDriveBackupDialog() {
        _detectedDriveBackup.value = null
    }

    // --- SAF Local JSON / CSV Export & Import ---

    fun exportSettingsJson(uri: Uri) = runTransfer { container.localTransferManager.exportSettingsJson(uri) }
    fun importSettingsJson(uri: Uri) = runTransfer { container.localTransferManager.importSettingsJson(uri) }
    fun exportCallLogsJson(uri: Uri) = runTransfer { container.localTransferManager.exportCallLogsJson(uri) }
    fun exportCallLogsCsv(uri: Uri) = runTransfer { container.localTransferManager.exportCallLogsCsv(uri) }
    fun importCallLogs(uri: Uri) = runTransfer { container.localTransferManager.importCallLogs(uri) }
    fun exportContactsJson(uri: Uri) = runTransfer { container.localTransferManager.exportContactsJson(uri) }
    fun exportContactsCsv(uri: Uri) = runTransfer { container.localTransferManager.exportContactsCsv(uri) }
    fun importContacts(uri: Uri) = runTransfer { container.localTransferManager.importContacts(uri) }

    private fun runTransfer(block: suspend () -> Result<String>) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = block()
            _isBusy.value = false
            res.onSuccess { _messages.tryEmit(it) }
                .onFailure { _messages.tryEmit(it.message ?: "File transfer failed.") }
        }
    }

    // --- In-Call Live Preview Launcher ---

    fun prepareInCallPreview(incoming: Boolean) {
        container.activeCallManager.startPreviewCall(
            incoming = incoming,
            callerName = "Elena Vance",
            phoneNumber = "+1 (415) 890-4421",
            isSavedContact = false,
            source = CallerIdSource.WHATSAPP
        )
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PulsePhoneViewModel(container) as T
        }
    }
}
