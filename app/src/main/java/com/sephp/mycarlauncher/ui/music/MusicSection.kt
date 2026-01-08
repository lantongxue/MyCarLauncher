package com.sephp.mycarlauncher.ui.music

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sephp.mycarlauncher.R
import com.sephp.mycarlauncher.data.model.MusicState
import com.sephp.mycarlauncher.service.MusicNotificationListener
import com.sephp.mycarlauncher.ui.components.MusicControlButton
import com.sephp.mycarlauncher.utils.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * 音乐播放区域组件
 */
@Composable
fun MusicSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var musicState by remember { mutableStateOf(MusicState()) }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // 异步更新音乐信息，包括封面
    fun updateMusicState(mediaController: MediaController?, forceResetPosition: Boolean = false) {
        if (mediaController == null) {
            musicState = MusicState()
            return
        }
        
        val metadata = mediaController.metadata
        val playbackState = mediaController.playbackState
        
        val newTitle = metadata?.getString("android.media.metadata.CUSTOM_FIELD_TITLE") ?: "未知曲目"
        val singer = metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: "未知歌手"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "未知艺术家"
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val totalDuration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val currentLyricLine = metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: "-"
        
        // 处理播放位置的三种情况
        val isFirstLoad = musicState.title == "未在播放" // APP刚打开，首次加载
        val isSongChanged = newTitle != musicState.title && !isFirstLoad // 切换歌曲
        
        val newPosition = when {
            forceResetPosition || isSongChanged -> 0L // 切歌时归零
            isFirstLoad -> playbackState?.position ?: 0L // 首次加载读取真实位置
            else -> musicState.currentPosition // 其他情况保持当前位置
        }

        // 更新音乐状态信息
        musicState = musicState.copy(
            title = newTitle,
            singer = singer,
            artist = artist,
            isPlaying = isPlaying,
            currentLyricLine = currentLyricLine,
            currentPosition = newPosition,
            totalDuration = totalDuration
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
    }

    val callback = remember {
        object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                Log.d("MusicSection", "Metadata changed: ${metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)}")
                updateMusicState(controller)
            }
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                Log.d("MusicSection", "Playback state changed: ${state?.state}")
                // 只更新播放状态，不更新位置（避免进度条跳变）
                // 位置由 LaunchedEffect 平滑更新
                musicState = musicState.copy(
                    isPlaying = state?.state == PlaybackState.STATE_PLAYING
                )
            }
        }
    }

    DisposableEffect(Unit) {
        if (!FormatUtils.isNotificationListenerEnabled(context)) {
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
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(context, "获取媒体会话失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        onDispose {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            controller?.unregisterCallback(callback)
        }
    }

    // 定时更新播放位置 - 使用增量计算避免跳变
    LaunchedEffect(musicState.isPlaying, musicState.title) {
        var lastUpdateTime = System.currentTimeMillis()
        
        while (musicState.isPlaying) {
            delay(100) // 每100ms更新一次，更平滑
            
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - lastUpdateTime
            lastUpdateTime = currentTime
            
            // 使用增量更新，避免直接读取播放器位置导致的跳变
            val newPosition = (musicState.currentPosition + elapsed).coerceAtMost(musicState.totalDuration)
            
            // 每5秒同步一次真实位置，纠正累积误差
            if (newPosition % 5000 < 100) {
                controller?.playbackState?.let { playbackState ->
                    val realPosition = playbackState.position
                    // 只在误差超过1秒时才同步
                    if (abs(realPosition - newPosition) > 1000) {
                        musicState = musicState.copy(currentPosition = realPosition)
                        return@let
                    }
                }
            }
            
            musicState = musicState.copy(currentPosition = newPosition)
        }
    }

    Box(
        modifier = modifier
            .border(2.dp, Color.Cyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // 主要内容层 - 垂直布局（适应30%宽度的垂直空间）
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部：专辑封面
            Box(
                modifier = Modifier
                    .size(100.dp)
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
                        modifier = Modifier.size(40.dp),
                        tint = Color.Gray
                    )
                }
            }
            
            // 中间：音乐信息（标题、艺术家）
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = musicState.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = musicState.singer,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            
            // 进度条 - 使用 Material3 LinearProgressIndicator
            val progress by remember {
                derivedStateOf {
                    if (musicState.totalDuration > 0) {
                        (musicState.currentPosition.toFloat() / musicState.totalDuration.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color.Cyan,
                trackColor = Color.White.copy(alpha = 0.2f),
                gapSize = 0.dp,
                strokeCap = StrokeCap.Square,
                drawStopIndicator = {} // 禁用尾部停止指示器
            )
            
            // 播放时间显示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = FormatUtils.formatDuration(musicState.currentPosition),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Text(
                    text = FormatUtils.formatDuration(musicState.totalDuration),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            
            // 控制按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MusicControlButton(
                    icon = ImageVector.vectorResource(R.drawable.skip_previous),
                    contentDescription = "Previous",
                    onClick = { controller?.transportControls?.skipToPrevious() }
                )
                MusicControlButton(
                    icon = ImageVector.vectorResource(if (musicState.isPlaying) R.drawable.pause else R.drawable.play_arrow),
                    contentDescription = if (musicState.isPlaying) "Pause" else "Play",
                    isMain = true,
                    onClick = { if (musicState.isPlaying) controller?.transportControls?.pause() else controller?.transportControls?.play() }
                )
                MusicControlButton(
                    icon = ImageVector.vectorResource(R.drawable.skip_next),
                    contentDescription = "Next",
                    onClick = { controller?.transportControls?.skipToNext() }
                )
            }
            
            // 当前歌词显示区域（占据剩余空间）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = musicState.currentLyricLine,
                    color = Color.Cyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
