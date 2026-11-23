package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.PulsePhoneApp
import com.example.ui.theme.PulsePhoneTheme
import com.example.ui.viewmodel.PulsePhoneViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialDialNumber = extractTelNumber(intent)
        val container = (application as PulsePhoneApplication).container

        setContent {
            val vm: PulsePhoneViewModel = viewModel(
                factory = PulsePhoneViewModel.Factory(container)
            )
            val prefs by vm.preferences.collectAsStateWithLifecycle()

            PulsePhoneTheme(
                themeMode = prefs.themeMode,
                useDynamicColor = prefs.useDynamicColor,
                accentPalette = prefs.accentPalette
            ) {
                PulsePhoneApp(
                    viewModel = vm,
                    initialDialNumber = initialDialNumber
                )
            }
        }
    }

    private fun extractTelNumber(intent: Intent?): String? {
        val data = intent?.data ?: return null
        return if (data.scheme.equals("tel", ignoreCase = true)) {
            data.schemeSpecificPart?.trim()?.takeIf { it.isNotBlank() }
        } else {
            null
        }
    }
}
