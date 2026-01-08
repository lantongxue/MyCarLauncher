package com.sephp.mycarlauncher.ui.dock

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sephp.mycarlauncher.R
import com.sephp.mycarlauncher.data.model.AppInfo
import com.sephp.mycarlauncher.data.repository.AppRepository
import com.sephp.mycarlauncher.data.repository.DockPreferences
import com.sephp.mycarlauncher.ui.components.DockAppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dock 栏组件
 */
@Composable
fun DockBar(
    modifier: Modifier = Modifier,
    onShowAppList: () -> Unit = {},
    onDockAppLongPress: (Int) -> Unit = {},
    context: Context,
    updateTrigger: Int = 0
) {
    // 异步加载 Dock 应用，防止阻塞主线程
    var dockApps by remember(updateTrigger) { mutableStateOf<List<AppInfo?>>(List(5) { null }) }
    
    LaunchedEffect(updateTrigger) {
        val packageManager = context.packageManager
        val loadedApps = withContext(Dispatchers.IO) {
            (0 until 5).map { index ->
                val packageName = DockPreferences.getDockApp(context, index)
                packageName?.let {
                    try {
                        val appInfo = packageManager.getApplicationInfo(it, 0)
                        AppInfo(
                            label = appInfo.loadLabel(packageManager).toString(),
                            packageName = it,
                            icon = appInfo.loadIcon(packageManager)
                        )
                    } catch (e: Exception) { null }
                }
            }
        }
        dockApps = loadedApps
    }
    
    val isDark = isSystemInDarkTheme()
    val iconColor = if (!isDark) Color.White else Color.Black
    val blurBackground = Color.Black.copy(alpha = 0.5f)
    
    Box(
        modifier = modifier
            .background(blurBackground)
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DockTimeDisplay()
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                items(5) { index ->
                    val app = dockApps[index]
                    key(app?.packageName ?: "empty_$index") {
                        DockAppItem(
                            appInfo = app,
                            onClick = { 
                                app?.let { 
                                    AppRepository.launchApp(context, it.packageName) 
                                } 
                            },
                            onLongClick = { onDockAppLongPress(index) },
                            iconColor = iconColor
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onShowAppList() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_action_apps),
                    contentDescription = "All Apps",
                    modifier = Modifier.size(32.dp),
                    colorFilter = ColorFilter.tint(iconColor)
                )
            }
        }
    }
}
