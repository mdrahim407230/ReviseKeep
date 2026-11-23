package com.example.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.i18n.AppStrings
import com.example.ui.components.FrostedBackground
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalGlassPalette
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    strings: AppStrings,
    biometricEnabled: Boolean,
    onVerifyPin: suspend (String) -> Boolean,
    onBiometricUnlocked: () -> Unit
) {
    val glass = LocalGlassPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    fun launchBiometric() {
        val activity = context as? FragmentActivity ?: return
        val manager = BiometricManager.from(activity)
        val canAuth = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onBiometricUnlocked()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(strings.appName)
            .setSubtitle(strings.unlockWithBiometric)
            .setNegativeButtonText(strings.cancel)
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) {
            launchBiometric()
        }
    }

    FrostedBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(modifier = Modifier.widthIn(max = 420.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = strings.appLockTitle,
                        tint = glass.primaryAccent,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = strings.enterPinPrompt,
                        style = MaterialTheme.typography.headlineMedium,
                        color = glass.inkPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        repeat(4) { index ->
                            val filled = index < enteredPin.length
                            Surface(
                                modifier = Modifier.size(20.dp),
                                shape = CircleShape,
                                color = if (filled) glass.primaryAccent else glass.glassBorder
                            ) {}
                        }
                    }

                    if (showError) {
                        Text(
                            text = strings.wrongPin,
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.missedColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf(if (biometricEnabled) "BIO" else "", "0", "DEL")
                    )

                    rows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { key ->
                                if (key.isEmpty()) {
                                    Spacer(modifier = Modifier.size(68.dp))
                                } else {
                                    Surface(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .testTag("pin_key_$key")
                                            .clickable {
                                                showError = false
                                                when (key) {
                                                    "DEL" -> if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                    }
                                                    "BIO" -> launchBiometric()
                                                    else -> if (enteredPin.length < 4) {
                                                        val next = enteredPin + key
                                                        enteredPin = next
                                                        if (next.length == 4) {
                                                            scope.launch {
                                                                val ok = onVerifyPin(next)
                                                                if (!ok) {
                                                                    showError = true
                                                                    enteredPin = ""
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                        shape = CircleShape,
                                        color = glass.glassElevated,
                                        border = BorderStroke(1.dp, glass.glassBorder)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            when (key) {
                                                "DEL" -> Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                                    contentDescription = "Delete",
                                                    tint = glass.inkPrimary
                                                )
                                                "BIO" -> Icon(
                                                    imageVector = Icons.Rounded.Fingerprint,
                                                    contentDescription = strings.unlockWithBiometric,
                                                    tint = glass.primaryAccent,
                                                    modifier = Modifier.size(30.dp)
                                                )
                                                else -> Text(
                                                    text = key,
                                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = glass.inkPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
