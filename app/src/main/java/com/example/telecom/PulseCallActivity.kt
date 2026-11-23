package com.example.telecom

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.PulsePhoneApplication
import com.example.data.preferences.PulsePhonePreferences
import com.example.ui.theme.PulsePhoneTheme
import kotlinx.coroutines.launch

class PulseCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenFlags()

        val container = (application as PulsePhoneApplication).container

        setContent {
            val prefs by container.preferencesRepository.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = PulsePhonePreferences())
            val callState by container.activeCallManager.callState.collectAsStateWithLifecycle()

            LaunchedEffect(callState) {
                if (callState == null) {
                    finish()
                }
            }

            PulsePhoneTheme(
                themeMode = prefs.themeMode,
                useDynamicColor = false,
                accentPalette = prefs.accentPalette,
                forceOledBlack = true
            ) {
                callState?.let { active ->
                    InCallScreen(
                        callState = active,
                        prefs = prefs,
                        onAnswer = { container.activeCallManager.answerCall() },
                        onDeclineOrEnd = { container.activeCallManager.declineOrEndCall() },
                        onToggleMute = { container.activeCallManager.toggleMute() },
                        onToggleSpeaker = { container.activeCallManager.toggleSpeaker() },
                        onToggleBluetooth = { container.activeCallManager.toggleBluetooth() },
                        onToggleKeypad = { container.activeCallManager.toggleKeypad() },
                        onDtmfPress = { digit -> container.activeCallManager.sendDtmf(digit) },
                        onToggleAutoRedial = { container.activeCallManager.toggleAutoRedialInCall() },
                        onSwitchSim = { container.activeCallManager.switchSimInPreviewOrRedial() },
                        onOpenContacts = { finish() },
                        onAddCall = { finish() },
                        onOneTapSaveContact = {
                            lifecycleScope.launch {
                                val res = container.dialerContactsRepository.oneTapSaveToGoogleContacts(
                                    displayName = active.callerName,
                                    phoneNumber = active.phoneNumber,
                                    photoUri = active.photoUri
                                )
                                if (res.isSuccess) {
                                    container.activeCallManager.markCurrentCallerSaved()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            km?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
