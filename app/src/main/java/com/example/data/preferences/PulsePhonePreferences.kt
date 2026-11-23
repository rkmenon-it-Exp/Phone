package com.example.data.preferences

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Daylight Light"),
    DARK("Deep Dark"),
    OLED_BLACK("OLED Pitch Black")
}

enum class AccentPalette(
    val label: String,
    val primaryHex: Long,
    val secondaryHex: Long
) {
    OCEAN_TEAL("Ocean Blue & Teal", 0xFF1A73E8, 0xFF00897B),
    COBALT_MINT("Cobalt & Mint", 0xFF2563EB, 0xFF059669),
    ROYAL_AMETHYST("Royal Amethyst", 0xFF7C3AED, 0xFFEC4899),
    EMERALD_GOLD("Emerald & Gold", 0xFF059669, 0xFFD97706),
    SUNSET_CRIMSON("Sunset Crimson", 0xFFE11D48, 0xFFF97316),
    CYBER_NEON("Cyber Neon", 0xFF06B6D4, 0xFF84CC16),
    ROSE_QUARTZ("Rose Quartz", 0xFFBE185D, 0xFF8B5CF6),
    GRAPHITE_AMBER("Graphite & Amber", 0xFF475569, 0xFFD97706)
}

enum class IconDesignStyle(val label: String, val subtitle: String) {
    GLASS_TRANSPARENCY("Glass Transparency", "Frosted translucent surfaces with luminous borders"),
    STEALTH_BLACK("Stealth Black", "Matte pitch-black capsules with crisp silver glyphs"),
    MINIMAL("Ultra Minimal", "Borderless thin outlined icons and airy typography"),
    VIBRANT_COLOR("Color Icons", "Multi-color vibrant badges across tabs, keys & SIMs"),
    NEON_OUTLINE("Neon Outline", "High-contrast glowing accent rings & outline buttons"),
    SOFT_MATERIAL("Soft Material 3", "Classic tonal rounded buttons with filled icons")
}

enum class BottomBarSize(val label: String, val barHeightDp: Int, val iconSizeDp: Int) {
    COMPACT("Compact", 56, 20),
    BALANCED("Balanced", 64, 22),
    COZY("Cozy", 72, 25)
}

data class PreinstalledThemePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val themeMode: ThemeMode,
    val accentPalette: AccentPalette,
    val iconStyle: IconDesignStyle,
    val previewBgHex: Long,
    val previewSurfaceHex: Long
)

val PreinstalledThemePresets: List<PreinstalledThemePreset> = listOf(
    PreinstalledThemePreset(
        id = "ocean_truephone",
        title = "Ocean TruePhone",
        subtitle = "System • Blue & Teal",
        themeMode = ThemeMode.SYSTEM,
        accentPalette = AccentPalette.OCEAN_TEAL,
        iconStyle = IconDesignStyle.SOFT_MATERIAL,
        previewBgHex = 0xFF0F172A,
        previewSurfaceHex = 0xFF1E293B
    ),
    PreinstalledThemePreset(
        id = "midnight_glass",
        title = "Midnight Glass",
        subtitle = "Dark • Frosted Glass",
        themeMode = ThemeMode.DARK,
        accentPalette = AccentPalette.COBALT_MINT,
        iconStyle = IconDesignStyle.GLASS_TRANSPARENCY,
        previewBgHex = 0xFF0B1120,
        previewSurfaceHex = 0xFF172554
    ),
    PreinstalledThemePreset(
        id = "obsidian_amoled",
        title = "Obsidian AMOLED",
        subtitle = "OLED Black • Stealth",
        themeMode = ThemeMode.OLED_BLACK,
        accentPalette = AccentPalette.OCEAN_TEAL,
        iconStyle = IconDesignStyle.STEALTH_BLACK,
        previewBgHex = 0xFF000000,
        previewSurfaceHex = 0xFF12161E
    ),
    PreinstalledThemePreset(
        id = "royal_amethyst",
        title = "Royal Amethyst",
        subtitle = "Dark • Violet & Pink",
        themeMode = ThemeMode.DARK,
        accentPalette = AccentPalette.ROYAL_AMETHYST,
        iconStyle = IconDesignStyle.VIBRANT_COLOR,
        previewBgHex = 0xFF130D24,
        previewSurfaceHex = 0xFF231942
    ),
    PreinstalledThemePreset(
        id = "cyber_neon",
        title = "Cyber Neon",
        subtitle = "OLED • Cyan & Lime",
        themeMode = ThemeMode.OLED_BLACK,
        accentPalette = AccentPalette.CYBER_NEON,
        iconStyle = IconDesignStyle.NEON_OUTLINE,
        previewBgHex = 0xFF020617,
        previewSurfaceHex = 0xFF082F49
    ),
    PreinstalledThemePreset(
        id = "emerald_forest",
        title = "Emerald Gold",
        subtitle = "Dark • Emerald & Amber",
        themeMode = ThemeMode.DARK,
        accentPalette = AccentPalette.EMERALD_GOLD,
        iconStyle = IconDesignStyle.GLASS_TRANSPARENCY,
        previewBgHex = 0xFF061B15,
        previewSurfaceHex = 0xFF0F2F25
    ),
    PreinstalledThemePreset(
        id = "sunset_crimson",
        title = "Sunset Crimson",
        subtitle = "Dark • Ruby & Coral",
        themeMode = ThemeMode.DARK,
        accentPalette = AccentPalette.SUNSET_CRIMSON,
        iconStyle = IconDesignStyle.VIBRANT_COLOR,
        previewBgHex = 0xFF1C0C12,
        previewSurfaceHex = 0xFF331420
    ),
    PreinstalledThemePreset(
        id = "nordic_frost",
        title = "Nordic Frost",
        subtitle = "Light • Ultra Minimal",
        themeMode = ThemeMode.LIGHT,
        accentPalette = AccentPalette.COBALT_MINT,
        iconStyle = IconDesignStyle.MINIMAL,
        previewBgHex = 0xFFF8FAFC,
        previewSurfaceHex = 0xFFE2E8F0
    ),
    PreinstalledThemePreset(
        id = "rose_quartz",
        title = "Rose Quartz",
        subtitle = "Light • Berry & Plum",
        themeMode = ThemeMode.LIGHT,
        accentPalette = AccentPalette.ROSE_QUARTZ,
        iconStyle = IconDesignStyle.SOFT_MATERIAL,
        previewBgHex = 0xFFFFF5F8,
        previewSurfaceHex = 0xFFFCE7F3
    ),
    PreinstalledThemePreset(
        id = "graphite_amber",
        title = "Graphite Amber",
        subtitle = "Dark • Industrial Slate",
        themeMode = ThemeMode.DARK,
        accentPalette = AccentPalette.GRAPHITE_AMBER,
        iconStyle = IconDesignStyle.STEALTH_BLACK,
        previewBgHex = 0xFF111317,
        previewSurfaceHex = 0xFF1F242C
    )
)

enum class AppTab(val label: String) {
    FAVORITES("Favorites"),
    GROUPS("Groups"),
    PHONE("Phone"),
    CONTACTS("Contacts"),
    RECENTS("Recents")
}

enum class SimCallingMode(val label: String, val subtitle: String) {
    DUAL_SIM_SPLIT("Dual-SIM Split Buttons", "Dedicated Blue SIM 1 & Teal SIM 2 buttons"),
    SINGLE_DEFAULT_SIM("Single Default SIM", "One primary call button using preferred SIM"),
    ASK_EVERY_TIME("Ask Every Time", "Prompt SIM picker popup before dialing")
}

enum class CallLogTapAction(val label: String, val subtitle: String) {
    DIRECT_CALL("Place Call Directly", "Tap any call log row to call immediately using last-used SIM"),
    USER_PREVIEW("User / Contact Preview", "Show a quick contact preview card with actions"),
    CALL_DETAILS("Call Details", "Show date, exact time, direction, duration & SIM details"),
    CALL_HISTORY("Full Call History", "Open complete chronological call log for that contact")
}

enum class CallLogRetentionPeriod(val label: String, val days: Long) {
    DAYS_7("7 days", 7L),
    DAYS_10("10 days", 10L),
    DAYS_15("15 days", 15L),
    YEAR_1("1 year", 365L),
    YEARS_5("5 years", 365L * 5L),
    YEARS_10("10 years", 365L * 10L)
}

enum class FavoritesDisplayMode(val label: String) {
    GRID_3("3-Column Photo Grid"),
    GRID_4("4-Column Grid"),
    COMPACT_LIST("Compact List")
}

enum class CallerPhotoStyle(val label: String) {
    FULL_SCREEN_GRADIENT("Full-Screen Background (60%+)"),
    LARGE_CIRCULAR("Large Circular Avatar"),
    MINIMALIST_NO_PHOTO("Minimalist / No Photo")
}

enum class IncomingAnswerStyle(val label: String) {
    SWIPE_UP_DOWN("Swipe Up / Down"),
    HORIZONTAL_SLIDE("Horizontal Slide"),
    TWO_BUTTONS("Two Separate Answer / Decline Buttons")
}

enum class InCallActionButton(val label: String) {
    ADD_CALL("Add call"),
    CHANGE_SIM("Change SIM"),
    CONTACTS("Contacts"),
    MUTE("Mute"),
    AUTO_REDIAL("Auto Redial"),
    BLUETOOTH("Bluetooth"),
    SPEAKER("Speaker"),
    KEYPAD("Keypad")
}

enum class DriveBackupFrequency(val label: String, val intervalHours: Long) {
    DAILY_NIGHT("Daily at Night", 24L),
    WEEKLY("Weekly", 24L * 7L),
    MONTHLY("Monthly", 24L * 30L)
}

/**
 * Single source of truth for 100% granular PulsePhone customization.
 * Every UI element, tab, icon pack, dialpad control, call log badge, in-call layout,
 * Smart Caller ID provider, sync rule, and backup setting is stored in Jetpack DataStore.
 */
data class PulsePhonePreferences(
    // 0. Appearance, Pre-Installed Themes & Icon Design Packs
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val accentPalette: AccentPalette = AccentPalette.OCEAN_TEAL,
    val iconStyleGroup: IconDesignStyle = IconDesignStyle.SOFT_MATERIAL,
    val numericButtonStyle: IconDesignStyle = IconDesignStyle.SOFT_MATERIAL,
    val simButtonStyle: IconDesignStyle = IconDesignStyle.SOFT_MATERIAL,
    val bottomTabIconStyle: IconDesignStyle = IconDesignStyle.SOFT_MATERIAL,
    val bottomBarSize: BottomBarSize = BottomBarSize.BALANCED,

    // 1. Tab & Navigation Settings (Default order: Favorites, Groups, Phone, Contacts, Recents)
    val orderedTabs: List<AppTab> = listOf(
        AppTab.FAVORITES,
        AppTab.GROUPS,
        AppTab.PHONE,
        AppTab.CONTACTS,
        AppTab.RECENTS
    ),
    val enabledTabs: Set<AppTab> = AppTab.entries.toSet(),
    val defaultStartupTab: AppTab = AppTab.PHONE,

    // 2. Dialpad & Calling Behavior Settings
    val t9SearchEnabled: Boolean = true,
    val dialpadVibrationEnabled: Boolean = true,
    val dialpadKeypressTonesEnabled: Boolean = true,
    val dialpadHeightScale: Float = 1.0f,
    val dialpadButtonSizeScale: Float = 1.0f,
    val dialpadFontScale: Float = 1.0f,
    val simCallingMode: SimCallingMode = SimCallingMode.DUAL_SIM_SPLIT,
    val defaultSimSlot: Int = 1,
    val rememberLastUsedSim: Boolean = true,
    val forceShowDualSimPreview: Boolean = false,
    val autoRedialEnabled: Boolean = false,
    val autoRedialAttempts: Int = 3,
    val autoRedialIntervalSeconds: Int = 10,

    // 3. Call Log & History Display Settings
    val callLogTapAction: CallLogTapAction = CallLogTapAction.USER_PREVIEW,
    val showLogAvatar: Boolean = true,
    val showCallCountBadge: Boolean = true,
    val showCallDirectionArrow: Boolean = true,
    val showSimBadge: Boolean = true,
    val showCallDuration: Boolean = true,
    val showRelativeTime: Boolean = true,
    val showQuickCallIcon: Boolean = true,
    val groupConsecutiveCalls: Boolean = true,

    // 4. Call Log Retention & Auto-Delete (Default OFF = Keep forever)
    val autoDeleteOldCallLogs: Boolean = false,
    val callLogRetentionPeriod: CallLogRetentionPeriod = CallLogRetentionPeriod.YEAR_1,

    // 5. Favorites Tab Settings
    val favoritesDisplayMode: FavoritesDisplayMode = FavoritesDisplayMode.GRID_3,
    val showFrequentlyContacted: Boolean = true,
    val favoriteOneTapDirectCall: Boolean = true,

    // 6. In-Call & Incoming Call Screen Settings
    val customFullScreenCallerId: Boolean = true,
    val callerPhotoStyle: CallerPhotoStyle = CallerPhotoStyle.FULL_SCREEN_GRADIENT,
    val incomingAnswerStyle: IncomingAnswerStyle = IncomingAnswerStyle.SWIPE_UP_DOWN,
    val inCallActionOrder: List<InCallActionButton> = listOf(
        InCallActionButton.ADD_CALL,
        InCallActionButton.CHANGE_SIM,
        InCallActionButton.CONTACTS,
        InCallActionButton.MUTE,
        InCallActionButton.AUTO_REDIAL,
        InCallActionButton.BLUETOOTH,
        InCallActionButton.SPEAKER,
        InCallActionButton.KEYPAD
    ),
    val enabledInCallActions: Set<InCallActionButton> = InCallActionButton.entries.toSet(),
    val flashlightAlertOnIncoming: Boolean = false,
    val vibrateOnConnectDisconnect: Boolean = true,

    // 7. Smart Caller ID & Social Auto-Finder Settings
    val findNameCallerEnabled: Boolean = false,
    val callerIdWizardSeen: Boolean = false,
    val notificationCaptureEnabled: Boolean = true,
    val accessibilitySocialLookupEnabled: Boolean = false,
    val customCallerIdApiEnabled: Boolean = false,
    val customCallerIdApiUrl: String = "",
    val customCallerIdApiToken: String = "",
    val showNotInContactsIndicator: Boolean = true,
    val showOneTapSaveToGoogleContacts: Boolean = true,

    // 8. Sync, Backup & Storage Settings
    val googleAccountEmail: String? = null,
    val googleAccountName: String? = null,
    val googleAccountPhotoUrl: String? = null,
    val googleContactsTwoWaySync: Boolean = true,
    val googleContactsWifiOnly: Boolean = true,
    val driveAutoBackupEnabled: Boolean = false,
    val driveBackupFrequency: DriveBackupFrequency = DriveBackupFrequency.DAILY_NIGHT,
    val driveBackupWifiOnly: Boolean = true,
    val lastContactsSyncMillis: Long = 0L,
    val lastDriveBackupMillis: Long = 0L
) {
    val activeOrderedTabs: List<AppTab>
        get() = orderedTabs.filter { it in enabledTabs }.ifEmpty { listOf(AppTab.PHONE) }

    val effectiveStartupTab: AppTab
        get() = if (defaultStartupTab in activeOrderedTabs) defaultStartupTab else activeOrderedTabs.first()

    val activeOrderedInCallActions: List<InCallActionButton>
        get() = inCallActionOrder.filter { it in enabledInCallActions }
}
