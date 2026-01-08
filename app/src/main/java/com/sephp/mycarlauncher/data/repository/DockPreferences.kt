package com.sephp.mycarlauncher.data.repository

import android.content.Context
import androidx.core.content.edit

/**
 * Dock 偏好设置管理
 */
object DockPreferences {
    private const val PREFS_NAME = "dock_prefs"
    private const val KEY_DOCK_APP = "dock_app_"
    
    fun saveDockApp(context: Context, index: Int, packageName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_DOCK_APP + index, packageName) }
    }
    
    fun getDockApp(context: Context, index: Int): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DOCK_APP + index, null)
    }
}
