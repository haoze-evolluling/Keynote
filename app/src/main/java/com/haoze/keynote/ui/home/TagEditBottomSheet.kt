package com.haoze.keynote.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haoze.keynote.data.db.entity.TagEntity
import com.haoze.keynote.ui.theme.DialogContent
import com.haoze.keynote.ui.theme.LocalAppColors
import com.haoze.keynote.ui.theme.ModalTokens
import androidx.compose.ui.res.painterResource
import com.haoze.keynote.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditBottomSheet(
    tags: List<TagEntity>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    var newTagText by remember { mutableStateOf("") }

    DialogContent(modifier = Modifier.padding(16.dp)) {
        Text(
            "编辑标签",
            style = ModalTokens.titleTextStyle,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (tags.isNotEmpty()) {
            Text(
                "当前标签",
                style = ModalTokens.labelTextStyle,
                color = colors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(tags) { tag ->
                    AssistChip(
                        onClick = { },
                        label = { Text("#${tag.name}") },
                        trailingIcon = {
                            IconButton(
                                onClick = { onRemoveTag(tag.id) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_close),
                                    contentDescription = "移除标签",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = newTagText,
                onValueChange = { newTagText = it },
                label = { Text("新标签名称") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = {
                    if (newTagText.isNotBlank()) {
                        onAddTag(newTagText.trim())
                        newTagText = ""
                    }
                }
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "添加标签")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("完成")
        }
    }
}
