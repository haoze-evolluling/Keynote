package com.haoze.keynote.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.haoze.keynote.R
import com.haoze.keynote.data.db.KeyNoteDatabase
import com.haoze.keynote.ui.common.ActionMenuDialog
import com.haoze.keynote.ui.components.SettingsDivider
import com.haoze.keynote.ui.components.SettingsGroup
import com.haoze.keynote.ui.components.SettingsGroupTitle
import com.haoze.keynote.ui.components.SettingsNavigationItem
import com.haoze.keynote.ui.components.SettingsScaffold
import com.haoze.keynote.ui.navigation.Screen

/**
 * 功能中心：承接原侧边栏抽屉的全部导航入口。
 * 与「首页」组成主界面双页 Pager，通过底部悬浮导航栏切换。
 */
@Composable
fun FeatureCenterScreen(
    onNavigateToRoute: (String) -> Unit,
    onNavigateToTag: (Long, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val tags by remember {
        KeyNoteDatabase.getDatabase(context).tagDao().getActiveTags()
    }.collectAsState(initial = emptyList())
    var showTagDialog by remember { mutableStateOf(false) }

    SettingsScaffold(
        title = "功能中心",
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            SettingsGroupTitle("笔记")
            SettingsGroup {
                SettingsNavigationItem(
                    title = "笔记列表",
                    subtitle = "查看、搜索和编辑全部笔记",
                    leadingIcon = painterResource(R.drawable.ic_description),
                    onClick = { onNavigateToRoute(Screen.Home.route) }
                )
                SettingsDivider()
                SettingsNavigationItem(
                    title = "按日期查看",
                    subtitle = "按更新时间回看笔记内容",
                    leadingIcon = painterResource(R.drawable.ic_date_range),
                    onClick = { onNavigateToRoute(Screen.DateGroupNotes.route) }
                )
                SettingsDivider()
                SettingsNavigationItem(
                    title = "标签分类",
                    subtitle = "按标签筛选查看笔记",
                    leadingIcon = painterResource(R.drawable.ic_label_mirrored),
                    onClick = { showTagDialog = true }
                )
                SettingsDivider()
                SettingsNavigationItem(
                    title = "资料库",
                    subtitle = "沉淀长期资料和知识条目",
                    leadingIcon = painterResource(R.drawable.ic_local_library),
                    onClick = { onNavigateToRoute(Screen.KnowledgeVault.route) }
                )
            }

            SettingsGroupTitle("日程")
            SettingsGroup {
                SettingsNavigationItem(
                    title = "日程列表",
                    subtitle = "管理日期、地点和提醒",
                    leadingIcon = painterResource(R.drawable.ic_event),
                    onClick = { onNavigateToRoute(Screen.Schedule.route) }
                )
                SettingsDivider()
                SettingsNavigationItem(
                    title = "待办事项",
                    subtitle = "跟踪任务状态和截止时间",
                    leadingIcon = painterResource(R.drawable.ic_check_box),
                    onClick = { onNavigateToRoute(Screen.Todo.route) }
                )
                SettingsDivider()
                SettingsNavigationItem(
                    title = "习惯打卡",
                    subtitle = "记录长期习惯和完成情况",
                    leadingIcon = painterResource(R.drawable.ic_fitness_center),
                    onClick = { onNavigateToRoute(Screen.Habit.route) }
                )
            }

            SettingsGroupTitle("记账")
            SettingsGroup {
                SettingsNavigationItem(
                    title = "记账",
                    subtitle = "记录收入、支出和分类",
                    leadingIcon = painterResource(R.drawable.ic_receipt),
                    onClick = { onNavigateToRoute(Screen.Bill.route) }
                )
                SettingsDivider()
                SettingsNavigationItem(
                    title = "统计",
                    subtitle = "查看账单趋势和分类占比",
                    leadingIcon = painterResource(R.drawable.ic_assessment),
                    onClick = { onNavigateToRoute(Screen.BillStats.route) }
                )
                SettingsDivider()
                SettingsNavigationItem(
                    title = "AA 计算",
                    subtitle = "管理多人分摊记录",
                    leadingIcon = painterResource(R.drawable.ic_people),
                    onClick = { onNavigateToRoute(Screen.AaSplit.route) }
                )
            }


            SettingsGroupTitle("系统")
            SettingsGroup {
                SettingsNavigationItem(
                    title = "导出数据",
                    subtitle = "备份笔记、日程和账单数据",
                    leadingIcon = painterResource(R.drawable.ic_file_download),
                    onClick = { onNavigateToRoute(Screen.DataExport.route) }
                )
                SettingsDivider()
                SettingsNavigationItem(
                    title = "回收站",
                    subtitle = "恢复或彻底删除已移除内容",
                    leadingIcon = painterResource(R.drawable.ic_delete),
                    onClick = { onNavigateToRoute(Screen.Trash.route) }
                )
                SettingsDivider()
                SettingsNavigationItem(
                    title = "设置",
                    subtitle = "主题、AI 厂商和应用偏好",
                    leadingIcon = painterResource(R.drawable.ic_settings),
                    onClick = { onNavigateToRoute(Screen.Settings.route) }
                )
            }

            // 底部留白，避免最后条目被悬浮导航栏遮挡
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.padding(bottom = 108.dp)
            )
        }
    }

    if (showTagDialog) {
        ActionMenuDialog(
            title = "标签分类",
            onDismiss = { showTagDialog = false }
        ) {
            if (tags.isEmpty()) {
                Text(
                    "暂无标签",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            } else {
                tags.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showTagDialog = false
                                onNavigateToTag(tag.id, tag.name)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "#${tag.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
