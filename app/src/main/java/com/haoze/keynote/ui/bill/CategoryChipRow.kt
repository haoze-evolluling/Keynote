package com.haoze.keynote.ui.bill

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haoze.keynote.data.db.entity.CategoryEntity
import com.haoze.keynote.ui.components.AppAlertDialog as AlertDialog
import com.haoze.keynote.ui.components.AppDialogButton
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.ModalTokens

@Composable
fun CategoryChipRow(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onSelectCategory: (Long?) -> Unit,
    onAddCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            "类别",
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategoryId == category.id,
                    onClick = {
                        onSelectCategory(if (selectedCategoryId == category.id) null else category.id)
                    },
                    label = { Text(category.name) },
                    shape = ModalTokens.innerShape
                )
            }
            FilterChip(
                selected = false,
                onClick = { showAddDialog = true },
                label = { Text("+") },
                shape = ModalTokens.innerShape
            )
        }
    }

    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("新建类别") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("类别名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ModalTokens.innerShape
                )
            },
            confirmButton = {
                AppDialogButton(
                    label = "创建",
                    onClick = {
                        if (newName.isNotBlank()) {
                            onAddCategory(newName.trim())
                            showAddDialog = false
                        }
                    },
                    enabled = newName.isNotBlank()
                )
            },
            dismissButton = {
                AppDialogButton(label = "取消", onClick = { showAddDialog = false })
            }
        )
    }
}
