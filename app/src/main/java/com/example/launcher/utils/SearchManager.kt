package com.example.launcher.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import android.provider.MediaStore
import android.content.ContentUris
import android.net.Uri
import kotlinx.coroutines.*

/**
 * SearchManager - Advanced search with apps, contacts, and quick actions
 */
class SearchManager(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    data class SearchResult(
        val type: ResultType,
        val id: String,
        val title: String,
        val subtitle: String? = null,
        val iconUri: String? = null,
        val action: SearchAction
    )
    
    enum class ResultType {
        APP, CONTACT, SETTING, WEB_SEARCH, QUICK_ACTION, FILE
    }
    
    sealed class SearchAction {
        data class LaunchApp(val packageName: String) : SearchAction()
        data class CallContact(val phoneNumber: String) : SearchAction()
        data class MessageContact(val phoneNumber: String) : SearchAction()
        data class OpenContact(val contactId: String) : SearchAction()
        data class OpenSetting(val settingAction: String) : SearchAction()
        data class WebSearch(val query: String, val provider: String) : SearchAction()
        data class WebUrl(val url: String) : SearchAction()
        data class QuickDial(val number: String) : SearchAction()
        data class OpenFile(val uri: String, val mimeType: String) : SearchAction()
        data class CopyToClipboard(val text: String, val label: String = "Result") : SearchAction()
        object ToggleFlashlight : SearchAction()
    }
    
    // Search providers
    private val searchProviders = mutableListOf(
        SearchProvider("google", "Google", "https://www.google.com/search?q="),
        SearchProvider("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q="),
        SearchProvider("bing", "Bing", "https://www.bing.com/search?q="),
        SearchProvider("youtube", "YouTube", "https://www.youtube.com/results?search_query=")
    )
    
    data class SearchProvider(
        val id: String,
        val name: String,
        val urlTemplate: String
    )
    
    /**
     * Perform a search across all sources
     */
    suspend fun search(query: String, includeContacts: Boolean = true): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        
        val results = mutableListOf<SearchResult>()
        val queryLower = query.lowercase().trim()
        
        // Add direct URL option if it looks like one
        if (isUrl(queryLower)) {
            val url = if (queryLower.startsWith("http")) queryLower else "https://$queryLower"
            results.add(SearchResult(
                type = ResultType.WEB_SEARCH,
                id = "url_$queryLower",
                title = "Go to $queryLower",
                subtitle = "Open in browser",
                action = SearchAction.WebUrl(url)
            ))
        }

        // Search apps
        results.addAll(searchApps(queryLower))
        
        // Search contacts (if permitted)
        if (includeContacts && hasContactPermission()) {
            results.addAll(searchContacts(queryLower))
        }
        
        // Add quick actions
        results.addAll(getQuickActions(query))
        
        // Search files
        results.addAll(searchFiles(queryLower))
        
        // Add web search option
        results.add(SearchResult(
            type = ResultType.WEB_SEARCH,
            id = "web_$query",
            title = "Search \"$query\"",
            subtitle = "Web search",
            action = SearchAction.WebSearch(query, "google")
        ))
        
        return results.take(15) // Limit results
    }

    private fun isUrl(query: String): Boolean {
        // Simple URL detection: starts with http OR contains a dot without spaces
        return query.startsWith("http://") || 
               query.startsWith("https://") || 
               (query.contains(".") && !query.contains(" ") && query.length > 3 && query.substringAfterLast(".").length >= 2)
    }
    
    private fun searchApps(query: String): List<SearchResult> {
        val pm = context.packageManager
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
        mainIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        
        val apps = pm.queryIntentActivities(mainIntent, 0)
        
        return apps.filter { resolveInfo ->
            val label = resolveInfo.loadLabel(pm).toString().lowercase()
            val packageName = resolveInfo.activityInfo.packageName.lowercase()
            label.contains(query) || packageName.contains(query)
        }.take(8).map { resolveInfo ->
            val label = resolveInfo.loadLabel(pm).toString()
            val packageName = resolveInfo.activityInfo.packageName
            
            SearchResult(
                type = ResultType.APP,
                id = packageName,
                title = label,
                subtitle = packageName,
                action = SearchAction.LaunchApp(packageName)
            )
        }
    }
    
    private fun searchFiles(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        // Standard File Search (Documents, Downloads)
        try {
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATA
            )
            
            val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$query%")
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            
            val queryUri = MediaStore.Files.getContentUri("external")
            
            context.contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                
                while (cursor.moveToNext() && results.size < 8) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown File"
                    val mimeType = cursor.getString(mimeColumn) ?: "application/octet-stream"
                    val contentUri = ContentUris.withAppendedId(queryUri, id)
                    
                    results.add(SearchResult(
                        type = ResultType.FILE,
                        id = contentUri.toString(),
                        title = name,
                        subtitle = formatMimeType(mimeType),
                        action = SearchAction.OpenFile(contentUri.toString(), mimeType)
                    ))
                }
            }
        } catch (e: Exception) {
            // File search failed or permission rejected
        }
        
        return results
    }
    
    private fun formatMimeType(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "Image"
            mimeType.startsWith("video/") -> "Video"
            mimeType.startsWith("audio/") -> "Audio"
            mimeType == "application/pdf" -> "PDF Document"
            mimeType.contains("word") -> "Word Document"
            mimeType.contains("excel") || mimeType.contains("spreadsheet") -> "Spreadsheet"
            mimeType.contains("presentation") || mimeType.contains("powerpoint") -> "Presentation"
            mimeType.startsWith("text/") -> "Text File"
            else -> "File"
        }
    }
    
    private fun searchContacts(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        try {
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            
            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                
                val seenIds = mutableSetOf<String>()
                
                while (it.moveToNext() && results.size < 5) {
                    val contactId = it.getString(idIndex) ?: continue
                    if (contactId in seenIds) continue
                    seenIds.add(contactId)
                    
                    val name = it.getString(nameIndex) ?: continue
                    val number = it.getString(numberIndex) ?: ""
                    val photoUri = if (photoIndex >= 0) it.getString(photoIndex) else null
                    
                    // Add call option
                    if (number.isNotEmpty()) {
                        results.add(SearchResult(
                            type = ResultType.CONTACT,
                            id = "call_$contactId",
                            title = name,
                            subtitle = "Call $number",
                            iconUri = photoUri,
                            action = SearchAction.CallContact(number)
                        ))
                        
                        // Add message option
                        results.add(SearchResult(
                            type = ResultType.CONTACT,
                            id = "msg_$contactId",
                            title = name,
                            subtitle = "Message",
                            iconUri = photoUri,
                            action = SearchAction.MessageContact(number)
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            // Contact search failed
        }
        
        return results
    }
    
    private fun getQuickActions(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        // Phone number detection
        val digitsOnly = query.filter { it.isDigit() }
        if (digitsOnly.length >= 3) {
            results.add(SearchResult(
                type = ResultType.QUICK_ACTION,
                id = "dial_$digitsOnly",
                title = "Call $query",
                subtitle = "Quick dial",
                action = SearchAction.QuickDial(query)
            ))
        }
        
        // Settings shortcuts
        val settingsKeywords = mapOf(
            "wifi" to android.provider.Settings.ACTION_WIFI_SETTINGS,
            "bluetooth" to android.provider.Settings.ACTION_BLUETOOTH_SETTINGS,
            "battery" to android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS,
            "display" to android.provider.Settings.ACTION_DISPLAY_SETTINGS,
            "sound" to android.provider.Settings.ACTION_SOUND_SETTINGS,
            "apps" to android.provider.Settings.ACTION_APPLICATION_SETTINGS
        )
        
        settingsKeywords.forEach { (keyword, action) ->
            if (keyword.contains(query.lowercase())) {
                results.add(SearchResult(
                    type = ResultType.SETTING,
                    id = "setting_$keyword",
                    title = keyword.replaceFirstChar { it.uppercase() },
                    subtitle = "Settings",
                    action = SearchAction.OpenSetting(action)
                ))
            }
        }
        
        // Flashlight toggle
        val flashlightKeywords = listOf("flashlight", "torch", "light on", "light off")
        if (flashlightKeywords.any { query.lowercase().contains(it) }) {
            results.add(SearchResult(
                type = ResultType.QUICK_ACTION,
                id = "toggle_flashlight",
                title = "Toggle Flashlight",
                subtitle = "Quick action",
                action = SearchAction.ToggleFlashlight
            ))
        }
        
        // Math Evaluation
        val mathRegex = Regex("^[0-9\\+\\-\\*/\\s\\.\\(\\)]+$")
        if (query.isNotBlank() && mathRegex.matches(query) && query.any { it.isDigit() } && query.any { "+-*/".contains(it) }) {
            try {
                val result = evaluateMath(query)
                // Format to remove trailing .0 if present
                val formattedResult = if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
                
                results.add(SearchResult(
                    type = ResultType.QUICK_ACTION,
                    id = "math_result",
                    title = "= $formattedResult",
                    subtitle = "Tap to copy",
                    action = SearchAction.CopyToClipboard(formattedResult, "Calculation Result")
                ))
            } catch (e: Exception) {
                // Invalid mathematical expression
            }
        }
        
        return results
    }
    
    /**
     * Recursive descent parser for basic math expressions.
     * Supports +, -, *, /, (), standard precedence.
     */
    private fun evaluateMath(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code)) x /= parseFactor() // division
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Double
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }

                return x
            }
        }.parse()
    }
    
    private fun hasContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // ===== Search Providers Management =====
    
    fun getSearchProviders(): List<SearchProvider> = searchProviders.toList()
    
    fun addSearchProvider(id: String, name: String, urlTemplate: String) {
        searchProviders.add(SearchProvider(id, name, urlTemplate))
    }
    
    fun removeSearchProvider(id: String) {
        searchProviders.removeAll { it.id == id }
    }
    
    fun getSearchUrl(query: String, providerId: String = "google"): String {
        val provider = searchProviders.find { it.id == providerId } ?: searchProviders.first()
        return provider.urlTemplate + android.net.Uri.encode(query)
    }
    
    fun destroy() {
        scope.cancel()
    }
}
