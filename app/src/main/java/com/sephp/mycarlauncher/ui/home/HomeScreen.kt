package com.sephp.mycarlauncher.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sephp.mycarlauncher.data.repository.DockPreferences
import com.sephp.mycarlauncher.ui.components.WallpaperBackground
import com.sephp.mycarlauncher.ui.dialogs.AppListOverlay
import com.sephp.mycarlauncher.ui.dialogs.AppSelectorDialog
import com.sephp.mycarlauncher.ui.dock.DockBar
import com.sephp.mycarlauncher.ui.theme.MyCarLauncherTheme

/**
 * 主界面组件
 */
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var showAppList by remember { mutableStateOf(false) }
    var showAppSelector by remember { mutableStateOf(false) }
    var selectedDockIndex by remember { mutableStateOf<Int?>(null) }
    var dockUpdateTrigger by remember { mutableIntStateOf(0) }
    
    // 壁纸URL状态，每次重组时生成新的时间戳以获取新壁纸
    val wallpaperUrl = remember {
        "https://bing.img.run/rand_m.php?t=${System.currentTimeMillis()}"
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 壁纸背景层（带高斯模糊）
        WallpaperBackground(
            imageUrl = wallpaperUrl,
            modifier = Modifier.fillMaxSize()
        )
        Row(modifier = Modifier.fillMaxSize()) {
            DockBar(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight(),
                onShowAppList = { showAppList = true },
                onDockAppLongPress = { index ->
                    selectedDockIndex = index
                    showAppSelector = true
                },
                context = context,
                updateTrigger = dockUpdateTrigger
            )
            
            ContentArea(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }
        
        if (showAppList) {
            AppListOverlay(onDismiss = { showAppList = false })
        }
        
        if (showAppSelector && selectedDockIndex != null) {
            AppSelectorDialog(
                onDismiss = { showAppSelector = false },
                onAppSelected = { appInfo ->
                    DockPreferences.saveDockApp(context, selectedDockIndex!!, appInfo.packageName)
                    dockUpdateTrigger++
                    showAppSelector = false
                },
                context = context
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
fun HomeScreenPreview() {
    MyCarLauncherTheme { HomeScreen() }
}
