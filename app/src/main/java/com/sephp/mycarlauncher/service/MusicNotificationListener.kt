package com.sephp.mycarlauncher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * 音乐通知监听服务
 * 用于获取媒体会话权限
 */
class MusicNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {}
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}
