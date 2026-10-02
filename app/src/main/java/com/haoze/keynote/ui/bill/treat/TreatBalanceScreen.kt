package com.haoze.keynote.ui.bill.treat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.haoze.keynote.R
import com.haoze.keynote.data.db.entity.TreatLedgerEntity
import com.haoze.keynote.data.db.entity.TreatRecordEntity
import com.haoze.keynote.ui.components.AppConfirmDialog
import com.haoze.keynote.ui.components.SettingsDivider
import com.haoze.keynote.ui.components.SettingsGroup
import com.haoze.keynote.ui.components.SettingsGroupTitle
import com.haoze.keynote.ui.components.SettingsScaffold
import com.haoze.keynote.ui.theme.LocalAppColors
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatBalanceScreen(
    onBack: () -> Unit = {},
    viewModel: TreatBalanceViewModel = koinViewModel()
) {
    val colors = LocalAppColors.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val ledgers by viewModel.ledgers.collectAsState()
    val currentLedger by viewModel.currentLedger.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val recordsWithBalance by viewModel.recordsWithBalance.collectAsState()

    var showCreateRecordDialog by remember { mutableStateOf(false) }
    var showEditRecordDialog by remember { mutableStateOf<TreatRecordEntity?>(null) }
    var showRecordDetail by remember { mutableStateOf<TreatRecordWithBalance?>(null) }
    var showDeleteRecordConfirm by remember { mutableStateOf<TreatRecordEntity?>(null) }

    var showLedgerManageSheet by remember { mutableStateOf(false) }
    var showCreateLedgerDialog by remember { mutableStateOf(false) }
    var showEditLedgerDialog by remember { mutableStateOf<TreatLedgerEntity?>(null) }
    var showDeleteLedgerConfirm by remember { mutableStateOf<TreatLedgerEntity?>(null) }
    var showSettleConfirm by remember { mutableStateOf(false) }

    val personA = currentLedger?.personAName ?: "我"
    val personB = currentLedger?.personBName ?: "对方"
    val totalA = summary?.totalA ?: 0.0
    val totalB = summary?.totalB ?: 0.0

    SettingsScaffold(
        title = "双人请客平衡",
        onBack = onBack,
        actions = {
            IconButton(onClick = { showLedgerManageSheet = true }) {
                Icon(painterResource(R.drawable.ic_account_balance), contentDescription = "账本管理")
            }
            IconButton(onClick = { currentLedger?.let { showEditLedgerDialog = it } }) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = "编辑账本设置")
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateRecordDialog = true },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "记一笔请客")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
        ) {
            // 1. 概览平衡卡片
            item {
                summary?.let { sum ->
                    TreatBalanceHeaderCard(
                        summary = sum,
                        onSwitchLedger = { showLedgerManageSheet = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // 2. 轮流请客智能建议卡片
            item {
                summary?.let { sum ->
                    TreatSuggestionCard(
                        summary = sum,
                        onSettleClick = { showSettleConfirm = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // 3. 消费记录列表
            item {
                SettingsGroupTitle("请客记录 (${recordsWithBalance.size})")
                if (recordsWithBalance.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无请客记录，点击右下角按钮记一笔吧",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                } else {
                    SettingsGroup {
                        recordsWithBalance.forEachIndexed { index, item ->
                            TreatRecordItemCard(
                                item = item,
                                personAName = personA,
                                personBName = personB,
                                dateFormat = dateFormat,
                                onClick = { showRecordDetail = item },
                                onLongClick = { showDeleteRecordConfirm = item.record }
                            )
                            if (index < recordsWithBalance.lastIndex) {
                                SettingsDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    // 弹窗与动作逻辑
    if (showCreateRecordDialog) {
        TreatRecordFormDialog(
            personAName = personA,
            personBName = personB,
            currentTotalA = totalA,
            currentTotalB = totalB,
            onDismiss = { showCreateRecordDialog = false },
            onConfirm = { title, amount, payer, amtA, amtB, date, note ->
                viewModel.createRecord(title, amount, payer, amtA, amtB, date, note)
                showCreateRecordDialog = false
            }
        )
    }

    if (showEditRecordDialog != null) {
        val editingRecord = showEditRecordDialog!!
        TreatRecordFormDialog(
            personAName = personA,
            personBName = personB,
            currentTotalA = totalA,
            currentTotalB = totalB,
            initialRecord = editingRecord,
            onDismiss = { showEditRecordDialog = null },
            onConfirm = { title, amount, payer, amtA, amtB, date, note ->
                viewModel.updateRecord(
                    editingRecord.copy(
                        title = title,
                        amount = amount,
                        payer = payer,
                        amountA = amtA,
                        amountB = amtB,
                        date = date,
                        note = note
                    )
                )
                showEditRecordDialog = null
            }
        )
    }

    if (showRecordDetail != null) {
        val item = showRecordDetail!!
        TreatRecordDetailDialog(
            item = item,
            personAName = personA,
            personBName = personB,
            dateFormat = dateFormat,
            onDismiss = { showRecordDetail = null },
            onEdit = {
                showEditRecordDialog = item.record
                showRecordDetail = null
            },
            onDelete = {
                showDeleteRecordConfirm = item.record
                showRecordDetail = null
            }
        )
    }

    if (showDeleteRecordConfirm != null) {
        val recordToDelete = showDeleteRecordConfirm!!
        AppConfirmDialog(
            onDismissRequest = { showDeleteRecordConfirm = null },
            title = "删除记录",
            message = "确定要删除这条【${recordToDelete.title}】请客记录吗？相关差额与建议将自动重新计算。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                viewModel.deleteRecord(recordToDelete)
                showDeleteRecordConfirm = null
            }
        )
    }

    if (showSettleConfirm) {
        val currentSummary = summary
        val settleDiff = currentSummary?.absDiff ?: 0.0
        val payerName = if ((currentSummary?.totalA ?: 0.0) < (currentSummary?.totalB ?: 0.0)) personA else personB
        AppConfirmDialog(
            onDismissRequest = { showSettleConfirm = false },
            title = "平账结算",
            message = "将生成一笔由【$payerName】出资 ¥${TreatBalanceCalculator.formatMoney(settleDiff)} 的平账记录，使双方累计支出立即拉平。是否确认？",
            confirmLabel = "确认平账",
            onConfirm = {
                viewModel.settleDifference()
                showSettleConfirm = false
            }
        )
    }

    if (showLedgerManageSheet) {
        TreatLedgerManageBottomSheet(
            ledgers = ledgers,
            currentLedgerId = currentLedger?.id,
            onSelectLedger = { viewModel.selectLedger(it) },
            onCreateLedger = {
                showLedgerManageSheet = false
                showCreateLedgerDialog = true
            },
            onEditLedger = { ledger ->
                showLedgerManageSheet = false
                showEditLedgerDialog = ledger
            },
            onDeleteLedger = { ledger ->
                showLedgerManageSheet = false
                showDeleteLedgerConfirm = ledger
            },
            onDismiss = { showLedgerManageSheet = false }
        )
    }

    if (showCreateLedgerDialog) {
        TreatLedgerEditDialog(
            onDismiss = { showCreateLedgerDialog = false },
            onConfirm = { name, aName, bName ->
                viewModel.createLedger(name, aName, bName)
                showCreateLedgerDialog = false
            }
        )
    }

    if (showEditLedgerDialog != null) {
        val targetLedger = showEditLedgerDialog!!
        TreatLedgerEditDialog(
            initialLedger = targetLedger,
            onDismiss = { showEditLedgerDialog = null },
            onConfirm = { name, aName, bName ->
                viewModel.updateLedger(
                    targetLedger.copy(
                        name = name,
                        personAName = aName,
                        personBName = bName
                    )
                )
                showEditLedgerDialog = null
            }
        )
    }

    if (showDeleteLedgerConfirm != null) {
        val ledgerToDelete = showDeleteLedgerConfirm!!
        AppConfirmDialog(
            onDismissRequest = { showDeleteLedgerConfirm = null },
            title = "删除账本",
            message = "确定要删除账本【${ledgerToDelete.name}】及其所有请客记录吗？此操作不可逆。",
            confirmLabel = "彻底删除",
            destructive = true,
            onConfirm = {
                viewModel.deleteLedger(ledgerToDelete)
                showDeleteLedgerConfirm = null
            }
        )
    }
}
