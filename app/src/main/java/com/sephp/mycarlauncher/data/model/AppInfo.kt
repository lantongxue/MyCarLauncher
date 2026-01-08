package com.sephp.mycarlauncher.data.model

import android.graphics.drawable.Drawable

/**
 * 应用信息数据类
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable? = null
)
