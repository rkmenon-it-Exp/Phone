package com.example.telecom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.preferences.CallerPhotoStyle
import com.example.data.preferences.InCallActionButton
import com.example.data.preferences.IncomingAnswerStyle
import com.example.data.preferences.PulsePhonePreferences
import com.example.ui.theme.CallAnswerEmerald
import com.example.ui.theme.CallDeclineCrimson
import com.example.ui.theme.OledBackground
import com.example.ui.theme.Sim1OceanBlue
import com.example.ui.theme.Sim2DeepTeal
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun InCallScreen(
    callState: ActiveCallUiState,
    prefs: PulsePhonePreferences,
    onAnswer: () -> Unit,
    onDeclineOrEnd: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleBluetooth: () -> Unit,
    onToggleKeypad: () -> Unit,
    onDtmfPress: (Char) -> Unit,
    onToggleAutoRedial: () -> Unit,
    onSwitchSim: () -> Unit,
    onOpenContacts: () -> Unit,
    onAddCall: () -> Unit,
    onOneTapSaveContact: () -> Unit
) {
    val simColor = if (callState.simSlot == 1) Sim1OceanBlue else Sim2DeepTeal

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackground)
    ) {
        // 1. Optional Full-Screen Background Caller Photo (62% height with OLED fade gradient)
        if (prefs.callerPhotoStyle == CallerPhotoStyle.FULL_SCREEN_GRADIENT) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.64f)
                    .align(Alignment.TopCenter)
            ) {
                if (!callState.photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = callState.photoUri,
                        contentDescription = "Caller photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        simColor.copy(alpha = 0.42f),
                                        Color(0xFF0A1424),
                                        OledBackground
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = callState.callerName.take(1).uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 112.sp),
                            color = Color.White.copy(alpha = 0.22f)
                        )
                    }
                }
                // OLED gradient scrim merging photo smoothly into #000000
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.78f),
                                    OledBackground
                                )
                            )
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Status Row (SIM Badge + Preview Mode Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = simColor.copy(alpha = 0.22f),
                    modifier = Modifier.border(1.dp, simColor.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SimCard,
                            contentDescription = "SIM ${callState.simSlot}",
                            tint = simColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "SIM ${callState.simSlot}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }

                if (callState.isPreviewMode) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = "PREVIEW MODE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 2. Large Circular Avatar mode (when selected)
            if (prefs.callerPhotoStyle == CallerPhotoStyle.LARGE_CIRCULAR) {
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(simColor.copy(alpha = 0.25f))
                        .border(2.dp, simColor.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!callState.photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = callState.photoUri,
                            contentDescription = "Caller avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = callState.callerName.take(2).uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            } else if (prefs.callerPhotoStyle == CallerPhotoStyle.FULL_SCREEN_GRADIENT) {
                Spacer(Modifier.height(48.dp))
            } else {
                Spacer(Modifier.height(28.dp))
            }

            // Caller Name & Number
            Text(
                text = callState.callerName,
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = callState.phoneNumber,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.72f)
            )

            // Smart Caller ID: "Not in contacts" muted indicator + Source + One-Tap Save to Google Contacts
            if (!callState.isSavedContact) {
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (prefs.showNotInContactsIndicator) {
                        val tagText = buildString {
                            append("Not in contacts")
                            callState.callerIdSource?.let { append(" • via ${it.label}") }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = tagText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    if (prefs.showOneTapSaveToGoogleContacts) {
                        AssistChip(
                            onClick = onOneTapSaveContact,
                            label = { Text("Save", color = Color.White) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = "Save to Google Contacts",
                                    tint = Sim2DeepTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Sim2DeepTeal.copy(alpha = 0.22f)
                            ),
                            modifier = Modifier.testTag("incall_one_tap_save_button")
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Call Status / Live Timer
            val statusLabel = when {
                callState.autoRedialCountdownSec > 0 ->
                    "Auto-redialing in ${callState.autoRedialCountdownSec}s (${callState.autoRedialAttemptsLeft} left)"
                callState.phase == CallPhase.RINGING -> "Incoming call..."
                callState.phase == CallPhase.DIALING -> "Dialing..."
                callState.phase == CallPhase.HOLDING -> "On Hold"
                callState.phase == CallPhase.ACTIVE -> formatCallTimer(callState.elapsedSeconds)
                else -> "Call ended"
            }
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = if (callState.phase == CallPhase.RINGING) Sim2DeepTeal else Color.White.copy(alpha = 0.85f)
            )

            Spacer(Modifier.weight(1f))

            // 3. Incoming Call Controls vs Active Call 2x4 Action Grid
            if (callState.phase == CallPhase.RINGING) {
                IncomingAnswerControls(
                    style = prefs.incomingAnswerStyle,
                    onAnswer = onAnswer,
                    onDecline = onDeclineOrEnd
                )
            } else {
                AnimatedVisibility(visible = callState.isKeypadVisible) {
                    InCallDtmfKeypad(
                        entered = callState.dtmfEntered,
                        onPress = onDtmfPress
                    )
                }

                if (!callState.isKeypadVisible) {
                    val activeButtons = prefs.activeOrderedInCallActions
                    if (activeButtons.isNotEmpty()) {
                        InCallActionGrid(
                            actions = activeButtons,
                            callState = callState,
                            onActionClick = { action ->
                                when (action) {
                                    InCallActionButton.ADD_CALL -> onAddCall()
                                    InCallActionButton.CHANGE_SIM -> onSwitchSim()
                                    InCallActionButton.CONTACTS -> onOpenContacts()
                                    InCallActionButton.MUTE -> onToggleMute()
                                    InCallActionButton.AUTO_REDIAL -> onToggleAutoRedial()
                                    InCallActionButton.BLUETOOTH -> onToggleBluetooth()
                                    InCallActionButton.SPEAKER -> onToggleSpeaker()
                                    InCallActionButton.KEYPAD -> onToggleKeypad()
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // End Call FAB
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (callState.isKeypadVisible) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { onToggleKeypad() }
                                .testTag("incall_hide_keypad_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Dialpad,
                                    contentDescription = "Hide keypad",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = CallDeclineCrimson,
                        modifier = Modifier
                            .size(72.dp)
                            .clickable { onDeclineOrEnd() }
                            .testTag("incall_end_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.CallEnd,
                                contentDescription = "End call",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IncomingAnswerControls(
    style: IncomingAnswerStyle,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    when (style) {
        IncomingAnswerStyle.TWO_BUTTONS -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = CallDeclineCrimson,
                        modifier = Modifier
                            .size(72.dp)
                            .clickable { onDecline() }
                            .testTag("incoming_decline_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.CallEnd, "Decline call", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Decline", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = CallAnswerEmerald,
                        modifier = Modifier
                            .size(72.dp)
                            .clickable { onAnswer() }
                            .testTag("incoming_answer_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Call, "Answer call", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Answer", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        IncomingAnswerStyle.SWIPE_UP_DOWN -> {
            var dragY by remember { mutableFloatStateOf(0f) }
            val animatedY by animateFloatAsState(
                targetValue = dragY,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "swipe_y"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, "Swipe up to answer", tint = CallAnswerEmerald)
                Text("Swipe up to Answer", style = MaterialTheme.typography.labelSmall, color = CallAnswerEmerald)
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, animatedY.roundToInt()) }
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                dragY < -35f -> CallAnswerEmerald
                                dragY > 35f -> CallDeclineCrimson
                                else -> Sim1OceanBlue
                            }
                        )
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    when {
                                        dragY < -95f -> onAnswer()
                                        dragY > 95f -> onDecline()
                                    }
                                    dragY = 0f
                                },
                                onDragCancel = { dragY = 0f }
                            ) { _, dragAmount ->
                                dragY = (dragY + dragAmount).coerceIn(-160f, 160f)
                            }
                        }
                        .testTag("incoming_swipe_vertical_handle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (dragY > 35f) Icons.Filled.CallEnd else Icons.Filled.Call,
                        contentDescription = "Swipe to answer or decline",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text("Swipe down to Decline", style = MaterialTheme.typography.labelSmall, color = CallDeclineCrimson)
                Icon(Icons.Filled.KeyboardArrowDown, "Swipe down to decline", tint = CallDeclineCrimson)
            }
        }

        IncomingAnswerStyle.HORIZONTAL_SLIDE -> {
            var dragX by remember { mutableFloatStateOf(0f) }
            val animatedX by animateFloatAsState(
                targetValue = dragX,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "slide_x"
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(RoundedCornerShape(37.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(37.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Decline", style = MaterialTheme.typography.labelMedium, color = CallDeclineCrimson)
                    Text("Answer", style = MaterialTheme.typography.labelMedium, color = CallAnswerEmerald)
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(animatedX.roundToInt(), 0) }
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                dragX > 40f -> CallAnswerEmerald
                                dragX < -40f -> CallDeclineCrimson
                                else -> Color.White
                            }
                        )
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    when {
                                        dragX > 120f -> onAnswer()
                                        dragX < -120f -> onDecline()
                                    }
                                    dragX = 0f
                                },
                                onDragCancel = { dragX = 0f }
                            ) { _, dragAmount ->
                                dragX = (dragX + dragAmount).coerceIn(-210f, 210f)
                            }
                        }
                        .testTag("incoming_slide_horizontal_handle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (dragX < -40f) Icons.Filled.CallEnd else Icons.Filled.Call,
                        contentDescription = "Slide right to answer, left to decline",
                        tint = if (dragX in -40f..40f) Color.Black else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InCallActionGrid(
    actions: List<InCallActionButton>,
    callState: ActiveCallUiState,
    onActionClick: (InCallActionButton) -> Unit
) {
    val rows = actions.chunked(4)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0E121A))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(vertical = 18.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowItems.forEach { action ->
                    val isActive = when (action) {
                        InCallActionButton.MUTE -> callState.isMuted
                        InCallActionButton.SPEAKER -> callState.isSpeakerOn
                        InCallActionButton.BLUETOOTH -> callState.isBluetoothOn
                        InCallActionButton.KEYPAD -> callState.isKeypadVisible
                        InCallActionButton.AUTO_REDIAL -> callState.isAutoRedialArmed
                        else -> false
                    }
                    InCallGridTile(
                        icon = iconForAction(action, isActive),
                        label = action.label,
                        isActive = isActive,
                        onClick = { onActionClick(action) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("incall_action_${action.name.lowercase(Locale.US)}")
                    )
                }
                repeat(4 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun InCallGridTile(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (isActive) Color.White else Color.White.copy(alpha = 0.11f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else Color.White,
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InCallDtmfKeypad(
    entered: String,
    onPress: (Char) -> Unit
) {
    val keys = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('*', '0', '#')
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0E121A))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (entered.isNotEmpty()) {
            Text(
                text = entered,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { ch ->
                    Box(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onPress(ch) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ch.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun iconForAction(action: InCallActionButton, isActive: Boolean): ImageVector = when (action) {
    InCallActionButton.ADD_CALL -> Icons.Filled.AddIcCall
    InCallActionButton.CHANGE_SIM -> Icons.Filled.SimCard
    InCallActionButton.CONTACTS -> Icons.Filled.Contacts
    InCallActionButton.MUTE -> if (isActive) Icons.Filled.MicOff else Icons.Filled.Mic
    InCallActionButton.AUTO_REDIAL -> Icons.Filled.Replay
    InCallActionButton.BLUETOOTH -> Icons.Filled.Bluetooth
    InCallActionButton.SPEAKER -> Icons.Filled.VolumeUp
    InCallActionButton.KEYPAD -> Icons.Filled.Dialpad
}

private fun formatCallTimer(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}
