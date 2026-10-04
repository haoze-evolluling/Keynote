package com.haoze.keynote.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 打开主界面（首页 tab，含底部导航栏）的回调。
 * 页面顶栏 menu 按钮通过它回到主界面；在 AppPage 中注入。
 */
val LocalOpenMainNav = staticCompositionLocalOf<(() -> Unit)?> { null }
