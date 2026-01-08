package com.sephp.mycarlauncher.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * 壁纸背景组件，从网络加载图片并应用高斯模糊效果
 */
@Composable
fun WallpaperBackground(
    imageUrl: String,
    modifier: Modifier = Modifier,
    blurRadius: Int = 10 // 模糊半径，范围 1-25 dp
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        // 默认背景色（加载中或加载失败时显示）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E))
        )
        
        // 壁纸图片（带高斯模糊）
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.DISABLED) // 禁用缓存以确保每次获取新图片
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build(),
            contentDescription = "壁纸背景",
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius.dp), // 应用高斯模糊
            contentScale = ContentScale.Crop,
            onLoading = { isLoading = true },
            onSuccess = { 
                isLoading = false
                hasError = false
            },
            onError = {
                isLoading = false
                hasError = true
                // 根据规则，所有异常必须 Toast 提示
                Toast.makeText(context, "加载壁纸失败", Toast.LENGTH_SHORT).show()
            }
        )
        
        // 叠加一层半透明黑色遮罩，增强内容可读性
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )
    }
}
