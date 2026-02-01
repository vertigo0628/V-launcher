package com.example.launcher.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import java.io.File

/**
 * SystemMonitor - Fetches real-time system vitals
 */
class SystemMonitor(private val context: Context) {

    data class BatteryInfo(
        val level: Int,
        val isCharging: Boolean,
        val temperature: Float
    )

    data class RamInfo(
        val total: Long,
        val available: Long,
        val used: Long,
        val percentUsed: Int
    )

    data class StorageInfo(
        val total: Long,
        val available: Long,
        val percentUsed: Int
    )

    fun getBatteryInfo(): BatteryInfo {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        
        val level = batteryStatus?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            (level * 100 / scale.toFloat()).toInt()
        } ?: 0

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
                
        val temp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        
        return BatteryInfo(level, isCharging, temp / 10f) // Temp is in tenths of degree
    }

    fun getRamInfo(): RamInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val total = memoryInfo.totalMem
        val avail = memoryInfo.availMem
        val used = total - avail
        val percent = ((used.toDouble() / total.toDouble()) * 100).toInt()

        return RamInfo(total, avail, used, percent)
    }

    fun getStorageInfo(): StorageInfo {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val total = totalBlocks * blockSize
        val available = availableBlocks * blockSize
        val used = total - available
        val percent = ((used.toDouble() / total.toDouble()) * 100).toInt()

        return StorageInfo(total, available, percent)
    }
}
