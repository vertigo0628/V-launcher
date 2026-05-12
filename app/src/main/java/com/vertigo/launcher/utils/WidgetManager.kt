package com.vertigo.launcher.utils

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent

/**
 * WidgetManager - Manages AppWidgetHost for the launcher
 * Handles adding, removing, and updating widgets
 */
class WidgetManager(private val context: Context) {
    
    companion object {
        private const val APPWIDGET_HOST_ID = 1024
        const val REQUEST_PICK_APPWIDGET = 9
        const val REQUEST_CREATE_APPWIDGET = 10
    }
    
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val appWidgetHost: AppWidgetHost = AppWidgetHost(context, APPWIDGET_HOST_ID)
    
    fun startListening() {
        appWidgetHost.startListening()
    }
    
    fun stopListening() {
        appWidgetHost.stopListening()
    }
    
    fun allocateAppWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }
    
    fun createWidgetView(appWidgetId: Int): android.appwidget.AppWidgetHostView? {
        val info = appWidgetManager.getAppWidgetInfo(appWidgetId) ?: return null
        return appWidgetHost.createView(context, appWidgetId, info) as android.appwidget.AppWidgetHostView
    }
    
    fun deleteAppWidgetId(appWidgetId: Int) {
        appWidgetHost.deleteAppWidgetId(appWidgetId)
    }
    
    fun getInstalledProviders(): List<AppWidgetProviderInfo> {
        return appWidgetManager.installedProviders
    }
    
    fun bindWidget(appWidgetId: Int, info: AppWidgetProviderInfo): Boolean {
        return appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)
    }
    
    fun getPickWidgetIntent(): Intent {
        val appWidgetId = allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        return pickIntent
    }
}
