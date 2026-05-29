package com.vertigo.launcher.compose

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.vertigo.launcher.model.AppModel
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale

/**
 * Shows a full-screen biometric lock overlay for a given [app].
 * Automatically launches the BiometricPrompt on display.
 * Calls [onUnlocked] on success, [onDismiss] if cancelled.
 */
@Composable
fun PrivacyShieldPrompt(
    app: AppModel,
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Animate lock icon pulse
    val infiniteTransition = rememberInfiniteTransition(label = "lock_pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )

    // Launch biometric prompt immediately when this composable enters composition
    LaunchedEffect(Unit) {
        launchBiometricPrompt(
            context = context,
            title = "Unlock ${app.label}",
            onSuccess = onUnlocked,
            onFail = onDismiss
        )
    }

    // Overlay UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xE6000D1A), Color(0xE60F0024))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated lock ring
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .background(Color(0x1AAC00FF))
                    .border(2.dp, Color(0xFFAC00FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🔒", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "APP LOCKED",
                color = Color(0xFFAC00FF),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${app.label} is protected\nAuthenticate to open",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Fingerprint icon hint
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x44FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👆", fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Touch the sensor",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Helper to launch Android BiometricPrompt from a Composable context.
 */
internal fun launchBiometricPrompt(
    context: Context,
    title: String,
    subtitle: String = "Use biometrics or device credential to continue",
    onSuccess: () -> Unit,
    onFail: () -> Unit
) {
    val activity = context as? FragmentActivity ?: run {
        // If we can't get a FragmentActivity, fallback to allowing launch
        onSuccess()
        return
    }

    val biometricManager = BiometricManager.from(context)
    val canAuthResult = biometricManager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
    )

    if (canAuthResult != BiometricManager.BIOMETRIC_SUCCESS) {
        // Device doesn't support biometrics or none enrolled — allow through
        onSuccess()
        return
    }

    val executor = ContextCompat.getMainExecutor(context)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                onFail()
            }
        }
        override fun onAuthenticationFailed() {
            // Wrong finger/face — stay on prompt, don't dismiss
        }
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
}
