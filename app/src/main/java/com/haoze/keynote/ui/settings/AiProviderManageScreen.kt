package com.haoze.keynote.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.haoze.keynote.R
import com.haoze.keynote.data.remote.AiProvider
import com.haoze.keynote.ui.components.SettingsCornerShape
import com.haoze.keynote.ui.components.SettingsDivider
import com.haoze.keynote.ui.components.SettingsGroup
import com.haoze.keynote.ui.components.SettingsGroupTitle
import com.haoze.keynote.ui.components.SettingsInfoText
import com.haoze.keynote.ui.components.SettingsItem
import com.haoze.keynote.ui.components.SettingsScaffold
import com.haoze.keynote.ui.theme.DialogContent

@Composable
fun AiProviderManageScreen(
    onNavigateBack: () -> Unit,
    providers: List<AiProvider>,
    activeProviderId: String,
    onSelectProvider: (String) -> Unit,
    onUpdateProvider: (AiProvider) -> Unit,
    onDeleteProvider: (String) -> Unit,
    onAddProvider: (String, String, String, String) -> Unit,
    sealKey: (String) -> String,
    openKey: (String) -> String
) {
    var editingProvider by remember { mutableStateOf<AiProvider?>(null) }
    var pendingDelete by remember { mutableStateOf<AiProvider?>(null) }

    SettingsScaffold(
        title = "AI 厂商管理",
        onBack = onNavigateBack,
        actions = {
            IconButton(
                onClick = {
                    editingProvider = AiProvider("", "", "", modelName = "")
                }
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "添加")
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
                SettingsInfoText("点击厂商可设为当前服务；编辑 API Key 时留空会保留原密钥。")
            }
            item {
                SettingsGroupTitle("厂商列表")
                SettingsGroup {
                    if (providers.isEmpty()) {
                        SettingsItem(
                            title = "暂无厂商",
                            subtitle = "点击右上角添加自定义 AI 厂商",
                            enabled = false
                        )
                    } else {
                        providers.forEachIndexed { index, provider ->
                            ProviderRow(
                                provider = provider,
                                isActive = provider.id == activeProviderId,
                                onSelect = { onSelectProvider(provider.id) },
                                onEdit = { editingProvider = provider },
                                onDelete = { pendingDelete = provider }
                            )
                            if (index < providers.lastIndex) {
                                SettingsDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { provider ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除厂商") },
            text = { Text("确定要删除“${provider.name}”吗？相关 API Key 也会从本机移除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProvider(provider.id)
                        pendingDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    editingProvider?.let { provider ->
        val placeholder = "••••••••••••••••"
        ProviderEditDialog(
            provider = provider,
            openKey = openKey,
            onDismiss = { editingProvider = null },
            onSave = { name, baseUrl, modelName, apiKey ->
                if (provider.id.isEmpty()) {
                    val sealedKey = if (apiKey.isNotBlank() && apiKey != placeholder) {
                        sealKey(apiKey)
                    } else {
                        ""
                    }
                    onAddProvider(name, baseUrl, modelName, sealedKey)
                } else {
                    val sealedKey = when {
                        apiKey.isBlank() || apiKey == placeholder -> provider.apiKey
                        else -> sealKey(apiKey)
                    }
                    onUpdateProvider(
                        provider.copy(
                            name = name,
                            baseUrl = baseUrl,
                            modelName = modelName,
                            apiKey = sealedKey
                        )
                    )
                    onSelectProvider(provider.id)
                }
                editingProvider = null
            }
        )
    }
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
            if (provider.modelName.isNotBlank()) {
                append("\n")
                append(provider.modelName)
            }
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
private fun ProviderEditDialog(
    provider: AiProvider,
    openKey: (String) -> String,
    onDismiss: () -> Unit,
    onSave: (name: String, baseUrl: String, modelName: String, apiKey: String) -> Unit
) {
    val isNewMode = provider.id.isEmpty()
    var name by remember(provider.id) { mutableStateOf(provider.name) }
    var baseUrl by remember(provider.id) { mutableStateOf(provider.baseUrl) }
    var modelName by remember(provider.id) { mutableStateOf(provider.modelName) }
    val decryptedKey = if (provider.apiKey.isNotBlank() && !isNewMode) openKey(provider.apiKey) else ""
    var apiKey by remember(provider.id) { mutableStateOf(decryptedKey) }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNewMode) "添加自定义厂商" else "编辑厂商") },
        shape = RoundedCornerShape(28.dp),
        text = {
            DialogContent(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("厂商名称") },
                    singleLine = true,
                    shape = SettingsCornerShape,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("基础地址") },
                    singleLine = true,
                    shape = SettingsCornerShape,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://api.example.com/v1") }
                )
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("模型名称") },
                    singleLine = true,
                    shape = SettingsCornerShape,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    shape = SettingsCornerShape,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) painterResource(R.drawable.ic_visibility) else painterResource(R.drawable.ic_visibility_off),
                                contentDescription = if (showKey) "隐藏" else "显示"
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), baseUrl.trim(), modelName.trim(), apiKey) },
                enabled = name.isNotBlank() && baseUrl.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
