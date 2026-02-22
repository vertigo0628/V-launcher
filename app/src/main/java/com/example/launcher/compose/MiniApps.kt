package com.example.launcher.compose

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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

// ─── Data models ───────────────────────────────────────────────────────────────

data class TaskItem(val id: Long = System.currentTimeMillis(), val text: String, val done: Boolean = false)

// ─── Mini-Apps Hub Composable ─────────────────────────────────────────────────

@Composable
fun MiniAppsPanel(modifier: Modifier = Modifier) {
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
            0 -> CalendarCard()
            1 -> NotesCard()
            2 -> TasksCard()
        }
    }
}

// ─── Calendar Card ─────────────────────────────────────────────────────────────

@Composable
fun CalendarCard() {
    val cal = remember { Calendar.getInstance() }
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

        Text(
            text = "No events today",
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

// ─── Notes Card ────────────────────────────────────────────────────────────────

@Composable
fun NotesCard() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mini_apps_prefs", Context.MODE_PRIVATE) }
    var noteText by remember { mutableStateOf(prefs.getString("note_content", "") ?: "") }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x1AFFFFFF))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            BasicTextField(
                value = noteText,
                onValueChange = { noteText = it },
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                cursorBrush = SolidColor(Color(0xFF00F0FF)),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { inner ->
                    if (noteText.isEmpty()) {
                        Text("Tap to write a note...", color = Color.Gray, fontSize = 13.sp)
                    }
                    inner()
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { prefs.edit().putString("note_content", noteText).apply() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) { Text("Save", color = Color.Black, fontSize = 11.sp) }

            OutlinedButton(
                onClick = {
                    noteText = ""
                    prefs.edit().remove("note_content").apply()
                },
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) { Text("Clear", color = Color.Gray, fontSize = 11.sp) }
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

    Column {
        // Add new task input
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                BasicTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    cursorBrush = SolidColor(Color(0xFF00F0FF)),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (newTaskText.isEmpty()) Text("Add a task...", color = Color.Gray, fontSize = 13.sp)
                        inner()
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00F0FF))
                    .clickable {
                        if (newTaskText.isNotBlank()) {
                            val updated = tasks.toMutableList().also { it.add(TaskItem(text = newTaskText.trim())) }
                            tasks = updated
                            saveTasks(prefs, updated)
                            newTaskText = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Task list
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tasks.forEach { task ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x0DFFFFFF))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Checkbox(
                        checked = task.done,
                        onCheckedChange = { checked ->
                            val updated = tasks.map {
                                if (it.id == task.id) it.copy(done = checked) else it
                            }.toMutableList()
                            tasks = updated
                            saveTasks(prefs, updated)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF00F0FF),
                            uncheckedColor = Color.Gray,
                            checkmarkColor = Color.Black
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.text,
                        color = if (task.done) Color.Gray else Color.White,
                        fontSize = 13.sp,
                        textDecoration = if (task.done) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f)
                    )
                    // Delete button
                    Text(
                        text = "✕",
                        color = Color(0x66FF4444),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable {
                                val updated = tasks.filter { it.id != task.id }.toMutableList()
                                tasks = updated
                                saveTasks(prefs, updated)
                            }
                    )
                }
            }

            if (tasks.isEmpty()) {
                Text(
                    text = "No tasks yet. Add one above!",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                )
            }
        }
    }
}
