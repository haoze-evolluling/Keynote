package com.haoze.keynote.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.haoze.keynote.R
import com.haoze.keynote.data.remote.AiProvider
import com.haoze.keynote.data.remote.AiProviderPreset
import com.haoze.keynote.ui.components.AppAlertDialog as AlertDialog
import com.haoze.keynote.ui.components.AppConfirmDialog
import com.haoze.keynote.ui.components.AppDialogButton
import com.haoze.keynote.ui.components.SettingsDivider
import com.haoze.keynote.ui.components.SettingsGroup
import com.haoze.keynote.ui.components.SettingsGroupTitle
import com.haoze.keynote.ui.components.SettingsInfoText
import com.haoze.keynote.ui.components.SettingsItem
import com.haoze.keynote.ui.components.SettingsScaffold
import com.haoze.keynote.ui.theme.DialogContent
import com.haoze.keynote.ui.theme.ModalTokens
import com.haoze.keynote.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * AI 厂商管理：上半部分是用户已配置的「我的厂商」，下半部分是内置预设厂商目录。
 * 预设只提供接口地址，模型一律在编辑框里用 API Key 现场向厂商 `GET /models` 拉取后选择。
 */
@Composable
fun AiProviderManageScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val providers by viewModel.providers.collectAsState()
    val activeProviderId by viewModel.activeProviderId.collectAsState()
    val presets = viewModel.providerPresets
    val scope = rememberCoroutineScope()

    var draft by remember { mutableStateOf<ProviderDraft?>(null) }
    var pendingDelete by remember { mutableStateOf<AiProvider?>(null) }
    var fetchedModelIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var fetchingModels by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 目录按分组连续展示，groupBy 保留插入顺序
    val presetsByGroup = presets.groupBy { it.group }.toList()
    val addedPresetIds = providers.mapTo(mutableSetOf()) { it.presetId }

    val openDraft: (ProviderDraft) -> Unit = { target ->
        fetchedModelIds = emptyList()
        draft = target
    }

    SettingsScaffold(
        title = "AI 厂商管理",
        onBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(onClick = { openDraft(ProviderDraft()) }) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "添加自定义厂商")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                SettingsInfoText("点选下方预设厂商会预填接口地址；模型不预置，填好 API Key 后在编辑框点「拉取模型列表」再选择。编辑时 API Key 留空会保留原密钥。")
            }
            item {
                SettingsGroupTitle("我的厂商")
                SettingsGroup {
                    if (providers.isEmpty()) {
                        SettingsItem(
                            title = "暂无厂商",
                            subtitle = "从下方预设厂商目录点选，或点击右上角添加自定义厂商",
                            enabled = false
                        )
                    } else {
                        providers.forEachIndexed { index, provider ->
                            ProviderRow(
                                provider = provider,
                                isActive = provider.id == activeProviderId,
                                onSelect = { viewModel.selectProvider(provider.id) },
                                onEdit = {
                                    openDraft(
                                        ProviderDraft(
                                            existing = provider,
                                            preset = presets.find { it.id == provider.presetId }
                                        )
                                    )
                                },
                                onDelete = { pendingDelete = provider }
                            )
                            if (index < providers.lastIndex) {
                                SettingsDivider()
                            }
                        }
                    }
                }
            }
            presetsByGroup.forEach { (group, entries) ->
                item {
                    SettingsGroupTitle(group)
                    SettingsGroup {
                        entries.forEachIndexed { index, preset ->
                            PresetRow(
                                preset = preset,
                                isAdded = preset.id in addedPresetIds,
                                onAdd = { openDraft(ProviderDraft(preset = preset)) }
                            )
                            if (index < entries.lastIndex) {
                                SettingsDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { provider ->
        AppConfirmDialog(
            onDismissRequest = { pendingDelete = null },
            title = "删除厂商",
            message = "确定要删除“${provider.name}”吗？相关 API Key 也会从本机移除。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                viewModel.deleteProvider(provider.id)
                pendingDelete = null
            }
        )
    }

    draft?.let { currentDraft ->
        ProviderEditorDialog(
            draft = currentDraft,
            initialApiKey = currentDraft.existing?.let { viewModel.revealApiKey(it) }.orEmpty(),
            fetchedModelIds = fetchedModelIds,
            fetchingModels = fetchingModels,
            onFetchModels = { baseUrl, plainKey ->
                fetchingModels = true
                viewModel.fetchModels(
                    baseUrl = baseUrl,
                    plainKey = plainKey,
                    onResult = { ids ->
                        fetchedModelIds = ids
                        fetchingModels = false
                        scope.launch { snackbarHostState.showSnackbar("已获取 ${ids.size} 个可用模型") }
                    },
                    onError = { message ->
                        fetchingModels = false
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
                )
            },
            onDismiss = { draft = null },
            onSave = { name, baseUrl, modelName, plainKey ->
                val existing = currentDraft.existing
                val presetId = existing?.presetId ?: currentDraft.preset?.id ?: ""
                if (existing == null) {
                    viewModel.addProvider(name, baseUrl, modelName, presetId, plainKey)
                } else {
                    viewModel.updateProvider(existing.id, name, baseUrl, modelName, presetId, plainKey)
                    viewModel.selectProvider(existing.id)
                }
                draft = null
            }
        )
    }
}

/** 编辑器要承载的三种入口：改已配置厂商、点预设新建、全手动新建。 */
private data class ProviderDraft(
    val existing: AiProvider? = null,
    val preset: AiProviderPreset? = null
) {
    val initialName: String get() = existing?.name ?: preset?.name ?: ""
    val initialBaseUrl: String get() = existing?.baseUrl ?: preset?.baseUrl ?: ""
    val initialModelName: String get() = existing?.modelName ?: ""
}

@Composable
private fun ProviderRow(
    provider: AiProvider,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SettingsItem(
        title = provider.name.ifBlank { "未命名厂商" },
        subtitle = buildString {
            append(provider.baseUrl.ifBlank { "未设置基础地址" })
            append("\n")
            append(provider.modelName.ifBlank { "未选择模型" })
        },
        leadingIcon = painterResource(R.drawable.ic_psychology_outlined),
        onClick = onSelect,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Icon(
                        painterResource(R.drawable.ic_check),
                        contentDescription = "当前厂商",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        painterResource(R.drawable.ic_edit),
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        painterResource(R.drawable.ic_delete),
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

@Composable
private fun PresetRow(
    preset: AiProviderPreset,
    isAdded: Boolean,
    onAdd: () -> Unit
) {
    SettingsItem(
        title = preset.name,
        subtitle = (if (isAdded) "已添加 · " else "") + preset.baseUrl,
        leadingIcon = painterResource(R.drawable.ic_apps),
        onClick = onAdd,
        trailing = {
            IconButton(onClick = onAdd) {
                Icon(
                    painterResource(R.drawable.ic_add),
                    contentDescription = "添加${preset.name}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
private fun ProviderEditorDialog(
    draft: ProviderDraft,
    initialApiKey: String,
    fetchedModelIds: List<String>,
    fetchingModels: Boolean,
    onFetchModels: (baseUrl: String, plainKey: String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (name: String, baseUrl: String, modelName: String, plainKey: String) -> Unit
) {
    var name by remember(draft) { mutableStateOf(draft.initialName) }
    var baseUrl by remember(draft) { mutableStateOf(draft.initialBaseUrl) }
    var modelName by remember(draft) { mutableStateOf(draft.initialModelName) }
    var apiKey by remember(draft) { mutableStateOf(initialApiKey) }
    var showKey by remember(draft) { mutableStateOf(false) }

    // 旧配置已选的模型排在最前，保证始终看得见当前取值；不跟随输入框实时变化
    val modelChoices = remember(fetchedModelIds, draft) {
        (listOf(draft.initialModelName).filter { it.isNotBlank() } + fetchedModelIds).distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    draft.existing != null -> "编辑厂商"
                    draft.preset != null -> "添加 ${draft.preset.name}"
                    else -> "添加自定义厂商"
                }
            )
        },
        text = {
            DialogContent(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("厂商名称") },
                    singleLine = true,
                    shape = ModalTokens.innerShape,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("基础地址") },
                    singleLine = true,
                    shape = ModalTokens.innerShape,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://api.example.com/v1") }
                )
                if (modelChoices.isNotEmpty()) {
                    Text(
                        text = "可用模型",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        modelChoices.forEach { id ->
                            FilterChip(
                                selected = id == modelName,
                                onClick = { modelName = id },
                                label = { Text(id, style = MaterialTheme.typography.labelLarge) },
                                shape = ModalTokens.innerShape
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("模型名称") },
                    singleLine = true,
                    shape = ModalTokens.innerShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(if (modelChoices.isEmpty()) "填好 Key 后点下方拉取，或手动填写" else "接口要求的 model 取值")
                    }
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    shape = ModalTokens.innerShape,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) painterResource(R.drawable.ic_visibility) else painterResource(R.drawable.ic_visibility_off),
                                contentDescription = if (showKey) "隐藏" else "显示"
                            )
                        }
                    }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppDialogButton(
                        label = if (fetchingModels) "拉取中…" else "拉取模型列表",
                        onClick = { onFetchModels(baseUrl, apiKey) },
                        enabled = !fetchingModels
                    )
                    if (fetchingModels) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    }
                }
            }
        },
        confirmButton = {
            AppDialogButton(
                label = "保存",
                onClick = { onSave(name.trim(), baseUrl.trim(), modelName.trim(), apiKey.trim()) },
                enabled = name.isNotBlank() && baseUrl.isNotBlank() && modelName.isNotBlank()
            )
        },
        dismissButton = {
            AppDialogButton(label = "取消", onClick = onDismiss)
        }
    )
}
