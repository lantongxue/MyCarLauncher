package com.sephp.mycarlauncher.service

import android.util.Log
import com.sephp.mycarlauncher.data.model.LyricLine

/**
 * 歌词解析服务
 * 负责解析LRC格式歌词并根据播放时间获取当前歌词
 */
object LyricService {
    
    private const val TAG = "LyricService"
    
    // LRC时间戳正则：支持 [00:12.34] [00:12:34] [00:12.345] [00:12] 等格式
    private val LRC_PATTERN = Regex("""\[(\d{2}):(\d{2})[\.:]?(\d{0,3})](.*)""")
    
    /**
     * 解析LRC格式歌词
     * @param lrcContent LRC歌词内容
     * @return 解析后的歌词行列表，按时间排序
     */
    fun parseLrc(lrcContent: String): List<LyricLine> {
        if (lrcContent.isBlank() || lrcContent == "-") {
            return emptyList()
        }
        
        val lyricLines = mutableListOf<LyricLine>()
        
        lrcContent.lines().forEach { line ->
            LRC_PATTERN.findAll(line).forEach { matchResult ->
                try {
                    val groups = matchResult.groupValues
                    val min = groups[1].toLongOrNull() ?: 0L
                    val sec = groups[2].toLongOrNull() ?: 0L
                    val msStr = groups[3]
                    val text = groups[4]
                    
                    val ms = parseMilliseconds(msStr)
                    val timeMs = min * 60 * 1000 + sec * 1000 + ms
                    
                    if (text.isNotBlank()) {
                        lyricLines.add(LyricLine(timeMs, text.trim()))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "解析歌词行失败: $line", e)
                }
            }
        }
        
        return lyricLines.sortedBy { it.timeMs }
    }
    
    /**
     * 根据当前播放时间获取对应的歌词行
     * @param lyrics 解析后的歌词列表
     * @param currentTimeMs 当前播放时间（毫秒）
     * @return 当前应显示的歌词文本
     */
    fun getCurrentLyric(lyrics: List<LyricLine>, currentTimeMs: Long): String {
        if (lyrics.isEmpty()) return "-"
        
        var currentLyric = "-"
        for (lyric in lyrics) {
            if (lyric.timeMs <= currentTimeMs) {
                currentLyric = lyric.text
            } else {
                break
            }
        }
        return currentLyric
    }
    
    /**
     * 根据当前播放时间获取歌词索引
     * @param lyrics 解析后的歌词列表
     * @param currentTimeMs 当前播放时间（毫秒）
     * @return 当前歌词的索引，-1表示未找到
     */
    fun getCurrentLyricIndex(lyrics: List<LyricLine>, currentTimeMs: Long): Int {
        if (lyrics.isEmpty()) return -1
        
        var currentIndex = -1
        for ((index, lyric) in lyrics.withIndex()) {
            if (lyric.timeMs <= currentTimeMs) {
                currentIndex = index
            } else {
                break
            }
        }
        return currentIndex
    }
    
    /**
     * 解析毫秒部分
     */
    private fun parseMilliseconds(msStr: String): Long {
        if (msStr.isEmpty()) return 0L
        
        return when (msStr.length) {
            1 -> msStr.toLongOrNull()?.times(100) ?: 0L  // 如 .3 -> 300ms
            2 -> msStr.toLongOrNull()?.times(10) ?: 0L   // 如 .34 -> 340ms
            else -> msStr.toLongOrNull() ?: 0L           // 如 .345 -> 345ms
        }
    }
}
