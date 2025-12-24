package com.sephp.mycarlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.sephp.mycarlauncher.ui.theme.MyCarLauncherTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置全屏模式
        enableEdgeToEdge()
        
        setContent {
            MyCarLauncherTheme {
                HomeScreen()
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var showAppList by remember { mutableStateOf(false) }
    var showAppSelector by remember { mutableStateOf(false) }
    var selectedDockIndex by remember { mutableStateOf<Int?>(null) }
    var dockUpdateTrigger by remember { mutableIntStateOf(0) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Dock栏 - Dock Bar
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
            
            // 中间内容区域 - Content Area
            ContentArea(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }
        
        // 应用列表浮动视图
        if (showAppList) {
            AppListOverlay(
                onDismiss = { showAppList = false }
            )
        }
        
        // 应用选择对话框
        if (showAppSelector && selectedDockIndex != null) {
            AppSelectorDialog(
                onDismiss = { showAppSelector = false },
                onAppSelected = { appInfo ->
                    DockPreferences.saveDockApp(context, selectedDockIndex!!, appInfo.packageName)
                    dockUpdateTrigger++ // 触发dock更新
                    showAppSelector = false
                },
                context = context
            )
        }
    }
}

@Composable
fun DockTimeDisplay() {
    var dateTime by remember { mutableStateOf(getDockDateTime()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            dateTime = getDockDateTime()
            kotlinx.coroutines.delay(1000)
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
    ) {
        // 时分
        Text(
            text = dateTime.first,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        // 星期
        Text(
            text = dateTime.second,
            color = Color.White,
            fontSize = 14.sp
        )
        // 日期
        Text(
            text = dateTime.third,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockBar(
    modifier: Modifier = Modifier,
    onShowAppList: () -> Unit = {},
    onDockAppLongPress: (Int) -> Unit = {},
    context: Context,
    updateTrigger: Int = 0
) {
    val packageManager = context.packageManager
    
    // 为每个dock位置加载应用信息
    val dockApps = remember(updateTrigger) {
        (0 until 5).map { index ->
            val packageName = DockPreferences.getDockApp(context, index)
            packageName?.let {
                try {
                    val appInfo = packageManager.getApplicationInfo(it, 0)
                    val icon = appInfo.loadIcon(packageManager)
                    val label = appInfo.loadLabel(packageManager).toString()
                    AppInfo(label, it, icon)
                } catch (e: Exception) {
                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                    null
                }
            }
        }
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
            // 时间显示 (Dock栏固定在顶部)
            DockTimeDisplay()
            
            // 应用列表 (占据中间剩余空间，可滚动)
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
                                app?.let { launchApp(context, it.packageName) }
                            },
                            onLongClick = {
                                onDockAppLongPress(index)
                            },
                            iconColor = iconColor
                        )
                    }
                }
            }
            
            // 所有应用按钮 (固定在Dock栏底部)
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

// Dock栏应用项
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockAppItem(
    appInfo: AppInfo?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    iconColor: Color = Color.Green
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    if (appInfo == null) {
                        onLongClick()
                    } else {
                        onClick()
                    }
                },
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

@Composable
fun ContentArea(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MapSection(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        MusicSection(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@Composable
fun MapSection(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(2.dp, Color.Blue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "地图区域",
            color = Color.Blue,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MusicSection(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(2.dp, Color.Cyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "音乐播放区域",
            color = Color.Cyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AppListOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val installedApps = remember { getInstalledApps(context) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.85f)
                .clickable(enabled = false) { }
                .background(Color(0xFF1A1A1A), RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(32.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 标题栏带关闭按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "所有应用",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "关闭",
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(8.dp)
                    )
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(installedApps) { appInfo ->
                        AppItem(
                            appInfo = appInfo,
                            onClick = {
                                launchApp(context, appInfo.packageName)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppItem(appInfo: AppInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            appInfo.icon?.let { drawable ->
                Image(
                    painter = rememberDrawablePainter(drawable = drawable),
                    contentDescription = appInfo.label,
                    modifier = Modifier.size(52.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = appInfo.label,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable? = null
)

fun getInstalledApps(context: Context): List<AppInfo> {
    val packageManager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    
    return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        .mapNotNull { resolveInfo ->
            try {
                val packageName = resolveInfo.activityInfo.packageName
                val applicationInfo = resolveInfo.activityInfo.applicationInfo
                val icon = applicationInfo?.loadIcon(packageManager)
                val label = applicationInfo?.loadLabel(packageManager)?.toString() 
                    ?: resolveInfo.loadLabel(packageManager).toString()
                
                AppInfo(label, packageName, icon)
            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                null
            }
        }
        .sortedBy { it.label.lowercase() }
}

fun launchApp(context: Context, packageName: String) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        intent?.let { context.startActivity(it) }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

object DockPreferences {
    private const val PREFS_NAME = "dock_prefs"
    private const val KEY_DOCK_APP = "dock_app_"
    
    fun saveDockApp(context: Context, index: Int, packageName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_DOCK_APP + index, packageName)
            }
    }
    
    fun getDockApp(context: Context, index: Int): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DOCK_APP + index, null)
    }
}

@Composable
fun AppSelectorDialog(
    onDismiss: () -> Unit,
    onAppSelected: (AppInfo) -> Unit,
    context: Context
) {
    val installedApps = remember { getInstalledApps(context) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.8f)
                .clickable(enabled = false) { }
                .background(Color(0xFF1A1A1A), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column {
                // 标题栏带关闭按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择应用",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "关闭",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(8.dp)
                    )
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(installedApps) { appInfo ->
                        AppItem(appInfo = appInfo, onClick = { onAppSelected(appInfo) })
                    }
                }
            }
        }
    }
}

fun getDockDateTime(): Triple<String, String, String> {
    val calendar = Calendar.getInstance()
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val weekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    return Triple(
        timeFormat.format(calendar.time),
        weekFormat.format(calendar.time),
        dateFormat.format(calendar.time)
    )
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
fun HomeScreenPreview() {
    MyCarLauncherTheme {
        HomeScreen()
    }
}
