package com.sephp.mycarlauncher

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.LatLng
import com.amap.api.navi.AMapNavi
import com.amap.api.navi.AimlessModeListener
import com.amap.api.navi.NaviSetting
import com.amap.api.navi.model.AMapCalcRouteResult
import com.amap.api.navi.model.AMapLaneInfo
import com.amap.api.navi.model.AMapModelCross
import com.amap.api.navi.model.AMapNaviCameraInfo
import com.amap.api.navi.model.AMapNaviCross
import com.amap.api.navi.model.AMapNaviLocation
import com.amap.api.navi.model.AMapNaviRouteNotifyData
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo
import com.amap.api.navi.model.AMapServiceAreaInfo
import com.amap.api.navi.model.AimLessModeCongestionInfo
import com.amap.api.navi.model.AimLessModeStat
import com.amap.api.navi.model.NaviInfo
import com.sephp.mycarlauncher.data.model.NavigationEvent
import com.sephp.mycarlauncher.ui.home.HomeScreen
import com.sephp.mycarlauncher.ui.theme.MyCarLauncherTheme

/**
 * 车载Launcher主Activity
 * 负责：
 * - 高德地图/导航SDK初始化
 * - 权限管理
 * - 智能巡航模式控制
 * - 导航事件分发
 */
class MainActivity : ComponentActivity() {
    private var mAMapNavi: AMapNavi? = null
    private var isCruiseModeActive = false
    private var isMapViewReady = false
    private var hasLocationPermission = false
    private var navigationCallback: ((NavigationEvent) -> Unit)? = null
    var currentAMap: AMap? = null
    
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                Toast.makeText(this, "定位权限已授予", Toast.LENGTH_SHORT).show()
                hasLocationPermission = true
                tryStartCruiseMode()
            }
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                Toast.makeText(this, "定位权限已授予", Toast.LENGTH_SHORT).show()
                hasLocationPermission = true
                tryStartCruiseMode()
            }
            else -> {
                Toast.makeText(this, "定位权限被拒绝，地图功能受限", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MyCarLauncher)
        super.onCreate(savedInstanceState)
        
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
        NaviSetting.updatePrivacyShow(this, true, true)
        NaviSetting.updatePrivacyAgree(this, true)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        enableEdgeToEdge()
        initAMapNavi()
        checkAndRequestPermissions()
        
        setContent {
            MyCarLauncherTheme {
                HomeScreen()
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            locationPermissionRequest.launch(permissionsToRequest.toTypedArray())
        } else {
            hasLocationPermission = true
            tryStartCruiseMode()
        }
    }
    
    private fun initAMapNavi() {
        try {
            mAMapNavi = AMapNavi.getInstance(applicationContext)
            
            mAMapNavi?.addAimlessModeListener(object : AimlessModeListener {
                override fun onUpdateTrafficFacility(infos: Array<out AMapNaviTrafficFacilityInfo>?) {
                    infos?.forEach { info ->
                        Log.d("CruiseMode", "道路设施: 距离=${info.distance}m, 限速=${info.limitSpeed}km/h")
                    }
                }
                
                override fun onUpdateAimlessModeElecCameraInfo(cameraInfo: Array<out AMapNaviTrafficFacilityInfo>?) {
                    cameraInfo?.forEach { info ->
                        Log.d("CruiseMode", "电子眼: 距离=${info.distance}m, 限速=${info.limitSpeed}km/h")
                    }
                }
                
                override fun updateAimlessModeStatistics(aimLessModeStat: AimLessModeStat?) {
                    aimLessModeStat?.let {
                        Log.d("CruiseMode", "巡航统计: 距离=${it.aimlessModeDistance}m, 时间=${it.aimlessModeTime}s")
                    }
                }
                
                override fun updateAimlessModeCongestionInfo(aimLessModeCongestionInfo: AimLessModeCongestionInfo?) {
                    aimLessModeCongestionInfo?.let { info ->
                        Log.d("CruiseMode", "拥堵信息: 道路=${info.roadName}, 状态=${info.congestionStatus}, 长度=${info.length}m")
                    }
                }
            })
            
            mAMapNavi?.addAMapNaviListener(object : com.amap.api.navi.AMapNaviListener {
                override fun onInitNaviFailure() {
                    Log.e("CruiseMode", "导航初始化失败")
                    Toast.makeText(this@MainActivity, "导航初始化失败", Toast.LENGTH_SHORT).show()
                }
                
                override fun onInitNaviSuccess() {
                    Log.d("CruiseMode", "导航初始化成功")
                }
                
                override fun onLocationChange(location: AMapNaviLocation?) {
                    location?.let {
                        currentAMap?.let { map ->
                            val latLng = LatLng(it.coord.latitude, it.coord.longitude)
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        }
                    }
                }
                
                override fun onStartNavi(type: Int) {}
                override fun onTrafficStatusUpdate() {}
                override fun onGetNavigationText(type: Int, text: String?) {}
                override fun onGetNavigationText(text: String?) {}
                override fun onEndEmulatorNavi() {}
                override fun onArriveDestination() {}
                override fun onCalculateRouteFailure(errorInfo: Int) {}
                override fun onReCalculateRouteForYaw() {}
                override fun onReCalculateRouteForTrafficJam() {}
                override fun onArrivedWayPoint(wayID: Int) {}
                override fun onGpsOpenStatus(enabled: Boolean) {
                    Log.d("CruiseMode", "GPS状态: ${if (enabled) "已开启" else "已关闭"}")
                }
                override fun updateCameraInfo(cameraInfos: Array<out AMapNaviCameraInfo>?) {}
                override fun onServiceAreaUpdate(serviceAreaInfos: Array<out AMapServiceAreaInfo>?) {}
                override fun OnUpdateTrafficFacility(trafficFacilityInfo: AMapNaviTrafficFacilityInfo?) {}
                override fun showCross(cross: AMapNaviCross?) {}
                override fun hideCross() {}
                override fun showLaneInfo(laneInfos: Array<out AMapLaneInfo>?, laneBackgroundInfo: ByteArray?, laneRecommendedInfo: ByteArray?) {}
                override fun hideLaneInfo() {}
                override fun onCalculateRouteSuccess(routeIds: IntArray?) {}
                override fun notifyParallelRoad(p0: Int) {}
                override fun OnUpdateTrafficFacility(trafficFacilityInfos: Array<out AMapNaviTrafficFacilityInfo>?) {}
                override fun updateAimlessModeStatistics(stat: AimLessModeStat?) {}
                override fun updateAimlessModeCongestionInfo(congestionInfo: AimLessModeCongestionInfo?) {}
                override fun onPlayRing(type: Int) {}
                override fun onNaviInfoUpdate(naviInfo: NaviInfo?) {}
                override fun showModeCross(modelCross: AMapModelCross?) {}
                override fun hideModeCross() {}
                override fun updateIntervalCameraInfo(camera1: AMapNaviCameraInfo?, camera2: AMapNaviCameraInfo?, type: Int) {}
                override fun showLaneInfo(laneInfo: AMapLaneInfo?) {}
                override fun onCalculateRouteSuccess(result: AMapCalcRouteResult?) {}
                override fun onCalculateRouteFailure(result: AMapCalcRouteResult?) {}
                override fun onNaviRouteNotify(notifyData: AMapNaviRouteNotifyData?) {}
                override fun onGpsSignalWeak(weak: Boolean) {}
            })
            
            mAMapNavi?.setUseInnerVoice(true)
        } catch (e: Exception) {
            Log.e("CruiseMode", "初始化导航失败", e)
            Toast.makeText(this, "初始化导航失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun tryStartCruiseMode() {
        if (hasLocationPermission && isMapViewReady) {
            startCruiseMode()
        } else {
            Log.d("CruiseMode", "等待条件满足 - 权限: $hasLocationPermission, 地图: $isMapViewReady")
        }
    }
    
    private fun startCruiseMode() {
        try {
            if (isCruiseModeActive) {
                Log.d("CruiseMode", "巡航模式已启动，无需重复启动")
                return
            }

            mAMapNavi?.let { navi ->
                navi.startAimlessMode(com.amap.api.navi.enums.AimLessMode.CAMERA_AND_SPECIALROAD_DETECTED)
                isCruiseModeActive = true
                Toast.makeText(this, "智能巡航已开启", Toast.LENGTH_SHORT).show()
                Log.d("CruiseMode", "智能巡航模式已启动(CAMERA_AND_SPECIALROAD_DETECTED)")
            } ?: run {
                Toast.makeText(this, "导航实例未初始化", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("CruiseMode", "启动巡航模式失败", e)
            Toast.makeText(this, "启动巡航模式失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun onMapViewReady() {
        isMapViewReady = true
        Log.d("CruiseMode", "MapView已就绪")
        tryStartCruiseMode()
    }
    
    private fun stopCruiseMode() {
        try {
            if (!isCruiseModeActive) return
            mAMapNavi?.stopAimlessMode()
            isCruiseModeActive = false
            Log.d("CruiseMode", "智能巡航模式已停止")
        } catch (e: Exception) {
            Log.e("CruiseMode", "停止巡航模式失败", e)
            Toast.makeText(this, "停止巡航模式失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun setNavigationCallback(callback: (NavigationEvent) -> Unit) {
        navigationCallback = callback
    }
    
    fun triggerNavigationEvent(event: NavigationEvent) {
        navigationCallback?.invoke(event)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            stopCruiseMode()
            AMapNavi.destroy()
        } catch (e: Exception) {
            Log.e("CruiseMode", "销毁导航实例失败", e)
        }
    }
}
