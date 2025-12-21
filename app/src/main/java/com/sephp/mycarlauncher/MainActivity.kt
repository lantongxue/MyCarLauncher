package com.sephp.mycarlauncher

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
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
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.sephp.mycarlauncher.ui.theme.MyCarLauncherTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置全屏模式
        enableEdgeToEdge()
        setupFullscreen()
        
        // 检查并请求设置为默认launcher
        //checkAndRequestDefaultLauncher()
        
        setContent {
            MyCarLauncherTheme {
                HomeScreen()
            }
        }
    }
    
    private fun setupFullscreen() {
        // 隐藏系统UI，实现全屏
        window.decorView.apply {
            systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }
    
    private fun checkAndRequestDefaultLauncher() {
        // 检查当前是否为默认launcher
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentLauncher = resolveInfo?.activityInfo?.packageName
        
        // 如果不是当前应用，则请求设置为默认launcher
        if (currentLauncher != packageName) {
            requestDefaultLauncher()
        }
    }
    
    private fun requestDefaultLauncher() {
        // 创建一个HOME intent来触发launcher选择器
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var showAppList by remember { mutableStateOf(false) }
    var showAppSelector by remember { mutableStateOf(false) }
    var selectedDockIndex by remember { mutableStateOf<Int?>(null) }
    var dockUpdateTrigger by remember { mutableStateOf(0) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 状态栏 - Status Bar (Red Border)
            StatusBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
            
            // 主内容区域 - Main Content Area
            Row(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Dock栏 - Dock Bar (Green Border)
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
                
                // 中间内容区域 - Content Area (Blue Border for each section)
                ContentArea(
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
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
fun StatusBar(modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf(getCurrentDateTime()) }
    
    // 更新时间
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime = getCurrentDateTime()
        }
    }
    
    Box(
        modifier = modifier
            .border(3.dp, Color.Red)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // 时间和日期在一行显示
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = currentTime.first,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currentTime.second,
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
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
    
    // 为每个dock位置加载应用信息，依赖updateTrigger触发重新加载
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
            .border(3.dp, Color.Green)
            .background(blurBackground)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 可滚动的应用图标区域
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp)
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
            
            // 固定在底部的所有应用按钮
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
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
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (appInfo != null) Color.White else Color.Transparent)
            //.border(1.dp, iconColor, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    if (appInfo == null) {
                        // 空位点击直接触发选择
                        onLongClick()
                    } else {
                        // 有应用时点击启动
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
                    modifier = Modifier.size(36.dp)
                )
            } ?: run {
                Text(
                    text = appInfo.label.take(1),
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // 空位显示加号
            Text(
                text = "+",
                color = iconColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ContentArea(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 地图区域 - Map Area
        MapSection(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        
        // 音乐播放器区域 - Music Player Area
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
            .border(3.dp, Color.Blue)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "地图区域\nMap Section",
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
            .border(3.dp, Color.Cyan)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "正在播放的音乐\nNow Playing",
            color = Color.Cyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// 应用列表浮动视图
@Composable
fun AppListOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val installedApps = remember { getInstalledApps(context) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // 应用列表面板
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.8f)
                .clickable(enabled = false) { } // 阻止点击穿透
                .background(
                    color = Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(2.dp, Color(0xFF00AA00), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "所有应用",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "关闭",
                        color = Color(0xFF00AA00),
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(8.dp)
                    )
                }
                
                // 应用网格
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
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

// 应用项组件
@Composable
fun AppItem(
    appInfo: AppInfo,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 应用图标
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            appInfo.icon?.let { drawable ->
                Image(
                    painter = rememberDrawablePainter(drawable = drawable),
                    contentDescription = appInfo.label,
                    modifier = Modifier.size(48.dp)
                )
            } ?: run {
                // 备用：显示首字母
                Text(
                    text = appInfo.label.take(1),
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 应用名称
        Text(
            text = appInfo.label,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp)
        )
    }
}

// 应用信息数据类
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable? = null
)

// 获取已安装的应用列表
fun getInstalledApps(context: Context): List<AppInfo> {
    val packageManager = context.packageManager
    
    // 创建查询launcher应用的intent
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    
    // 使用queryIntentActivities配合QUERY_ALL_PACKAGES权限获取所有应用
    val apps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        .mapNotNull { resolveInfo ->
            try {
                val packageName = resolveInfo.activityInfo.packageName
                val applicationInfo = resolveInfo.activityInfo.applicationInfo
                val icon = applicationInfo?.loadIcon(packageManager)
                val label = applicationInfo?.loadLabel(packageManager)?.toString() 
                    ?: resolveInfo.loadLabel(packageManager).toString()
                
                AppInfo(
                    label = label,
                    packageName = packageName,
                    icon = icon
                )
            } catch (e: Exception) {
                null // 忽略无法加载的应用
            }
        }
        .sortedBy { it.label.lowercase() }
    
    return apps
}

// 启动应用
fun launchApp(context: Context, packageName: String) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Dock应用持久化管理
object DockPreferences {
    private const val PREFS_NAME = "dock_prefs"
    private const val KEY_DOCK_APP = "dock_app_"
    
    fun saveDockApp(context: Context, index: Int, packageName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DOCK_APP + index, packageName)
            .apply()
    }
    
    fun getDockApp(context: Context, index: Int): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DOCK_APP + index, null)
    }
    
    fun removeDockApp(context: Context, index: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DOCK_APP + index)
            .apply()
    }
}

// 应用选择对话框
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
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // 应用选择面板
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .fillMaxHeight(0.7f)
                .clickable(enabled = false) { }
                .background(
                    color = Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(2.dp, Color(0xFF00AA00), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "取消",
                        color = Color(0xFF00AA00),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(8.dp)
                    )
                }
                
                // 应用网格
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(installedApps) { appInfo ->
                        AppItem(
                            appInfo = appInfo,
                            onClick = {
                                onAppSelected(appInfo)
                            }
                        )
                    }
                }
            }
        }
    }
}

// 获取当前日期和时间
fun getCurrentDateTime(): Pair<String, String> {
    val calendar = Calendar.getInstance()
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val dateFormat = SimpleDateFormat("yyyy-MM-dd EEEE", Locale.getDefault())
    
    val time = timeFormat.format(calendar.time)
    val date = dateFormat.format(calendar.time)
    
    return Pair(time, date)
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
fun HomeScreenPreview() {
    MyCarLauncherTheme {
        HomeScreen()
    }
}