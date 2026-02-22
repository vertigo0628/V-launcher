package com.example.launcher.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launcher.ui.HomeViewModel

@Composable
fun SystemMonitorsPanel(
    state: HomeViewModel.NeuralHubState,
    onBoostRam: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SYSTEM VITALS",
            color = Color(0xFF00F0FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PerformanceCard(
                title = "BATTERY",
                value = "${state.batteryLevel}%",
                subValue = if (state.isCharging) "CHARGING" else state.batteryHealth,
                progress = state.batteryLevel / 100f,
                accentColor = if (state.batteryLevel < 20) Color.Red else Color(0xFF00F0FF),
                modifier = Modifier.weight(1f)
            )
            PerformanceCard(
                title = "RAM",
                value = "${state.ramPercent}%",
                subValue = state.ramText,
                progress = state.ramPercent / 100f,
                accentColor = Color(0xFFAC00FF),
                modifier = Modifier.weight(1f),
                action = {
                    Button(
                        onClick = onBoostRam,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33AC00FF)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("BOOST", fontSize = 10.sp, color = Color(0xFFAC00FF))
                    }
                }
            )
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PerformanceCard(
                title = "STORAGE",
                value = "${state.storagePercent}%",
                subValue = state.storageText,
                progress = state.storagePercent / 100f,
                accentColor = Color(0xFF00FF9C),
                modifier = Modifier.weight(1f)
            )
            PerformanceCard(
                title = "CPU",
                value = "${state.cpuLoad}%",
                subValue = "SYSTEM LOAD",
                progress = state.cpuLoad / 100f,
                accentColor = Color(0xFFFF9C00),
                modifier = Modifier.weight(1f)
            )
        }
        
        // Detailed Battery Health Info
        BatteryDetailsCard(state)
    }
}

@Composable
fun PerformanceCard(
    title: String,
    value: String,
    subValue: String,
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE100000))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                action?.invoke()
            }
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(subValue, color = Color.Gray, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(CircleShape),
                color = accentColor,
                trackColor = Color(0x22FFFFFF)
            )
        }
    }
}

@Composable
fun BatteryDetailsCard(state: HomeViewModel.NeuralHubState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x1A00F0FF))
            .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text("BATTERY HEALTH DIAGNOSTIC", color = Color(0xFF00F0FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoItem("TEMP", "${state.batteryTemp}°C")
                InfoItem("VOLTAGE", "${state.batteryVoltage}mV")
                InfoItem("TECH", state.batteryTech)
                InfoItem("HEALTH", state.batteryHealth)
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 8.sp)
        Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── CPU History Graph (Phase 11) ──────────────────────────────────────────────

@Composable
fun CpuHistoryGraph(
    history: List<Int>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFFFF9C00),
    fillTopColor: Color = Color(0x55FF9C00),
    fillBottomColor: Color = Color(0x00FF9C00)
) {
    val displayHistory = if (history.isEmpty()) List(30) { 0 } else history

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0DFFFFFF))
            .border(1.dp, Color(0x22FF9C00), RoundedCornerShape(12.dp))
    ) {
        // Labels
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text("CPU LOAD", color = Color(0xFFFF9C00), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text("${displayHistory.lastOrNull() ?: 0}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Canvas(modifier = Modifier.fillMaxSize().padding(top = 20.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)) {
            val w = size.width
            val h = size.height
            val maxSamples = displayHistory.size.coerceAtLeast(2)
            val stepX = w / (maxSamples - 1).toFloat()

            fun xFor(i: Int) = i * stepX
            fun yFor(v: Int) = h - (v / 100f) * h

            // Build fill path
            val fillPath = Path().apply {
                moveTo(xFor(0), h)
                displayHistory.forEachIndexed { i, v ->
                    lineTo(xFor(i), yFor(v))
                }
                lineTo(xFor(displayHistory.lastIndex), h)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(fillTopColor, fillBottomColor),
                    startY = 0f, endY = h
                )
            )

            // Build line path
            val linePath = Path().apply {
                displayHistory.forEachIndexed { i, v ->
                    if (i == 0) moveTo(xFor(0), yFor(v)) else lineTo(xFor(i), yFor(v))
                }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw current value dot
            val lastIdx = displayHistory.lastIndex
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = Offset(xFor(lastIdx), yFor(displayHistory[lastIdx]))
            )
        }
    }
}
