package com.haoze.keynote.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.haoze.keynote.ui.bill.AaSplitScreen
import com.haoze.keynote.ui.bill.BillScreen
import com.haoze.keynote.ui.bill.BillStatsScreen
import com.haoze.keynote.ui.chat.AIChatScreen
import com.haoze.keynote.ui.edit.EditNoteScreen
import com.haoze.keynote.ui.habit.HabitScreen
import com.haoze.keynote.ui.home.DateGroupNotesScreen
import com.haoze.keynote.ui.home.ExportDataScreen
import com.haoze.keynote.ui.home.FeatureCenterScreen
import com.haoze.keynote.ui.home.HomeScreen
import com.haoze.keynote.ui.schedule.ScheduleScreen
import com.haoze.keynote.ui.settings.AiProviderManageScreen
import com.haoze.keynote.ui.settings.SettingsScreen
import com.haoze.keynote.ui.tag.TagNotesScreen
import com.haoze.keynote.ui.todo.TodoScreen
import com.haoze.keynote.ui.toolbox.KnowledgeVaultScreen
import com.haoze.keynote.ui.trash.TrashScreen
import com.haoze.keynote.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

sealed class Screen(val route: String, val title: String) {
    // 主界面路由：首页现为 AI 对话页（嵌入主界面 Pager，带底部导航栏）
    data object FeatureHome : Screen("feature_home", "首页")
    data object Home : Screen("home", "笔记")
    data object EditNote : Screen("edit_note", "编辑笔记")
    data object Bill : Screen("bill", "记账")
    data object BillStats : Screen("bill_stats", "账单统计")
    data object AaSplit : Screen("aa_split", "AA计算")
    data object Habit : Screen("habit", "习惯打卡")
    data object Settings : Screen("settings", "设置")
    data object AiProviderManage : Screen("ai_provider_manage", "AI厂商管理")
    data object Todo : Screen("todo", "待办事项")
    data object TagNotes : Screen("tag_notes", "标签笔记")
    data object KnowledgeVault : Screen("knowledge_vault", "资料库")
    data object DateGroupNotes : Screen("date_group_notes", "按日期查看")
    data object Trash : Screen("trash", "回收站")
    data object DataExport : Screen("data_export", "数据导出")
    data object Schedule : Screen("schedule", "日程")
}

@Composable
fun AppPage(
    route: String,
    noteId: Long? = null,
    tagId: Long? = null,
    tagName: String? = null,
    initialPage: Int = 0,
    onNavigate: (String, Long?, Long?, String?, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    // 页面顶栏 menu 按钮：回到主界面（底部导航栏所在的功能中心）
    CompositionLocalProvider(
        LocalOpenMainNav provides { onNavigate(Screen.FeatureHome.route, null, null, null, false) }
    ) {
        when (route) {
            Screen.FeatureHome.route -> MainHomeContent(
                initialPage = initialPage,
                onNavigateToRoute = { target -> onNavigate(target, null, null, null, false) },
                onNavigateToTag = { id, name -> onNavigate(Screen.TagNotes.route, null, id, name, false) },
                onNavigateToEditNote = { id -> onNavigate(Screen.EditNote.route, id, null, null, false) }
            )
            Screen.Home.route -> HomeScreen(
                onNavigateToEdit = { id -> onNavigate(Screen.EditNote.route, id, null, null, false) },
                onNavigateToTagNotes = { id, name -> onNavigate(Screen.TagNotes.route, null, id, name, false) },
                onBack = onBack
            )
            Screen.EditNote.route -> noteId?.let { EditNoteScreen(it, onNavigateBack = onBack) }
            Screen.Bill.route -> BillScreen(onBack = onBack)
            Screen.BillStats.route -> BillStatsScreen(onBack = onBack)
            Screen.AaSplit.route -> AaSplitScreen(onBack = onBack)
            Screen.Habit.route -> HabitScreen(viewModel = koinViewModel(), onBack = onBack)
            Screen.DateGroupNotes.route -> DateGroupNotesScreen(
                onNavigateToEdit = { id -> onNavigate(Screen.EditNote.route, id, null, null, false) },
                onNavigateToTagNotes = { id, name -> onNavigate(Screen.TagNotes.route, null, id, name, false) },
                onBack = onBack
            )
            Screen.Trash.route -> TrashScreen(onBack = onBack)
            Screen.DataExport.route -> ExportDataScreen(onBack = onBack)
            Screen.Todo.route -> TodoScreen(viewModel = koinViewModel(), onBack = onBack)
            Screen.Schedule.route -> ScheduleScreen(viewModel = koinViewModel(), onBack = onBack)
            Screen.KnowledgeVault.route -> KnowledgeVaultScreen(onBack = onBack)
            Screen.Settings.route -> SettingsScreen(
                onNavigateToProviderManage = {
                    onNavigate(Screen.AiProviderManage.route, null, null, null, false)
                },
                onBack = onBack
            )
            Screen.AiProviderManage.route -> ProviderManagePage(onBack)
            Screen.TagNotes.route -> if (tagId != null) TagNotesScreen(
                tagId = tagId,
                tagName = tagName.orEmpty(),
                onNavigateToEdit = { id -> id?.let { onNavigate(Screen.EditNote.route, it, null, null, false) } },
                onBack = onBack
            )
        }
    }
}

/**
 * 主界面：双页结构（AI 对话 / 功能中心），
 * HorizontalPager 承载两页，底部悬浮 FloatingNavigationBar 联动切换。
 * 首页即 AI 对话页，避免与功能中心的导航入口重复。
 */
@Composable
private fun MainHomeContent(
    initialPage: Int = 0,
    onNavigateToRoute: (String) -> Unit,
    onNavigateToTag: (Long, String) -> Unit,
    onNavigateToEditNote: (Long) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { 2 }
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage == 1) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                page = 0,
                animationSpec = tween(durationMillis = 280)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page == 0) {
                // 首页 = AI 对话页；输入框内部已为底部悬浮导航栏预留空间
                AIChatScreen(
                    onCreateNote = onNavigateToEditNote
                )
            } else {
                FeatureCenterScreen(
                    onNavigateToRoute = onNavigateToRoute,
                    onNavigateToTag = onNavigateToTag
                )
            }
        }

        val density = LocalDensity.current
        val isImeVisible = WindowInsets.ime.getBottom(density) > 0

        AnimatedVisibility(
            visible = !isImeVisible,
            enter = fadeIn(animationSpec = tween(150)) + slideInVertically(animationSpec = tween(150)) { it / 2 },
            exit = fadeOut(animationSpec = tween(100)) + slideOutVertically(animationSpec = tween(100)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            FloatingNavigationBar(
                currentPage = pagerState.currentPage,
                onPageSelected = { targetPage ->
                    if (pagerState.currentPage != targetPage) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = targetPage,
                                animationSpec = tween(durationMillis = 280)
                            )
                        }
                    }
                },
                pagerProgress = { pagerState.currentPage + pagerState.currentPageOffsetFraction }
            )
        }
    }
}

@Composable
private fun ProviderManagePage(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = koinViewModel()
    val providers = viewModel.providers.collectAsState().value
    val activeProviderId = viewModel.activeProviderId.collectAsState().value
    AiProviderManageScreen(
        onNavigateBack = onBack,
        providers = providers,
        activeProviderId = activeProviderId,
        onSelectProvider = viewModel::selectProvider,
        onUpdateProvider = viewModel::updateProvider,
        onDeleteProvider = viewModel::deleteCustomProvider,
        onAddProvider = { name, url, model, key -> viewModel.addCustomProvider(name, url, model, key) },
        sealKey = viewModel::sealZidaipass,
        openKey = viewModel::openZidaipass
    )
}
