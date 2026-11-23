package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AccentPalette
import com.example.data.preferences.AppTab
import com.example.data.preferences.BottomBarSize
import com.example.data.preferences.IconDesignStyle
import com.example.data.preferences.PreinstalledThemePreset
import com.example.data.preferences.PreinstalledThemePresets
import com.example.data.preferences.PulsePhonePreferences
import com.example.data.preferences.ThemeMode
import com.example.ui.components.PulseBottomNavigationBar
import com.example.ui.components.StyledSimCallButton
import com.example.ui.theme.Sim1OceanBlue
import com.example.ui.theme.Sim2DeepTeal
import com.example.ui.viewmodel.PulsePhoneViewModel

@Composable
fun ThemeGallerySettingsContent(
    prefs: PulsePhonePreferences,
    viewModel: PulsePhoneViewModel,
    modifier: Modifier = Modifier
) {
    val presetRows = PreinstalledThemePresets.chunked(2)

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 28.dp)) {
        item { SettingsSectionHeader("Pre-Installed Themes (One-Tap)") }
        item {
            Text(
                text = "Tap any curated theme below to apply its color design, dark/light surface, and matching icon pack instantly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        items(presetRows.size, key = { "theme_row_$it" }) { rowIndex ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presetRows[rowIndex].forEach { preset ->
                    val isSelected = !prefs.useDynamicColor &&
                        prefs.themeMode == preset.themeMode &&
                        prefs.accentPalette == preset.accentPalette &&
                        prefs.iconStyleGroup == preset.iconStyle

                    ThemePresetCard(
                        preset = preset,
                        selected = isSelected,
                        onClick = { viewModel.applyPreinstalledThemePreset(preset) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_preset_${preset.id}")
                    )
                }
                if (presetRows[rowIndex].size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        item { SettingsSectionHeader("Theme Mode") }
        item {
            SettingsRadioGroup(
                options = ThemeMode.entries,
                selected = prefs.themeMode,
                labelProvider = { it.label },
                subtitleProvider = {
                    when (it) {
                        ThemeMode.SYSTEM -> "Follows Android light/dark system theme"
                        ThemeMode.LIGHT -> "Crisp Daylight light background"
                        ThemeMode.DARK -> "Deep charcoal low-glare dark mode"
                        ThemeMode.OLED_BLACK -> "True #000000 pitch black for AMOLED power savings"
                    }
                },
                onSelect = { mode -> viewModel.updatePreferences { it.copy(themeMode = mode) } }
            )
        }

        item { SettingsSectionHeader("Color System") }
        item {
            SettingsSwitchRow(
                title = "Material You Dynamic Color",
                subtitle = "Derive accent colors from your wallpaper (Android 12+)",
                checked = prefs.useDynamicColor,
                onCheckedChange = { enabled -> viewModel.updatePreferences { it.copy(useDynamicColor = enabled) } }
            )
        }

        item { SettingsSectionHeader("Accent Color Swatches") }
        items(AccentPalette.entries.chunked(2).size, key = { "accent_row_$it" }) { idx ->
            val row = AccentPalette.entries.chunked(2)[idx]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { pal ->
                    val active = prefs.accentPalette == pal && !prefs.useDynamicColor
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(
                            width = if (active) 1.8.dp else 0.75.dp,
                            color = if (active) Color(pal.primaryHex) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.updatePreferences {
                                    it.copy(accentPalette = pal, useDynamicColor = false)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(pal.primaryHex))
                            )
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(pal.secondaryHex))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = pal.label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePresetCard(
    preset: PreinstalledThemePreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = Color(preset.accentPalette.primaryHex)
    val secondary = Color(preset.accentPalette.secondaryHex)
    val bg = Color(preset.previewBgHex)
    val cardSurf = Color(preset.previewSurfaceHex)
    val isLightPreview = preset.themeMode == ThemeMode.LIGHT
    val textColor = if (isLightPreview) Color(0xFF0F172A) else Color.White

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(
            width = if (selected) 2.2.dp else 1.dp,
            color = if (selected) primary else Color.White.copy(alpha = 0.14f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(primary)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(secondary)
                    )
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected theme",
                        tint = primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Mini dialpad/pill preview bar inside the theme swatch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardSurf)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(primary)
                )
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(secondary)
                )
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(textColor.copy(alpha = 0.25f))
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = preset.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = preset.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun IconStylesSettingsContent(
    prefs: PulsePhonePreferences,
    viewModel: PulsePhoneViewModel,
    modifier: Modifier = Modifier
) {
    val styleRows = IconDesignStyle.entries.chunked(2)

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 28.dp)) {
        // Live Interactive Preview of Numeric Buttons + SIM Buttons + Bottom Tab Strip
        item { SettingsSectionHeader("Live Icon & Button Preview") }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Numeric button preview row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("1" to "∞", "2" to "ABC", "3" to "DEF").forEachIndexed { idx, (digit, letters) ->
                            PreviewNumericKey(
                                digit = digit,
                                letters = letters,
                                index = idx,
                                style = prefs.numericButtonStyle,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // SIM buttons preview row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StyledSimCallButton(
                            label = "SIM 1",
                            baseColor = Sim1OceanBlue,
                            style = prefs.simButtonStyle,
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        StyledSimCallButton(
                            label = "SIM 2",
                            baseColor = Sim2DeepTeal,
                            style = prefs.simButtonStyle,
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Bottom Tab Bar preview strip (Favorites, Groups, Phone, Contacts, Recents)
                    Box(modifier = Modifier.clip(RoundedCornerShape(16.dp))) {
                        PulseBottomNavigationBar(
                            tabs = prefs.activeOrderedTabs,
                            selectedTab = AppTab.PHONE.takeIf { it in prefs.activeOrderedTabs } ?: prefs.activeOrderedTabs.first(),
                            iconStyle = prefs.bottomTabIconStyle,
                            barSize = prefs.bottomBarSize,
                            onSelectTab = {}
                        )
                    }
                }
            }
        }

        // All-in-One Icon Style Groups (One tap updates Numeric + SIM + Bottom Tabs together)
        item { SettingsSectionHeader("Unified Icon Style Groups (Change All Together)") }
        item {
            Text(
                text = "Choose a design group to restyle Numeric Buttons, SIM Buttons, and Bottom Tab Icons simultaneously.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        items(styleRows.size, key = { "icon_group_row_$it" }) { idx ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                styleRows[idx].forEach { style ->
                    val isGroupSelected = prefs.numericButtonStyle == style &&
                        prefs.simButtonStyle == style &&
                        prefs.bottomTabIconStyle == style
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isGroupSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        border = BorderStroke(
                            width = if (isGroupSelected) 1.8.dp else 0.75.dp,
                            color = if (isGroupSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.applyIconStyleGroup(style) }
                            .testTag("icon_group_${style.name.lowercase()}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = style.label,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isGroupSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Selected group",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = style.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Individual Granular Overrides per Element Category
        item { SettingsSectionHeader("Customize Individual Icon Categories") }
        item {
            SettingsDropdownCard(
                title = "Numeric Dialpad Buttons Style",
                selected = prefs.numericButtonStyle,
                options = IconDesignStyle.entries,
                labelProvider = { it.label },
                onSelect = { s -> viewModel.updatePreferences { it.copy(numericButtonStyle = s) } }
            )
        }
        item {
            SettingsDropdownCard(
                title = "SIM Calling Buttons Style",
                selected = prefs.simButtonStyle,
                options = IconDesignStyle.entries,
                labelProvider = { it.label },
                onSelect = { s -> viewModel.updatePreferences { it.copy(simButtonStyle = s) } }
            )
        }
        item {
            SettingsDropdownCard(
                title = "Bottom Tab Bar Icons Style",
                selected = prefs.bottomTabIconStyle,
                options = IconDesignStyle.entries,
                labelProvider = { it.label },
                onSelect = { s -> viewModel.updatePreferences { it.copy(bottomTabIconStyle = s) } }
            )
        }

        item { SettingsSectionHeader("Bottom Navigation Bar Sizing") }
        item {
            SettingsRadioGroup(
                options = BottomBarSize.entries,
                selected = prefs.bottomBarSize,
                labelProvider = { it.label },
                subtitleProvider = {
                    when (it) {
                        BottomBarSize.COMPACT -> "Slim 56dp height • Extra screen space for call logs"
                        BottomBarSize.BALANCED -> "Ergonomic 64dp height • Crisp spacing for all 5 tabs"
                        BottomBarSize.COZY -> "Tall 72dp height • Larger icons & touch area"
                    }
                },
                onSelect = { size -> viewModel.updatePreferences { it.copy(bottomBarSize = size) } }
            )
        }
    }
}

@Composable
private fun PreviewNumericKey(
    digit: String,
    letters: String,
    index: Int,
    style: IconDesignStyle,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val vibrant = listOf(cs.primary, cs.secondary, Color(0xFF7C3AED))[index % 3]
    val shape = when (style) {
        IconDesignStyle.NEON_OUTLINE -> RoundedCornerShape(24.dp)
        IconDesignStyle.MINIMAL -> RoundedCornerShape(10.dp)
        else -> RoundedCornerShape(16.dp)
    }
    val bg = when (style) {
        IconDesignStyle.GLASS_TRANSPARENCY -> cs.primary.copy(alpha = 0.13f)
        IconDesignStyle.STEALTH_BLACK -> Color(0xFF0A0D12)
        IconDesignStyle.MINIMAL -> Color.Transparent
        IconDesignStyle.VIBRANT_COLOR -> vibrant.copy(alpha = 0.16f)
        IconDesignStyle.NEON_OUTLINE -> cs.surface.copy(alpha = 0.5f)
        IconDesignStyle.SOFT_MATERIAL -> cs.surface
    }
    val borderColor = when (style) {
        IconDesignStyle.GLASS_TRANSPARENCY -> cs.primary.copy(alpha = 0.42f)
        IconDesignStyle.STEALTH_BLACK -> Color.White.copy(alpha = 0.16f)
        IconDesignStyle.VIBRANT_COLOR -> vibrant.copy(alpha = 0.42f)
        IconDesignStyle.NEON_OUTLINE -> cs.primary
        else -> Color.Transparent
    }
    val digitColor = when (style) {
        IconDesignStyle.STEALTH_BLACK -> Color.White
        IconDesignStyle.VIBRANT_COLOR -> vibrant
        IconDesignStyle.NEON_OUTLINE -> cs.primary
        else -> cs.onSurface
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(shape)
            .background(bg)
            .then(
                if (borderColor != Color.Transparent) Modifier.border(1.2.dp, borderColor, shape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(digit, style = MaterialTheme.typography.titleLarge, color = digitColor)
            Text(
                letters,
                style = MaterialTheme.typography.labelSmall,
                color = digitColor.copy(alpha = 0.65f)
            )
        }
    }
}
