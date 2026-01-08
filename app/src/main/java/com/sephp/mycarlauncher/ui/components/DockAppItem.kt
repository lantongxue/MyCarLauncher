package com.sephp.mycarlauncher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.sephp.mycarlauncher.data.model.AppInfo

/**
 * Dock 栏应用项组件
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockAppItem(
    appInfo: AppInfo?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    iconColor: Color
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { if (appInfo == null) onLongClick() else onClick() },
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (appInfo != null) {
            appInfo.icon?.let { drawable ->
                Image(
                    painter = rememberDrawablePainter(drawable = drawable),
                    contentDescription = appInfo.label,
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
            Text(
                text = "+",
                color = iconColor.copy(alpha = 0.5f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
