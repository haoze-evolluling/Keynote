package com.haoze.keynote.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun DualPaneLayout(
    isExpanded: Boolean,
    leftPane: @Composable (Modifier) -> Unit,
    rightPane: @Composable (Modifier) -> Unit
) {
    if (!isExpanded) {
        leftPane(Modifier.fillMaxSize())
        return
    }

    var dividerPosition by remember { mutableStateOf(0.35f) }
    val dividerWidth = 8.dp
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalWidth = maxWidth
        val leftWidth = totalWidth * dividerPosition
        val rightWidth = totalWidth - leftWidth - dividerWidth

        Row(modifier = Modifier.fillMaxSize()) {
            leftPane(Modifier.width(leftWidth).fillMaxHeight())

            Box(
                modifier = Modifier
                    .width(dividerWidth)
                    .fillMaxHeight()
                    .background(dividerColor)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            val totalWidthPx = with(density) { totalWidth.toPx() }
                            val delta = dragAmount / totalWidthPx
                            dividerPosition = (dividerPosition + delta).coerceIn(0.2f, 0.6f)
                        }
                    }
            )

            rightPane(Modifier.width(rightWidth).fillMaxHeight())
        }
    }
}
