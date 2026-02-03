package com.example.launcher.logic

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationHelper(context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    /**
     * Get current location. Returns null if location unavailable or permission denied.
     * Note: Caller must ensure location permission is granted before calling.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()
            
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                continuation.resume(location)
            }.addOnFailureListener {
                continuation.resume(null)
            }
            
            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }
    
    /**
     * Get last known location (faster but may be stale).
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }
}
