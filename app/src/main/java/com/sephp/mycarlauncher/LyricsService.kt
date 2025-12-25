package com.sephp.mycarlauncher

import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// QQ音乐歌词API数据模型
data class QQMusicSearchResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("data") val data: QQMusicSearchData?
)

data class QQMusicSearchData(
    @SerializedName("song") val song: QQMusicSongList?
)

data class QQMusicSongList(
    @SerializedName("list") val list: List<QQMusicSong>?
)

data class QQMusicSong(
    @SerializedName("songid") val songId: Long,
    @SerializedName("songmid") val songMid: String,
    @SerializedName("songname") val songName: String,
    @SerializedName("singer") val singers: List<QQMusicSinger>?
)

data class QQMusicSinger(
    @SerializedName("name") val name: String
)

data class QQMusicLyricResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("lyric") val lyric: String?,
    @SerializedName("trans") val translation: String?
)

// QQ音乐API接口
interface QQMusicApi {
    // 搜索歌曲
    @GET("soso/fcgi-bin/client_search_cp")
    suspend fun searchSong(
        @Query("w") keyword: String,
        @Query("p") page: Int = 1,
        @Query("n") pageSize: Int = 1,
        @Query("format") format: String = "json"
    ): QQMusicSearchResponse

    // 获取歌词
    @GET("lyric/fcgi-bin/fcg_query_lyric_new.fcg")
    suspend fun getLyric(
        @Query("songmid") songMid: String,
        @Query("format") format: String = "json",
        @Query("nobase64") noBase64: Int = 1
    ): QQMusicLyricResponse
}

// 歌词服务类
object LyricsService {
    private const val TAG = "LyricsService"
    private const val BASE_URL = "https://c.y.qq.com/"

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Referer", "https://y.qq.com")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val api: QQMusicApi by lazy {
        retrofit.create(QQMusicApi::class.java)
    }

    /**
     * 根据歌曲名和艺术家获取歌词
     */
    suspend fun getLyrics(songTitle: String, artist: String): List<LyricLine> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 搜索歌曲获取songmid
                val searchKeyword = "$songTitle $artist"
                Log.d(TAG, "Searching for: $searchKeyword")
                
                val searchResponse = api.searchSong(searchKeyword)
                
                if (searchResponse.code != 0 || searchResponse.data?.song?.list.isNullOrEmpty()) {
                    Log.e(TAG, "Song not found")
                    return@withContext emptyList()
                }

                val song = searchResponse.data.song.list.first()
                Log.d(TAG, "Found song: ${song.songName} - ${song.songMid}")

                // 2. 获取歌词
                val lyricResponse = api.getLyric(song.songMid)
                
                if (lyricResponse.code != 0 || lyricResponse.lyric.isNullOrEmpty()) {
                    Log.e(TAG, "Lyric not found")
                    return@withContext emptyList()
                }

                // 3. 解析LRC格式歌词
                val lyrics = parseLyric(lyricResponse.lyric)
                Log.d(TAG, "Parsed ${lyrics.size} lyric lines")
                
                lyrics
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching lyrics", e)
                emptyList()
            }
        }
    }

    /**
     * 解析LRC格式歌词
     * 格式示例: [00:12.50]歌词内容
     */
    private fun parseLyric(lrcText: String): List<LyricLine> {
        val lyrics = mutableListOf<LyricLine>()
        val lines = lrcText.split("\n")
        
        // LRC时间戳正则: [mm:ss.xx]
        val timePattern = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")
        
        for (line in lines) {
            val matchResult = timePattern.find(line) ?: continue
            
            try {
                val minutes = matchResult.groupValues[1].toInt()
                val seconds = matchResult.groupValues[2].toInt()
                val milliseconds = matchResult.groupValues[3].padEnd(3, '0').take(3).toInt()
                val text = matchResult.groupValues[4].trim()
                
                if (text.isNotEmpty()) {
                    val timeInMillis = (minutes * 60 * 1000L) + (seconds * 1000L) + milliseconds
                    lyrics.add(LyricLine(timeInMillis, text))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse lyric line: $line", e)
            }
        }
        
        // 按时间排序
        return lyrics.sortedBy { it.time }
    }
}
