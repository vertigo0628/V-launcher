package com.example.launcher.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launcher.ui.HomeViewModel

@Composable
fun NeuralHub(
    state: HomeViewModel.NeuralHubState,
    musicState: HomeViewModel.MusicState,
    onClose: () -> Unit,
    onMusicPlayPause: () -> Unit = {},
    onMusicNext: () -> Unit = {},
    onMusicPrev: () -> Unit = {},
    cpuHistory: List<Int> = emptyList(),
    neuralInsight: String = "System running optimally"
) {
    // Cyberpunk Gradient Background
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xB30F172A), // 70% Dark Blue
            Color(0xB3000000)  // 70% Black
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .clickable(enabled = false) {}, // Catch clicks
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val isWideScreen = maxWidth > 600.dp
            val containerWidth = if (isWideScreen) 500.dp else maxWidth

            Box(
                modifier = Modifier
                    .width(containerWidth)
                    .fillMaxHeight()
                    .align(Alignment.Center),
                contentAlignment = Alignment.TopCenter
            ) {
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEURAL HUB",
                    color = Color(0xFF00F0FF),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = onClose) {
                    Text(text = "✕", color = Color.White, fontSize = 24.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Power Core (Battery)
            PowerCore(
                batteryLevel = state.batteryLevel,
                isCharging = state.isCharging,
                modifier = Modifier.size(240.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // System Vitals Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // RAM Card
                SystemCard(
                    title = "MEMORY",
                    value = state.ramPercent,
                    detail = state.ramText,
                    color = Color(0xFFBD00FF), // Neon Purple
                    modifier = Modifier.weight(1f)
                )
                
                // Storage Card
                SystemCard(
                    title = "STORAGE",
                    value = state.storagePercent,
                    detail = state.storageText,
                    color = Color(0xFFFF006E), // Neon Pink
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Music Player Widget
            MusicPlayerCard(
                state = musicState,
                onPlayPause = onMusicPlayPause,
                onNext = onMusicNext,
                onPrev = onMusicPrev
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CPU History Graph (Phase 11)
            CpuHistoryGraph(history = cpuHistory)

            Spacer(modifier = Modifier.height(12.dp))

            // Neural Insights Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1AFFFFFF))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🧠 NEURAL INSIGHTS",
                        color = Color(0xFF00F0FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = neuralInsight,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }  // end AI Predictions Box

            Spacer(modifier = Modifier.height(24.dp))

            // Mini Apps Panel
            MiniAppsPanel()

            Spacer(modifier = Modifier.height(24.dp))
        } // end Column
            } // end Box (width constraint)
        } // end BoxWithConstraints
    } // end Main Background Box
}

@Composable
fun SystemCard(
    title: String,
    value: Int,
    detail: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = title, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress Bar
            LinearProgressIndicator(
                progress = value / 100f,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "$value%", color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = detail, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}
