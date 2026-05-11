package com.example.launcher.utils

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/**
 * ShizukuSetup — Lifecycle-aware Shizuku connection and permission manager.
 * 
 * Exposes a StateFlow<ShizukuState> that UI components can observe
 * to show connection status and enable/disable power features.
 */
object ShizukuSetup {

    private const val TAG = "ShizukuSetup"
    const val SHIZUKU_PERMISSION_REQUEST_CODE = 7749

    enum class ShizukuState {
        /** Shizuku app is not installed or service not started */
        UNAVAILABLE,
        /** Shizuku is running but we haven't been granted permission */
        AVAILABLE,
        /** Shizuku is running and we have shell-level permission */
        AUTHORIZED
    }

    private val _state = MutableStateFlow(ShizukuState.UNAVAILABLE)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private var listenerRegistered = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d(TAG, "Shizuku binder received")
        refreshState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.d(TAG, "Shizuku binder dead")
        _state.value = ShizukuState.UNAVAILABLE
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            Log.d(TAG, "Permission result: ${if (grantResult == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
            refreshState()
        }
    }

    /**
     * Call from Activity.onCreate() or Application.onCreate() to start listening.
     */
    fun init() {
        if (listenerRegistered) return
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            listenerRegistered = true
            refreshState()
            Log.d(TAG, "Shizuku listeners registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register Shizuku listeners", e)
            _state.value = ShizukuState.UNAVAILABLE
        }
    }

    /**
     * Call from Activity.onDestroy() if cleaning up.
     */
    fun destroy() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
            listenerRegistered = false
        } catch (e: Exception) {
            Log.e(TAG, "Error removing Shizuku listeners", e)
        }
    }

    /** Refresh the current state by checking binder and permission */
    fun refreshState() {
        _state.value = try {
            if (!Shizuku.pingBinder()) {
                ShizukuState.UNAVAILABLE
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                ShizukuState.AUTHORIZED
            } else {
                ShizukuState.AVAILABLE
            }
        } catch (e: Exception) {
            ShizukuState.UNAVAILABLE
        }
        Log.d(TAG, "State refreshed: ${_state.value}")
    }

    /** Request permission if Shizuku is available but not yet authorized */
    fun requestPermissionIfNeeded() {
        if (_state.value == ShizukuState.AVAILABLE) {
            ShizukuShell.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        }
    }
}
