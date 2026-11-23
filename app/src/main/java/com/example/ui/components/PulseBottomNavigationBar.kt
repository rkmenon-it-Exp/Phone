package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AppTab
import com.example.data.preferences.BottomBarSize
import com.example.data.preferences.IconDesignStyle
import com.example.ui.theme.TabColorContacts
import com.example.ui.theme.TabColorFavorites
import com.example.ui.theme.TabColorGroups
import com.example.ui.theme.TabColorPhone
import com.example.ui.theme.TabColorRecents
import java.util.Locale

fun pulseTabIcon(tab: AppTab, selected: Boolean, style: IconDesignStyle): ImageVector {
    val useFilled = when (style) {
        IconDesignStyle.MINIMAL, IconDesignStyle.NEON_OUTLINE -> false
        else -> selected
    }
    return when (tab) {
        AppTab.FAVORITES -> if (useFilled) Icons.Filled.Star else Icons.Outlined.StarOutline
        AppTab.GROUPS -> if (useFilled) Icons.Filled.Group else Icons.Outlined.Group
        AppTab.PHONE -> if (useFilled) Icons.Filled.Dialpad else Icons.Outlined.Dialpad
        AppTab.CONTACTS -> if (useFilled) Icons.Filled.Contacts else Icons.Outlined.Contacts
        AppTab.RECENTS -> if (useFilled) Icons.Filled.History else Icons.Outlined.History
    }
}

fun vibrantTabColor(tab: AppTab): Color = when (tab) {
    AppTab.FAVORITES -> TabColorFavorites
    AppTab.GROUPS -> TabColorGroups
    AppTab.PHONE -> TabColorPhone
    AppTab.CONTACTS -> TabColorContacts
    AppTab.RECENTS -> TabColorRecents
}

/**
 * Ergonomically resized, customizable bottom navigation bar.
 * Fits all 5 tabs (Favorites, Groups, Phone, Contacts, Recents) cleanly on compact devices
 * with dynamic IconDesignStyle theming and proper navigationBarsPadding().
 */
@Composable
fun PulseBottomNavigationBar(
    tabs: List<AppTab>,
    selectedTab: AppTab,
    iconStyle: IconDesignStyle,
    barSize: BottomBarSize,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val barBg = when (iconStyle) {
        IconDesignStyle.GLASS_TRANSPARENCY -> cs.surface.copy(alpha = 0.86f)
        IconDesignStyle.STEALTH_BLACK -> Color(0xFF07090C)
        IconDesignStyle.MINIMAL -> cs.background
        IconDesignStyle.VIBRANT_COLOR -> cs.surface
        IconDesignStyle.NEON_OUTLINE -> cs.surface
        IconDesignStyle.SOFT_MATERIAL -> cs.surface
    }
    val topBorderColor = when (iconStyle) {
        IconDesignStyle.GLASS_TRANSPARENCY -> cs.primary.copy(alpha = 0.28f)
        IconDesignStyle.STEALTH_BLACK -> Color.White.copy(alpha = 0.12f)
        IconDesignStyle.NEON_OUTLINE -> cs.primary.copy(alpha = 0.55f)
        else -> cs.outline.copy(alpha = 0.22f)
    }

    Surface(
        color = barBg,
        tonalElevation = if (iconStyle == IconDesignStyle.MINIMAL || iconStyle == IconDesignStyle.STEALTH_BLACK) 0.dp else 4.dp,
        border = BorderStroke(0.75.dp, topBorderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(barSize.barHeightDp.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = tab == selectedTab
                val isCenterPhone = tab == AppTab.PHONE
                val tabAccent = when (iconStyle) {
                    IconDesignStyle.VIBRANT_COLOR -> vibrantTabColor(tab)
                    IconDesignStyle.STEALTH_BLACK -> Color.White
                    else -> cs.primary
                }

                val contentColor by animateColorAsState(
                    targetValue = when {
                        iconStyle == IconDesignStyle.VIBRANT_COLOR ->
                            if (selected) tabAccent else tabAccent.copy(alpha = 0.62f)
                        iconStyle == IconDesignStyle.STEALTH_BLACK ->
                            if (selected) Color.White else Color.White.copy(alpha = 0.52f)
                        selected -> tabAccent
                        else -> cs.onSurfaceVariant
                    },
                    label = "tab_tint_${tab.name}"
                )

                val pillShape = if (isCenterPhone && tabs.size == 5) CircleShape else RoundedCornerShape(14.dp)
                val pillHeight = when (barSize) {
                    BottomBarSize.COMPACT -> 28.dp
                    BottomBarSize.BALANCED -> 32.dp
                    BottomBarSize.COZY -> 36.dp
                }
                val pillWidth = if (isCenterPhone && tabs.size == 5) {
                    pillHeight + 10.dp
                } else {
                    44.dp
                }

                val pillModifier = when (iconStyle) {
                    IconDesignStyle.GLASS_TRANSPARENCY -> {
                        if (selected) {
                            Modifier
                                .size(width = pillWidth, height = pillHeight)
                                .clip(pillShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            tabAccent.copy(alpha = 0.28f),
                                            cs.secondary.copy(alpha = 0.16f)
                                        )
                                    )
                                )
                                .border(1.dp, tabAccent.copy(alpha = 0.65f), pillShape)
                        } else {
                            Modifier
                                .size(width = pillWidth, height = pillHeight)
                                .clip(pillShape)
                                .background(cs.onSurface.copy(alpha = 0.04f))
                                .border(0.6.dp, cs.onSurface.copy(alpha = 0.10f), pillShape)
                        }
                    }

                    IconDesignStyle.STEALTH_BLACK -> {
                        if (selected) {
                            Modifier
                                .size(width = pillWidth, height = pillHeight)
                                .clip(pillShape)
                                .background(Color(0xFF1A1F29))
                                .border(1.dp, Color.White.copy(alpha = 0.35f), pillShape)
                        } else {
                            Modifier.size(width = pillWidth, height = pillHeight)
                        }
                    }

                    IconDesignStyle.MINIMAL -> {
                        Modifier.size(width = pillWidth, height = pillHeight)
                    }

                    IconDesignStyle.VIBRANT_COLOR -> {
                        Modifier
                            .size(width = pillWidth, height = pillHeight)
                            .clip(pillShape)
                            .background(
                                if (selected) tabAccent.copy(alpha = 0.22f)
                                else tabAccent.copy(alpha = 0.08f)
                            )
                    }

                    IconDesignStyle.NEON_OUTLINE -> {
                        if (selected) {
                            Modifier
                                .size(width = pillWidth, height = pillHeight)
                                .clip(pillShape)
                                .background(tabAccent.copy(alpha = 0.12f))
                                .border(1.5.dp, tabAccent, pillShape)
                        } else {
                            Modifier
                                .size(width = pillWidth, height = pillHeight)
                                .clip(pillShape)
                                .border(0.8.dp, cs.outline.copy(alpha = 0.35f), pillShape)
                        }
                    }

                    IconDesignStyle.SOFT_MATERIAL -> {
                        if (selected) {
                            Modifier
                                .size(width = pillWidth, height = pillHeight)
                                .clip(pillShape)
                                .background(cs.primaryContainer)
                        } else {
                            Modifier.size(width = pillWidth, height = pillHeight)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = { onSelectTab(tab) }
                        )
                        .padding(vertical = 2.dp)
                        .testTag("nav_tab_${tab.name.lowercase(Locale.US)}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = pillModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = pulseTabIcon(tab, selected, iconStyle),
                            contentDescription = tab.label,
                            tint = contentColor,
                            modifier = Modifier.size(barSize.iconSizeDp.dp)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = if (barSize == BottomBarSize.COMPACT) 10.sp else 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = contentColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
