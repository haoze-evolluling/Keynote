package com.haoze.keynote.ui.bill

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.haoze.keynote.data.db.entity.BillEntity
import com.haoze.keynote.util.toDayStartMillis
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import android.content.ClipData
import android.content.ClipboardManager
import com.haoze.keynote.ui.components.SettingsDivider
import com.haoze.keynote.ui.components.SettingsGroup
import com.haoze.keynote.ui.components.SettingsGroupTitle
import com.haoze.keynote.ui.components.SettingsScaffold
import com.haoze.keynote.ui.theme.DialogContent
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.ModalTokens
import com.haoze.keynote.ui.theme.SpacingTokens
import androidx.compose.ui.res.painterResource
import com.haoze.keynote.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillScreen(
    onBack: () -> Unit = {},
    viewModel: BillViewModel = koinViewModel()
) {
    val colors = LocalAppColors.current
    val bills by viewModel.bills.collectAsState()
    val billsWithCategory by viewModel.billsWithCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val context = LocalContext.current
    val categoryMap = remember(billsWithCategory) { billsWithCategory.associateBy { it.billId } }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showActionDialogForBill by remember { mutableStateOf<Long?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<BillEntity?>(null) }
    var showEditDialogForBill by remember { mutableStateOf<BillEntity?>(null) }
    var showBillDetailsForBill by remember { mutableStateOf<BillEntity?>(null) }

    SettingsScaffold(
        title = "记账",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
            ) { Icon(painterResource(R.drawable.ic_add), contentDescription = "新建账单") }
        }
    ) { innerPadding ->
        if (bills.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text("暂无账单记录", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant) }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    SettingsGroupTitle("全部账单 (${bills.size})")
                    SettingsGroup {
                        bills.forEachIndexed { index, bill ->
                            BillCard(
                                bill = bill,
                                categoryName = categoryMap[bill.id]?.catName,
                                dateFormat = dateFormat,
                                onLongClick = { showActionDialogForBill = bill.id }
                            )
                            if (index < bills.lastIndex) {
                                SettingsDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showActionDialogForBill != null) {
        val currentBill = bills.find { it.id == showActionDialogForBill }
        if (currentBill != null) {
            BillActionBottomSheet(
                billItem = currentBill.item,
                billAmount = currentBill.amount,
                onEdit = { showEditDialogForBill = currentBill; showActionDialogForBill = null },
                onViewDetails = { showBillDetailsForBill = currentBill; showActionDialogForBill = null },
                onCopyItem = {
                    (context.getSystemService(ClipboardManager::class.java))?.setPrimaryClip(ClipData.newPlainText("KeyNote", currentBill.item))
                    showActionDialogForBill = null
                },
                onCopyAmount = {
                    (context.getSystemService(ClipboardManager::class.java))?.setPrimaryClip(ClipData.newPlainText("KeyNote", String.format("%.2f", currentBill.amount)))
                    showActionDialogForBill = null
                },
                onDelete = { showDeleteConfirm = currentBill; showActionDialogForBill = null },
                onDismiss = { showActionDialogForBill = null }
            )
        }
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除账单") },
            text = { Text("确定要删除这条账单记录吗？") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm?.let { viewModel.deleteBill(it) }; showDeleteConfirm = null }) {
                    Text("删除", color = colors.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
        )
    }

    if (showBillDetailsForBill != null) {
        val bill = showBillDetailsForBill
        val rawBill = bill?.let { categoryMap[it.id] }
        if (bill != null) {
            AlertDialog(
                onDismissRequest = { showBillDetailsForBill = null },
                title = { Text("账单详情") },
                text = {
                    DialogContent(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("消费项目", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                        Text(bill.item, style = ModalTokens.bodyTextStyle, fontWeight = FontWeight.SemiBold)
                        if (!rawBill?.catName.isNullOrBlank()) {
                            Text("类别", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                            Text(rawBill!!.catName!!, style = ModalTokens.bodyTextStyle)
                        }
                        Text("金额", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                        Text("¥${String.format("%.2f", bill.amount)}", style = ModalTokens.bodyTextStyle, fontWeight = FontWeight.Bold, color = colors.primary)
                        Text("时间", style = ModalTokens.labelTextStyle, color = colors.onSurfaceVariant)
                        Text(dateFormat.format(Date(bill.date)), style = ModalTokens.bodyTextStyle)
                    }
                },
                confirmButton = { TextButton(onClick = { showBillDetailsForBill = null }) { Text("关闭") } },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                textContentColor = colors.onSurface,
                shape = RoundedCornerShape(28.dp),
            )
        }
    }

    if (showEditDialogForBill != null) {
        BillFormDialog(
            title = "编辑账单",
            initialItem = showEditDialogForBill!!.item,
            initialAmount = String.format("%.2f", showEditDialogForBill!!.amount),
            initialDate = showEditDialogForBill!!.date,
            initialCategoryId = showEditDialogForBill!!.categoryId,
            categories = categories,
            colors = colors,
            dateFormat = dateFormat,
            addCategory = { viewModel.addCategory(it) },
            onDismiss = { showEditDialogForBill = null },
            onConfirm = { item, amount, date, categoryId ->
                viewModel.updateBill(showEditDialogForBill!!.copy(item = item, amount = amount, date = date, categoryId = categoryId))
                showEditDialogForBill = null
            }
        )
    }

    if (showCreateDialog) {
        BillFormDialog(
            title = "新建账单",
            initialItem = "",
            initialAmount = "",
            initialDate = null,
            initialCategoryId = null,
            categories = categories,
            colors = colors,
            dateFormat = dateFormat,
            addCategory = { viewModel.addCategory(it) },
            onDismiss = { showCreateDialog = false },
            onConfirm = { item, amount, date, categoryId ->
                viewModel.createBill(item, amount, date, categoryId)
                showCreateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillFormDialog(
    title: String,
    initialItem: String,
    initialAmount: String,
    initialDate: Long?,
    initialCategoryId: Long?,
    categories: List<com.haoze.keynote.data.db.entity.CategoryEntity>,
    colors: com.haoze.keynote.ui.theme.AppColors,
    dateFormat: SimpleDateFormat,
    addCategory: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (item: String, amount: Double, date: Long, categoryId: Long?) -> Unit
) {
    var editItem by remember { mutableStateOf(initialItem) }
    var editAmount by remember { mutableStateOf(initialAmount) }
    var editDate by remember { mutableStateOf(initialDate) }
    var editCategoryId by remember { mutableStateOf(initialCategoryId) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingTime by remember { mutableStateOf(false) }

    LaunchedEffect(pendingTime) {
        if (pendingTime) { delay(350); showTimePicker = true; pendingTime = false }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            DialogContent(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryChipRow(
                    categories = categories, selectedCategoryId = editCategoryId,
                    onSelectCategory = { editCategoryId = it }, onAddCategory = addCategory
                )
                OutlinedTextField(value = editItem, onValueChange = { editItem = it }, label = { Text("消费项目") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = editAmount,
                    onValueChange = { editAmount = it },
                    label = { Text("金额") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                )
                DateField(editDate = editDate, dateFormat = dateFormat, colors = colors, onClick = { showDatePicker = true })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = editAmount.toDoubleOrNull()
                    if (editItem.isNotBlank() && amount != null) {
                        onConfirm(editItem, amount, editDate ?: System.currentTimeMillis(), editCategoryId)
                    }
                },
                enabled = editItem.isNotBlank() && editAmount.toDoubleOrNull() != null
            ) { Text(if (title == "新建账单") "创建" else "保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        textContentColor = colors.onSurface,
        shape = RoundedCornerShape(28.dp),
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = editDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        editDate = it.toDayStartMillis(); showDatePicker = false; pendingTime = true
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val cal = remember { Calendar.getInstance().apply { editDate?.let { timeInMillis = it } } }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY), initialMinute = cal.get(Calendar.MINUTE), is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择时间") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    editDate = Calendar.getInstance().apply {
                        editDate?.let { timeInMillis = it }
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("取消") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textContentColor = colors.onSurface,
            shape = RoundedCornerShape(28.dp),
        )
    }
}

@Composable
private fun DateField(
    editDate: Long?,
    dateFormat: SimpleDateFormat,
    colors: com.haoze.keynote.ui.theme.AppColors,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        OutlinedTextField(
            value = editDate?.let { dateFormat.format(Date(it)) } ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("时间") },
            placeholder = { Text("点击选择时间") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = colors.onSurface,
                disabledBorderColor = colors.outline,
                disabledLabelColor = colors.onSurfaceVariant,
                disabledPlaceholderColor = colors.onSurfaceVariant,
            ),
            trailingIcon = { Icon(painterResource(R.drawable.ic_date_range), contentDescription = "选择时间") }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BillCard(
    bill: BillEntity,
    categoryName: String?,
    dateFormat: SimpleDateFormat,
    onLongClick: () -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = bill.item, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!categoryName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                AssistChip(onClick = {}, label = { Text(categoryName, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.height(24.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = dateFormat.format(Date(bill.date)), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Text(
            text = "¥${String.format("%.2f", bill.amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = colors.primary,
            textAlign = TextAlign.End, modifier = Modifier.padding(end = 8.dp)
        )
    }
}
