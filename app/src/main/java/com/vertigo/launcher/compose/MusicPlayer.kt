package com.vertigo.launcher.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vertigo.launcher.R
import com.vertigo.launcher.ui.HomeViewModel

@Composable
fun MusicPlayerCard(
    state: HomeViewModel.MusicState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animated pulse ring for isPlaying
    val infiniteTransition = rememberInfiniteTransition(label = "music_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state.isPlaying) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (state.isPlaying) 1f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0x1A00F0FF), Color(0x1AAC00FF))
                )
            )
            .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            // Header row with status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.isPlaying) "NOW PLAYING" else "MUSIC",
                    color = Color(0xFF00F0FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                if (state.isPlaying) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Live indicator dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FF88))
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Album Art with Pulse
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(pulseScale)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF1A0033), Color(0xFF000D1A))
                            )
                        )
                        .border(1.dp, Color(0xFF00F0FF).copy(alpha = pulseAlpha), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.albumArt != null) {
                        Image(
                            bitmap = state.albumArt.asImageBitmap(),
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "♪",
                            color = Color(0xFF00F0FF).copy(alpha = pulseAlpha),
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Track Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.title.isBlank()) "Tap ▶ to play music" else state.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (state.artist.isBlank()) "—" else state.artist,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback Controls - wider tap targets, brighter colors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                MediaControlButton(
                    iconRes = android.R.drawable.ic_media_previous,
                    tint = Color.White.copy(alpha = 0.85f),
                    size = 44,
                    onClick = onPrev
                )

                Spacer(modifier = Modifier.width(20.dp))

                // Play / Pause (Primary)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(if (state.isPlaying) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF00F0FF), Color(0xFF0060FF))
                            )
                        )
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    val iconRes = if (state.isPlaying) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Next
                MediaControlButton(
                    iconRes = android.R.drawable.ic_media_next,
                    tint = Color.White.copy(alpha = 0.85f),
                    size = 44,
                    onClick = onNext
                )
            }
        }
    }
}

@Composable
private fun MediaControlButton(
    iconRes: Int,
    tint: Color,
    size: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(0x26FFFFFF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size((size * 0.5).dp)
        )
    }
}
