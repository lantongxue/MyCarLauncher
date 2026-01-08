package com.sephp.mycarlauncher.data.model

import android.graphics.Bitmap

/**
 * 音乐播放状态数据类
 */
data class MusicState(
    val title: String = "未在播放",
    val artist: String = "点击播放开始享受音乐",
    val currentLyricLine: String = "-",
    val lyricLines: String = "",
    val parsedLyrics: List<LyricLine> = emptyList(),  // 缓存解析后的歌词列表
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val totalDuration: Long = 0L
)
