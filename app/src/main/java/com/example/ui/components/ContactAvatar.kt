package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.CallDirection
import com.example.ui.theme.CallAnswerEmerald
import com.example.ui.theme.CallMissedCrimson
import com.example.ui.theme.Sim1OceanBlue
import com.example.ui.theme.Sim2DeepTeal
import java.util.Locale
import kotlin.math.abs

private val AvatarTones = listOf(
    Color(0xFF1A73E8),
    Color(0xFF00897B),
    Color(0xFF4F46E5),
    Color(0xFFD97706),
    Color(0xFF0891B2),
    Color(0xFFBE185D),
    Color(0xFF15803D)
)

@Composable
fun ContactAvatar(
    name: String,
    photoUri: String?,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val bgColor = AvatarTones[abs(name.hashCode()) % AvatarTones.size]
    val initials = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase(Locale.getDefault()) }
        .ifBlank { "#" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = "$name avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        } else {
            Text(
                text = initials,
                style = if (size >= 56.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = bgColor
            )
        }
    }
}

@Composable
fun CallDirectionIcon(
    direction: CallDirection,
    modifier: Modifier = Modifier
) {
    val (icon, tint, desc) = when (direction) {
        CallDirection.INCOMING -> Triple(Icons.AutoMirrored.Filled.CallReceived, CallAnswerEmerald, "Incoming call")
        CallDirection.OUTGOING -> Triple(Icons.AutoMirrored.Filled.CallMade, Sim1OceanBlue, "Outgoing call")
        CallDirection.MISSED -> Triple(Icons.AutoMirrored.Filled.CallMissed, CallMissedCrimson, "Missed call")
        CallDirection.REJECTED -> Triple(Icons.Filled.Block, CallMissedCrimson, "Rejected call")
        CallDirection.VOICEMAIL -> Triple(Icons.Filled.Voicemail, Sim2DeepTeal, "Voicemail")
    }
    Icon(
        imageVector = icon,
        contentDescription = desc,
        tint = tint,
        modifier = modifier.size(15.dp)
    )
}

@Composable
fun SimSlotBadge(
    simSlot: Int,
    modifier: Modifier = Modifier
) {
    val color = if (simSlot == 1) Sim1OceanBlue else Sim2DeepTeal
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = color.copy(alpha = 0.16f),
        modifier = modifier
    ) {
        Text(
            text = simSlot.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}
