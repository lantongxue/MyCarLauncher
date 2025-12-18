package com.sephp.mycarlauncher

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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        checkAndRequestDefaultLauncher()
        
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
    var showAppList by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 状态栏 - Status Bar (Red Border)
            StatusBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
            
            // 主内容区域 - Main Content Area
            Row(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Dock栏 - Dock Bar (Green Border)
                DockBar(
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight(),
                    onShowAppList = { showAppList = true }
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
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 日期和时间
            Column {
                Text(
                    text = currentTime.first,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentTime.second,
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
            
            // 右侧可以添加其他状态信息
            Text(
                text = "状态栏",
                color = Color.Red,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun DockBar(
    modifier: Modifier = Modifier,
    onShowAppList: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .border(3.dp, Color.Green)
            .background(Color(0xFF1A1A1A))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 上部分：标题和应用图标
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Dock栏",
                    color = Color.Green,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // 这里将来添加应用图标
                repeat(5) { index ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.DarkGray)
                            .border(1.dp, Color.Green),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "App${index + 1}",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            // 底部：所有应用按钮
            Button(
                onClick = onShowAppList,
                modifier = Modifier
                    .size(64.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00AA00)
                )
            ) {
                Text(
                    text = "All\nApps",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
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
            .background(Color(0xFF1A1A1A))
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
            .background(Color(0xFF1A1A1A))
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
        // 应用图标占位
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appInfo.label.take(1),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
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
    val packageName: String
)

// 获取已安装的应用列表
fun getInstalledApps(context: Context): List<AppInfo> {
    val packageManager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    
    val apps = packageManager.queryIntentActivities(intent, 0)
        .map { resolveInfo ->
            AppInfo(
                label = resolveInfo.loadLabel(packageManager).toString(),
                packageName = resolveInfo.activityInfo.packageName
            )
        }
        .sortedBy { it.label }
    
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