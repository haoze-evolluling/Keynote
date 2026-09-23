package com.haoze.keynote.ui.chat

enum class AssistantType { CHAT, BILL, PLANNER }

internal data class AssistantUiText(
    val menuLabel: String,
    val subtitle: String,
    val welcomeTitle: String,
    val quickPrompts: List<String>,
    val inputPlaceholder: String
)

internal fun assistantUiText(type: AssistantType): AssistantUiText {
    return when (type) {
        AssistantType.CHAT -> AssistantUiText(
            menuLabel = "日常对话",
            subtitle = "日常对话 · 陪你聊天、解释、出主意",
            welcomeTitle = "想聊什么都可以",
            quickPrompts = listOf(
                "今天有点累，陪我聊聊",
                "帮我把这件事想清楚",
                "给我一个周末放松计划"
            ),
            inputPlaceholder = "和 KeyNote 聊聊"
        )

        AssistantType.BILL -> AssistantUiText(
            menuLabel = "账单助手",
            subtitle = "账单助手 · 记账、分类、消费分析",
            welcomeTitle = "把消费说给我听",
            quickPrompts = listOf(
                "晚餐花了 15 块钱",
                "打车 20 块去公司",
                "这个月饮食支出怎么控制？"
            ),
            inputPlaceholder = "记录消费或询问账单"
        )

        AssistantType.PLANNER -> AssistantUiText(
            menuLabel = "日程待办",
            subtitle = "日程待办 · 安排日程、创建待办",
            welcomeTitle = "把安排交给我整理",
            quickPrompts = listOf(
                "提醒我明天早上 10 点上体育课",
                "下周一 9 点项目复盘",
                "添加一个高优先级待办：完成周报"
            ),
            inputPlaceholder = "记录日程或待办"
        )
    }
}
