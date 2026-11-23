package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val Context.pulseDataStore: DataStore<Preferences> by preferencesDataStore(name = "pulsephone_settings")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        // Appearance & Icon Styles
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val ICON_STYLE_GROUP = stringPreferencesKey("icon_style_group")
        val NUMERIC_BUTTON_STYLE = stringPreferencesKey("numeric_button_style")
        val SIM_BUTTON_STYLE = stringPreferencesKey("sim_button_style")
        val BOTTOM_TAB_ICON_STYLE = stringPreferencesKey("bottom_tab_icon_style")
        val BOTTOM_BAR_SIZE = stringPreferencesKey("bottom_bar_size")

        // Tabs & Navigation
        val ORDERED_TABS = stringPreferencesKey("ordered_tabs")
        val ENABLED_TABS = stringPreferencesKey("enabled_tabs")
        val DEFAULT_STARTUP_TAB = stringPreferencesKey("default_startup_tab")

        // Dialpad & Calling
        val T9_SEARCH = booleanPreferencesKey("t9_search_enabled")
        val DIALPAD_VIBRATION = booleanPreferencesKey("dialpad_vibration_enabled")
        val DIALPAD_TONES = booleanPreferencesKey("dialpad_tones_enabled")
        val DIALPAD_HEIGHT_SCALE = floatPreferencesKey("dialpad_height_scale")
        val DIALPAD_BUTTON_SIZE_SCALE = floatPreferencesKey("dialpad_button_size_scale")
        val DIALPAD_FONT_SCALE = floatPreferencesKey("dialpad_font_scale")
        val SIM_CALLING_MODE = stringPreferencesKey("sim_calling_mode")
        val DEFAULT_SIM_SLOT = intPreferencesKey("default_sim_slot")
        val REMEMBER_LAST_USED_SIM = booleanPreferencesKey("remember_last_used_sim")
        val FORCE_SHOW_DUAL_SIM_PREVIEW = booleanPreferencesKey("force_show_dual_sim_preview")
        val AUTO_REDIAL_ENABLED = booleanPreferencesKey("auto_redial_enabled")
        val AUTO_REDIAL_ATTEMPTS = intPreferencesKey("auto_redial_attempts")
        val AUTO_REDIAL_INTERVAL_SEC = intPreferencesKey("auto_redial_interval_sec")

        // Call Log Display
        val CALL_LOG_TAP_ACTION = stringPreferencesKey("call_log_tap_action")
        val SHOW_LOG_AVATAR = booleanPreferencesKey("show_log_avatar")
        val SHOW_CALL_COUNT_BADGE = booleanPreferencesKey("show_call_count_badge")
        val SHOW_CALL_DIRECTION_ARROW = booleanPreferencesKey("show_call_direction_arrow")
        val SHOW_SIM_BADGE = booleanPreferencesKey("show_sim_badge")
        val SHOW_CALL_DURATION = booleanPreferencesKey("show_call_duration")
        val SHOW_RELATIVE_TIME = booleanPreferencesKey("show_relative_time")
        val SHOW_QUICK_CALL_ICON = booleanPreferencesKey("show_quick_call_icon")
        val GROUP_CONSECUTIVE_CALLS = booleanPreferencesKey("group_consecutive_calls")

        // Retention
        val AUTO_DELETE_OLD_LOGS = booleanPreferencesKey("auto_delete_old_logs")
        val RETENTION_PERIOD = stringPreferencesKey("call_log_retention_period")

        // Favorites
        val FAVORITES_DISPLAY_MODE = stringPreferencesKey("favorites_display_mode")
        val SHOW_FREQUENTLY_CONTACTED = booleanPreferencesKey("show_frequently_contacted")
        val FAVORITE_ONE_TAP_CALL = booleanPreferencesKey("favorite_one_tap_call")

        // In-Call & Incoming
        val CUSTOM_FULLSCREEN_CALLER_ID = booleanPreferencesKey("custom_fullscreen_caller_id")
        val CALLER_PHOTO_STYLE = stringPreferencesKey("caller_photo_style")
        val INCOMING_ANSWER_STYLE = stringPreferencesKey("incoming_answer_style")
        val INCALL_ACTION_ORDER = stringPreferencesKey("incall_action_order")
        val ENABLED_INCALL_ACTIONS = stringPreferencesKey("enabled_incall_actions")
        val FLASHLIGHT_ALERT_INCOMING = booleanPreferencesKey("flashlight_alert_incoming")
        val VIBRATE_CONNECT_DISCONNECT = booleanPreferencesKey("vibrate_connect_disconnect")

        // Smart Caller ID & Social Auto-Finder
        val FIND_NAME_CALLER_ENABLED = booleanPreferencesKey("find_name_caller_enabled")
        val CALLER_ID_WIZARD_SEEN = booleanPreferencesKey("caller_id_wizard_seen")
        val NOTIFICATION_CAPTURE_ENABLED = booleanPreferencesKey("notification_capture_enabled")
        val ACCESSIBILITY_LOOKUP_ENABLED = booleanPreferencesKey("accessibility_lookup_enabled")
        val CUSTOM_CALLER_ID_API_ENABLED = booleanPreferencesKey("custom_caller_id_api_enabled")
        val CUSTOM_CALLER_ID_API_URL = stringPreferencesKey("custom_caller_id_api_url")
        val CUSTOM_CALLER_ID_API_TOKEN = stringPreferencesKey("custom_caller_id_api_token")
        val SHOW_NOT_IN_CONTACTS = booleanPreferencesKey("show_not_in_contacts")
        val SHOW_ONE_TAP_SAVE_GOOGLE = booleanPreferencesKey("show_one_tap_save_google")

        // Sync & Backup
        val GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
        val GOOGLE_ACCOUNT_NAME = stringPreferencesKey("google_account_name")
        val GOOGLE_ACCOUNT_PHOTO = stringPreferencesKey("google_account_photo")
        val GOOGLE_CONTACTS_TWO_WAY = booleanPreferencesKey("google_contacts_two_way")
        val GOOGLE_CONTACTS_WIFI_ONLY = booleanPreferencesKey("google_contacts_wifi_only")
        val DRIVE_AUTO_BACKUP = booleanPreferencesKey("drive_auto_backup")
        val DRIVE_BACKUP_FREQUENCY = stringPreferencesKey("drive_backup_frequency")
        val DRIVE_BACKUP_WIFI_ONLY = booleanPreferencesKey("drive_backup_wifi_only")
        val LAST_CONTACTS_SYNC = longPreferencesKey("last_contacts_sync")
        val LAST_DRIVE_BACKUP = longPreferencesKey("last_drive_backup")
    }

    val preferencesFlow: Flow<PulsePhonePreferences> = context.pulseDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs -> mapPreferences(prefs) }

    suspend fun getSnapshot(): PulsePhonePreferences = preferencesFlow.first()

    private fun mapPreferences(p: Preferences): PulsePhonePreferences {
        val default = PulsePhonePreferences()
        val orderedTabs = parseEnumList(p[Keys.ORDERED_TABS], AppTab.entries, default.orderedTabs)
        val enabledTabs = parseEnumSet(p[Keys.ENABLED_TABS], AppTab.entries, default.enabledTabs)
            .ifEmpty { setOf(AppTab.PHONE) }
        val inCallOrder = parseEnumList(p[Keys.INCALL_ACTION_ORDER], InCallActionButton.entries, default.inCallActionOrder)
        val enabledActions = parseEnumSet(p[Keys.ENABLED_INCALL_ACTIONS], InCallActionButton.entries, default.enabledInCallActions)

        return PulsePhonePreferences(
            themeMode = parseEnum(p[Keys.THEME_MODE], default.themeMode),
            useDynamicColor = p[Keys.USE_DYNAMIC_COLOR] ?: default.useDynamicColor,
            accentPalette = parseEnum(p[Keys.ACCENT_PALETTE], default.accentPalette),
            iconStyleGroup = parseEnum(p[Keys.ICON_STYLE_GROUP], default.iconStyleGroup),
            numericButtonStyle = parseEnum(p[Keys.NUMERIC_BUTTON_STYLE], default.numericButtonStyle),
            simButtonStyle = parseEnum(p[Keys.SIM_BUTTON_STYLE], default.simButtonStyle),
            bottomTabIconStyle = parseEnum(p[Keys.BOTTOM_TAB_ICON_STYLE], default.bottomTabIconStyle),
            bottomBarSize = parseEnum(p[Keys.BOTTOM_BAR_SIZE], default.bottomBarSize),

            orderedTabs = orderedTabs,
            enabledTabs = enabledTabs,
            defaultStartupTab = parseEnum(p[Keys.DEFAULT_STARTUP_TAB], default.defaultStartupTab),

            t9SearchEnabled = p[Keys.T9_SEARCH] ?: default.t9SearchEnabled,
            dialpadVibrationEnabled = p[Keys.DIALPAD_VIBRATION] ?: default.dialpadVibrationEnabled,
            dialpadKeypressTonesEnabled = p[Keys.DIALPAD_TONES] ?: default.dialpadKeypressTonesEnabled,
            dialpadHeightScale = p[Keys.DIALPAD_HEIGHT_SCALE] ?: default.dialpadHeightScale,
            dialpadButtonSizeScale = p[Keys.DIALPAD_BUTTON_SIZE_SCALE] ?: default.dialpadButtonSizeScale,
            dialpadFontScale = p[Keys.DIALPAD_FONT_SCALE] ?: default.dialpadFontScale,
            simCallingMode = parseEnum(p[Keys.SIM_CALLING_MODE], default.simCallingMode),
            defaultSimSlot = (p[Keys.DEFAULT_SIM_SLOT] ?: default.defaultSimSlot).coerceIn(1, 2),
            rememberLastUsedSim = p[Keys.REMEMBER_LAST_USED_SIM] ?: default.rememberLastUsedSim,
            forceShowDualSimPreview = p[Keys.FORCE_SHOW_DUAL_SIM_PREVIEW] ?: default.forceShowDualSimPreview,
            autoRedialEnabled = p[Keys.AUTO_REDIAL_ENABLED] ?: default.autoRedialEnabled,
            autoRedialAttempts = (p[Keys.AUTO_REDIAL_ATTEMPTS] ?: default.autoRedialAttempts).coerceIn(1, 20),
            autoRedialIntervalSeconds = (p[Keys.AUTO_REDIAL_INTERVAL_SEC] ?: default.autoRedialIntervalSeconds).coerceIn(5, 60),

            callLogTapAction = parseEnum(p[Keys.CALL_LOG_TAP_ACTION], default.callLogTapAction),
            showLogAvatar = p[Keys.SHOW_LOG_AVATAR] ?: default.showLogAvatar,
            showCallCountBadge = p[Keys.SHOW_CALL_COUNT_BADGE] ?: default.showCallCountBadge,
            showCallDirectionArrow = p[Keys.SHOW_CALL_DIRECTION_ARROW] ?: default.showCallDirectionArrow,
            showSimBadge = p[Keys.SHOW_SIM_BADGE] ?: default.showSimBadge,
            showCallDuration = p[Keys.SHOW_CALL_DURATION] ?: default.showCallDuration,
            showRelativeTime = p[Keys.SHOW_RELATIVE_TIME] ?: default.showRelativeTime,
            showQuickCallIcon = p[Keys.SHOW_QUICK_CALL_ICON] ?: default.showQuickCallIcon,
            groupConsecutiveCalls = p[Keys.GROUP_CONSECUTIVE_CALLS] ?: default.groupConsecutiveCalls,

            autoDeleteOldCallLogs = p[Keys.AUTO_DELETE_OLD_LOGS] ?: default.autoDeleteOldCallLogs,
            callLogRetentionPeriod = parseEnum(p[Keys.RETENTION_PERIOD], default.callLogRetentionPeriod),

            favoritesDisplayMode = parseEnum(p[Keys.FAVORITES_DISPLAY_MODE], default.favoritesDisplayMode),
            showFrequentlyContacted = p[Keys.SHOW_FREQUENTLY_CONTACTED] ?: default.showFrequentlyContacted,
            favoriteOneTapDirectCall = p[Keys.FAVORITE_ONE_TAP_CALL] ?: default.favoriteOneTapDirectCall,

            customFullScreenCallerId = p[Keys.CUSTOM_FULLSCREEN_CALLER_ID] ?: default.customFullScreenCallerId,
            callerPhotoStyle = parseEnum(p[Keys.CALLER_PHOTO_STYLE], default.callerPhotoStyle),
            incomingAnswerStyle = parseEnum(p[Keys.INCOMING_ANSWER_STYLE], default.incomingAnswerStyle),
            inCallActionOrder = inCallOrder,
            enabledInCallActions = enabledActions,
            flashlightAlertOnIncoming = p[Keys.FLASHLIGHT_ALERT_INCOMING] ?: default.flashlightAlertOnIncoming,
            vibrateOnConnectDisconnect = p[Keys.VIBRATE_CONNECT_DISCONNECT] ?: default.vibrateOnConnectDisconnect,

            findNameCallerEnabled = p[Keys.FIND_NAME_CALLER_ENABLED] ?: default.findNameCallerEnabled,
            callerIdWizardSeen = p[Keys.CALLER_ID_WIZARD_SEEN] ?: default.callerIdWizardSeen,
            notificationCaptureEnabled = p[Keys.NOTIFICATION_CAPTURE_ENABLED] ?: default.notificationCaptureEnabled,
            accessibilitySocialLookupEnabled = p[Keys.ACCESSIBILITY_LOOKUP_ENABLED] ?: default.accessibilitySocialLookupEnabled,
            customCallerIdApiEnabled = p[Keys.CUSTOM_CALLER_ID_API_ENABLED] ?: default.customCallerIdApiEnabled,
            customCallerIdApiUrl = p[Keys.CUSTOM_CALLER_ID_API_URL] ?: default.customCallerIdApiUrl,
            customCallerIdApiToken = p[Keys.CUSTOM_CALLER_ID_API_TOKEN] ?: default.customCallerIdApiToken,
            showNotInContactsIndicator = p[Keys.SHOW_NOT_IN_CONTACTS] ?: default.showNotInContactsIndicator,
            showOneTapSaveToGoogleContacts = p[Keys.SHOW_ONE_TAP_SAVE_GOOGLE] ?: default.showOneTapSaveToGoogleContacts,

            googleAccountEmail = p[Keys.GOOGLE_ACCOUNT_EMAIL],
            googleAccountName = p[Keys.GOOGLE_ACCOUNT_NAME],
            googleAccountPhotoUrl = p[Keys.GOOGLE_ACCOUNT_PHOTO],
            googleContactsTwoWaySync = p[Keys.GOOGLE_CONTACTS_TWO_WAY] ?: default.googleContactsTwoWaySync,
            googleContactsWifiOnly = p[Keys.GOOGLE_CONTACTS_WIFI_ONLY] ?: default.googleContactsWifiOnly,
            driveAutoBackupEnabled = p[Keys.DRIVE_AUTO_BACKUP] ?: default.driveAutoBackupEnabled,
            driveBackupFrequency = parseEnum(p[Keys.DRIVE_BACKUP_FREQUENCY], default.driveBackupFrequency),
            driveBackupWifiOnly = p[Keys.DRIVE_BACKUP_WIFI_ONLY] ?: default.driveBackupWifiOnly,
            lastContactsSyncMillis = p[Keys.LAST_CONTACTS_SYNC] ?: 0L,
            lastDriveBackupMillis = p[Keys.LAST_DRIVE_BACKUP] ?: 0L
        )
    }

    suspend fun update(transform: (PulsePhonePreferences) -> PulsePhonePreferences) {
        val current = getSnapshot()
        val updated = transform(current)
        saveAll(updated)
    }

    suspend fun saveAll(prefs: PulsePhonePreferences) {
        context.pulseDataStore.edit { p ->
            p[Keys.THEME_MODE] = prefs.themeMode.name
            p[Keys.USE_DYNAMIC_COLOR] = prefs.useDynamicColor
            p[Keys.ACCENT_PALETTE] = prefs.accentPalette.name
            p[Keys.ICON_STYLE_GROUP] = prefs.iconStyleGroup.name
            p[Keys.NUMERIC_BUTTON_STYLE] = prefs.numericButtonStyle.name
            p[Keys.SIM_BUTTON_STYLE] = prefs.simButtonStyle.name
            p[Keys.BOTTOM_TAB_ICON_STYLE] = prefs.bottomTabIconStyle.name
            p[Keys.BOTTOM_BAR_SIZE] = prefs.bottomBarSize.name

            p[Keys.ORDERED_TABS] = prefs.orderedTabs.joinToString(",") { it.name }
            p[Keys.ENABLED_TABS] = prefs.enabledTabs.ifEmpty { setOf(AppTab.PHONE) }.joinToString(",") { it.name }
            p[Keys.DEFAULT_STARTUP_TAB] = prefs.defaultStartupTab.name

            p[Keys.T9_SEARCH] = prefs.t9SearchEnabled
            p[Keys.DIALPAD_VIBRATION] = prefs.dialpadVibrationEnabled
            p[Keys.DIALPAD_TONES] = prefs.dialpadKeypressTonesEnabled
            p[Keys.DIALPAD_HEIGHT_SCALE] = prefs.dialpadHeightScale.coerceIn(0.75f, 1.30f)
            p[Keys.DIALPAD_BUTTON_SIZE_SCALE] = prefs.dialpadButtonSizeScale.coerceIn(0.80f, 1.25f)
            p[Keys.DIALPAD_FONT_SCALE] = prefs.dialpadFontScale.coerceIn(0.80f, 1.35f)
            p[Keys.SIM_CALLING_MODE] = prefs.simCallingMode.name
            p[Keys.DEFAULT_SIM_SLOT] = prefs.defaultSimSlot.coerceIn(1, 2)
            p[Keys.REMEMBER_LAST_USED_SIM] = prefs.rememberLastUsedSim
            p[Keys.FORCE_SHOW_DUAL_SIM_PREVIEW] = prefs.forceShowDualSimPreview
            p[Keys.AUTO_REDIAL_ENABLED] = prefs.autoRedialEnabled
            p[Keys.AUTO_REDIAL_ATTEMPTS] = prefs.autoRedialAttempts.coerceIn(1, 20)
            p[Keys.AUTO_REDIAL_INTERVAL_SEC] = prefs.autoRedialIntervalSeconds.coerceIn(5, 60)

            p[Keys.CALL_LOG_TAP_ACTION] = prefs.callLogTapAction.name
            p[Keys.SHOW_LOG_AVATAR] = prefs.showLogAvatar
            p[Keys.SHOW_CALL_COUNT_BADGE] = prefs.showCallCountBadge
            p[Keys.SHOW_CALL_DIRECTION_ARROW] = prefs.showCallDirectionArrow
            p[Keys.SHOW_SIM_BADGE] = prefs.showSimBadge
            p[Keys.SHOW_CALL_DURATION] = prefs.showCallDuration
            p[Keys.SHOW_RELATIVE_TIME] = prefs.showRelativeTime
            p[Keys.SHOW_QUICK_CALL_ICON] = prefs.showQuickCallIcon
            p[Keys.GROUP_CONSECUTIVE_CALLS] = prefs.groupConsecutiveCalls

            p[Keys.AUTO_DELETE_OLD_LOGS] = prefs.autoDeleteOldCallLogs
            p[Keys.RETENTION_PERIOD] = prefs.callLogRetentionPeriod.name

            p[Keys.FAVORITES_DISPLAY_MODE] = prefs.favoritesDisplayMode.name
            p[Keys.SHOW_FREQUENTLY_CONTACTED] = prefs.showFrequentlyContacted
            p[Keys.FAVORITE_ONE_TAP_CALL] = prefs.favoriteOneTapDirectCall

            p[Keys.CUSTOM_FULLSCREEN_CALLER_ID] = prefs.customFullScreenCallerId
            p[Keys.CALLER_PHOTO_STYLE] = prefs.callerPhotoStyle.name
            p[Keys.INCOMING_ANSWER_STYLE] = prefs.incomingAnswerStyle.name
            p[Keys.INCALL_ACTION_ORDER] = prefs.inCallActionOrder.joinToString(",") { it.name }
            p[Keys.ENABLED_INCALL_ACTIONS] = prefs.enabledInCallActions.joinToString(",") { it.name }
            p[Keys.FLASHLIGHT_ALERT_INCOMING] = prefs.flashlightAlertOnIncoming
            p[Keys.VIBRATE_CONNECT_DISCONNECT] = prefs.vibrateOnConnectDisconnect

            p[Keys.FIND_NAME_CALLER_ENABLED] = prefs.findNameCallerEnabled
            p[Keys.CALLER_ID_WIZARD_SEEN] = prefs.callerIdWizardSeen
            p[Keys.NOTIFICATION_CAPTURE_ENABLED] = prefs.notificationCaptureEnabled
            p[Keys.ACCESSIBILITY_LOOKUP_ENABLED] = prefs.accessibilitySocialLookupEnabled
            p[Keys.CUSTOM_CALLER_ID_API_ENABLED] = prefs.customCallerIdApiEnabled
            p[Keys.CUSTOM_CALLER_ID_API_URL] = prefs.customCallerIdApiUrl
            p[Keys.CUSTOM_CALLER_ID_API_TOKEN] = prefs.customCallerIdApiToken
            p[Keys.SHOW_NOT_IN_CONTACTS] = prefs.showNotInContactsIndicator
            p[Keys.SHOW_ONE_TAP_SAVE_GOOGLE] = prefs.showOneTapSaveToGoogleContacts

            if (prefs.googleAccountEmail != null) p[Keys.GOOGLE_ACCOUNT_EMAIL] = prefs.googleAccountEmail else p.remove(Keys.GOOGLE_ACCOUNT_EMAIL)
            if (prefs.googleAccountName != null) p[Keys.GOOGLE_ACCOUNT_NAME] = prefs.googleAccountName else p.remove(Keys.GOOGLE_ACCOUNT_NAME)
            if (prefs.googleAccountPhotoUrl != null) p[Keys.GOOGLE_ACCOUNT_PHOTO] = prefs.googleAccountPhotoUrl else p.remove(Keys.GOOGLE_ACCOUNT_PHOTO)
            p[Keys.GOOGLE_CONTACTS_TWO_WAY] = prefs.googleContactsTwoWaySync
            p[Keys.GOOGLE_CONTACTS_WIFI_ONLY] = prefs.googleContactsWifiOnly
            p[Keys.DRIVE_AUTO_BACKUP] = prefs.driveAutoBackupEnabled
            p[Keys.DRIVE_BACKUP_FREQUENCY] = prefs.driveBackupFrequency.name
            p[Keys.DRIVE_BACKUP_WIFI_ONLY] = prefs.driveBackupWifiOnly
            p[Keys.LAST_CONTACTS_SYNC] = prefs.lastContactsSyncMillis
            p[Keys.LAST_DRIVE_BACKUP] = prefs.lastDriveBackupMillis
        }
    }

    suspend fun applyRichTruePhonePreset() {
        val current = getSnapshot()
        saveAll(
            PulsePhonePreferences(
                themeMode = current.themeMode,
                useDynamicColor = current.useDynamicColor,
                accentPalette = current.accentPalette,
                googleAccountEmail = current.googleAccountEmail,
                googleAccountName = current.googleAccountName,
                googleAccountPhotoUrl = current.googleAccountPhotoUrl,
                lastContactsSyncMillis = current.lastContactsSyncMillis,
                lastDriveBackupMillis = current.lastDriveBackupMillis
            )
        )
    }

    suspend fun applyMinimalPreset() {
        val current = getSnapshot()
        saveAll(
            current.copy(
                enabledTabs = setOf(AppTab.PHONE, AppTab.CONTACTS),
                defaultStartupTab = AppTab.PHONE,
                simCallingMode = SimCallingMode.SINGLE_DEFAULT_SIM,
                showLogAvatar = false,
                showCallCountBadge = false,
                showSimBadge = false,
                showCallDuration = false,
                showQuickCallIcon = true,
                showFrequentlyContacted = false,
                favoritesDisplayMode = FavoritesDisplayMode.COMPACT_LIST,
                callerPhotoStyle = CallerPhotoStyle.MINIMALIST_NO_PHOTO,
                incomingAnswerStyle = IncomingAnswerStyle.TWO_BUTTONS
            )
        )
    }

    fun toJson(prefs: PulsePhonePreferences): JSONObject = JSONObject().apply {
        put("themeMode", prefs.themeMode.name)
        put("useDynamicColor", prefs.useDynamicColor)
        put("accentPalette", prefs.accentPalette.name)
        put("iconStyleGroup", prefs.iconStyleGroup.name)
        put("numericButtonStyle", prefs.numericButtonStyle.name)
        put("simButtonStyle", prefs.simButtonStyle.name)
        put("bottomTabIconStyle", prefs.bottomTabIconStyle.name)
        put("bottomBarSize", prefs.bottomBarSize.name)
        put("orderedTabs", JSONArray(prefs.orderedTabs.map { it.name }))
        put("enabledTabs", JSONArray(prefs.enabledTabs.map { it.name }))
        put("defaultStartupTab", prefs.defaultStartupTab.name)
        put("t9SearchEnabled", prefs.t9SearchEnabled)
        put("dialpadVibrationEnabled", prefs.dialpadVibrationEnabled)
        put("dialpadKeypressTonesEnabled", prefs.dialpadKeypressTonesEnabled)
        put("dialpadHeightScale", prefs.dialpadHeightScale.toDouble())
        put("dialpadButtonSizeScale", prefs.dialpadButtonSizeScale.toDouble())
        put("dialpadFontScale", prefs.dialpadFontScale.toDouble())
        put("simCallingMode", prefs.simCallingMode.name)
        put("defaultSimSlot", prefs.defaultSimSlot)
        put("rememberLastUsedSim", prefs.rememberLastUsedSim)
        put("forceShowDualSimPreview", prefs.forceShowDualSimPreview)
        put("autoRedialEnabled", prefs.autoRedialEnabled)
        put("autoRedialAttempts", prefs.autoRedialAttempts)
        put("autoRedialIntervalSeconds", prefs.autoRedialIntervalSeconds)
        put("callLogTapAction", prefs.callLogTapAction.name)
        put("showLogAvatar", prefs.showLogAvatar)
        put("showCallCountBadge", prefs.showCallCountBadge)
        put("showCallDirectionArrow", prefs.showCallDirectionArrow)
        put("showSimBadge", prefs.showSimBadge)
        put("showCallDuration", prefs.showCallDuration)
        put("showRelativeTime", prefs.showRelativeTime)
        put("showQuickCallIcon", prefs.showQuickCallIcon)
        put("groupConsecutiveCalls", prefs.groupConsecutiveCalls)
        put("autoDeleteOldCallLogs", prefs.autoDeleteOldCallLogs)
        put("callLogRetentionPeriod", prefs.callLogRetentionPeriod.name)
        put("favoritesDisplayMode", prefs.favoritesDisplayMode.name)
        put("showFrequentlyContacted", prefs.showFrequentlyContacted)
        put("favoriteOneTapDirectCall", prefs.favoriteOneTapDirectCall)
        put("customFullScreenCallerId", prefs.customFullScreenCallerId)
        put("callerPhotoStyle", prefs.callerPhotoStyle.name)
        put("incomingAnswerStyle", prefs.incomingAnswerStyle.name)
        put("inCallActionOrder", JSONArray(prefs.inCallActionOrder.map { it.name }))
        put("enabledInCallActions", JSONArray(prefs.enabledInCallActions.map { it.name }))
        put("flashlightAlertOnIncoming", prefs.flashlightAlertOnIncoming)
        put("vibrateOnConnectDisconnect", prefs.vibrateOnConnectDisconnect)
        put("findNameCallerEnabled", prefs.findNameCallerEnabled)
        put("notificationCaptureEnabled", prefs.notificationCaptureEnabled)
        put("accessibilitySocialLookupEnabled", prefs.accessibilitySocialLookupEnabled)
        put("customCallerIdApiEnabled", prefs.customCallerIdApiEnabled)
        put("customCallerIdApiUrl", prefs.customCallerIdApiUrl)
        put("showNotInContactsIndicator", prefs.showNotInContactsIndicator)
        put("showOneTapSaveToGoogleContacts", prefs.showOneTapSaveToGoogleContacts)
        put("googleContactsTwoWaySync", prefs.googleContactsTwoWaySync)
        put("googleContactsWifiOnly", prefs.googleContactsWifiOnly)
        put("driveAutoBackupEnabled", prefs.driveAutoBackupEnabled)
        put("driveBackupFrequency", prefs.driveBackupFrequency.name)
        put("driveBackupWifiOnly", prefs.driveBackupWifiOnly)
    }

    suspend fun importFromJson(json: JSONObject) {
        val cur = getSnapshot()
        val orderedTabs = json.optJSONArray("orderedTabs")?.let { arr ->
            (0 until arr.length()).mapNotNull { runCatching { AppTab.valueOf(arr.getString(it)) }.getOrNull() }
        }?.let { list ->
            (list + AppTab.entries.filterNot { it in list }).distinct()
        } ?: cur.orderedTabs

        val enabledTabs = json.optJSONArray("enabledTabs")?.let { arr ->
            (0 until arr.length()).mapNotNull { runCatching { AppTab.valueOf(arr.getString(it)) }.getOrNull() }.toSet()
        }?.ifEmpty { setOf(AppTab.PHONE) } ?: cur.enabledTabs

        val inCallOrder = json.optJSONArray("inCallActionOrder")?.let { arr ->
            (0 until arr.length()).mapNotNull { runCatching { InCallActionButton.valueOf(arr.getString(it)) }.getOrNull() }
        }?.let { list ->
            (list + InCallActionButton.entries.filterNot { it in list }).distinct()
        } ?: cur.inCallActionOrder

        val enabledInCall = json.optJSONArray("enabledInCallActions")?.let { arr ->
            (0 until arr.length()).mapNotNull { runCatching { InCallActionButton.valueOf(arr.getString(it)) }.getOrNull() }.toSet()
        } ?: cur.enabledInCallActions

        saveAll(
            cur.copy(
                themeMode = parseEnum(json.optString("themeMode"), cur.themeMode),
                useDynamicColor = json.optBoolean("useDynamicColor", cur.useDynamicColor),
                accentPalette = parseEnum(json.optString("accentPalette"), cur.accentPalette),
                iconStyleGroup = parseEnum(json.optString("iconStyleGroup"), cur.iconStyleGroup),
                numericButtonStyle = parseEnum(json.optString("numericButtonStyle"), cur.numericButtonStyle),
                simButtonStyle = parseEnum(json.optString("simButtonStyle"), cur.simButtonStyle),
                bottomTabIconStyle = parseEnum(json.optString("bottomTabIconStyle"), cur.bottomTabIconStyle),
                bottomBarSize = parseEnum(json.optString("bottomBarSize"), cur.bottomBarSize),
                orderedTabs = orderedTabs,
                enabledTabs = enabledTabs,
                defaultStartupTab = parseEnum(json.optString("defaultStartupTab"), cur.defaultStartupTab),
                t9SearchEnabled = json.optBoolean("t9SearchEnabled", cur.t9SearchEnabled),
                dialpadVibrationEnabled = json.optBoolean("dialpadVibrationEnabled", cur.dialpadVibrationEnabled),
                dialpadKeypressTonesEnabled = json.optBoolean("dialpadKeypressTonesEnabled", cur.dialpadKeypressTonesEnabled),
                dialpadHeightScale = json.optDouble("dialpadHeightScale", cur.dialpadHeightScale.toDouble()).toFloat(),
                dialpadButtonSizeScale = json.optDouble("dialpadButtonSizeScale", cur.dialpadButtonSizeScale.toDouble()).toFloat(),
                dialpadFontScale = json.optDouble("dialpadFontScale", cur.dialpadFontScale.toDouble()).toFloat(),
                simCallingMode = parseEnum(json.optString("simCallingMode"), cur.simCallingMode),
                defaultSimSlot = json.optInt("defaultSimSlot", cur.defaultSimSlot),
                rememberLastUsedSim = json.optBoolean("rememberLastUsedSim", cur.rememberLastUsedSim),
                forceShowDualSimPreview = json.optBoolean("forceShowDualSimPreview", cur.forceShowDualSimPreview),
                autoRedialEnabled = json.optBoolean("autoRedialEnabled", cur.autoRedialEnabled),
                autoRedialAttempts = json.optInt("autoRedialAttempts", cur.autoRedialAttempts),
                autoRedialIntervalSeconds = json.optInt("autoRedialIntervalSeconds", cur.autoRedialIntervalSeconds),
                callLogTapAction = parseEnum(json.optString("callLogTapAction"), cur.callLogTapAction),
                showLogAvatar = json.optBoolean("showLogAvatar", cur.showLogAvatar),
                showCallCountBadge = json.optBoolean("showCallCountBadge", cur.showCallCountBadge),
                showCallDirectionArrow = json.optBoolean("showCallDirectionArrow", cur.showCallDirectionArrow),
                showSimBadge = json.optBoolean("showSimBadge", cur.showSimBadge),
                showCallDuration = json.optBoolean("showCallDuration", cur.showCallDuration),
                showRelativeTime = json.optBoolean("showRelativeTime", cur.showRelativeTime),
                showQuickCallIcon = json.optBoolean("showQuickCallIcon", cur.showQuickCallIcon),
                groupConsecutiveCalls = json.optBoolean("groupConsecutiveCalls", cur.groupConsecutiveCalls),
                autoDeleteOldCallLogs = json.optBoolean("autoDeleteOldCallLogs", cur.autoDeleteOldCallLogs),
                callLogRetentionPeriod = parseEnum(json.optString("callLogRetentionPeriod"), cur.callLogRetentionPeriod),
                favoritesDisplayMode = parseEnum(json.optString("favoritesDisplayMode"), cur.favoritesDisplayMode),
                showFrequentlyContacted = json.optBoolean("showFrequentlyContacted", cur.showFrequentlyContacted),
                favoriteOneTapDirectCall = json.optBoolean("favoriteOneTapDirectCall", cur.favoriteOneTapDirectCall),
                customFullScreenCallerId = json.optBoolean("customFullScreenCallerId", cur.customFullScreenCallerId),
                callerPhotoStyle = parseEnum(json.optString("callerPhotoStyle"), cur.callerPhotoStyle),
                incomingAnswerStyle = parseEnum(json.optString("incomingAnswerStyle"), cur.incomingAnswerStyle),
                inCallActionOrder = inCallOrder,
                enabledInCallActions = enabledInCall,
                flashlightAlertOnIncoming = json.optBoolean("flashlightAlertOnIncoming", cur.flashlightAlertOnIncoming),
                vibrateOnConnectDisconnect = json.optBoolean("vibrateOnConnectDisconnect", cur.vibrateOnConnectDisconnect),
                findNameCallerEnabled = json.optBoolean("findNameCallerEnabled", cur.findNameCallerEnabled),
                notificationCaptureEnabled = json.optBoolean("notificationCaptureEnabled", cur.notificationCaptureEnabled),
                accessibilitySocialLookupEnabled = json.optBoolean("accessibilitySocialLookupEnabled", cur.accessibilitySocialLookupEnabled),
                customCallerIdApiEnabled = json.optBoolean("customCallerIdApiEnabled", cur.customCallerIdApiEnabled),
                customCallerIdApiUrl = json.optString("customCallerIdApiUrl", cur.customCallerIdApiUrl),
                showNotInContactsIndicator = json.optBoolean("showNotInContactsIndicator", cur.showNotInContactsIndicator),
                showOneTapSaveToGoogleContacts = json.optBoolean("showOneTapSaveToGoogleContacts", cur.showOneTapSaveToGoogleContacts),
                googleContactsTwoWaySync = json.optBoolean("googleContactsTwoWaySync", cur.googleContactsTwoWaySync),
                googleContactsWifiOnly = json.optBoolean("googleContactsWifiOnly", cur.googleContactsWifiOnly),
                driveAutoBackupEnabled = json.optBoolean("driveAutoBackupEnabled", cur.driveAutoBackupEnabled),
                driveBackupFrequency = parseEnum(json.optString("driveBackupFrequency"), cur.driveBackupFrequency),
                driveBackupWifiOnly = json.optBoolean("driveBackupWifiOnly", cur.driveBackupWifiOnly)
            )
        )
    }

    private inline fun <reified T : Enum<T>> parseEnum(raw: String?, fallback: T): T {
        if (raw.isNullOrBlank()) return fallback
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
    }

    private fun <T : Enum<T>> parseEnumList(raw: String?, all: List<T>, fallback: List<T>): List<T> {
        if (raw.isNullOrBlank()) return fallback
        val parsed = raw.split(",").mapNotNull { token -> all.firstOrNull { it.name == token.trim() } }
        if (parsed.isEmpty()) return fallback
        return (parsed + all.filterNot { it in parsed }).distinct()
    }

    private fun <T : Enum<T>> parseEnumSet(raw: String?, all: List<T>, fallback: Set<T>): Set<T> {
        if (raw == null) return fallback
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { token -> all.firstOrNull { it.name == token.trim() } }.toSet()
    }
}
