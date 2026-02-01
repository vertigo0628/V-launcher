package com.example.launcher.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PowerCore(
    batteryLevel: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    // Colors
    val neonCyan = Color(0xFF00F0FF)
    val neonGreen = Color(0xFF00FF9D)
    val neonRed = Color(0xFFFF003C)
    
    val activeColor = when {
        batteryLevel < 20 -> neonRed
        isCharging -> neonGreen
        else -> neonCyan
    }
    
    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "core_pulse")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Smooth progress animation
    val animatedProgress by animateFloatAsState(
        targetValue = batteryLevel / 100f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size.width / 2f
            val strokeWidth = 20f
            val radius = (size.width - strokeWidth * 2) / 2f
            
            // Draw Core Background (Dim)
            drawCircle(
                color = activeColor.copy(alpha = 0.1f),
                radius = radius - 20f,
                center = Offset(center, center)
            )
            
            // Draw Track (Dim)
            drawArc(
                color = activeColor.copy(alpha = 0.2f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center - radius, center - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Draw Progress
            drawArc(
                color = activeColor,
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                topLeft = Offset(center - radius, center - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Draw Rotating Decor (if charging)
            if (isCharging) {
                rotate(degrees = rotation) {
                    drawCircle(
                        color = activeColor.copy(alpha = 0.4f),
                        radius = radius + 15f,
                        center = Offset(center, center),
                        style = Stroke(width = 4f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f))
                    )
                }
            }
        }
        
        // Center Text
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$batteryLevel%",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = if (isCharging) "CHARGING" else "POWER",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
