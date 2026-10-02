package com.haoze.keynote.ui.bill.treat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.haoze.keynote.R
import com.haoze.keynote.data.db.entity.TreatLedgerEntity
import com.haoze.keynote.data.db.entity.TreatRecordEntity
import com.haoze.keynote.ui.components.AppAlertDialog as AlertDialog
import com.haoze.keynote.ui.components.AppDatePickerDialog
import com.haoze.keynote.ui.components.AppDialogButton
import com.haoze.keynote.ui.components.AppTimePickerDialog
import com.haoze.keynote.ui.components.SettingsDivider
import com.haoze.keynote.ui.theme.DialogContent
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.ModalTokens
import com.haoze.keynote.util.toDayStartMillis
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val QUICK_TITLES = listOf("晚餐", "午餐", "火锅", "咖啡/奶茶", "夜宵", "电影", "下午茶", "聚会", "买单")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatRecordFormDialog(
    personAName: String,
    personBName: String,
    currentTotalA: Double,
    currentTotalB: Double,
    initialRecord: TreatRecordEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, Double, Double, Long, String?) -> Unit
) {
    val colors = LocalAppColors.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    var title by remember { mutableStateOf(initialRecord?.title ?: "") }
    var payer by remember { mutableIntStateOf(initialRecord?.payer ?: 0) }
    var amountText by remember { mutableStateOf(if (initialRecord != null) String.format(Locale.CHINA, "%.2f", initialRecord.amount) else "") }
    var amountAText by remember { mutableStateOf(if (initialRecord != null) String.format(Locale.CHINA, "%.2f", initialRecord.amountA) else "") }
    var amountBText by remember { mutableStateOf(if (initialRecord != null) String.format(Locale.CHINA, "%.2f", initialRecord.amountB) else "") }
    var recordDate by remember { mutableLongStateOf(initialRecord?.date ?: System.currentTimeMillis()) }
    var note by remember { mutableStateOf(initialRecord?.note ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingTimePicker by remember { mutableStateOf(false) }

    val totalAmt = amountText.toDoubleOrNull() ?: 0.0
    val amtA = if (payer == 0) totalAmt else if (payer == 1) 0.0 else (amountAText.toDoubleOrNull() ?: 0.0)
    val amtB = if (payer == 1) totalAmt else if (payer == 0) 0.0 else (amountBText.toDoubleOrNull() ?: 0.0)
    val finalTotal = if (payer == 2) amtA + amtB else totalAmt

    val isValid = finalTotal > 0 && (payer != 2 || (amtA >= 0 && amtB >= 0))

    // 扣除旧记录金额后的基准
    val baseTotalA = currentTotalA - (initialRecord?.amountA ?: 0.0)
    val baseTotalB = currentTotalB - (initialRecord?.amountB ?: 0.0)
    val simulationText = remember(baseTotalA, baseTotalB, amtA, amtB, payer, personAName, personBName) {
        if (isValid) {
            TreatBalanceCalculator.simulateBalanceImpact(baseTotalA, baseTotalB, amtA, amtB, personAName, personBName)
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRecord == null) "记一笔请客" else "编辑请客记录") },
        text = {
            DialogContent(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 谁请客选择器
                Text("请客方", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = payer == 0,
                        onClick = { payer = 0 },
                        label = { Text(personAName, fontWeight = if (payer == 0) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = payer == 1,
                        onClick = { payer = 1 },
                        label = { Text(personBName, fontWeight = if (payer == 1) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = payer == 2,
                        onClick = { payer = 2 },
                        label = { Text("共同分担", fontWeight = if (payer == 2) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1.2f)
                    )
                }

                // 金额输入
                if (payer != 2) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("消费金额") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = ModalTokens.innerShape,
                        prefix = { Text("¥") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = amountAText,
                            onValueChange = { amountAText = it },
                            label = { Text("$personAName 出资") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = ModalTokens.innerShape,
                            prefix = { Text("¥") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                        )
                        OutlinedTextField(
                            value = amountBText,
                            onValueChange = { amountBText = it },
                            label = { Text("$personBName 出资") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = ModalTokens.innerShape,
                            prefix = { Text("¥") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                        )
                    }
                }

                // 快速消费项目预设
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(QUICK_TITLES) { chipTitle ->
                        SuggestionChip(
                            onClick = { title = chipTitle },
                            label = { Text(chipTitle, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("消费项目（例如：聚餐火锅）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ModalTokens.innerShape
                )

                // 时间选择
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = ModalTokens.innerShape
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("消费时间", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                        Text(
                            dateFormat.format(Date(recordDate)),
                            style = ModalTokens.bodyTextStyle,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = ModalTokens.innerShape
                )

                // 实时平衡模拟反馈
                if (simulationText != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.primary.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = simulationText,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppDialogButton(
                label = if (initialRecord == null) "保存记录" else "更新记录",
                onClick = {
                    if (isValid) {
                        val recordTitle = title.ifBlank { "请客消费" }
                        onConfirm(recordTitle, finalTotal, payer, amtA, amtB, recordDate, note.ifBlank { null })
                    }
                },
                enabled = isValid
            )
        },
        dismissButton = {
            AppDialogButton(label = "取消", onClick = onDismiss)
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = recordDate)
        AppDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                AppDialogButton(label = "确定", onClick = {
                    datePickerState.selectedDateMillis?.let {
                        recordDate = it.toDayStartMillis()
                        showDatePicker = false
                        pendingTimePicker = true
                    }
                })
            },
            dismissButton = { AppDialogButton(label = "取消", onClick = { showDatePicker = false }) }
        ) { DatePicker(state = datePickerState) }
    }

    LaunchedEffect(pendingTimePicker) {
        if (pendingTimePicker) {
            showTimePicker = true
            pendingTimePicker = false
        }
    }

    if (showTimePicker) {
        val cal = remember { Calendar.getInstance().apply { timeInMillis = recordDate } }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AppTimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            content = { TimePicker(state = timePickerState) },
            onConfirm = {
                val updatedCal = Calendar.getInstance().apply {
                    timeInMillis = recordDate
                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    set(Calendar.MINUTE, timePickerState.minute)
                }
                recordDate = updatedCal.timeInMillis
                showTimePicker = false
            }
        )
    }
}

@Composable
fun TreatRecordDetailDialog(
    item: TreatRecordWithBalance,
    personAName: String,
    personBName: String,
    dateFormat: SimpleDateFormat,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalAppColors.current
    val record = item.record

    val payerLabel = when (record.payer) {
        0 -> personAName
        1 -> personBName
        else -> "双方分担"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(record.title) },
        text = {
            DialogContent(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("消费总金额", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                Text(
                    "¥${TreatBalanceCalculator.formatMoney(record.amount)}",
                    style = ModalTokens.bodyTextStyle,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                Text("请客出资方", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                if (record.payer == 2) {
                    Text(
                        "$personAName 支付 ¥${TreatBalanceCalculator.formatMoney(record.amountA)} · $personBName 支付 ¥${TreatBalanceCalculator.formatMoney(record.amountB)}",
                        style = ModalTokens.bodyTextStyle
                    )
                } else {
                    Text("由【$payerLabel】买单请客", style = ModalTokens.bodyTextStyle)
                }

                Text("记后累计平衡状态", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                Text(
                    item.balanceDescription,
                    style = ModalTokens.bodyTextStyle,
                    fontWeight = FontWeight.SemiBold
                )

                Text("消费时间", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                Text(dateFormat.format(Date(record.date)), style = ModalTokens.bodyTextStyle)

                if (!record.note.isNullOrBlank()) {
                    Text("备注", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                    Text(record.note, style = ModalTokens.bodyTextStyle)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppDialogButton(label = "编辑", onClick = onEdit)
                AppDialogButton(label = "关闭", onClick = onDismiss)
            }
        },
        dismissButton = {
            AppDialogButton(label = "删除", destructive = true, onClick = onDelete)
        }
    )
}

@Composable
fun TreatLedgerEditDialog(
    initialLedger: TreatLedgerEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, personAName: String, personBName: String) -> Unit
) {
    var name by remember { mutableStateOf(initialLedger?.name ?: "") }
    var personA by remember { mutableStateOf(initialLedger?.personAName ?: "我") }
    var personB by remember { mutableStateOf(initialLedger?.personBName ?: "对方") }

    val isValid = name.isNotBlank() && personA.isNotBlank() && personB.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialLedger == null) "新建请客账本" else "编辑账本设置") },
        text = {
            DialogContent(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账本名称") },
                    placeholder = { Text("例如：我和小红的平衡账本") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ModalTokens.innerShape
                )
                OutlinedTextField(
                    value = personA,
                    onValueChange = { personA = it },
                    label = { Text("一方姓名/昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ModalTokens.innerShape
                )
                OutlinedTextField(
                    value = personB,
                    onValueChange = { personB = it },
                    label = { Text("另一方姓名/昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ModalTokens.innerShape
                )
            }
        },
        confirmButton = {
            AppDialogButton(
                label = "确定",
                onClick = { if (isValid) onConfirm(name.trim(), personA.trim(), personB.trim()) },
                enabled = isValid
            )
        },
        dismissButton = {
            AppDialogButton(label = "取消", onClick = onDismiss)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatLedgerManageBottomSheet(
    ledgers: List<TreatLedgerEntity>,
    currentLedgerId: Long?,
    onSelectLedger: (Long) -> Unit,
    onCreateLedger: () -> Unit,
    onEditLedger: (TreatLedgerEntity) -> Unit,
    onDeleteLedger: (TreatLedgerEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("切换与管理请客账本", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onCreateLedger) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = "新建账本", tint = colors.primary)
                }
            }

            ledgers.forEach { ledger ->
                val isSelected = ledger.id == currentLedgerId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectLedger(ledger.id)
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = ledger.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.primary else colors.onSurface
                            )
                            if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = colors.primary.copy(alpha = 0.15f)
                                ) {
                                    Text("当前", modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall, color = colors.primary)
                                }
                            }
                        }
                        Text(
                            text = "双方：${ledger.personAName} & ${ledger.personBName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onEditLedger(ledger) }) {
                            Icon(painterResource(R.drawable.ic_edit), contentDescription = "编辑", tint = colors.onSurfaceVariant)
                        }
                        if (ledgers.size > 1) {
                            IconButton(onClick = { onDeleteLedger(ledger) }) {
                                Icon(painterResource(R.drawable.ic_delete), contentDescription = "删除", tint = colors.error)
                            }
                        }
                    }
                }
                SettingsDivider()
            }
        }
    }
}
