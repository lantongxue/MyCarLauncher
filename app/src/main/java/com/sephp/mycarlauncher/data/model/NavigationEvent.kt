package com.sephp.mycarlauncher.data.model

/**
 * 导航事件类型
 */
sealed class NavigationEvent {
    data object ArriveDestination : NavigationEvent()
    data object StopNavigation : NavigationEvent()
}
