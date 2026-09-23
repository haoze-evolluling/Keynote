package com.haoze.keynote.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.haoze.keynote.R
import com.haoze.keynote.ui.components.SettingsDivider
import com.haoze.keynote.ui.components.SettingsGroup
import com.haoze.keynote.ui.components.SettingsGroupTitle
import com.haoze.keynote.ui.components.SettingsInfoText
import com.haoze.keynote.ui.components.SettingsItem
import com.haoze.keynote.ui.components.SettingsNavigationItem
import com.haoze.keynote.ui.components.SettingsRadioItem
import com.haoze.keynote.ui.components.SettingsScaffold
import com.haoze.keynote.ui.navigation.LocalOpenMainNav
import com.haoze.keynote.ui.theme.DarkModePreference
import com.haoze.keynote.viewmodel.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onNavigateToProviderManage: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel()
) {
    val providers by viewModel.providers.collectAsState()
    val activeProviderId by viewModel.activeProviderId.collectAsState()
    val noteFontSize by viewModel.noteFontSize.collectAsState()
    val darkModePreference by viewModel.darkModePreference.collectAsState()
    val activeProvider = providers.find { it.id == activeProviderId }
    val scrollState = rememberScrollState()

    SettingsScaffold(
        title = "设置",
        onBack = onBack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            SettingsGroupTitle("外观")
            SettingsGroup {
                SettingsRadioItem(
                    title = "跟随系统",
                    subtitle = "根据系统深色模式自动切换",
                    leadingIcon = painterResource(R.drawable.ic_settings_brightness),
                    selected = darkModePreference == DarkModePreference.SYSTEM,
                    onClick = { viewModel.setDarkMode(DarkModePreference.SYSTEM) }
                )
                SettingsDivider()
                SettingsRadioItem(
                    title = "始终浅色",
                    subtitle = "固定使用浅色界面",
                    leadingIcon = painterResource(R.drawable.ic_light_mode),
                    selected = darkModePreference == DarkModePreference.LIGHT,
                    onClick = { viewModel.setDarkMode(DarkModePreference.LIGHT) }
                )
                SettingsDivider()
                SettingsRadioItem(
                    title = "始终深色",
                    subtitle = "固定使用深色界面",
                    leadingIcon = painterResource(R.drawable.ic_dark_mode),
                    selected = darkModePreference == DarkModePreference.DARK,
                    onClick = { viewModel.setDarkMode(DarkModePreference.DARK) }
                )
            }

            SettingsGroupTitle("笔记")
            SettingsGroup {
                SettingsItem(
                    title = "正文字体大小",
                    subtitle = "影响笔记编辑与阅读时的正文显示",
                    leadingIcon = painterResource(R.drawable.ic_text_fields),
                    trailing = {
                        Text(
                            text = "${noteFontSize}sp",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Slider(
                    value = noteFontSize.toFloat(),
                    onValueChange = { viewModel.setNoteFontSize(it.toInt()) },
                    valueRange = 12f..28f,
                    steps = 7,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
            SettingsInfoText("字号会即时应用到笔记内容，不影响标题和系统导航文字。")

            SettingsGroupTitle("AI 配置")
            SettingsGroup {
                SettingsNavigationItem(
                    title = "AI 厂商管理",
                    subtitle = "选择、添加或编辑 AI 服务厂商",
                    value = activeProvider?.name ?: "未设置",
                    leadingIcon = painterResource(R.drawable.ic_auto_awesome),
                    onClick = onNavigateToProviderManage
                )
            }
            SettingsInfoText("API Key 仅保存在本机；导出厂商配置时请注意明文密钥风险。")
        }
    }
}
