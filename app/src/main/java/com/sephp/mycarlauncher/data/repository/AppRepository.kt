package com.sephp.mycarlauncher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.sephp.mycarlauncher.data.model.AppInfo

/**
 * 应用数据仓库
 */
object AppRepository {
    
    /**
     * 获取所有已安装的可启动应用列表
     */
    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { 
            addCategory(Intent.CATEGORY_LAUNCHER) 
        }
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).mapNotNull { resolveInfo ->
            try {
                val appInfo = resolveInfo.activityInfo.applicationInfo
                AppInfo(
                    label = appInfo.loadLabel(pm).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = appInfo.loadIcon(pm)
                )
            } catch (e: Exception) { 
                null 
            }
        }.sortedBy { it.label.lowercase() }
    }
    
    /**
     * 启动指定包名的应用
     */
    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            intent?.let { 
                context.startActivity(it) 
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
