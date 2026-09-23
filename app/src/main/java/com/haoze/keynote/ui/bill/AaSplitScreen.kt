package com.haoze.keynote.ui.bill

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.haoze.keynote.data.db.entity.AaSplitEntity
import com.haoze.keynote.ui.components.SettingsDivider
import com.haoze.keynote.ui.components.SettingsGroup
import com.haoze.keynote.ui.components.SettingsGroupTitle
import com.haoze.keynote.ui.components.SettingsScaffold
import androidx.compose.foundation.shape.RoundedCornerShape
import com.haoze.keynote.ui.theme.DialogContent
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.ModalTokens
import com.haoze.keynote.ui.theme.SpacingTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.painterResource
import com.haoze.keynote.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AaSplitScreen(
    onBack: () -> Unit = {},
    viewModel: AaSplitViewModel = koinViewModel()
) {
    val colors = LocalAppColors.current
    val aaSplits by viewModel.aaSplits.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf<AaSplitEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<AaSplitEntity?>(null) }

    SettingsScaffold(
        title = "AA 计算",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
            ) { Icon(painterResource(R.drawable.ic_add), contentDescription = "新建AA计算") }
        }
    ) { innerPadding ->
        if (aaSplits.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text("暂无AA计算记录", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant) }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    SettingsGroupTitle("分摊记录 (${aaSplits.size})")
                    SettingsGroup {
                        aaSplits.forEachIndexed { index, aaSplit ->
                            AaSplitCard(
                                aaSplit = aaSplit,
                                dateFormat = dateFormat,
                                onClick = { showDetailDialog = aaSplit }
                            )
                            if (index < aaSplits.lastIndex) {
                                SettingsDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AaSplitCreateDialog(
            colors = colors,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, totalAmount, personCount, note ->
                viewModel.createAaSplit(title, totalAmount, personCount, note)
                showCreateDialog = false
            }
        )
    }

    if (showDetailDialog != null) {
        val aaSplit = showDetailDialog!!
        AlertDialog(
            onDismissRequest = { showDetailDialog = null },
            title = { Text(aaSplit.title) },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
            text = {
                DialogContent(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("总金额", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                    Text("¥${String.format("%.2f", aaSplit.totalAmount)}", style = ModalTokens.bodyTextStyle, fontWeight = FontWeight.Bold)
                    Text("参与人数", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                    Text("${aaSplit.personCount}人", style = ModalTokens.bodyTextStyle)
                    Text("人均费用", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                    Text("¥${String.format("%.2f", aaSplit.perPersonAmount)}", style = ModalTokens.bodyTextStyle, fontWeight = FontWeight.Bold, color = colors.primary)
                    Text("时间", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                    Text(dateFormat.format(Date(aaSplit.date)), style = ModalTokens.bodyTextStyle)
                    if (!aaSplit.note.isNullOrBlank()) {
                        Text("备注", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                        Text(aaSplit.note, style = ModalTokens.bodyTextStyle)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailDialog = null }) { Text("关闭") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = aaSplit; showDetailDialog = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                ) { Text("删除") }
            }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除记录") },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
            text = { Text("确定要删除这条AA计算记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteAaSplit(showDeleteConfirm!!); showDeleteConfirm = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AaSplitCard(
    aaSplit: AaSplitEntity,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                aaSplit.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "¥${String.format("%.2f", aaSplit.perPersonAmount)}/人",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "共${aaSplit.personCount}人 · 总计¥${String.format("%.2f", aaSplit.totalAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Text(
                dateFormat.format(Date(aaSplit.date)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AaSplitCreateDialog(
    colors: com.haoze.keynote.ui.theme.AppColors,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var personCount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val totalAmountValue = totalAmount.toDoubleOrNull()
    val personCountValue = personCount.toIntOrNull()
    val perPersonPreview = if (totalAmountValue != null && personCountValue != null && personCountValue > 0) {
        String.format("%.2f", totalAmountValue / personCountValue)
    } else "--"

    val isValid = totalAmountValue != null && totalAmountValue >= 0 &&
            personCountValue != null && personCountValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建AA计算") },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        textContentColor = colors.onSurface,
        shape = RoundedCornerShape(28.dp),
        text = {
            DialogContent(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = { totalAmount = it },
                    label = { Text("总金额") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("¥") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = personCount,
                    onValueChange = { personCount = it },
                    label = { Text("参与人数") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text("人") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "人均：¥$perPersonPreview",
                    style = ModalTokens.bodyTextStyle,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        onConfirm(title.takeIf { it.isNotBlank() } ?: "AA计算", totalAmountValue!!, personCountValue!!, note.takeIf { it.isNotBlank() })
                    }
                },
                enabled = isValid
            ) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
