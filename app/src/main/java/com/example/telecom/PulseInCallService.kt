package com.example.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.example.PulsePhoneApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PulseInCallService : InCallService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            val app = applicationContext as? PulsePhoneApplication ?: return
            app.container.activeCallManager.onCallStateChanged(call, state)
        }
    }

    override fun onBind(intent: Intent?): android.os.IBinder? {
        (applicationContext as? PulsePhoneApplication)?.container?.activeCallManager?.attachInCallService(this)
        return super.onBind(intent)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callCallback)
        val app = applicationContext as? PulsePhoneApplication ?: return
        app.container.activeCallManager.onCallAdded(call)

        serviceScope.launch {
            val prefs = app.container.preferencesRepository.getSnapshot()
            // Launch full-screen OLED Call Activity only if Custom Full-Screen Caller ID is enabled
            if (prefs.customFullScreenCallerId) {
                val intent = Intent(this@PulseInCallService, PulseCallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        val app = applicationContext as? PulsePhoneApplication ?: return
        app.container.activeCallManager.onCallRemoved(call)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        if (audioState != null) {
            (applicationContext as? PulsePhoneApplication)
                ?.container
                ?.activeCallManager
                ?.onAudioStateChanged(audioState)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (applicationContext as? PulsePhoneApplication)?.container?.activeCallManager?.attachInCallService(null)
        serviceScope.cancel()
    }
}
