package com.haoze.keynote.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.haoze.keynote.R

/**
 * 悬浮底栏目标项定义（参考谛听 DITING 项目架构）。
 */
enum class BottomBarDestination(
    val id: String,
    val title: String,
    val tabLabel: String,
    val description: String,
    @param:DrawableRes val iconRes: Int
) {
    AI_CHAT(
        id = "ai_chat",
        title = "AI 对话",
        tabLabel = "AI 对话",
        description = "日常对话与智能助手",
        iconRes = R.drawable.ic_psychology_outlined
    ),
    FEATURE_CENTER(
        id = "feature_center",
        title = "功能中心",
        tabLabel = "功能中心",
        description = "全部功能与设置入口",
        iconRes = R.drawable.ic_apps
    ),
    NOTES(
        id = "notes",
        title = "笔记",
        tabLabel = "笔记",
        description = "查看、搜索和编辑全部笔记",
        iconRes = R.drawable.ic_description
    ),
    BILL(
        id = "bill",
        title = "记账",
        tabLabel = "记账",
        description = "记录收入、支出和分类",
        iconRes = R.drawable.ic_receipt
    ),
    SCHEDULE(
        id = "schedule",
        title = "日程",
        tabLabel = "日程",
        description = "管理日期、地点和提醒",
        iconRes = R.drawable.ic_event
    ),
    TODO(
        id = "todo",
        title = "待办",
        tabLabel = "待办",
        description = "跟踪任务状态和截止时间",
        iconRes = R.drawable.ic_check_box
    );

    @Composable
    fun painter(): Painter = painterResource(iconRes)

    companion object {
        const val MIN_COUNT = 2
        const val MAX_COUNT = 4

        val DEFAULT_DESTINATIONS: List<BottomBarDestination> = listOf(AI_CHAT, FEATURE_CENTER)

        fun fromId(id: String): BottomBarDestination? = entries.firstOrNull { it.id == id }
    }
}
