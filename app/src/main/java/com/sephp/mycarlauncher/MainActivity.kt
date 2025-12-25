package com.sephp.mycarlauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.sephp.mycarlauncher.ui.theme.MyCarLauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 切换到正常主题，防止冷启动黑屏
        setTheme(R.style.Theme_MyCarLauncher)
        super.onCreate(savedInstanceState)
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
        Row(modifier = Modifier.fillMaxSize()) {
            DockBar(
                modifier = Modifier.width(80.dp).fillMaxHeight(),
                onShowAppList = { showAppList = true },
                onDockAppLongPress = { index ->
                    selectedDockIndex = index
                    showAppSelector = true
                },
                context = context,
                updateTrigger = dockUpdateTrigger
            )
            
            ContentArea(
                modifier = Modifier.fillMaxSize().padding(8.dp)
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

@Composable
fun DockTimeDisplay() {
    var dateTime by remember { mutableStateOf(getDockDateTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            dateTime = getDockDateTime()
            kotlinx.coroutines.delay(1000)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = dateTime.first, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = dateTime.second, color = Color.White, fontSize = 14.sp)
        Text(text = dateTime.third, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
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
    
    Box(modifier = modifier.background(blurBackground).padding(vertical = 6.dp)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            DockTimeDisplay()
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                items(5) { index ->
                    val app = dockApps[index]
                    key(app?.packageName ?: "empty_$index") {
                        DockAppItem(
                            appInfo = app,
                            onClick = { app?.let { launchApp(context, it.packageName) } },
                            onLongClick = { onDockAppLongPress(index) },
                            iconColor = iconColor
                        )
                    }
                }
            }
            Box(
                modifier = Modifier.padding(top = 10.dp).size(50.dp).clip(RoundedCornerShape(12.dp)).clickable { onShowAppList() },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockAppItem(appInfo: AppInfo?, onClick: () -> Unit, onLongClick: () -> Unit, iconColor: Color) {
    Box(
        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).combinedClickable(
            onClick = { if (appInfo == null) onLongClick() else onClick() },
            onLongClick = onLongClick
        ),
        contentAlignment = Alignment.Center
    ) {
        if (appInfo != null) {
            appInfo.icon?.let { drawable ->
                Image(painter = rememberDrawablePainter(drawable = drawable), contentDescription = appInfo.label, modifier = Modifier.size(48.dp))
            }
        } else {
            Text(text = "+", color = iconColor.copy(alpha = 0.5f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ContentArea(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MapSection(modifier = Modifier.fillMaxWidth().weight(1f))
        MusicSection(modifier = Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
fun MapSection(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.border(2.dp, Color.Blue.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "地图区域", color = Color.Blue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

data class LyricLine(
    val time: Long,      // 歌词对应的时间（毫秒）
    val text: String     // 歌词文本
)

data class MusicState(
    val title: String = "未在播放",
    val artist: String = "点击播放开始享受音乐",
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val totalDuration: Long = 0L,
    val lyrics: List<LyricLine> = emptyList()
)

@Composable
fun MusicSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var musicState by remember { mutableStateOf(MusicState()) }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // 异步更新音乐信息，包括封面
    fun updateMusicState(mediaController: MediaController?) {
        if (mediaController == null) {
            musicState = MusicState()
            return
        }
        
        val metadata = mediaController.metadata
        val playbackState = mediaController.playbackState
        
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "未知曲目"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "未知艺术家"
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val currentPosition = playbackState?.position ?: 0L
        val totalDuration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        
        // 先更新文字信息（使用空歌词）
        musicState = musicState.copy(
            title = title,
            artist = artist,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            totalDuration = totalDuration,
            lyrics = emptyList()  // 先清空歌词
        )
        
        // 异步加载封面
        coroutineScope.launch {
            val albumArt = withContext(Dispatchers.IO) {
                try {
                    metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                } catch (e: Exception) {
                    Log.e("MusicSection", "Error loading album art", e)
                    null
                }
            }
            musicState = musicState.copy(albumArt = albumArt)
        }
        
        // 异步加载真实歌词
        coroutineScope.launch {
            try {
                Log.d("MusicSection", "Fetching lyrics for: $title - $artist")
                val lyrics = LyricsService.getLyrics(title, artist)
                musicState = musicState.copy(lyrics = lyrics)
                Log.d("MusicSection", "Lyrics loaded: ${lyrics.size} lines")
            } catch (e: Exception) {
                Log.e("MusicSection", "Error loading lyrics", e)
                // 歌词加载失败时使用演示歌词
                musicState = musicState.copy(lyrics = generateDemoLyrics(title))
            }
        }
    }

    val callback = remember {
        object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                Log.d("MusicSection", "Metadata changed: ${metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)}")
                updateMusicState(controller)
            }
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                Log.d("MusicSection", "Playback state changed: ${state?.state}")
                musicState = musicState.copy(
                    isPlaying = state?.state == PlaybackState.STATE_PLAYING,
                    currentPosition = state?.position ?: 0L
                )
            }
        }
    }

    DisposableEffect(Unit) {
        if (!isNotificationListenerEnabled(context)) {
            Toast.makeText(context, "请授予通知访问权限以显示音乐信息", Toast.LENGTH_LONG).show()
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(context, MusicNotificationListener::class.java)

        val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            controller?.unregisterCallback(callback)
            val activeController = controllers?.firstOrNull()
            controller = activeController
            activeController?.registerCallback(callback)
            updateMusicState(activeController)
        }

        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
            val initialControllers = mediaSessionManager.getActiveSessions(componentName)
            val activeController = initialControllers.firstOrNull()
            controller = activeController
            activeController?.registerCallback(callback)
            updateMusicState(activeController)
            // AudioVisualizer 已在 LaunchedEffect 中初始化
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(context, "获取媒体会话失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        onDispose {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            controller?.unregisterCallback(callback)
        }
    }

    // 定时更新播放位置
    LaunchedEffect(musicState.isPlaying) {
        while (musicState.isPlaying) {
            controller?.playbackState?.let { playbackState ->
                musicState = musicState.copy(currentPosition = playbackState.position)
            }
            kotlinx.coroutines.delay(1000) // 每秒更新
        }
    }

    Box(
        modifier = modifier
            .border(2.dp, Color.Cyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(20.dp)
    ) {
        // 主要内容层
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // 左侧：音乐信息和控制按钮区域
            Box(modifier = Modifier.weight(1f)) {
                // 背景进度条层 - 只在左侧音乐控制区域显示
                val targetProgress = if (musicState.totalDuration > 0) {
                    (musicState.currentPosition.toFloat() / musicState.totalDuration.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                
                // 添加平滑动画过渡
                val animatedProgress by animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = tween(
                        durationMillis = 80,
                        easing = LinearEasing
                    ),
                    label = "progress_animation"
                )
                
                // 背景进度条 - 独立层级
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Cyan.copy(alpha = 0.15f))
                )
                
                // 音乐信息内容层
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = musicState.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = musicState.artist, color = Color.White, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MusicControlButton(icon = ImageVector.vectorResource(R.drawable.skip_previous), contentDescription = "Previous", onClick = { controller?.transportControls?.skipToPrevious() })
                        MusicControlButton(
                            icon = ImageVector.vectorResource(if (musicState.isPlaying) R.drawable.pause else R.drawable.play_arrow),
                            contentDescription = if (musicState.isPlaying) "Pause" else "Play",
                            isMain = true,
                            onClick = { if (musicState.isPlaying) controller?.transportControls?.pause() else controller?.transportControls?.play() }
                        )
                        MusicControlButton(icon = ImageVector.vectorResource(R.drawable.skip_next), contentDescription = "Next", onClick = { controller?.transportControls?.skipToNext() })
                    }
                }
            }
            
            // 中间：歌词显示区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                LyricsDisplay(
                    lyrics = musicState.lyrics,
                    currentPosition = musicState.currentPosition
                )
            }
            
            // 右侧：专辑封面
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                if (musicState.albumArt != null) {
                    Image(
                        bitmap = musicState.albumArt!!.asImageBitmap(),
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.play_arrow),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun MusicControlButton(icon: ImageVector, contentDescription: String, isMain: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(if (isMain) 64.dp else 48.dp).clip(CircleShape).background(if (isMain) Color.Cyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(if (isMain) 36.dp else 28.dp), tint = if (isMain) Color.Cyan else Color.White)
    }
}


@Composable
fun LyricsDisplay(lyrics: List<LyricLine>, currentPosition: Long) {
    if (lyrics.isEmpty()) {
        // 无歌词时显示提示
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.play_arrow),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Gray.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "正在享受音乐",
                color = Color.Gray.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        }
    } else {
        // 有歌词时显示歌词列表
        val listState = rememberLazyListState()
        
        // 查找当前歌词索引
        val currentLyricIndex = remember(currentPosition, lyrics) {
            lyrics.indexOfLast { it.time <= currentPosition }.coerceAtLeast(0)
        }
        
        // 自动滚动到当前歌词
        LaunchedEffect(currentLyricIndex) {
            if (currentLyricIndex > 0 && currentLyricIndex < lyrics.size) {
                try {
                    listState.animateScrollToItem(
                        index = (currentLyricIndex - 1).coerceAtLeast(0),
                        scrollOffset = 0
                    )
                } catch (e: Exception) {
                    Log.e("LyricsDisplay", "Error scrolling to lyric", e)
                }
            }
        }
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lyrics.size) { index ->
                val lyric = lyrics[index]
                val isCurrent = index == currentLyricIndex
                val isPast = index < currentLyricIndex
                
                Text(
                    text = lyric.text,
                    color = when {
                        isCurrent -> Color.Cyan
                        isPast -> Color.Gray.copy(alpha = 0.5f)
                        else -> Color.White.copy(alpha = 0.7f)
                    },
                    fontSize = if (isCurrent) 20.sp else 16.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun isNotificationListenerEnabled(context: Context): Boolean {
    val packageName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat?.contains(packageName) == true
}

class MusicNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {}
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}

@Composable
fun AppListOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        try {
            val apps = withContext(Dispatchers.IO) {
                getInstalledApps(context)
            }
            installedApps = apps
        } catch (e: Exception) {
            Toast.makeText(context, "加载应用列表失败: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.85f).fillMaxHeight(0.85f).clickable(enabled = false) {}.background(Color(0xFF1A1A1A), RoundedCornerShape(24.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)).padding(32.dp)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "所有应用", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(text = "关闭", color = Color.White, fontSize = 18.sp, modifier = Modifier.clickable { onDismiss() }.padding(8.dp))
                }
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "加载中...", color = Color.White, fontSize = 18.sp)
                    }
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(6), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        items(installedApps) { appInfo -> AppItem(appInfo = appInfo, onClick = { launchApp(context, appInfo.packageName); onDismiss() }) }
                    }
                }
            }
        }
    }
}

@Composable
fun AppItem(appInfo: AppInfo, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF2A2A2A)), contentAlignment = Alignment.Center) {
            appInfo.icon?.let { drawable ->
                Image(painter = rememberDrawablePainter(drawable = drawable), contentDescription = appInfo.label, modifier = Modifier.size(52.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = appInfo.label, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

data class AppInfo(val label: String, val packageName: String, val icon: Drawable? = null)

fun getInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).mapNotNull { resolveInfo ->
        try {
            val appInfo = resolveInfo.activityInfo.applicationInfo
            AppInfo(appInfo.loadLabel(pm).toString(), resolveInfo.activityInfo.packageName, appInfo.loadIcon(pm))
        } catch (e: Exception) { null }
    }.sortedBy { it.label.lowercase() }
}

fun launchApp(context: Context, packageName: String) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        intent?.let { context.startActivity(it) }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "启动应用失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

object DockPreferences {
    private const val PREFS_NAME = "dock_prefs"
    private const val KEY_DOCK_APP = "dock_app_"
    fun saveDockApp(context: Context, index: Int, packageName: String) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putString(KEY_DOCK_APP + index, packageName) }
    fun getDockApp(context: Context, index: Int) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_DOCK_APP + index, null)
}

@Composable
fun AppSelectorDialog(onDismiss: () -> Unit, onAppSelected: (AppInfo) -> Unit, context: Context) {
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        try {
            val apps = withContext(Dispatchers.IO) {
                getInstalledApps(context)
            }
            installedApps = apps
        } catch (e: Exception) {
            Toast.makeText(context, "加载应用列表失败: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.8f).fillMaxHeight(0.8f).clickable(enabled = false) {}.background(Color(0xFF1A1A1A), RoundedCornerShape(24.dp)).padding(24.dp)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "选择应用", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(text = "关闭", color = Color.White, fontSize = 16.sp, modifier = Modifier.clickable { onDismiss() }.padding(8.dp))
                }
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "加载中...", color = Color.White, fontSize = 18.sp)
                    }
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(5), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(installedApps) { appInfo -> AppItem(appInfo = appInfo, onClick = { onAppSelected(appInfo) }) }
                    }
                }
            }
        }
    }
}

fun getDockDateTime(): Triple<String, String, String> {
    val calendar = Calendar.getInstance()
    return Triple(SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time), SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time), SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time))
}

// 演示用的模拟歌词生成器（实际应用中应从外部源获取）
fun generateDemoLyrics(songTitle: String): List<LyricLine> {
    // 这里可以根据歌曲名称返回模拟歌词
    // 实际应用中应该：
    // 1. 从本地LRC文件读取
    // 2. 从在线API获取（如QQ音乐、网易云音乐API）
    // 3. 从媒体元数据中解析（如果支持）
    
    return listOf(
        LyricLine(0, "正在播放: $songTitle"),
        LyricLine(5000, "欢迎使用车载启动器"),
        LyricLine(10000, "享受你的音乐时光"),
        LyricLine(15000, "此处应显示真实歌词"),
        LyricLine(20000, "可以通过LRC文件加载"),
        LyricLine(25000, "或者从在线API获取"),
        LyricLine(30000, "歌词会根据播放进度"),
        LyricLine(35000, "自动滚动并高亮显示"),
        LyricLine(40000, "当前歌词以青色显示"),
        LyricLine(45000, "已播放的歌词变暗"),
        LyricLine(50000, "未播放的歌词为白色"),
        LyricLine(55000, "感谢你的使用")
    )
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
fun HomeScreenPreview() {
    MyCarLauncherTheme { HomeScreen() }
}
