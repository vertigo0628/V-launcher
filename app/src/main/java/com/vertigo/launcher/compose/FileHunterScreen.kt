package com.vertigo.launcher.compose

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.vertigo.launcher.logic.filehunter.FileModel
import com.vertigo.launcher.logic.filehunter.FileHunterShizukuHelper
import com.vertigo.launcher.logic.filehunter.StorageManagerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ScreenState(val path: String, val files: List<FileModel>)

// ── Expert Mode Color System ─────────────────────────────────────────────
// These colors help the user instantly understand what they're looking at
val VaultIconColor = Color(0xFFFF9800)     // Orange — vault/hidden folder
val PhotoIconColor = Color(0xFFE91E63)     // Pink — disguised photo
val GhostIconColor = Color(0xFFAAAAAA)     // Gray — hidden dot-file
val SystemIconColor = Color(0xFF607D8B)    // Blue-gray — normal system file
val SafeDeleteColor = Color(0xFF4CAF50)    // Green — safe to delete (cache/tmp)
val CautionColor = Color(0xFFFFC107)       // Amber — proceed with caution
val DangerColor = Color(0xFFF44336)        // Red — critical system file

val LighterRed = Color(0xFFFFCCCC)

// Known patterns that are safe to delete
private val SAFE_DELETE_PATTERNS = listOf("cache", "Cache", "tmp", "temp", "log", ".thumbnails", ".trash")
private val CAUTION_PATTERNS = listOf("files", "databases", "shared_prefs")

private fun getDeleteSafety(file: FileModel): String {
    val name = file.name.lowercase()
    return when {
        SAFE_DELETE_PATTERNS.any { name.contains(it.lowercase()) } -> "SAFE"
        file.isVault -> "SAFE"
        file.path.contains("/Android/data") && file.isDirectory && CAUTION_PATTERNS.any { name == it } -> "CAUTION"
        file.path.contains("/Android/data") && !file.isDirectory -> "SAFE"
        file.name.startsWith(".") && !file.isDirectory -> "SAFE"
        else -> "UNKNOWN"
    }
}

enum class ClipboardAction { COPY, MOVE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileHunterScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigation state
    var currentPath by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<FileModel>>(emptyList()) }
    var history by remember { mutableStateOf<List<ScreenState>>(emptyList()) }

    // Feature toggles
    var showHiddenMedia by remember { mutableStateOf(false) }
    var isShizukuPermitted by remember { mutableStateOf(FileHunterShizukuHelper.checkPermission()) }
    var isLoading by remember { mutableStateOf(false) }

    // Single-file bottom sheet
    var selectedFile by remember { mutableStateOf<FileModel?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    // Multi-select state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedFiles by remember { mutableStateOf<Set<FileModel>>(emptySet()) }

    // Clipboard state for copy/move
    var clipboardFiles by remember { mutableStateOf<List<FileModel>>(emptyList()) }
    var clipboardAction by remember { mutableStateOf<ClipboardAction?>(null) }

    // Delete confirmation dialog
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var filesToDelete by remember { mutableStateOf<List<FileModel>>(emptyList()) }

    LaunchedEffect(Unit) {
        val storageVolumes = StorageManagerHelper.getStorageVolumes(context)
        files = storageVolumes.map {
            FileModel(
                name = it.title,
                path = it.path,
                isDirectory = true,
                size = 0,
                isVault = false,
                isPhoto = false,
                isHidden = false
            )
        }
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedFiles = emptySet()
    }

    fun loadPath(path: String) {
        coroutineScope.launch {
            isLoading = true
            exitSelectionMode()
            val isRestricted = path.contains("/Android/data")
            val loadedFiles = withContext(Dispatchers.IO) {
                FileHunterShizukuHelper.listFiles(context, path, isRestricted && isShizukuPermitted)
            }
            if (currentPath != path && currentPath.isNotEmpty()) {
                history = history + ScreenState(currentPath, files)
            }
            files = loadedFiles
            currentPath = path
            isLoading = false
        }
    }

    fun reloadCurrentPath() {
        if (currentPath.isNotEmpty() && currentPath != "Deep Scan Results") {
            val path = currentPath
            coroutineScope.launch {
                isLoading = true
                val isRestricted = path.contains("/Android/data")
                val loadedFiles = withContext(Dispatchers.IO) {
                    FileHunterShizukuHelper.listFiles(context, path, isRestricted && isShizukuPermitted)
                }
                files = loadedFiles
                isLoading = false
            }
        }
    }

    fun runDeepScan() {
        if (!isShizukuPermitted) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Shizuku is required for Deep Scan") }
            return
        }
        coroutineScope.launch {
            isLoading = true
            exitSelectionMode()
            snackbarHostState.showSnackbar("Scanning for vaults...", duration = SnackbarDuration.Short)
            val vaults = withContext(Dispatchers.IO) {
                FileHunterShizukuHelper.findVaults(context, "/storage/emulated/0")
            }
            if (currentPath.isNotEmpty()) {
                history = history + ScreenState(currentPath, files)
            }
            files = vaults
            currentPath = "Deep Scan Results"
            isLoading = false
        }
    }

    fun prepareFileUri(file: FileModel): Uri? {
        val isRestricted = !file.path.startsWith("/storage/emulated/0") ||
                file.path.contains("/Android/data") ||
                file.path.contains("/Android/obb")
        
        val fileToShare = if (isRestricted) {
            FileHunterShizukuHelper.copyFileToCache(file.path, context)
        } else {
            File(file.path)
        }

        return if (fileToShare != null && fileToShare.exists()) {
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, fileToShare)
        } else null
    }

    fun openFile(file: FileModel) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Opening ${file.name}...", duration = SnackbarDuration.Short)
            val uri = withContext(Dispatchers.IO) { prepareFileUri(file) }
            if (uri != null) {
                try {
                    val extension = file.name.substringAfterLast('.', "").lowercase()
                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: when (extension) {
                        "pdf" -> "application/pdf"
                        "epub" -> "application/epub+zip"
                        "txt" -> "text/plain"
                        "doc", "docx" -> "application/msword"
                        "xls", "xlsx" -> "application/vnd.ms-excel"
                        "ppt", "pptx" -> "application/vnd.ms-powerpoint"
                        "zip" -> "application/zip"
                        "rar" -> "application/x-rar-compressed"
                        "apk" -> "application/vnd.android.package-archive"
                        "mp3" -> "audio/mpeg"
                        "wav" -> "audio/wav"
                        "ogg" -> "audio/ogg"
                        "m4a" -> "audio/x-m4a"
                        "flac" -> "audio/flac"
                        "mp4" -> "video/mp4"
                        "mkv" -> "video/x-matroska"
                        "webm" -> "video/webm"
                        "avi" -> "video/x-msvideo"
                        "jpg", "jpeg" -> "image/jpeg"
                        "png" -> "image/png"
                        "webp" -> "image/webp"
                        "gif" -> "image/gif"
                        else -> "*/*"
                    }
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error: No app found to open this file")
                }
            } else {
                snackbarHostState.showSnackbar("Failed to prepare file for opening")
            }
            showBottomSheet = false
        }
    }

    fun shareFiles(filesToShare: List<FileModel>) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Preparing ${filesToShare.size} file(s)...", duration = SnackbarDuration.Short)
            val uris = withContext(Dispatchers.IO) {
                filesToShare.filter { !it.isDirectory }.mapNotNull { prepareFileUri(it) }
            }
            if (uris.isNotEmpty()) {
                val intent = if (uris.size == 1) {
                    Intent(Intent.ACTION_SEND).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_STREAM, uris.first())
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "*/*"
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                context.startActivity(Intent.createChooser(intent, "Share files"))
            } else {
                snackbarHostState.showSnackbar("No files to share")
            }
            exitSelectionMode()
            showBottomSheet = false
        }
    }

    fun performDelete(toDelete: List<FileModel>) {
        coroutineScope.launch {
            isLoading = true
            var successCount = 0
            withContext(Dispatchers.IO) {
                for (file in toDelete) {
                    if (FileHunterShizukuHelper.deleteFile(file.path, isShizukuPermitted)) {
                        successCount++
                    }
                }
            }
            snackbarHostState.showSnackbar("Deleted $successCount/${toDelete.size} items")
            exitSelectionMode()
            reloadCurrentPath()
            isLoading = false
            showBottomSheet = false
        }
    }

    fun performPaste() {
        if (clipboardFiles.isEmpty() || clipboardAction == null || currentPath.isEmpty()) return
        coroutineScope.launch {
            isLoading = true
            val action = clipboardAction!!
            val destPath = currentPath
            var successCount = 0
            withContext(Dispatchers.IO) {
                for (file in clipboardFiles) {
                    val ok = when (action) {
                        ClipboardAction.COPY -> FileHunterShizukuHelper.copyFileTo(file.path, destPath, isShizukuPermitted)
                        ClipboardAction.MOVE -> FileHunterShizukuHelper.moveFileTo(file.path, destPath, isShizukuPermitted)
                    }
                    if (ok) successCount++
                }
            }
            val verb = if (action == ClipboardAction.COPY) "Copied" else "Moved"
            snackbarHostState.showSnackbar("$verb $successCount/${clipboardFiles.size} items")
            clipboardFiles = emptyList()
            clipboardAction = null
            reloadCurrentPath()
            isLoading = false
        }
    }

    fun goBack() {
        if (isSelectionMode) {
            exitSelectionMode()
        } else if (history.isNotEmpty()) {
            val prevState = history.last()
            history = history.dropLast(1)
            currentPath = prevState.path
            files = prevState.files
        } else {
            onClose()
        }
    }

    BackHandler { goBack() }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // ── Selection Mode Top Bar ───────────────────────────────────
                TopAppBar(
                    title = { Text("${selectedFiles.size} selected", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { exitSelectionMode() }) {
                            Icon(Icons.Default.Close, "Cancel selection")
                        }
                    },
                    actions = {
                        // Select All
                        val visibleFiles = if (showHiddenMedia) files else files.filter { !it.isHidden }
                        IconButton(onClick = {
                            selectedFiles = if (selectedFiles.size == visibleFiles.size) emptySet() else visibleFiles.toSet()
                        }) {
                            Icon(Icons.Default.SelectAll, "Select All")
                        }
                        // Share (files only)
                        IconButton(onClick = { shareFiles(selectedFiles.toList()) }, enabled = selectedFiles.any { !it.isDirectory }) {
                            Icon(Icons.Default.Share, "Share")
                        }
                        // Copy
                        IconButton(onClick = {
                            clipboardFiles = selectedFiles.toList()
                            clipboardAction = ClipboardAction.COPY
                            exitSelectionMode()
                            coroutineScope.launch { snackbarHostState.showSnackbar("${clipboardFiles.size} item(s) copied — navigate and paste") }
                        }) {
                            Icon(Icons.Default.ContentCopy, "Copy")
                        }
                        // Move
                        IconButton(onClick = {
                            clipboardFiles = selectedFiles.toList()
                            clipboardAction = ClipboardAction.MOVE
                            exitSelectionMode()
                            coroutineScope.launch { snackbarHostState.showSnackbar("${clipboardFiles.size} item(s) cut — navigate and paste") }
                        }) {
                            Icon(Icons.Default.DriveFileMove, "Move")
                        }
                        // Delete
                        IconButton(onClick = {
                            filesToDelete = selectedFiles.toList()
                            showDeleteConfirm = true
                        }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                        }
                    }
                )
            } else {
                // ── Normal Top Bar ───────────────────────────────────────────
                TopAppBar(
                    title = { Text("File Hunter", fontWeight = FontWeight.Bold) },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            TextButton(onClick = { runDeepScan() }) {
                                Text("Deep Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Hunter", fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
                            Switch(checked = showHiddenMedia, onCheckedChange = { showHiddenMedia = it })
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Show Paste FAB when clipboard has content
            if (clipboardFiles.isNotEmpty() && currentPath.isNotEmpty() && currentPath != "Deep Scan Results") {
                val label = if (clipboardAction == ClipboardAction.COPY) "Paste Here" else "Move Here"
                ExtendedFloatingActionButton(
                    onClick = { performPaste() },
                    icon = { Icon(Icons.Default.ContentPaste, label) },
                    text = { Text("$label (${clipboardFiles.size})") },
                    containerColor = if (clipboardAction == ClipboardAction.COPY) Color(0xFF1E88E5) else Color(0xFFFF9800)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // ── Shizuku Status Banner ────────────────────────────────────
            if (!isShizukuPermitted) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Shizuku not active — restricted paths unavailable", fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (FileHunterShizukuHelper.isShizukuAvailable()) {
                                    FileHunterShizukuHelper.requestPermission(101)
                                    isShizukuPermitted = FileHunterShizukuHelper.checkPermission()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Check Permission", fontSize = 11.sp)
                        }
                    }
                }
            }

            // ── Clipboard Banner ─────────────────────────────────────────
            if (clipboardFiles.isNotEmpty()) {
                Surface(
                    color = if (clipboardAction == ClipboardAction.COPY) Color(0xFF1E88E5).copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val verb = if (clipboardAction == ClipboardAction.COPY) "📋 Copied" else "✂️ Cut"
                        Text("$verb ${clipboardFiles.size} item(s)", fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { clipboardFiles = emptyList(); clipboardAction = null }) {
                            Text("Cancel", fontSize = 11.sp, color = Color.Red)
                        }
                    }
                }
            }

            // ── Breadcrumb / Back Bar ────────────────────────────────────
            if (currentPath.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { goBack() }
                ) {
                    Text(
                        text = "← $currentPath",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 13.sp
                    )
                }
            }

            // ── Loading ──────────────────────────────────────────────────
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // ── File List ────────────────────────────────────────────────
            val visibleFiles = if (showHiddenMedia) files else files.filter { !it.isHidden }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(visibleFiles, key = { it.path }) { file ->
                    val isSelected = selectedFiles.contains(file)
                    FileListItem(
                        file = file,
                        showHiddenMedia = showHiddenMedia,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                selectedFiles = if (isSelected) selectedFiles - file else selectedFiles + file
                                if (selectedFiles.isEmpty()) exitSelectionMode()
                            } else if (file.isDirectory) {
                                loadPath(file.path)
                            } else {
                                openFile(file)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedFiles = setOf(file)
                            } else {
                                // In selection mode, long press opens single-file menu
                                selectedFile = file
                                showBottomSheet = true
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }
            }
        }
    }

    // ── Single-File Bottom Sheet ─────────────────────────────────────────
    if (showBottomSheet && selectedFile != null) {
        val file = selectedFile!!
        val safety = getDeleteSafety(file)
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(file.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                
                // Expert mode safety indicator
                val safetyText = when (safety) {
                    "SAFE" -> "🟢 Safe to delete (cache/temp/hidden)"
                    "CAUTION" -> "🟡 Caution — may contain app settings"
                    else -> "⚪ Unknown — check before deleting"
                }
                Text(safetyText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                
                Spacer(modifier = Modifier.height(16.dp))

                if (!file.isDirectory) {
                    ListItem(
                        headlineContent = { Text("Open") },
                        leadingContent = { Icon(Icons.Default.OpenInNew, null) },
                        modifier = Modifier.combinedClickable(onClick = { openFile(file) })
                    )
                    ListItem(
                        headlineContent = { Text("Share") },
                        leadingContent = { Icon(Icons.Default.Share, null) },
                        modifier = Modifier.combinedClickable(onClick = { shareFiles(listOf(file)) })
                    )
                } else {
                    ListItem(
                        headlineContent = { Text("Open Folder") },
                        leadingContent = { Icon(Icons.Default.Folder, null) },
                        modifier = Modifier.combinedClickable(onClick = {
                            showBottomSheet = false
                            loadPath(file.path)
                        })
                    )
                }

                ListItem(
                    headlineContent = { Text("Copy") },
                    leadingContent = { Icon(Icons.Default.ContentCopy, null) },
                    modifier = Modifier.combinedClickable(onClick = {
                        clipboardFiles = listOf(file)
                        clipboardAction = ClipboardAction.COPY
                        showBottomSheet = false
                        coroutineScope.launch { snackbarHostState.showSnackbar("Copied — navigate and paste") }
                    })
                )
                ListItem(
                    headlineContent = { Text("Move") },
                    leadingContent = { Icon(Icons.Default.DriveFileMove, null) },
                    modifier = Modifier.combinedClickable(onClick = {
                        clipboardFiles = listOf(file)
                        clipboardAction = ClipboardAction.MOVE
                        showBottomSheet = false
                        coroutineScope.launch { snackbarHostState.showSnackbar("Cut — navigate and paste") }
                    })
                )

                ListItem(
                    headlineContent = { Text("Delete", color = Color.Red) },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                    modifier = Modifier.combinedClickable(onClick = {
                        showBottomSheet = false
                        filesToDelete = listOf(file)
                        showDeleteConfirm = true
                    })
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ── Delete Confirmation Dialog ───────────────────────────────────────
    if (showDeleteConfirm && filesToDelete.isNotEmpty()) {
        val safeCount = filesToDelete.count { getDeleteSafety(it) == "SAFE" }
        val cautionCount = filesToDelete.count { getDeleteSafety(it) == "CAUTION" }
        val unknownCount = filesToDelete.count { getDeleteSafety(it) == "UNKNOWN" }

        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; filesToDelete = emptyList() },
            title = { Text("⚠️ Delete ${filesToDelete.size} item(s)?") },
            text = {
                Column {
                    if (filesToDelete.size == 1) {
                        Text(filesToDelete.first().name, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (safeCount > 0) Text("🟢 $safeCount safe to delete", fontSize = 13.sp)
                    if (cautionCount > 0) Text("🟡 $cautionCount may affect app settings", fontSize = 13.sp, color = CautionColor)
                    if (unknownCount > 0) Text("⚪ $unknownCount unknown — check first", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This action cannot be undone.", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        performDelete(filesToDelete)
                        filesToDelete = emptyList()
                    }
                ) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false; filesToDelete = emptyList() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── File List Item Composable ────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    file: FileModel,
    showHiddenMedia: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val safety = getDeleteSafety(file)

    // Background color logic: selection > vault highlight > safety hint
    val targetBgColor = when {
        isSelected -> Color(0xFF1E88E5).copy(alpha = 0.2f)
        showHiddenMedia && file.isVault -> LighterRed.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val backgroundColor by animateColorAsState(targetBgColor, label = "bg")

    val isMedia = file.name.let { n ->
        n.endsWith(".mp4", true) || n.endsWith(".mkv", true) || n.endsWith(".avi", true) ||
        n.endsWith(".jpg", true) || n.endsWith(".jpeg", true) || n.endsWith(".png", true) ||
        n.endsWith(".webp", true) || n.endsWith(".gif", true) ||
        n.endsWith(".mp3", true) || n.endsWith(".flac", true) || n.endsWith(".wav", true) ||
        n.endsWith(".aac", true) || n.endsWith(".ogg", true)
    }
    val isVideo = file.name.let { n ->
        n.endsWith(".mp4", true) || n.endsWith(".mkv", true) || n.endsWith(".avi", true)
    }
    val isImage = file.name.let { n ->
        n.endsWith(".jpg", true) || n.endsWith(".jpeg", true) || n.endsWith(".png", true) ||
        n.endsWith(".webp", true) || n.endsWith(".gif", true)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Selection Checkbox ───────────────────────────────────
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFF1E88E5) else Color.Gray,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                )
            }

            // ── Thumbnail / Icon ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.DarkGray.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val isRestricted = !file.path.startsWith("/storage/emulated/0") ||
                        file.path.contains("/Android/data") ||
                        file.path.contains("/Android/obb")
                var localFile by remember(file.path) { mutableStateOf<File?>(if (isRestricted) null else File(file.path)) }
                val ctx = LocalContext.current

                if (!file.isDirectory && (isImage || isVideo) && file.size < 50_000_000L) {
                    if (isRestricted && localFile == null) {
                        LaunchedEffect(file.path) {
                            withContext(Dispatchers.IO) {
                                val cacheDir = ctx.externalCacheDir ?: ctx.cacheDir
                                val thumbCache = File(cacheDir, "temp_" + file.name)
                                if (thumbCache.exists() && thumbCache.length() > 0L) {
                                    localFile = thumbCache
                                } else {
                                    localFile = FileHunterShizukuHelper.copyFileToCache(file.path, ctx)
                                }
                            }
                        }
                    }

                    if (localFile != null) {
                        val request = ImageRequest.Builder(LocalContext.current)
                            .data(localFile)
                            .crossfade(true)
                            .size(128)
                        if (isVideo) {
                            request.videoFrameMillis(1000)
                        }
                        AsyncImage(
                            model = request.build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
                        )
                    } else {
                        // Fallback icon while loading
                        val icon = if (isVideo) Icons.Default.PlayArrow else Icons.Default.Image
                        Icon(imageVector = icon, contentDescription = null, tint = PhotoIconColor, modifier = Modifier.size(28.dp))
                    }
                } else {
                    val icon = when {
                        file.isVault && file.isDirectory -> Icons.Default.Lock
                        file.isPhoto                     -> Icons.Default.Image
                        file.isDirectory                 -> Icons.Default.Folder
                        else                             -> Icons.Default.InsertDriveFile
                    }
                    val iconTint = when {
                        file.isVault && file.isDirectory -> VaultIconColor
                        file.isPhoto                     -> PhotoIconColor
                        file.isHidden                    -> GhostIconColor
                        file.isDirectory                 -> SystemIconColor
                        else                             -> SystemIconColor
                    }
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ── File Info ────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.label ?: file.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (file.label != null) file.name else file.path,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ── Safety Indicator Dot (Expert Mode) ───────────────────
            if (showHiddenMedia && !file.isDirectory) {
                val dotColor = when (safety) {
                    "SAFE" -> SafeDeleteColor
                    "CAUTION" -> CautionColor
                    else -> Color.Gray.copy(alpha = 0.3f)
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = file.displaySize,
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
