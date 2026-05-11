package com.example.launcher.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * ShizukuShell — Unified command execution layer via Shizuku.
 * 
 * This gives V-Launcher ADB-level shell access without root,
 * enabling app freezing, force-stopping, permission management, etc.
 */
object ShizukuShell {

    private const val TAG = "ShizukuShell"

    data class CommandResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    ) {
        val isSuccess: Boolean get() = exitCode == 0
    }

    private fun newProcessCompat(command: Array<String>): ShizukuRemoteProcess {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(null, command, null, null) as ShizukuRemoteProcess
    }

    /** Check if Shizuku service is currently running and bound */
    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    /** Check if our app has been granted Shizuku permission */
    fun hasPermission(): Boolean {
        return try {
            if (!isAvailable()) return false
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    /** Request Shizuku permission (will show system dialog) */
    fun requestPermission(requestCode: Int) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request Shizuku permission", e)
        }
    }

    /**
     * Execute a shell command via Shizuku and return the result.
     * Must be called from a coroutine (runs on IO dispatcher).
     */
    suspend fun exec(command: String): CommandResult = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            Log.w(TAG, "exec() called without Shizuku permission")
            return@withContext CommandResult(-1, "", "Shizuku permission not granted")
        }

        try {
            Log.d(TAG, "Executing: $command")
            val process = newProcessCompat(arrayOf("sh", "-c", command))
            
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()

            Log.d(TAG, "Exit: $exitCode | stdout: ${stdout.take(200)} | stderr: ${stderr.take(200)}")
            CommandResult(exitCode, stdout.trim(), stderr.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Command failed: $command", e)
            CommandResult(-1, "", e.message ?: "Unknown error")
        }
    }

    /**
     * Fire-and-forget execution. Launches on IO but doesn't wait for result.
     */
    suspend fun execAsync(command: String) = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            Log.w(TAG, "execAsync() called without Shizuku permission")
            return@withContext
        }

        try {
            Log.d(TAG, "Async executing: $command")
            newProcessCompat(arrayOf("sh", "-c", command))
        } catch (e: Exception) {
            Log.e(TAG, "Async command failed: $command", e)
        }
    }
}
