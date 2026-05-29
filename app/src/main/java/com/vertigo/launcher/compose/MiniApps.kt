package com.vertigo.launcher.compose

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.vertigo.launcher.ui.HomeViewModel
import com.vertigo.launcher.utils.rememberBouncyOverscrollModifier

// ─── Data models ───────────────────────────────────────────────────────────────
data class TaskItem(val id: Long = System.currentTimeMillis(), val text: String, val done: Boolean = false, val priority: Int = 1) // 0=low, 1=medium, 2=high
data class NoteItem(val id: Long = System.currentTimeMillis(), val text: String, val timestamp: Long = System.currentTimeMillis(), val colorIdx: Int = 0, val pinned: Boolean = false)
// ─── Mini-Apps Hub Composable ─────────────────────────────────────────────────

@Composable
fun MiniAppsPanel(modifier: Modifier = Modifier, viewModel: HomeViewModel? = null) {
    val tabs = listOf("📅 Calendar", "📝 Notes", "✅ Tasks")
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x0DFFFFFF))
            .border(1.dp, Color(0x2200F0FF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "MINI APPS",
            color = Color(0xFF00F0FF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Tab row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val selected = index == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color(0xFF00F0FF) else Color(0x1AFFFFFF))
                        .clickable { selectedTab = index }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.Black else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> CalendarCard(
                events = viewModel?.todayEvents?.collectAsState()?.value ?: emptyList(),
                viewModel = viewModel
            )
            1 -> NotesCard()
            2 -> TasksCard()
        }
    }
}

// ─── Calendar Card ─────────────────────────────────────────────────────────────

@Composable
fun DidYouKnowSection(
    holiday: String?,
    insights: List<String>,
    cosmic: String?,
    viewModel: HomeViewModel? = null
) {
    var showSettings by remember { mutableStateOf(false) }
    val prefs by viewModel?.insightPrefs?.collectAsState() ?: mutableStateOf(emptyMap())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (showSettings) "INSIGHT SETTINGS" else "DID YOU KNOW?",
                color = Color(0xFF00F0FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            
            // Toggle Settings Icon
            Text(
                text = if (showSettings) "DONE" else "FILTER",
                color = Color(0xFF00F0FF),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x3300F0FF))
                    .clickable { showSettings = !showSettings }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (showSettings) {
            // Settings View
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                prefs.forEach { (category, enabled) ->
                    CategoryToggle(
                        label = category,
                        enabled = enabled,
                        onToggle = { viewModel?.toggleInsightPref(category) }
                    )
                }
            }
        } else {
            // Content View
            if (holiday != null) {
                Text(
                    text = "Today is $holiday",
                    color = Color(0xFFBD00FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (insights.isNotEmpty()) {
                insights.forEach { insight ->
                    BulletPoint(text = insight)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text("Scanning historical archives...", color = Color.Gray, fontSize = 10.sp)
            }
            
            if (cosmic != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    cosmic,
                    color = Color(0xFF00F0FF).copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CategoryToggle(label: String, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) Color(0x2200F0FF) else Color(0x11FFFFFF))
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            color = if (enabled) Color.White else Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (enabled) Color(0xFF00F0FF) else Color.DarkGray)
        )
    }
}

@Composable
fun BulletPoint(text: String) {
    Row {
        Text("• ", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold)
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}
@Composable
fun CalendarCard(events: List<com.vertigo.launcher.ui.HomeViewModel.CalendarEvent>, viewModel: com.vertigo.launcher.ui.HomeViewModel? = null) {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val month = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.time)

    // Build 7-day strip starting from 3 days before today
    val days = remember {
        val list = mutableListOf<Pair<Int, String>>() // day number, day abbrev
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_MONTH, -3)
        repeat(7) {
            list.add(c.get(Calendar.DAY_OF_MONTH) to SimpleDateFormat("EEE", Locale.getDefault()).format(c.time))
            c.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    Column {
        // Month header
        Text(text = month, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = dayOfWeek, color = Color.Gray, fontSize = 12.sp)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Did You Know? Section (Clean & Elegant)
        val holiday by viewModel?.currentHoliday?.collectAsState() ?: mutableStateOf(null)
        val dailyInsights by viewModel?.dailyInsight?.collectAsState() ?: mutableStateOf(emptyList())
        val cosmicInsight by viewModel?.cosmicInsight?.collectAsState() ?: mutableStateOf(null)

        DidYouKnowSection(
            holiday = holiday,
            insights = dailyInsights,
            cosmic = cosmicInsight,
            viewModel = viewModel
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // 7-day strip
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { (dayNum, dayAbbrev) ->
                val isToday = dayNum == today
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isToday) Color(0xFF00F0FF) else Color.Transparent)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(dayAbbrev, color = if (isToday) Color.Black else Color.Gray, fontSize = 9.sp)
                    Text("$dayNum", color = if (isToday) Color.Black else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color(0x2200F0FF))
        Spacer(modifier = Modifier.height(8.dp))

        if (events.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 150.dp)
                    .then(rememberBouncyOverscrollModifier())
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                events.forEach { event ->
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val startTime = timeFormat.format(Date(event.startTimeMs))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1AFFFFFF))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp, 24.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(event.color?.let { Color(it) } ?: Color(0xFF00F0FF))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = event.title, color = Color.White, fontSize = 13.sp, maxLines = 1)
                            Text(text = startTime, color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Notes Card ────────────────────────────────────────────────────────────────

private const val NOTES_KEY = "notes_json"

private fun loadNotes(prefs: SharedPreferences): MutableList<NoteItem> {
    val json = prefs.getString(NOTES_KEY, null) ?: return mutableListOf()
    return try {
        val type = object : TypeToken<MutableList<NoteItem>>() {}.type
        Gson().fromJson(json, type)
    } catch (e: Exception) { mutableListOf() }
}

private fun saveNotes(prefs: SharedPreferences, notes: List<NoteItem>) {
    prefs.edit().putString(NOTES_KEY, Gson().toJson(notes)).apply()
}

@Composable
fun NotesCard() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mini_apps_prefs", Context.MODE_PRIVATE) }
    var notes by remember { mutableStateOf(loadNotes(prefs)) }
    var newNoteText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableIntStateOf(0) }
    var editingNoteId by remember { mutableStateOf<Long?>(null) }
    var editText by remember { mutableStateOf("") }

    val noteColors = listOf(
        Color(0xFF00F0FF), // Cyan
        Color(0xFFBD00FF), // Purple
        Color(0xFFFF006E), // Pink
        Color(0xFF10B981), // Green
        Color(0xFFF59E0B)  // Amber
    )

    Column {
        // ── Color Picker Row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("COLOR", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.width(4.dp))
            noteColors.forEachIndexed { idx, color ->
                val sel = idx == selectedColor
                Box(
                    modifier = Modifier
                        .size(if (sel) 20.dp else 16.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = if (sel) 1f else 0.4f))
                        .then(
                            if (sel) Modifier.border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            else Modifier
                        )
                        .clickable { selectedColor = idx }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${notes.size} notes",
                color = Color.Gray.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Add Note Input ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, noteColors[selectedColor].copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = newNoteText,
                    onValueChange = { newNoteText = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    cursorBrush = SolidColor(noteColors[selectedColor]),
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newNoteText.isNotBlank()) {
                            val updated = notes.toMutableList().also {
                                it.add(0, NoteItem(text = newNoteText.trim(), colorIdx = selectedColor))
                            }
                            notes = updated
                            saveNotes(prefs, updated)
                            newNoteText = ""
                        }
                    }),
                    decorationBox = { inner ->
                        if (newNoteText.isEmpty()) Text("Write a note...", color = Color.Gray, fontSize = 13.sp)
                        inner()
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(noteColors[selectedColor])
                    .clickable {
                        if (newNoteText.isNotBlank()) {
                            val updated = notes.toMutableList().also {
                                it.add(0, NoteItem(text = newNoteText.trim(), colorIdx = selectedColor))
                            }
                            notes = updated
                            saveNotes(prefs, updated)
                            newNoteText = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Note List ──
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .then(rememberBouncyOverscrollModifier())
                .verticalScroll(rememberScrollState())
        ) {
            // Pinned first, then by timestamp
            val sortedNotes = remember(notes) {
                notes.sortedWith(compareByDescending<NoteItem> { it.pinned }.thenByDescending { it.timestamp })
            }

            sortedNotes.forEach { note ->
                val accent = noteColors.getOrElse(note.colorIdx) { noteColors[0] }
                val isEditing = editingNoteId == note.id

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x0DFFFFFF))
                        .then(
                            if (note.pinned) Modifier.border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            else Modifier
                        )
                ) {
                    // Left accent bar
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .defaultMinSize(minHeight = 48.dp)
                            .background(accent)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(12.dp)
                    ) {
                        if (isEditing) {
                            BasicTextField(
                                value = editText,
                                onValueChange = { editText = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                cursorBrush = SolidColor(accent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    val updated = notes.map {
                                        if (it.id == note.id) it.copy(text = editText.trim()) else it
                                    }.toMutableList()
                                    notes = updated
                                    saveNotes(prefs, updated)
                                    editingNoteId = null
                                }),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = note.text,
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.clickable {
                                    editingNoteId = note.id
                                    editText = note.text
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val timeAgo = remember(note.timestamp) {
                                val diff = System.currentTimeMillis() - note.timestamp
                                val mins = diff / 60000
                                val hours = mins / 60
                                val days = hours / 24
                                when {
                                    days > 0 -> "${days}d ago"
                                    hours > 0 -> "${hours}h ago"
                                    mins > 0 -> "${mins}m ago"
                                    else -> "Just now"
                                }
                            }
                            Text(
                                text = timeAgo,
                                color = Color.Gray.copy(alpha = 0.4f),
                                fontSize = 10.sp
                            )
                            if (note.pinned) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📌", fontSize = 10.sp)
                            }
                        }
                    }

                    // Actions column
                    Column(
                        modifier = Modifier.padding(top = 8.dp, end = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Pin/Unpin
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val updated = notes.map {
                                        if (it.id == note.id) it.copy(pinned = !it.pinned) else it
                                    }.toMutableList()
                                    notes = updated
                                    saveNotes(prefs, updated)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (note.pinned) "📌" else "📍",
                                fontSize = 12.sp
                            )
                        }
                        // Delete
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val updated = notes.filter { it.id != note.id }.toMutableList()
                                    notes = updated
                                    saveNotes(prefs, updated)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = Color(0x66FF4444), fontSize = 12.sp)
                        }
                    }
                }
            }

            if (notes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📝", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No notes yet. Start writing!",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ─── Tasks Card ────────────────────────────────────────────────────────────────

private const val TASKS_PREFS = "mini_apps_prefs"
private const val TASKS_KEY = "tasks_json"

private fun loadTasks(prefs: SharedPreferences): MutableList<TaskItem> {
    val json = prefs.getString(TASKS_KEY, null) ?: return mutableListOf()
    return try {
        val type = object : TypeToken<MutableList<TaskItem>>() {}.type
        Gson().fromJson(json, type)
    } catch (e: Exception) { mutableListOf() }
}

private fun saveTasks(prefs: SharedPreferences, tasks: List<TaskItem>) {
    prefs.edit().putString(TASKS_KEY, Gson().toJson(tasks)).apply()
}

@Composable
fun TasksCard() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(TASKS_PREFS, Context.MODE_PRIVATE) }
    var tasks by remember { mutableStateOf(loadTasks(prefs)) }
    var newTaskText by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableIntStateOf(1) }

    val priorityColors = listOf(
        Color(0xFF10B981), // Low — green
        Color(0xFFF59E0B), // Medium — amber
        Color(0xFFEF4444)  // High — red
    )
    val priorityLabels = listOf("LOW", "MED", "HIGH")

    val activeTasks = remember(tasks) { tasks.filter { !it.done } }
    val completedTasks = remember(tasks) { tasks.filter { it.done } }
    val progress = remember(tasks) {
        if (tasks.isEmpty()) 0f else completedTasks.size.toFloat() / tasks.size
    }

    Column {
        // ── Progress Header ──
        if (tasks.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${completedTasks.size}/${tasks.size} completed",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = Color(0xFF00F0FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF00F0FF),
                trackColor = Color(0x1AFFFFFF),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Priority Selector ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            priorityLabels.forEachIndexed { idx, label ->
                val sel = idx == selectedPriority
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) priorityColors[idx].copy(alpha = 0.25f) else Color(0x0DFFFFFF))
                        .border(
                            1.dp,
                            if (sel) priorityColors[idx] else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedPriority = idx }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (sel) priorityColors[idx] else Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Add Task Input ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, priorityColors[selectedPriority].copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    cursorBrush = SolidColor(Color(0xFF00F0FF)),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newTaskText.isNotBlank()) {
                            val updated = tasks.toMutableList().also {
                                it.add(TaskItem(text = newTaskText.trim(), priority = selectedPriority))
                            }
                            tasks = updated
                            saveTasks(prefs, updated)
                            newTaskText = ""
                        }
                    }),
                    decorationBox = { inner ->
                        if (newTaskText.isEmpty()) Text("Add a task...", color = Color.Gray, fontSize = 13.sp)
                        inner()
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(priorityColors[selectedPriority])
                    .clickable {
                        if (newTaskText.isNotBlank()) {
                            val updated = tasks.toMutableList().also {
                                it.add(TaskItem(text = newTaskText.trim(), priority = selectedPriority))
                            }
                            tasks = updated
                            saveTasks(prefs, updated)
                            newTaskText = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Task List ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .then(rememberBouncyOverscrollModifier())
                .verticalScroll(rememberScrollState())
        ) {
            // Active tasks
            if (activeTasks.isNotEmpty()) {
                activeTasks.sortedByDescending { it.priority }.forEach { task ->
                    val pColor = priorityColors.getOrElse(task.priority) { priorityColors[1] }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x0DFFFFFF))
                            .border(1.dp, pColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        // Priority dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(pColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Checkbox(
                            checked = false,
                            onCheckedChange = {
                                val updated = tasks.map {
                                    if (it.id == task.id) it.copy(done = true) else it
                                }.toMutableList()
                                tasks = updated
                                saveTasks(prefs, updated)
                            },
                            colors = CheckboxDefaults.colors(
                                uncheckedColor = pColor.copy(alpha = 0.6f),
                                checkmarkColor = Color.Black
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = task.text,
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val updated = tasks.filter { it.id != task.id }.toMutableList()
                                    tasks = updated
                                    saveTasks(prefs, updated)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = Color(0x66FF4444), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Completed section
            if (completedTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COMPLETED",
                        color = Color.Gray.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "CLEAR ALL",
                        color = Color(0xFF00F0FF).copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                val updated = tasks.filter { !it.done }.toMutableList()
                                tasks = updated
                                saveTasks(prefs, updated)
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                completedTasks.forEach { task ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x06FFFFFF))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = true,
                            onCheckedChange = {
                                val updated = tasks.map {
                                    if (it.id == task.id) it.copy(done = false) else it
                                }.toMutableList()
                                tasks = updated
                                saveTasks(prefs, updated)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF00F0FF).copy(alpha = 0.4f),
                                checkmarkColor = Color.Black
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = task.text,
                            color = Color.Gray.copy(alpha = 0.4f),
                            fontSize = 13.sp,
                            textDecoration = TextDecoration.LineThrough,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Empty state
            if (tasks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✅", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All clear! Add your first task above.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
