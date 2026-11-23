package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ContactEntity
import com.example.data.preferences.AppTab
import com.example.data.preferences.CallLogTapAction
import com.example.data.repository.DisplayCallLogItem
import com.example.data.repository.T9SearchEngine
import com.example.ui.components.CallLogDetailsDialog
import com.example.ui.components.CallLogHistoryDialog
import com.example.ui.components.CallLogLongPressMenuDialog
import com.example.ui.components.CallLogUserPreviewDialog
import com.example.ui.components.ContactEditorDialog
import com.example.ui.components.PulseBottomNavigationBar
import com.example.ui.components.pulseTabIcon
import com.example.ui.screens.settings.SafTransferAction
import com.example.ui.screens.settings.SettingsHubScreen
import com.example.ui.theme.Sim1OceanBlue
import com.example.ui.theme.Sim2DeepTeal
import com.example.ui.viewmodel.PulsePhoneViewModel
import com.google.android.gms.auth.api.identity.Identity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulsePhoneApp(
    viewModel: PulsePhoneViewModel,
    initialDialNumber: String? = null
) {
    val context = LocalContext.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val contacts by viewModel.allContacts.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteContacts.collectAsStateWithLifecycle()
    val frequentlyContacted by viewModel.frequentlyContacted.collectAsStateWithLifecycle()
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()
    val displayCallLogs by viewModel.displayCallLogs.collectAsStateWithLifecycle()
    val rawCallLogs by viewModel.rawCallLogs.collectAsStateWithLifecycle()
    val simHardwareInfo by viewModel.simHardwareInfo.collectAsStateWithLifecycle()
    val totalRawLogsCount by viewModel.totalCallLogCount.collectAsStateWithLifecycle()
    val dialedQuery by viewModel.dialedQuery.collectAsStateWithLifecycle()
    val t9Results by viewModel.t9Results.collectAsStateWithLifecycle()
    val pendingSimNumber by viewModel.pendingSimChoiceNumber.collectAsStateWithLifecycle()
    val detectedBackup by viewModel.detectedDriveBackup.collectAsStateWithLifecycle()
    val pendingAuthIntent by viewModel.pendingGoogleAuthIntent.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val showCallerIdWizard by viewModel.showCallerIdWizard.collectAsStateWithLifecycle()

    val isDualSimAvailable = simHardwareInfo.isDualSimAvailable || prefs.forceShowDualSimPreview

    val snackbarHostState = remember { SnackbarHostState() }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(prefs.effectiveStartupTab) }
    var startupApplied by rememberSaveable { mutableStateOf(false) }
    var bannerDismissed by rememberSaveable { mutableStateOf(false) }
    var isDefaultDialer by remember { mutableStateOf(viewModel.isDefaultDialer) }

    // Contact editor & Call Log interactive modal states
    var editingContact by remember { mutableStateOf<ContactEntity?>(null) }
    var creatingContactPrefill by remember { mutableStateOf<Pair<String, String>?>(null) }
    var longPressedLogItem by remember { mutableStateOf<DisplayCallLogItem?>(null) }
    var previewLogItem by remember { mutableStateOf<DisplayCallLogItem?>(null) }
    var detailsLogItem by remember { mutableStateOf<DisplayCallLogItem?>(null) }
    var historyLogItem by remember { mutableStateOf<DisplayCallLogItem?>(null) }

    // Ensure selectedTab stays within user's enabled tabs and respects startup tab on first load
    LaunchedEffect(prefs.activeOrderedTabs, prefs.effectiveStartupTab) {
        if (!startupApplied) {
            selectedTab = prefs.effectiveStartupTab
            startupApplied = true
        } else if (selectedTab !in prefs.activeOrderedTabs) {
            selectedTab = prefs.activeOrderedTabs.first()
        }
    }

    LaunchedEffect(initialDialNumber) {
        if (!initialDialNumber.isNullOrBlank()) {
            viewModel.setDialQuery(initialDialNumber)
            if (AppTab.PHONE in prefs.activeOrderedTabs) {
                selectedTab = AppTab.PHONE
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Core runtime permissions launcher (Contacts, Call Log, Phone)
    val requiredRuntimePermissions = remember {
        arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )
    }

    var hasCorePermissions by remember {
        mutableStateOf(
            requiredRuntimePermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCorePermissions = results.values.all { it }
        viewModel.refreshSimHardware()
        if (results[Manifest.permission.READ_CONTACTS] == true ||
            results[Manifest.permission.READ_CALL_LOG] == true
        ) {
            viewModel.importFromDevice()
        }
    }

    // Auto-import once on launch if device permissions are already granted
    LaunchedEffect(hasCorePermissions) {
        viewModel.refreshSimHardware()
        if (hasCorePermissions) {
            viewModel.importFromDevice()
        }
    }

    // RoleManager (ROLE_DIALER) Default Phone App launcher
    val defaultDialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultDialer = viewModel.isDefaultDialer
    }

    // Google OAuth2 Authorization resolution launcher
    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        runCatching {
            val authResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(res.data)
            viewModel.onGoogleAuthorizationGranted(authResult.accessToken)
        }
    }

    LaunchedEffect(pendingAuthIntent) {
        val intent = pendingAuthIntent
        if (intent != null) {
            viewModel.clearPendingGoogleAuthIntent()
            runCatching {
                googleAuthLauncher.launch(IntentSenderRequest.Builder(intent.intentSender).build())
            }
        }
    }

    // Zero-permission SAF JSON/CSV launchers
    var pendingSafAction by remember { mutableStateOf<SafTransferAction?>(null) }

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            when (pendingSafAction) {
                SafTransferAction.EXPORT_SETTINGS_JSON -> viewModel.exportSettingsJson(uri)
                SafTransferAction.EXPORT_LOGS_JSON -> viewModel.exportCallLogsJson(uri)
                SafTransferAction.EXPORT_CONTACTS_JSON -> viewModel.exportContactsJson(uri)
                else -> Unit
            }
        }
        pendingSafAction = null
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            when (pendingSafAction) {
                SafTransferAction.EXPORT_LOGS_CSV -> viewModel.exportCallLogsCsv(uri)
                SafTransferAction.EXPORT_CONTACTS_CSV -> viewModel.exportContactsCsv(uri)
                else -> Unit
            }
        }
        pendingSafAction = null
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            when (pendingSafAction) {
                SafTransferAction.IMPORT_SETTINGS_JSON -> viewModel.importSettingsJson(uri)
                SafTransferAction.IMPORT_LOGS -> viewModel.importCallLogs(uri)
                SafTransferAction.IMPORT_CONTACTS -> viewModel.importContacts(uri)
                else -> Unit
            }
        }
        pendingSafAction = null
    }

    val triggerSaf: (SafTransferAction) -> Unit = { action ->
        pendingSafAction = action
        when (action) {
            SafTransferAction.EXPORT_SETTINGS_JSON -> createJsonLauncher.launch("pulsephone_settings.json")
            SafTransferAction.EXPORT_LOGS_JSON -> createJsonLauncher.launch("pulsephone_call_logs.json")
            SafTransferAction.EXPORT_CONTACTS_JSON -> createJsonLauncher.launch("pulsephone_contacts.json")
            SafTransferAction.EXPORT_LOGS_CSV -> createCsvLauncher.launch("pulsephone_call_logs.csv")
            SafTransferAction.EXPORT_CONTACTS_CSV -> createCsvLauncher.launch("pulsephone_contacts.csv")
            SafTransferAction.IMPORT_SETTINGS_JSON -> openDocLauncher.launch(arrayOf("application/json", "*/*"))
            SafTransferAction.IMPORT_LOGS -> openDocLauncher.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "*/*"))
            SafTransferAction.IMPORT_CONTACTS -> openDocLauncher.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "*/*"))
        }
    }

    val requestDefaultDialer: () -> Unit = {
        val intent = viewModel.createDefaultDialerIntent()
        if (intent != null) {
            defaultDialerLauncher.launch(intent)
        }
    }

    if (showSettings) {
        Box(modifier = Modifier.fillMaxSize()) {
            SettingsHubScreen(
                prefs = prefs,
                viewModel = viewModel,
                isDefaultDialer = isDefaultDialer,
                showCallerIdWizard = showCallerIdWizard,
                onRequestDefaultDialer = requestDefaultDialer,
                onSignInGoogle = { act -> viewModel.signInWithGoogle(act) },
                onLaunchSafAction = triggerSaf,
                onBack = { showSettings = false }
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    } else {
        val activeTabs = prefs.activeOrderedTabs

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "PulsePhone",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = selectedTab.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    if (hasCorePermissions) {
                                        viewModel.importFromDevice()
                                    } else {
                                        permissionsLauncher.launch(requiredRuntimePermissions)
                                    }
                                },
                                modifier = Modifier.testTag("topbar_sync_device_button")
                            ) {
                                Icon(Icons.Filled.Sync, contentDescription = "Sync device contacts and call logs")
                            }
                            IconButton(
                                onClick = { showSettings = true },
                                modifier = Modifier.testTag("topbar_settings_button")
                            ) {
                                Icon(Icons.Filled.Settings, contentDescription = "Open Settings & Customization Hub")
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useRail && activeTabs.size > 1) {
                        PulseBottomNavigationBar(
                            tabs = activeTabs,
                            selectedTab = selectedTab,
                            iconStyle = prefs.bottomTabIconStyle,
                            barSize = prefs.bottomBarSize,
                            onSelectTab = { selectedTab = it }
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail && activeTabs.size > 1) {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            activeTabs.forEach { tab ->
                                val selected = tab == selectedTab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            pulseTabIcon(tab, selected, prefs.bottomTabIconStyle),
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) }
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        if (isBusy) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        // Compact permission / Default Dialer onboarding banner when needed
                        if (!bannerDismissed && (!hasCorePermissions || !isDefaultDialer)) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (!hasCorePermissions) "Grant Phone & Contacts Access" else "Set PulsePhone as Default Dialer",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = if (!hasCorePermissions) {
                                                "Import device call logs & contacts into unlimited Room storage."
                                            } else {
                                                "Enable custom OLED full-screen Caller ID & Dual-SIM calling."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (!hasCorePermissions) {
                                                permissionsLauncher.launch(requiredRuntimePermissions)
                                            } else {
                                                requestDefaultDialer()
                                            }
                                        },
                                        modifier = Modifier.testTag("onboarding_action_button")
                                    ) {
                                        Text(if (!hasCorePermissions) "Grant" else "Set")
                                    }
                                    TextButton(onClick = { bannerDismissed = true }) {
                                        Text("Later")
                                    }
                                }
                            }
                        }

                        val openByNumber: (String, String) -> Unit = { name, number ->
                            val key = T9SearchEngine.numberMatchKey(number)
                            val matched = contacts.firstOrNull { T9SearchEngine.numberMatchKey(it.primaryNumber) == key }
                            if (matched != null) {
                                editingContact = matched
                            } else {
                                creatingContactPrefill = name to number
                            }
                        }

                        // Strictly honor user's customizable CallLogTapAction from Settings
                        val handleCallLogRowClick: (DisplayCallLogItem) -> Unit = { item ->
                            when (prefs.callLogTapAction) {
                                CallLogTapAction.DIRECT_CALL -> viewModel.callBackFromRecentLog(item)
                                CallLogTapAction.USER_PREVIEW -> previewLogItem = item
                                CallLogTapAction.CALL_DETAILS -> detailsLogItem = item
                                CallLogTapAction.CALL_HISTORY -> historyLogItem = item
                            }
                        }

                        when (selectedTab) {
                            AppTab.FAVORITES -> FavoritesTabScreen(
                                prefs = prefs,
                                favorites = favorites,
                                frequentlyContacted = frequentlyContacted,
                                onCallContact = { c -> viewModel.initiateCallRespectingSimMode(c.primaryNumber) },
                                onOpenContactDetails = { c -> editingContact = c },
                                onToggleFavorite = { c -> viewModel.toggleFavorite(c.id, !c.isFavorite) },
                                modifier = Modifier.weight(1f)
                            )

                            AppTab.PHONE -> PhoneTabScreen(
                                prefs = prefs,
                                callLogs = displayCallLogs,
                                dialedQuery = dialedQuery,
                                t9Results = t9Results,
                                isDualSimAvailable = isDualSimAvailable,
                                sim1Label = simHardwareInfo.sim1Label,
                                sim2Label = simHardwareInfo.sim2Label,
                                onAppendChar = { viewModel.appendDialChar(it) },
                                onBackspace = { viewModel.backspaceDial() },
                                onClear = { viewModel.clearDial() },
                                onCallNumber = { num, sim -> viewModel.initiateCallRespectingSimMode(num, sim) },
                                onCallLogRowClick = handleCallLogRowClick,
                                onCallLogRowLongClick = { item -> longPressedLogItem = item },
                                onQuickCallLogItem = { item -> viewModel.callBackFromRecentLog(item) },
                                onOpenContactByNumber = openByNumber,
                                onOneTapSaveContact = { name, num, photo ->
                                    viewModel.oneTapSaveToGoogleContacts(name, num, photo)
                                },
                                onLookupUnknown = { num -> viewModel.lookupUnknownNumberNow(num) },
                                onDeleteCallLog = { item -> viewModel.deleteCallLogItem(item) },
                                onImportFromDevice = {
                                    if (hasCorePermissions) viewModel.importFromDevice()
                                    else permissionsLauncher.launch(requiredRuntimePermissions)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            AppTab.CONTACTS -> ContactsTabScreen(
                                contacts = contacts,
                                onCallContact = { c -> viewModel.initiateCallRespectingSimMode(c.primaryNumber) },
                                onOpenContact = { c -> editingContact = c },
                                onToggleFavorite = { c -> viewModel.toggleFavorite(c.id, !c.isFavorite) },
                                onAddNewContact = { creatingContactPrefill = "" to "" },
                                onImportFromDevice = {
                                    if (hasCorePermissions) viewModel.importFromDevice()
                                    else permissionsLauncher.launch(requiredRuntimePermissions)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            AppTab.GROUPS -> GroupsTabScreen(
                                contacts = contacts,
                                groups = groups,
                                onAddGroup = { viewModel.addGroup(it) },
                                onCallContact = { c -> viewModel.initiateCallRespectingSimMode(c.primaryNumber) },
                                onOpenContact = { c -> editingContact = c },
                                modifier = Modifier.weight(1f)
                            )

                            AppTab.RECENTS -> RecentsTabScreen(
                                prefs = prefs,
                                callLogs = displayCallLogs,
                                totalRawLogsCount = totalRawLogsCount,
                                isDualSimAvailable = isDualSimAvailable,
                                onCallLogRowClick = handleCallLogRowClick,
                                onCallLogRowLongClick = { item -> longPressedLogItem = item },
                                onQuickCallLogItem = { item -> viewModel.callBackFromRecentLog(item) },
                                onOneTapSaveContact = { name, num, photo ->
                                    viewModel.oneTapSaveToGoogleContacts(name, num, photo)
                                },
                                onLookupUnknown = { num -> viewModel.lookupUnknownNumberNow(num) },
                                onDeleteCallLog = { item -> viewModel.deleteCallLogItem(item) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // SIM Picker Popup ("Ask Every Time" mode — shown only when Dual-SIM is available)
    if (isDualSimAvailable) {
        pendingSimNumber?.let { numberToCall ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissSimPicker() },
                title = { Text("Choose SIM to Call") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Dialing $numberToCall", style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = { viewModel.placeCallNow(numberToCall, 1) },
                            colors = ButtonDefaults.buttonColors(containerColor = Sim1OceanBlue, contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sim_popup_sim1")
                        ) {
                            Icon(Icons.Filled.SimCard, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Call with ${simHardwareInfo.sim1Label}")
                        }
                        Button(
                            onClick = { viewModel.placeCallNow(numberToCall, 2) },
                            colors = ButtonDefaults.buttonColors(containerColor = Sim2DeepTeal, contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sim_popup_sim2")
                        ) {
                            Icon(Icons.Filled.SimCard, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Call with ${simHardwareInfo.sim2Label}")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissSimPicker() }) { Text("Cancel") }
                }
            )
        }
    }

    // Long-Press Context Menu on Call Log Items (Dual-SIM other-SIM option auto-hidden on Single-SIM)
    longPressedLogItem?.let { item ->
        CallLogLongPressMenuDialog(
            item = item,
            isDualSimAvailable = isDualSimAvailable,
            sim1Label = simHardwareInfo.sim1Label,
            sim2Label = simHardwareInfo.sim2Label,
            prefs = prefs,
            onCallWithSim = { simSlot -> viewModel.placeCallNow(item.number, simSlot) },
            onOpenPreview = { previewLogItem = item },
            onOpenDetails = { detailsLogItem = item },
            onOpenHistory = { historyLogItem = item },
            onEditBeforeCall = {
                viewModel.setDialQuery(item.number)
                if (AppTab.PHONE in prefs.activeOrderedTabs) {
                    selectedTab = AppTab.PHONE
                }
            },
            onOneTapSave = {
                viewModel.oneTapSaveToGoogleContacts(item.displayName, item.number, item.photoUri)
            },
            onDelete = { viewModel.deleteCallLogItem(item) },
            onDismiss = { longPressedLogItem = null }
        )
    }

    // User / Contact Preview Dialog
    previewLogItem?.let { item ->
        val matched = contacts.firstOrNull {
            T9SearchEngine.numberMatchKey(it.primaryNumber) == T9SearchEngine.numberMatchKey(item.number)
        }
        CallLogUserPreviewDialog(
            item = item,
            matchedContact = matched,
            isDualSimAvailable = isDualSimAvailable,
            sim1Label = simHardwareInfo.sim1Label,
            sim2Label = simHardwareInfo.sim2Label,
            onCallWithSim = { simSlot -> viewModel.placeCallNow(item.number, simSlot) },
            onEditOrSaveContact = {
                if (matched != null) {
                    editingContact = matched
                } else {
                    creatingContactPrefill = item.displayName.takeIf { it != item.number }.orEmpty() to item.number
                }
            },
            onOpenDetails = { detailsLogItem = item },
            onOpenHistory = { historyLogItem = item },
            onDismiss = { previewLogItem = null }
        )
    }

    // Call Details Dialog
    detailsLogItem?.let { item ->
        CallLogDetailsDialog(
            item = item,
            isDualSimAvailable = isDualSimAvailable,
            sim1Label = simHardwareInfo.sim1Label,
            sim2Label = simHardwareInfo.sim2Label,
            onCallWithSim = { simSlot -> viewModel.placeCallNow(item.number, simSlot) },
            onOpenHistory = { historyLogItem = item },
            onDismiss = { detailsLogItem = null }
        )
    }

    // Full Chronological Call History Dialog
    historyLogItem?.let { item ->
        CallLogHistoryDialog(
            item = item,
            rawCallLogs = rawCallLogs,
            isDualSimAvailable = isDualSimAvailable,
            sim1Label = simHardwareInfo.sim1Label,
            sim2Label = simHardwareInfo.sim2Label,
            onCallWithSim = { simSlot -> viewModel.placeCallNow(item.number, simSlot) },
            onDismiss = { historyLogItem = null }
        )
    }

    // Google Drive Auto-Restore Detection Dialog (shown upon signing in when backup exists)
    detectedBackup?.let { meta ->
        val sizeKb = (meta.sizeBytes / 1024L).coerceAtLeast(1L)
        val formattedDate = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
            .format(Date(meta.modifiedTimestampMillis))
        AlertDialog(
            onDismissRequest = { viewModel.dismissDriveBackupDialog() },
            icon = { Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = Sim1OceanBlue) },
            title = { Text("Google Drive Backup Found") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("An existing PulsePhone backup was detected in your Google Drive AppData:")
                    Spacer(Modifier.height(4.dp))
                    Text("Last updated: $formattedDate", style = MaterialTheme.typography.labelLarge)
                    Text("Backup size: $sizeKb KB", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Restore all saved Call Logs, Favorites, and DataStore Settings now?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.restoreFromGoogleDriveNow(meta.fileId) },
                    modifier = Modifier.testTag("drive_auto_restore_confirm_button")
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissDriveBackupDialog() }) {
                    Text("Skip")
                }
            }
        )
    }

    // Contact Details / Add-Edit Dialog
    if (editingContact != null || creatingContactPrefill != null) {
        ContactEditorDialog(
            initialContact = editingContact,
            prefillName = creatingContactPrefill?.first.orEmpty(),
            prefillNumber = creatingContactPrefill?.second.orEmpty(),
            onSave = { contact -> viewModel.saveContact(contact) },
            onDelete = { id -> viewModel.deleteContact(id) },
            onCall = { number -> viewModel.initiateCallRespectingSimMode(number) },
            onDismiss = {
                editingContact = null
                creatingContactPrefill = null
            }
        )
    }
}
