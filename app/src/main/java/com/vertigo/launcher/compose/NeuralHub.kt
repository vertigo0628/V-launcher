package com.vertigo.launcher.compose

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
import com.vertigo.launcher.ui.HomeViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import android.provider.Settings
import com.vertigo.launcher.service.VLauncherAccessibilityService
import kotlinx.coroutines.launch

@Composable
fun NeuralHub(
    state: HomeViewModel.NeuralHubState,
    musicState: HomeViewModel.MusicState,
    onClose: () -> Unit,
    onMusicPlayPause: () -> Unit = {},
    onMusicNext: () -> Unit = {},
    onMusicPrev: () -> Unit = {},
    cpuHistory: List<Int> = emptyList(),
    neuralInsight: String = "System running optimally",
    viewModel: HomeViewModel? = null,
    shizukuState: com.vertigo.launcher.utils.ShizukuSetup.ShizukuState = com.vertigo.launcher.utils.ShizukuSetup.ShizukuState.UNAVAILABLE
) {
    // Cyberpunk Gradient Background
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xB30F172A), // 70% Dark Blue
            Color(0xB3000000)  // 70% Black
        )
    )
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showKillSwitchConfirmation by remember { mutableStateOf(false) }
    
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
                        .imePadding()
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
            
            // Power Core (Battery) - Responsive size
            val coreSize = (containerWidth * 0.6f).coerceIn(160.dp, 300.dp)
            PowerCore(
                batteryLevel = state.batteryLevel,
                isCharging = state.isCharging,
                modifier = Modifier.size(coreSize)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Shizuku Status Indicator
            val shizukuColor = when (shizukuState) {
                com.vertigo.launcher.utils.ShizukuSetup.ShizukuState.AUTHORIZED -> Color(0xFF10B981) // Green
                com.vertigo.launcher.utils.ShizukuSetup.ShizukuState.AVAILABLE -> Color(0xFFF59E0B)  // Yellow
                else -> Color(0xFFEF4444) // Red
            }
            val shizukuText = when (shizukuState) {
                com.vertigo.launcher.utils.ShizukuSetup.ShizukuState.AUTHORIZED -> "SHIZUKU CONNECTED"
                com.vertigo.launcher.utils.ShizukuSetup.ShizukuState.AVAILABLE -> "SHIZUKU PENDING"
                else -> "SHIZUKU UNAVAILABLE"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1AFFFFFF))
                    .clickable { 
                        if (shizukuState == com.vertigo.launcher.utils.ShizukuSetup.ShizukuState.AVAILABLE) {
                            com.vertigo.launcher.utils.ShizukuSetup.requestPermissionIfNeeded()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(shizukuColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = shizukuText,
                    color = shizukuColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            
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
                onPrev = onMusicPrev,
                onSeek = { pos -> viewModel?.musicSeekTo(pos) }
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

            // Shizuku Power Actions (Boost & Kill Switch)
            val isShizukuAuthorized = shizukuState == com.vertigo.launcher.utils.ShizukuSetup.ShizukuState.AUTHORIZED
            
            Button(
                onClick = {
                    if (isShizukuAuthorized) {
                        scope.launch {
                            com.vertigo.launcher.logic.AppCommander.trimMemory()
                            android.widget.Toast.makeText(context, "🚀 Neural Boost Activated", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        com.vertigo.launcher.utils.ShizukuSetup.requestPermissionIfNeeded()
                        android.widget.Toast.makeText(context, "Please authorize Shizuku to run Boost", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isShizukuAuthorized) Color(0xFF00F0FF).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (isShizukuAuthorized) Color(0xFF00F0FF) else Color.Gray.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = "🚀 NEURAL BOOST", 
                    color = if (isShizukuAuthorized) Color(0xFF00F0FF) else Color.Gray, 
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = {
                    if (isShizukuAuthorized) {
                        showKillSwitchConfirmation = true
                    } else {
                        com.vertigo.launcher.utils.ShizukuSetup.requestPermissionIfNeeded()
                        android.widget.Toast.makeText(context, "Please authorize Shizuku to run Kill Switch", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isShizukuAuthorized) Color(0xFFFF003C).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (isShizukuAuthorized) Color(0xFFFF003C) else Color.Gray.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = "💀 SYSTEM KILL SWITCH", 
                    color = if (isShizukuAuthorized) Color(0xFFFF003C) else Color.Gray, 
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (!isShizukuAuthorized) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🔒 System actions require Shizuku setup",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (showKillSwitchConfirmation) {
                AlertDialog(
                    onDismissRequest = { showKillSwitchConfirmation = false },
                    title = {
                        Text(
                            text = "⚠️ ACTIVATE KILL SWITCH?",
                            color = Color(0xFFFF003C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                    },
                    text = {
                        Text(
                            text = "This will forcefully stop all background and user applications, freeing up memory like a reboot. Unsaved app progress will be lost.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showKillSwitchConfirmation = false
                                scope.launch {
                                    android.widget.Toast.makeText(context, "Executing System Kill Switch...", android.widget.Toast.LENGTH_LONG).show()
                                    val result = com.vertigo.launcher.logic.AppCommander.killAllApps(context)
                                    if (result.isSuccess) {
                                        android.widget.Toast.makeText(context, "💀 System Purged successfully", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Execution failed: ${result.stderr}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF003C))
                        ) {
                            Text("PURGE SYSTEM", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showKillSwitchConfirmation = false }
                        ) {
                            Text("CANCEL", color = Color.White.copy(alpha = 0.6f))
                        }
                    },
                    containerColor = Color(0xFF1E1E2E),
                    textContentColor = Color.White,
                    titleContentColor = Color(0xFFFF003C),
                    shape = RoundedCornerShape(24.dp)
                )
            }

            // Mini Apps Panel
            MiniAppsPanel(viewModel = viewModel)

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
