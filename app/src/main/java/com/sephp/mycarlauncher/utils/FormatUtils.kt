package com.sephp.mycarlauncher.utils

import android.content.Context
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 格式化工具类
 */
object FormatUtils {
    
    /**
     * 格式化播放时长（毫秒转 mm:ss）
     */
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
    
    /**
     * 格式化距离（米转公里或米）
     */
    fun formatDistance(distanceInMeters: Int): String {
        return if (distanceInMeters >= 1000) {
            "%.1f公里".format(distanceInMeters / 1000.0)
        } else {
            "${distanceInMeters}米"
        }
    }
    
    /**
     * 格式化时间（秒转小时分钟）
     */
    fun formatTime(timeInSeconds: Int): String {
        val hours = timeInSeconds / 3600
        val minutes = (timeInSeconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}小时${minutes}分钟"
            else -> "${minutes}分钟"
        }
    }
    
    /**
     * 获取 Dock 栏显示的日期时间
     * @return Triple<时间, 星期, 日期>
     */
    fun getDockDateTime(): Triple<String, String, String> {
        val calendar = Calendar.getInstance()
        return Triple(
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time),
            SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        )
    }
    
    /**
     * 检查通知监听权限是否已启用
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }
}
