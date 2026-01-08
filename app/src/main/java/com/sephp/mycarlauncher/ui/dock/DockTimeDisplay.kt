package com.sephp.mycarlauncher.ui.dock

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sephp.mycarlauncher.utils.FormatUtils
import kotlinx.coroutines.delay

/**
 * Dock 栏时间显示组件
 */
@Composable
fun DockTimeDisplay() {
    var dateTime by remember { mutableStateOf(FormatUtils.getDockDateTime()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            dateTime = FormatUtils.getDockDateTime()
            delay(1000)
        }
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = dateTime.first,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = dateTime.second,
            color = Color.White,
            fontSize = 14.sp
        )
        Text(
            text = dateTime.third,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
    }
}
