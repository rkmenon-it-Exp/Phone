package com.example.telecom

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.Call
import android.telecom.CallAudioState
import com.example.callerid.SmartCallerIdResolver
import com.example.data.local.CallerIdSource
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.DialerContactsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class CallPhase {
    RINGING,
    DIALING,
    ACTIVE,
    HOLDING,
    DISCONNECTED
}

data class ActiveCallUiState(
    val callerName: String,
    val phoneNumber: String,
    val photoUri: String? = null,
    val isSavedContact: Boolean = true,
    val callerIdSource: CallerIdSource? = null,
    val simSlot: Int = 1,
    val phase: CallPhase = CallPhase.RINGING,
    val elapsedSeconds: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isBluetoothOn: Boolean = false,
    val isKeypadVisible: Boolean = false,
    val dtmfEntered: String = "",
    val isAutoRedialArmed: Boolean = false,
    val autoRedialCountdownSec: Int = 0,
    val autoRedialAttemptsLeft: Int = 0,
    val isPreviewMode: Boolean = false
)

class ActiveCallManager(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val callerIdResolver: SmartCallerIdResolver
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _callState = MutableStateFlow<ActiveCallUiState?>(null)
    val callState: StateFlow<ActiveCallUiState?> = _callState.asStateFlow()

    @Volatile
    private var currentCall: Call? = null
    @Volatile
    private var boundService: PulseInCallService? = null
    @Volatile
    private var dialerRepoRef: DialerContactsRepository? = null

    private var timerJob: Job? = null
    private var flashlightJob: Job? = null
    private var autoRedialJob: Job? = null
    private var callConnectedAtMillis: Long = 0L

    fun attachDialerRepository(repo: DialerContactsRepository) {
        dialerRepoRef = repo
    }

    fun attachInCallService(service: PulseInCallService?) {
        boundService = service
    }

    fun onCallAdded(call: Call) {
        currentCall = call
        val number = call.details?.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() } ?: "Unknown"
        val initialPhase = mapTelecomState(call.state)
        val simSlot = if (call.details?.accountHandle?.id?.contains("2") == true) 2 else 1

        scope.launch {
            val prefs = preferencesRepository.getSnapshot()
            val resolved = callerIdResolver.resolveNumber(number)

            _callState.value = ActiveCallUiState(
                callerName = resolved.displayName,
                phoneNumber = number,
                photoUri = resolved.photoUri,
                isSavedContact = resolved.isSavedContact,
                callerIdSource = resolved.source,
                simSlot = simSlot,
                phase = initialPhase,
                isAutoRedialArmed = prefs.autoRedialEnabled,
                autoRedialAttemptsLeft = prefs.autoRedialAttempts,
                isPreviewMode = false
            )

            if (initialPhase == CallPhase.RINGING && prefs.flashlightAlertOnIncoming) {
                startFlashlightAlert()
            }
            if (initialPhase == CallPhase.ACTIVE) {
                startCallTimer()
            }
        }
    }

    fun onCallStateChanged(call: Call, state: Int) {
        val phase = mapTelecomState(state)
        val prevPhase = _callState.value?.phase
        _callState.update { it?.copy(phase = phase) }

        scope.launch {
            val prefs = preferencesRepository.getSnapshot()
            when (phase) {
                CallPhase.ACTIVE -> {
                    stopFlashlightAlert()
                    callConnectedAtMillis = System.currentTimeMillis()
                    if (prevPhase != CallPhase.ACTIVE && prefs.vibrateOnConnectDisconnect) {
                        vibratePulse(60L)
                    }
                    startCallTimer()
                }
                CallPhase.RINGING -> {
                    if (prefs.flashlightAlertOnIncoming) startFlashlightAlert()
                }
                CallPhase.DISCONNECTED -> {
                    stopFlashlightAlert()
                    timerJob?.cancel()
                    if (prefs.vibrateOnConnectDisconnect) {
                        vibratePulse(110L)
                    }
                    val snapshot = _callState.value
                    val wasAnswered = callConnectedAtMillis > 0L
                    callConnectedAtMillis = 0L

                    if (snapshot != null && !wasAnswered && snapshot.isAutoRedialArmed && snapshot.autoRedialAttemptsLeft > 0) {
                        triggerAutoRedial(snapshot.phoneNumber, snapshot.simSlot, snapshot.autoRedialAttemptsLeft, prefs.autoRedialIntervalSeconds)
                    }
                }
                else -> Unit
            }
        }
    }

    fun onCallRemoved(call: Call) {
        if (currentCall == call) {
            currentCall = null
        }
        stopFlashlightAlert()
        timerJob?.cancel()
        if (_callState.value?.autoRedialCountdownSec == 0) {
            _callState.value = null
        }
    }

    fun onAudioStateChanged(audioState: CallAudioState) {
        _callState.update {
            it?.copy(
                isMuted = audioState.isMuted,
                isSpeakerOn = audioState.route == CallAudioState.ROUTE_SPEAKER,
                isBluetoothOn = audioState.route == CallAudioState.ROUTE_BLUETOOTH
            )
        }
    }

    fun startPreviewCall(
        incoming: Boolean,
        callerName: String = "Elena Vance",
        phoneNumber: String = "+1 (415) 890-4421",
        isSavedContact: Boolean = false,
        source: CallerIdSource? = CallerIdSource.WHATSAPP
    ) {
        stopFlashlightAlert()
        timerJob?.cancel()
        autoRedialJob?.cancel()
        _callState.value = ActiveCallUiState(
            callerName = callerName,
            phoneNumber = phoneNumber,
            photoUri = null,
            isSavedContact = isSavedContact,
            callerIdSource = source,
            simSlot = 1,
            phase = if (incoming) CallPhase.RINGING else CallPhase.ACTIVE,
            elapsedSeconds = 0L,
            isPreviewMode = true
        )
        if (!incoming) {
            startCallTimer()
        }
    }

    fun answerCall() {
        val state = _callState.value ?: return
        stopFlashlightAlert()
        if (state.isPreviewMode) {
            _callState.update { it?.copy(phase = CallPhase.ACTIVE) }
            startCallTimer()
            scope.launch {
                if (preferencesRepository.getSnapshot().vibrateOnConnectDisconnect) vibratePulse(60L)
            }
            return
        }
        currentCall?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
    }

    fun declineOrEndCall() {
        val state = _callState.value ?: return
        stopFlashlightAlert()
        timerJob?.cancel()
        autoRedialJob?.cancel()
        if (state.isPreviewMode) {
            _callState.value = null
            return
        }
        if (state.phase == CallPhase.RINGING) {
            currentCall?.reject(false, null)
        } else {
            currentCall?.disconnect()
        }
        _callState.value = null
    }

    fun toggleMute() {
        val current = _callState.value ?: return
        val next = !current.isMuted
        _callState.update { it?.copy(isMuted = next) }
        if (!current.isPreviewMode) {
            boundService?.setMuted(next)
        }
    }

    fun toggleSpeaker() {
        val current = _callState.value ?: return
        val next = !current.isSpeakerOn
        _callState.update { it?.copy(isSpeakerOn = next, isBluetoothOn = if (next) false else it.isBluetoothOn) }
        if (!current.isPreviewMode) {
            @Suppress("DEPRECATION")
            boundService?.setAudioRoute(
                if (next) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
            )
        }
    }

    fun toggleBluetooth() {
        val current = _callState.value ?: return
        val next = !current.isBluetoothOn
        _callState.update { it?.copy(isBluetoothOn = next, isSpeakerOn = if (next) false else it.isSpeakerOn) }
        if (!current.isPreviewMode) {
            @Suppress("DEPRECATION")
            boundService?.setAudioRoute(
                if (next) CallAudioState.ROUTE_BLUETOOTH else CallAudioState.ROUTE_EARPIECE
            )
        }
    }

    fun toggleKeypad() {
        _callState.update { it?.copy(isKeypadVisible = !it.isKeypadVisible) }
    }

    fun sendDtmf(digit: Char) {
        val current = _callState.value ?: return
        _callState.update { it?.copy(dtmfEntered = (it.dtmfEntered + digit).takeLast(24)) }
        if (!current.isPreviewMode) {
            currentCall?.playDtmfTone(digit)
            scope.launch {
                delay(160L)
                currentCall?.stopDtmfTone()
            }
        }
    }

    fun toggleAutoRedialInCall() {
        scope.launch {
            val prefs = preferencesRepository.getSnapshot()
            _callState.update { cur ->
                if (cur == null) return@update null
                val next = !cur.isAutoRedialArmed
                cur.copy(
                    isAutoRedialArmed = next,
                    autoRedialAttemptsLeft = if (next) prefs.autoRedialAttempts else 0
                )
            }
        }
    }

    fun switchSimInPreviewOrRedial() {
        _callState.update { cur ->
            cur?.copy(simSlot = if (cur.simSlot == 1) 2 else 1)
        }
    }

    fun markCurrentCallerSaved() {
        _callState.update { it?.copy(isSavedContact = true, callerIdSource = null) }
    }

    private fun triggerAutoRedial(number: String, simSlot: Int, attemptsLeft: Int, intervalSec: Int) {
        autoRedialJob?.cancel()
        autoRedialJob = scope.launch {
            for (sec in intervalSec downTo 1) {
                _callState.update {
                    it?.copy(autoRedialCountdownSec = sec, autoRedialAttemptsLeft = attemptsLeft)
                }
                delay(1000L)
            }
            _callState.update { it?.copy(autoRedialCountdownSec = 0, autoRedialAttemptsLeft = attemptsLeft - 1) }
            dialerRepoRef?.placeOutgoingCall(number, simSlot)
        }
    }

    private fun startCallTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                _callState.update { cur ->
                    if (cur != null && cur.phase == CallPhase.ACTIVE) {
                        cur.copy(elapsedSeconds = cur.elapsedSeconds + 1L)
                    } else {
                        cur
                    }
                }
            }
        }
    }

    private fun startFlashlightAlert() {
        flashlightJob?.cancel()
        flashlightJob = scope.launch(Dispatchers.IO) {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return@launch
            val torchCameraId = runCatching {
                cm.cameraIdList.firstOrNull { id ->
                    cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }
            }.getOrNull() ?: return@launch

            var on = false
            try {
                while (isActive) {
                    on = !on
                    runCatching { cm.setTorchMode(torchCameraId, on) }
                    delay(320L)
                }
            } finally {
                runCatching { cm.setTorchMode(torchCameraId, false) }
            }
        }
    }

    private fun stopFlashlightAlert() {
        flashlightJob?.cancel()
        flashlightJob = null
    }

    private fun vibratePulse(durationMs: Long) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        }
    }

    private fun mapTelecomState(state: Int): CallPhase = when (state) {
        Call.STATE_RINGING -> CallPhase.RINGING
        Call.STATE_DIALING, Call.STATE_CONNECTING -> CallPhase.DIALING
        Call.STATE_ACTIVE -> CallPhase.ACTIVE
        Call.STATE_HOLDING -> CallPhase.HOLDING
        else -> CallPhase.DISCONNECTED
    }
}
