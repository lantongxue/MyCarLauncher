package com.sephp.mycarlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 音乐控制按钮组件
 */
@Composable
fun MusicControlButton(
    icon: ImageVector,
    contentDescription: String,
    isMain: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (isMain) 64.dp else 48.dp)
            .clip(CircleShape)
            .background(
                if (isMain) Color.Cyan.copy(alpha = 0.2f) 
                else Color.White.copy(alpha = 0.1f)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(if (isMain) 36.dp else 28.dp),
            tint = if (isMain) Color.Cyan else Color.White
        )
    }
}
