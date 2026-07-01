package com.vertigo.launcher.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vertigo.launcher.R
import kotlinx.coroutines.delay
import kotlin.random.Random

data class CloudConfig(
    val xOffsetStart: Float,
    val yOffset: Float,
    val sizeDp: Int,
    val speedFactor: Int,
    val opacity: Float,
    val scaleX: Float = 1f,
    val resourceVariant: Int = 0  // 0 or 1 to alternate between cloud PNGs
)

val SineToggleEasing = Easing { fraction ->
    kotlin.math.sin(fraction * kotlin.math.PI).toFloat()
}

@Composable
fun WeatherAtmosphereOverlay(weatherCode: Int) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (weatherCode) {
            0 -> {
                // Clear Sky - subtle sun flare
                RealisticSunFlare()
            }
            1, 2, 3 -> {
                // Partly Cloudy / Cloudy
                // 1 = Mainly Clear, 2 = Partly Cloudy, 3 = Overcast
                val density = when (weatherCode) {
                    1 -> 0.3f
                    2 -> 0.7f
                    else -> 1.2f
                }
                RealisticClouds(density = density)
            }
            45, 48 -> {
                // Foggy
                RealisticFog()
            }
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> {
                // Drizzle, Rain, and Rain Showers
                val isHeavy = weatherCode == 65 || weatherCode == 67 || weatherCode == 82
                RealisticRain(isHeavy = isHeavy)
                RealisticClouds(density = 1f, isDark = true)
            }
            71, 73, 75, 77, 85, 86 -> {
                // Snow and Snow Showers
                val isHeavy = weatherCode == 75 || weatherCode == 86
                RealisticSnow(isHeavy = isHeavy)
            }
            95, 96, 99 -> {
                // Thunderstorm
                RealisticRain(isHeavy = true)
                RealisticClouds(density = 1.5f, isDark = true)
                LightningFlashes()
            }
        }
    }
}

@Composable
fun RealisticClouds(density: Float, isDark: Boolean = false) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()

    // Each cloud has a phase (0.0–1.0) representing where it starts in its journey
    // This ensures clouds are already spread across the screen on first render
    data class CloudLayer(
        val yOffset: Float,
        val sizeDp: Int,
        val driftDurationMs: Int,
        val opacity: Float,
        val scaleX: Float = 1f,
        val resourceVariant: Int = 0,
        val initialPhase: Float = 0f  // 0.0 = starts at left edge, 0.5 = starts mid-screen
    )

    val cloudLayers = remember(density, isDark) {
        val base = mutableListOf(
            CloudLayer(30f,  420, 65000, if (isDark) 0.80f else 0.70f, resourceVariant = 0, initialPhase = 0.1f),
            CloudLayer(200f, 480, 50000, if (isDark) 0.85f else 0.75f, resourceVariant = 1, initialPhase = 0.55f),
            CloudLayer(400f, 360, 58000, if (isDark) 0.75f else 0.65f, -1f, resourceVariant = 0, initialPhase = 0.3f)
        )
        if (density >= 1f) {
            base.add(CloudLayer(600f, 400, 72000, if (isDark) 0.78f else 0.68f, resourceVariant = 1, initialPhase = 0.75f))
        }
        if (density > 1f) {
            base.add(CloudLayer(120f, 440, 55000, if (isDark) 0.85f else 0.72f, -1f, resourceVariant = 0, initialPhase = 0.45f))
            base.add(CloudLayer(500f, 340, 62000, if (isDark) 0.78f else 0.65f, resourceVariant = 1, initialPhase = 0.85f))
        }
        base
    }

    Box(modifier = Modifier.fillMaxSize()) {
        cloudLayers.forEach { cloud ->
            // Total travel distance: from fully off-screen left to fully off-screen right
            val cloudWidthDp = cloud.sizeDp.toFloat()
            val totalTravel = screenWidthDp + cloudWidthDp

            // Animate a 0→1 fraction over the drift duration
            val drift = rememberInfiniteTransition(label = "drift")
            val fraction by drift.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(cloud.driftDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ), label = "frac"
            )

            // Apply phase offset and wrap with modulo for seamless looping
            val wrappedFraction = (fraction + cloud.initialPhase) % 1f
            // Map fraction to screen X: -cloudWidth → screenWidth
            val x = -cloudWidthDp + (wrappedFraction * totalTravel)

            // Gentle bobbing
            val bob = rememberInfiniteTransition(label = "bob")
            val dy by bob.animateFloat(
                initialValue = -8f, targetValue = 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(cloud.driftDurationMs / 6, easing = SineToggleEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "dy"
            )

            val cloudRes = if (isDark) {
                R.drawable.cloud_dark_storm
            } else {
                if (cloud.resourceVariant == 0) R.drawable.cloud_realistic_1 else R.drawable.cloud_realistic_2
            }

            // Elongated: width is the sizeDp, height is 35% of width (wide and flat)
            val wDp = cloud.sizeDp.dp
            val hDp = (cloud.sizeDp * 0.35f).dp

            Image(
                painter = painterResource(id = cloudRes),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .width(wDp)
                    .height(hDp)
                    .offset(x = x.dp, y = (cloud.yOffset + dy).dp)
                    .alpha(cloud.opacity)
                    .scale(scaleX = cloud.scaleX, scaleY = 1f)
            )
        }
    }
}



@Composable
fun RealisticFog() {
    val infiniteTransition = rememberInfiniteTransition(label = "Fog")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FogAlpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .blur(30.dp)
            .background(Color.White.copy(alpha = 0.5f))
    )
}

@Composable
fun RealisticRain(isHeavy: Boolean) {
    val dropCount = if (isHeavy) 200 else 100

    val drops = remember {
        List(dropCount) {
            RainDrop(
                x = Random.nextFloat(),
                yOffset = Random.nextFloat(),
                speed = Random.nextFloat() * 0.6f + 0.7f,
                length = Random.nextFloat() * 55f + 30f,   // longer streaks
                alpha = Random.nextFloat() * 0.5f + 0.45f  // much more opaque
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Rain")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),  // faster
            repeatMode = RepeatMode.Restart
        ),
        label = "Time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val slant = 0.18f // slight rightward angle

        drops.forEach { drop ->
            val currentY = ((drop.yOffset + time * drop.speed * 6f) % 1f) * h
            val xPos = drop.x * w

            val x1 = xPos + drop.length * slant
            val y1 = currentY
            val x2 = xPos - drop.length * slant
            val y2 = currentY + drop.length

            // Main streak — white/blue tint, thick
            drawLine(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color(0xAAE8F4FF),
                        Color(0xFFB8D8F0),
                        Color(0x44AACCEE)
                    ),
                    start = androidx.compose.ui.geometry.Offset(x1, y1),
                    end = androidx.compose.ui.geometry.Offset(x2, y2)
                ),
                start = androidx.compose.ui.geometry.Offset(x1, y1),
                end = androidx.compose.ui.geometry.Offset(x2, y2),
                strokeWidth = if (isHeavy) 3.2f else 2.4f,
                alpha = drop.alpha
            )
            // Bright core highlight line inside streak
            drawLine(
                color = Color.White.copy(alpha = drop.alpha * 0.5f),
                start = androidx.compose.ui.geometry.Offset(x1, y1 + drop.length * 0.2f),
                end = androidx.compose.ui.geometry.Offset(x2, y2 - drop.length * 0.2f),
                strokeWidth = 1f
            )
        }
    }
}


@Composable
fun RealisticSnow(isHeavy: Boolean) {
    val configuration = LocalConfiguration.current
    val screenWidthPx = configuration.screenWidthDp.toFloat()
    val screenHeightPx = configuration.screenHeightDp.toFloat()

    val flakeCount = if (isHeavy) 100 else 40
    
    val flakes = remember {
        List(flakeCount) {
            SnowFlake(
                x = Random.nextFloat(),
                yOffset = Random.nextFloat(),
                speed = Random.nextFloat() * 0.2f + 0.1f,
                size = Random.nextFloat() * 16f + 8f,
                wobbleSpeed = Random.nextFloat() * 2f + 1f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Snow")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Time"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        flakes.forEach { flake ->
            val currentY = ((flake.yOffset + time * flake.speed * 2f) % 1f) * screenHeightPx
            val wobble = kotlin.math.sin((time * flake.wobbleSpeed * kotlin.math.PI * 2) + flake.x * 10).toFloat()
            val xPos = (flake.x * screenWidthPx) + (wobble * 30f)
            
            Image(
                painter = painterResource(id = R.drawable.realistic_snow),
                contentDescription = null,
                modifier = Modifier
                    .size(flake.size.dp)
                    .offset(x = xPos.dp, y = currentY.dp)
                    .alpha(0.8f)
            )
        }
    }
}

@Composable
fun LightningFlashes() {
    var flashAlpha by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(3000, 15000))
            // Flash 1
            flashAlpha = 0.8f
            delay(50)
            flashAlpha = 0f
            delay(100)
            // Flash 2 (sometimes)
            if (Random.nextBoolean()) {
                flashAlpha = 0.6f
                delay(80)
                flashAlpha = 0f
            }
        }
    }
    
    if (flashAlpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = flashAlpha))
        )
    }
}

@Composable
fun RealisticSunFlare() {
    val infiniteTransition = rememberInfiniteTransition(label = "SunFlare")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Draw the glowing sun flare in the top right corner
        Canvas(modifier = Modifier
            .size(300.dp)
            .align(androidx.compose.ui.Alignment.TopEnd)
            .offset(x = 50.dp, y = (-50).dp)
            .alpha(alpha)
            .graphicsLayer(rotationZ = rotation)
        ) {
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF0D4),
                        Color(0xFFFFD479).copy(alpha = 0.5f),
                        Color.Transparent
                    )
                ),
                radius = size.width / 2f
            )
            // Draw cross-flare lines
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = 4f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 4f
            )
        }
    }
}

data class RainDrop(val x: Float, val yOffset: Float, val speed: Float, val length: Float, val alpha: Float)
data class SnowFlake(val x: Float, val yOffset: Float, val speed: Float, val size: Float, val wobbleSpeed: Float)
