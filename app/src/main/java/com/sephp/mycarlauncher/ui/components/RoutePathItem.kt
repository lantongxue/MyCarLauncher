package com.sephp.mycarlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sephp.mycarlauncher.data.model.RoutePathInfo
import com.sephp.mycarlauncher.utils.FormatUtils

/**
 * 路线信息项组件
 */
@Composable
fun RoutePathItem(
    routeInfo: RoutePathInfo,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = routeInfo.strategyName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "距离: ${FormatUtils.formatDistance(routeInfo.length)}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "预计时间: ${FormatUtils.formatTime(routeInfo.time)}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                
                if (routeInfo.tollCost > 0) {
                    Text(
                        text = "过路费: ¥${routeInfo.tollCost}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B00)
                    )
                } else {
                    Text(
                        text = "免过路费",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00B578)
                    )
                }
            }
        }
    }
}
