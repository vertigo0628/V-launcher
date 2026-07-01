package com.vertigo.launcher.compose

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vertigo.launcher.model.AppModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

// 2D Point class for gesture matching
data class Point2D(val x: Float, val y: Float)

@Composable
fun ScribbleSearchOverlay(
    apps: List<AppModel>,
    onAppClick: (AppModel) -> Unit,
    onClose: () -> Unit,
    themeAccentColor: Color,
    glassmorphismEnabled: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    var currentStroke = remember { mutableStateListOf<Offset>() }
    var detectedLetter by remember { mutableStateOf<String?>(null) }
    var matchingApps by remember { mutableStateOf<List<AppModel>>(emptyList()) }
    var showOverlayContent by remember { mutableStateOf(true) }
    
    // Fade out drawing path after a delay when finger is lifted
    var fadeAlpha by remember { mutableStateOf(1f) }

    // Pre-calculate templates for matching
    val gestureTemplates = remember { getPredefinedTemplates() }

    // Handle template matching
    fun matchStrokeToLetter(points: List<Offset>) {
        if (points.size < 5) return
        
        // 1. Convert Offset to Point2D
        val rawPoints = points.map { Point2D(it.x, it.y) }
        
        // 2. Resample path to 32 points
        val resampled = resample(rawPoints, 32)
        
        // 3. Normalize: Scale to 100x100 and Translate to origin
        val normalized = normalize(resampled)
        
        // 4. Compare with templates
        var bestMatch: String? = null
        var minDistance = Float.MAX_VALUE
        
        gestureTemplates.forEach { (letter, templatePoints) ->
            val distance = pathDistance(normalized, templatePoints)
            if (distance < minDistance) {
                minDistance = distance
                bestMatch = letter
            }
        }
        
        // Set match threshold (adjustable)
        if (minDistance < 85f) {
            detectedLetter = bestMatch
            detectedLetter?.let { letter ->
                matchingApps = apps.filter { it.label.startsWith(letter, ignoreCase = true) }
            }
        } else {
            detectedLetter = null
            matchingApps = emptyList()
        }
    }

    // Glassmorphic background container modifier
    val containerModifier = if (glassmorphismEnabled) {
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .blur(12.dp)
    } else {
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Blur background
        Box(modifier = containerModifier.clickable { onClose() })

        // Interactive Content Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SCRIBBLE SEARCH",
                    color = themeAccentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .size(36.dp)
                ) {
                    Text(text = "✕", color = Color.White, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Draw any letter below to filter system modules",
                color = Color.Gray,
                fontSize = 13.sp
            )

            // Drawing Area Canvas
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentStroke.clear()
                                currentStroke.add(offset)
                                fadeAlpha = 1f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentStroke.add(change.position)
                            },
                            onDragEnd = {
                                if (currentStroke.isNotEmpty()) {
                                    matchStrokeToLetter(currentStroke.toList())
                                    coroutineScope.launch {
                                        // Fade out animation
                                        delay(400)
                                        var a = 1f
                                        while (a > 0f) {
                                            a -= 0.1f
                                            fadeAlpha = a.coerceAtLeast(0f)
                                            delay(20)
                                        }
                                        currentStroke.clear()
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Stroke Drawing
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (currentStroke.size > 1) {
                        val path = Path().apply {
                            moveTo(currentStroke[0].x, currentStroke[0].y)
                            for (i in 1 until currentStroke.size) {
                                lineTo(currentStroke[i].x, currentStroke[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = themeAccentColor.copy(alpha = fadeAlpha),
                            style = Stroke(
                                width = 8.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Show big glowing placeholder letter inside canvas
                if (detectedLetter != null) {
                    Text(
                        text = detectedLetter!!,
                        color = themeAccentColor.copy(alpha = 0.25f),
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Black
                    )
                } else if (currentStroke.isEmpty()) {
                    Text(
                        text = "DRAW HERE",
                        color = Color.White.copy(alpha = 0.07f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                }
            }

            // Results Section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (detectedLetter != null) "MATCHING MODULES ('${detectedLetter}')" else "MATCHING MODULES",
                    color = themeAccentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (matchingApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.02f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (detectedLetter != null) "No modules start with '$detectedLetter'" else "Awaiting input stroke...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 72.dp),
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(matchingApps, key = { it.packageName }) { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable {
                                        onAppClick(app)
                                        onClose()
                                    }
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(themeAccentColor.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncAppIcon(
                                        app = app,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = app.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Mathematical resampler and normalizer for $1 Gesture Recognizer ───────────────────

private fun resample(points: List<Point2D>, n: Int): List<Point2D> {
    if (points.isEmpty()) return emptyList()
    val interval = pathLength(points) / (n - 1)
    var accumulatedDistance = 0f
    val newPoints = mutableListOf<Point2D>()
    newPoints.add(points[0])
    
    val mutablePoints = points.toMutableList()
    var i = 1
    while (i < mutablePoints.size) {
        val prev = mutablePoints[i - 1]
        val curr = mutablePoints[i]
        val dist = distance(prev, curr)
        
        if (accumulatedDistance + dist >= interval) {
            val qx = prev.x + ((interval - accumulatedDistance) / dist) * (curr.x - prev.x)
            val qy = prev.y + ((interval - accumulatedDistance) / dist) * (curr.y - prev.y)
            val q = Point2D(qx, qy)
            newPoints.add(q)
            mutablePoints.add(i, q) // insert q so it becomes prev on next iteration
            accumulatedDistance = 0f
        } else {
            accumulatedDistance += dist
        }
        i++
    }
    
    // Ensure exact number of points due to rounding errors
    while (newPoints.size < n) {
        newPoints.add(points.last())
    }
    while (newPoints.size > n) {
        newPoints.removeLast()
    }
    return newPoints
}

private fun normalize(points: List<Point2D>): List<Point2D> {
    // 1. Compute Centroid
    var cx = 0f
    var cy = 0f
    points.forEach {
        cx += it.x
        cy += it.y
    }
    cx /= points.size
    cy /= points.size
    
    // 2. Translate to Origin (0,0) and get bounding box
    val translated = points.map { Point2D(it.x - cx, it.y - cy) }
    
    var minX = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    
    translated.forEach {
        minX = min(minX, it.x)
        maxX = max(maxX, it.x)
        minY = min(minY, it.y)
        maxY = max(maxY, it.y)
    }
    
    val width = maxX - minX
    val height = maxY - minY
    val scale = max(width, height).coerceAtLeast(1f)
    
    // 3. Scale to fit 100x100 box
    return translated.map { Point2D(it.x * (100f / scale), it.y * (100f / scale)) }
}

private fun distance(p1: Point2D, p2: Point2D): Float {
    return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
}

private fun pathLength(points: List<Point2D>): Float {
    var length = 0f
    for (i in 1 until points.size) {
        length += distance(points[i - 1], points[i])
    }
    return length
}

private fun pathDistance(pts1: List<Point2D>, pts2: List<Point2D>): Float {
    var distance = 0f
    val limit = min(pts1.size, pts2.size)
    for (i in 0 until limit) {
        distance += distance(pts1[i], pts2[i])
    }
    return distance / limit
}

// ─── Gesture Templates for A-Z ──────────────────────────────────────────

private fun getPredefinedTemplates(): Map<String, List<Point2D>> {
    val rawTemplates = mapOf(
        "A" to listOf(Point2D(10f, 90f), Point2D(50f, 10f), Point2D(90f, 90f)),
        "B" to listOf(Point2D(20f, 10f), Point2D(20f, 90f), Point2D(50f, 90f), Point2D(50f, 50f), Point2D(20f, 50f), Point2D(60f, 50f), Point2D(60f, 10f), Point2D(20f, 10f)),
        "C" to listOf(Point2D(80f, 20f), Point2D(30f, 20f), Point2D(20f, 50f), Point2D(30f, 80f), Point2D(80f, 80f)),
        "D" to listOf(Point2D(20f, 10f), Point2D(20f, 90f), Point2D(60f, 90f), Point2D(80f, 50f), Point2D(60f, 10f), Point2D(20f, 10f)),
        "E" to listOf(Point2D(80f, 10f), Point2D(20f, 10f), Point2D(20f, 50f), Point2D(60f, 50f), Point2D(20f, 50f), Point2D(20f, 90f), Point2D(80f, 90f)),
        "F" to listOf(Point2D(80f, 10f), Point2D(20f, 10f), Point2D(20f, 50f), Point2D(60f, 50f), Point2D(20f, 50f), Point2D(20f, 90f)),
        "G" to listOf(Point2D(80f, 20f), Point2D(30f, 20f), Point2D(20f, 50f), Point2D(30f, 80f), Point2D(80f, 80f), Point2D(80f, 50f), Point2D(60f, 50f)),
        "H" to listOf(Point2D(20f, 10f), Point2D(20f, 90f), Point2D(20f, 50f), Point2D(80f, 50f), Point2D(80f, 10f), Point2D(80f, 90f)),
        "I" to listOf(Point2D(50f, 10f), Point2D(50f, 90f)),
        "J" to listOf(Point2D(80f, 10f), Point2D(80f, 80f), Point2D(50f, 90f), Point2D(20f, 70f)),
        "K" to listOf(Point2D(20f, 10f), Point2D(20f, 90f), Point2D(20f, 50f), Point2D(70f, 10f), Point2D(20f, 50f), Point2D(70f, 90f)),
        "L" to listOf(Point2D(20f, 10f), Point2D(20f, 90f), Point2D(80f, 90f)),
        "M" to listOf(Point2D(10f, 90f), Point2D(10f, 10f), Point2D(50f, 60f), Point2D(90f, 10f), Point2D(90f, 90f)),
        "N" to listOf(Point2D(10f, 90f), Point2D(10f, 10f), Point2D(90f, 90f), Point2D(90f, 10f)),
        "O" to listOf(Point2D(50f, 10f), Point2D(80f, 30f), Point2D(80f, 70f), Point2D(50f, 90f), Point2D(20f, 70f), Point2D(20f, 30f), Point2D(50f, 10f)),
        "P" to listOf(Point2D(20f, 90f), Point2D(20f, 10f), Point2D(60f, 10f), Point2D(60f, 50f), Point2D(20f, 50f)),
        "Q" to listOf(Point2D(50f, 10f), Point2D(80f, 30f), Point2D(80f, 70f), Point2D(50f, 90f), Point2D(20f, 70f), Point2D(20f, 30f), Point2D(50f, 10f), Point2D(60f, 70f), Point2D(90f, 95f)),
        "R" to listOf(Point2D(20f, 90f), Point2D(20f, 10f), Point2D(60f, 10f), Point2D(60f, 50f), Point2D(20f, 50f), Point2D(40f, 50f), Point2D(80f, 90f)),
        "S" to listOf(Point2D(80f, 20f), Point2D(30f, 20f), Point2D(20f, 40f), Point2D(50f, 60f), Point2D(80f, 70f), Point2D(70f, 90f), Point2D(20f, 90f)),
        "T" to listOf(Point2D(20f, 10f), Point2D(80f, 10f), Point2D(50f, 10f), Point2D(50f, 90f)),
        "U" to listOf(Point2D(20f, 10f), Point2D(20f, 70f), Point2D(50f, 90f), Point2D(80f, 70f), Point2D(80f, 10f)),
        "V" to listOf(Point2D(10f, 10f), Point2D(50f, 90f), Point2D(90f, 10f)),
        "W" to listOf(Point2D(10f, 10f), Point2D(30f, 90f), Point2D(50f, 50f), Point2D(70f, 90f), Point2D(90f, 10f)),
        "X" to listOf(Point2D(20f, 10f), Point2D(80f, 90f), Point2D(50f, 50f), Point2D(20f, 90f), Point2D(80f, 10f)),
        "Y" to listOf(Point2D(10f, 10f), Point2D(50f, 50f), Point2D(90f, 10f), Point2D(50f, 50f), Point2D(50f, 90f)),
        "Z" to listOf(Point2D(20f, 10f), Point2D(80f, 10f), Point2D(20f, 90f), Point2D(80f, 90f))
    )

    // Normalize and resample templates ahead of time
    return rawTemplates.mapValues { (_, pts) ->
        normalize(resample(pts, 32))
    }
}
