package com.sephp.mycarlauncher.data.model

/**
 * 路线信息数据类
 */
data class RoutePathInfo(
    val routeId: Int,
    val length: Int, // 总里程（米）
    val time: Int, // 预计时间（秒）
    val tollCost: Int, // 过路费（元）
    val strategyName: String // 路线名称
)
