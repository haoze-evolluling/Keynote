package com.haoze.keynote.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.haoze.keynote.R

/**
 * 悬浮底栏目标项定义。
 */
enum class BottomBarDestination(
    val tabLabel: String,
    @param:DrawableRes val iconRes: Int
) {
    AI_CHAT(
        tabLabel = "AI 对话",
        iconRes = R.drawable.ic_psychology_outlined
    ),
    FEATURE_CENTER(
        tabLabel = "功能中心",
        iconRes = R.drawable.ic_apps
    );

    @Composable
    fun painter(): Painter = painterResource(iconRes)

    companion object {
        val DEFAULT_DESTINATIONS: List<BottomBarDestination> = listOf(AI_CHAT, FEATURE_CENTER)
    }
}
