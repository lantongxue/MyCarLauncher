package com.sephp.mycarlauncher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sephp.mycarlauncher.ui.map.MapSection
import com.sephp.mycarlauncher.ui.music.MusicSection

/**
 * 主内容区域组件
 * 包含地图和音乐两个区域
 */
@Composable
fun ContentArea(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MapSection(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.7f)
        )
        MusicSection(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.3f)
        )
    }
}
