package com.haoze.keynote.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haoze.keynote.data.db.entity.TagEntity
import androidx.compose.ui.res.painterResource
import com.haoze.keynote.R

@Composable
fun TagChipRow(
    tags: List<TagEntity>,
    onRemoveTag: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showRemove: Boolean = true
) {
    FlowRow(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tags.forEach { tag ->
            InputChip(
                selected = false,
                onClick = { },
                label = { Text("#${tag.name}", maxLines = 1) },
                trailingIcon = if (showRemove) {
                    {
                        IconButton(
                            onClick = { onRemoveTag(tag.id) },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_close),
                                contentDescription = "移除标签",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else null
            )
        }
    }
}
