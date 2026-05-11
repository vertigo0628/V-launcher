package com.example.launcher.logic

import android.util.Log
import com.example.launcher.utils.ShizukuShell

/**
 * PermissionCommander — Shizuku-powered system control operations.
 * 
 * Grant/revoke permissions, enable/disable app components,
 * modify system settings — all without root.
 */
object PermissionCommander {

    private const val TAG = "PermissionCommander"

    // ─── Permission Management ───────────────────────────────────────────

    /** Grant a runtime permission to an app */
    suspend fun grantPermission(packageName: String, permission: String): ShizukuShell.CommandResult {
        Log.d(TAG, "Granting $permission to $packageName")
        return ShizukuShell.exec("pm grant $packageName $permission")
    }

    /** Revoke a runtime permission from an app */
    suspend fun revokePermission(packageName: String, permission: String): ShizukuShell.CommandResult {
        Log.d(TAG, "Revoking $permission from $packageName")
        return ShizukuShell.exec("pm revoke $packageName $permission")
    }

    /** Get all permissions for an app with their grant status */
    suspend fun getAppPermissions(packageName: String): List<PermissionInfo> {
        val result = ShizukuShell.exec("dumpsys package $packageName | grep -A1 'runtime permissions'")
        if (!result.isSuccess) return emptyList()

        val permissions = mutableListOf<PermissionInfo>()
        val grantedResult = ShizukuShell.exec("dumpsys package $packageName | grep 'android.permission'")
        
        grantedResult.stdout.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.contains("android.permission.")) {
                val perm = trimmed.substringBefore(":").trim()
                val granted = trimmed.contains("granted=true")
                if (perm.startsWith("android.permission.")) {
                    permissions.add(PermissionInfo(perm, granted))
                }
            }
        }
        return permissions
    }

    // ─── Component Control ───────────────────────────────────────────────

    /** Enable a specific component (activity, service, receiver) */
    suspend fun enableComponent(packageName: String, className: String): ShizukuShell.CommandResult {
        Log.d(TAG, "Enabling component: $packageName/$className")
        return ShizukuShell.exec("pm enable $packageName/$className")
    }

    /** Disable a specific component */
    suspend fun disableComponent(packageName: String, className: String): ShizukuShell.CommandResult {
        Log.d(TAG, "Disabling component: $packageName/$className")
        return ShizukuShell.exec("pm disable $packageName/$className")
    }

    /** Get all receivers for an app (useful for identifying bloatware trackers) */
    suspend fun getReceivers(packageName: String): List<String> {
        val result = ShizukuShell.exec("dumpsys package $packageName | grep -A1 'receiver'")
        if (!result.isSuccess) return emptyList()
        return result.stdout.lines()
            .filter { it.trim().contains("/") }
            .map { it.trim() }
    }

    // ─── System Settings ─────────────────────────────────────────────────

    /** Read a secure system setting */
    suspend fun getSecureSetting(key: String): String {
        val result = ShizukuShell.exec("settings get secure $key")
        return if (result.isSuccess) result.stdout.trim() else ""
    }

    /** Write a secure system setting */
    suspend fun putSecureSetting(key: String, value: String): ShizukuShell.CommandResult {
        Log.d(TAG, "Setting secure: $key = $value")
        return ShizukuShell.exec("settings put secure $key $value")
    }

    /** Read a global system setting */
    suspend fun getGlobalSetting(key: String): String {
        val result = ShizukuShell.exec("settings get global $key")
        return if (result.isSuccess) result.stdout.trim() else ""
    }

    /** Write a global system setting */
    suspend fun putGlobalSetting(key: String, value: String): ShizukuShell.CommandResult {
        Log.d(TAG, "Setting global: $key = $value")
        return ShizukuShell.exec("settings put global $key $value")
    }

    // ─── Default App Management ──────────────────────────────────────────

    /** Get current default launcher */
    suspend fun getDefaultLauncher(): String {
        val result = ShizukuShell.exec("cmd role get-role-holders android.app.role.HOME")
        return if (result.isSuccess) result.stdout.trim() else "Unknown"
    }

    // ─── Data Classes ────────────────────────────────────────────────────

    data class PermissionInfo(
        val permission: String,
        val isGranted: Boolean
    )
}
