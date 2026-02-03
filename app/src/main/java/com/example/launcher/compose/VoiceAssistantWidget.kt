package com.example.launcher.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VoiceAssistantWidget(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation for Active state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.1f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Colors
    val activeGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF00F0FF), Color(0xFF0099FF)) // Neon Cyan
    )
    val inactiveGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFEF4444), Color(0xFF991B1B)) // Red "Off"
    )
    
    val bgBrush = if (isListening) activeGradient else inactiveGradient
    
    val iconRes = if (isListening) android.R.drawable.ic_btn_speak_now else android.R.drawable.ic_lock_silent_mode_off
    // Fallback icon if silent mode off isn't standard, checking: 
    // actually ic_lock_silent_mode_off might not be universal.
    // Let's use ic_menu_call for mic and ic_menu_close_clear_cancel for off?
    // Better: ic_btn_speak_now (mic) vs ic_menu_close_clear_cancel (X)
    
    val statusText = if (isListening) "LISTENING" else "MIC OFF"

    Box(
        modifier = modifier
            .scale(pulseScale)
            .clip(RoundedCornerShape(32.dp))
            .background(bgBrush)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp), // Pill padding
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = if (isListening) android.R.drawable.ic_btn_speak_now else android.R.drawable.ic_menu_close_clear_cancel),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Text(
                text = statusText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
