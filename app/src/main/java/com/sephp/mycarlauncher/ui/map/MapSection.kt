package com.sephp.mycarlauncher.ui.map

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.navi.AmapNaviPage
import com.amap.api.navi.AmapNaviParams
import com.amap.api.navi.AmapNaviType
import com.amap.api.navi.AmapPageType
import com.amap.api.navi.INaviInfoCallback
import com.amap.api.navi.model.AMapNaviLocation
import com.amap.api.services.core.PoiItemV2
import com.amap.api.services.poisearch.PoiResultV2
import com.amap.api.services.poisearch.PoiSearchV2
import com.sephp.mycarlauncher.MainActivity
import com.sephp.mycarlauncher.data.model.NavigationEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 地图区域组件
 */
@Composable
fun MapSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mainActivity = context as? MainActivity
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var aMap by remember { mutableStateOf<AMap?>(null) }
    var naviInfo by remember { mutableStateOf("点击地图开始导航") }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PoiItemV2>>(emptyList()) }
    var showSearchResults by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // 搜索功能
    fun performSearch(keyword: String) {
        if (keyword.isBlank()) {
            searchResults = emptyList()
            showSearchResults = false
            return
        }
        
        isSearching = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val query = PoiSearchV2.Query(keyword, "", "")
                query.pageSize = 10
                query.pageNum = 1
                
                val poiSearch = PoiSearchV2(context, query)
                poiSearch.setOnPoiSearchListener(object : PoiSearchV2.OnPoiSearchListener {
                    override fun onPoiSearched(result: PoiResultV2?, code: Int) {
                        isSearching = false
                        if (code == 1000 && result != null) {
                            searchResults = result.pois ?: emptyList()
                            showSearchResults = searchResults.isNotEmpty()
                        } else {
                            coroutineScope.launch(Dispatchers.Main) {
                                Toast.makeText(context, "搜索失败，错误码: $code", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    
                    override fun onPoiItemSearched(poiItem: PoiItemV2?, code: Int) {}
                })
                poiSearch.searchPOIAsyn()
            } catch (e: Exception) {
                isSearching = false
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "搜索异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // 开始导航到指定位置 - 使用高德内置导航页面
    fun startNavigation(poi: PoiItemV2) {
        try {
            // 停止巡航模式
            mainActivity?.let { activity ->
                try {
                    val stopMethod = MainActivity::class.java.getDeclaredMethod("stopCruiseMode")
                    stopMethod.isAccessible = true
                    stopMethod.invoke(activity)
                } catch (e: Exception) {
                    Log.e("MapSection", "停止巡航失败", e)
                }
            }
            
            val endPoint = com.amap.api.maps.model.Poi(
                poi.title,
                LatLng(poi.latLonPoint.latitude, poi.latLonPoint.longitude),
                poi.poiId
            )
            
            val params = AmapNaviParams(null, null, endPoint, AmapNaviType.DRIVER, AmapPageType.NAVI)
            params.setUseInnerVoice(true)
            
            AmapNaviPage.getInstance().showRouteActivity(
                context.applicationContext,
                params,
                object : INaviInfoCallback {
                    override fun onInitNaviFailure() {
                        Toast.makeText(context, "导航初始化失败", Toast.LENGTH_SHORT).show()
                    }
                    
                    override fun onGetNavigationText(text: String?) {
                        text?.let { naviInfo = it }
                    }
                    
                    override fun onLocationChange(location: AMapNaviLocation?) {}
                    override fun onArriveDestination(success: Boolean) {
                        if (success) {
                            Toast.makeText(context, "已到达目的地", Toast.LENGTH_SHORT).show()
                        }
                        // 到达目的地后恢复巡航
                        mainActivity?.triggerNavigationEvent(NavigationEvent.ArriveDestination)
                    }
                    override fun onStartNavi(type: Int) {
                        naviInfo = "导航中..."
                    }
                    override fun onCalculateRouteSuccess(ints: IntArray?) {
                        naviInfo = "算路成功"
                    }
                    override fun onCalculateRouteFailure(errorCode: Int) {
                        Toast.makeText(context, "路线计算失败: $errorCode", Toast.LENGTH_SHORT).show()
                    }
                    override fun onStopSpeaking() {}
                    override fun onReCalculateRoute(type: Int) {}
                    override fun onExitPage(type: Int) {
                        // 退出导航页面后恢复巡航
                        mainActivity?.triggerNavigationEvent(NavigationEvent.StopNavigation)
                    }
                    override fun onStrategyChanged(strategy: Int) {}
                    override fun onArrivedWayPoint(wayPointIndex: Int) {}
                    override fun getCustomNaviBottomView(): android.view.View? = null
                    override fun getCustomNaviView(): android.view.View? = null
                    override fun onMapTypeChanged(mapType: Int) {}
                    override fun getCustomMiddleView(): android.view.View? = null
                    override fun onNaviDirectionChanged(naviMode: Int) {}
                    override fun onDayAndNightModeChanged(mode: Int) {}
                    override fun onBroadcastModeChanged(mode: Int) {}
                    override fun onScaleAutoChanged(enable: Boolean) {}
                }
            )
            
            // 在地图上添加标记
            aMap?.let { map ->
                map.clear()
                map.addMarker(
                    MarkerOptions()
                        .position(LatLng(poi.latLonPoint.latitude, poi.latLonPoint.longitude))
                        .title(poi.title)
                        .snippet(poi.snippet)
                )
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(poi.latLonPoint.latitude, poi.latLonPoint.longitude), 15f
                ))
            }
            
            showSearchResults = false
            naviInfo = "正在导航至: ${poi.title}"
        } catch (e: Exception) {
            Log.e("MapSection", "启动导航失败", e)
            Toast.makeText(context, "启动导航失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 监听导航事件，恢复巡航
    LaunchedEffect(Unit) {
        mainActivity?.setNavigationCallback { event ->
            when (event) {
                NavigationEvent.ArriveDestination, NavigationEvent.StopNavigation -> {
                    // 停止导航，恢复巡航
                    coroutineScope.launch {
                        delay(500) // 稍微延迟以确保导航完全停止
                        mainActivity.let { activity ->
                            try {
                                val startMethod = MainActivity::class.java.getDeclaredMethod("tryStartCruiseMode")
                                startMethod.isAccessible = true
                                startMethod.invoke(activity)
                            } catch (e: Exception) {
                                Log.e("MapSection", "恢复巡航失败", e)
                            }
                        }
                        naviInfo = "已恢复巡航模式"
                    }
                }
            }
        }
    }
    
    // 地图生命周期管理
    DisposableEffect(Unit) {
        onDispose {
            try {
                mapView?.onDestroy()
            } catch (e: Exception) {
                Log.e("MapSection", "Error disposing map", e)
            }
        }
    }
    
    Box(
        modifier = modifier
            .border(2.dp, Color.Blue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // 高德地图View
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    onCreate(null)
                    mapView = this
                    map?.let { map ->
                        aMap = map
                        map.mapType = AMap.MAP_TYPE_NORMAL
                        map.isTrafficEnabled = true
                        map.isTouchPoiEnable = true

                        val myLocationStyle = MyLocationStyle()
                        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE)
                        map.myLocationStyle = myLocationStyle
                        map.isMyLocationEnabled = true
                        map.moveCamera(CameraUpdateFactory.zoomTo(15f))
                        
                        // 保存地图对象到MainActivity，用于位置更新
                        (context as? MainActivity)?.let { activity ->
                            activity.currentAMap = map
                            activity.onMapViewReady()
                        }
                        Log.d("MapSection", "地图初始化完成")
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 搜索框
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .widthIn(max = 400.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("搜索目的地", fontSize = 14.sp) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = TextStyle(fontSize = 14.sp)
                )
                
                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = "清除",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                searchQuery = ""
                                searchResults = emptyList()
                                showSearchResults = false
                            }
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Blue)
                        .clickable { performSearch(searchQuery) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSearching) {
                        Text("…", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("🔍", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
            
            // 搜索结果列表
            if (showSearchResults && searchResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(vertical = 4.dp)
                ) {
                    items(searchResults) { poi ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { startNavigation(poi) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = poi.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = poi.snippet ?: "",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (searchResults.last() != poi) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                thickness = 0.5.dp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
        
        // 导航信息显示层
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(12.dp)
        ) {
            Text(
                text = naviInfo,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
