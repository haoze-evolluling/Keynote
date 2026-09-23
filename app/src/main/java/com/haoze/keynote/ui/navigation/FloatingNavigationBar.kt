package com.haoze.keynote.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastRoundToInt
import com.haoze.keynote.ui.component.liquid.DampedDragAnimation
import com.haoze.keynote.ui.component.liquid.InnerShadow
import com.haoze.keynote.ui.component.liquid.InteractiveHighlight
import com.haoze.keynote.ui.component.liquid.IosIndicatorSpecular
import com.haoze.keynote.ui.component.liquid.drawSpecularHighlight
import com.haoze.keynote.ui.component.liquid.innerShadow
import com.haoze.keynote.ui.component.liquid.rememberDeviceTilt
import com.haoze.keynote.ui.component.liquid.rememberGravityRotatedHighlight
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/** Pager 进度与目标指示器距离容差，小于此值判定为已追赶上。 */
private const val PAGER_CATCH_UP_TOLERANCE = 0.02f

/** 安全超时：当 pager 未能平稳停靠在目标时让出指示器所有权（纳秒）。 */
private const val PAGER_CATCH_UP_TIMEOUT_NANO = 800_000_000L

/**
 * 流体拟物悬浮药丸底栏（参考谛听 DITING 项目与 KernelSU / SyncTouch 设计语言）。
 *
 * 交互特性：
 * - 拖拽跟随：手势在底栏区域滑动时指示器 1:1 跟随手指，附带指示器速度挤压拉伸与整体胶囊弹性橡胶过冲；
 * - 按压微动：手指按下时指示器适度放大，同时点亮交互式动态高光触摸光斑；
 * - 释放停靠：手指抬起自动磁吸对齐至最近 Tab，单击（未超出 slop 阈值）快速选中触摸点对应的 Tab；
 * - 重力高光：底栏外层和指示器高光跟随设备真实重力倾斜，生命周期感知（后台暂停监听，低功耗防抖）；
 * - Pager 追赶机制：[pagerProgress] 保证手指滑动页面时胶囊平滑跟随；点击或拖动底栏触发页面切换时，
 *   在 Pager 追赶上目标之前保持底栏动画，杜绝回弹扯动。
 */
@Composable
fun FloatingNavigationBar(
    selectedPage: Int,
    onPageSelected: (Int) -> Unit,
    items: List<BottomBarDestination>,
    modifier: Modifier = Modifier,
    pagerProgress: (() -> Float)? = null,
    isGlassEnabled: Boolean = true,
) {
    val isInDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val pillShape = remember { CircleShape }
    val accentColor = MaterialTheme.colorScheme.primary
    val tabContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val containerColor = if (isGlassEnabled) {
        if (isInDark) surfaceContainer.copy(alpha = 0.52f) else surfaceContainer.copy(alpha = 0.58f)
    } else {
        surfaceContainer
    }

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val tabsCount = items.size
    val navSectionWidthDp = when (tabsCount) {
        2 -> 204.dp
        3 -> 276.dp
        4 -> 340.dp
        else -> 204.dp
    }
    val barHeightDp = 64.dp
    val totalWidthDp = navSectionWidthDp

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var navWidthPx by remember { mutableFloatStateOf(0f) }
    var isUserDragging by remember { mutableStateOf(false) }
    var isPagerCatchUpPending by remember { mutableStateOf(false) }
    var pagerCatchUpDeadlineNano by remember { mutableLongStateOf(0L) }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (navWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / navWidthPx).fastCoerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    val dampedDragAnimation = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedPage.coerceIn(0, tabsCount - 1).toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f
        )
    }

    // 页面滑动联动
    if (pagerProgress != null) {
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { pagerProgress() }
                .collect { progress ->
                    if (!isUserDragging) {
                        val pagerValue = progress.fastCoerceIn(0f, (tabsCount - 1).toFloat())
                        if (isPagerCatchUpPending) {
                            val isStillAnimating = dampedDragAnimation.isRunning ||
                                abs(dampedDragAnimation.value - dampedDragAnimation.targetValue) > PAGER_CATCH_UP_TOLERANCE
                            val isTimedOut = System.nanoTime() >= pagerCatchUpDeadlineNano
                            if (isStillAnimating && !isTimedOut) {
                                return@collect
                            }
                            isPagerCatchUpPending = false
                        }
                        dampedDragAnimation.snapToValue(pagerValue)
                    }
                }
        }
    } else {
        LaunchedEffect(selectedPage) {
            if (!isUserDragging) {
                dampedDragAnimation.animateToValue(selectedPage.toFloat())
            }
        }
    }

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { size, touchOffset ->
                Offset(
                    touchOffset.x.fastCoerceIn(0f, size.width),
                    size.height / 2f
                )
            }
        )
    }

    // 单个重力传感器驱动内外高光，生命周期感知
    val deviceTilt = rememberDeviceTilt(enabled = isGlassEnabled)
    val baseHighlight = rememberGravityRotatedHighlight(IosIndicatorSpecular, extraDegrees = -45f, tiltState = deviceTilt)
    val pillHighlight = rememberGravityRotatedHighlight(IosIndicatorSpecular, extraDegrees = 90f, tiltState = deviceTilt)

    Box(
        modifier = modifier
            .width(totalWidthDp)
            .height(barHeightDp),
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. 底栏外壳背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = panelOffset }
                .then(
                    if (isGlassEnabled) {
                        Modifier
                            .dropShadow(
                                shape = pillShape,
                                shadow = Shadow(
                                    radius = 10.dp,
                                    color = Color.Black,
                                    alpha = if (isInDark) 0.25f else 0.12f,
                                ),
                            )
                            .clip(pillShape)
                            .background(containerColor, pillShape)
                            .drawSpecularHighlight(
                                shape = pillShape,
                                highlight = baseHighlight,
                                alpha = 0.85f
                            )
                            .then(interactiveHighlight.modifier)
                    } else {
                        Modifier
                            .shadow(
                                elevation = 6.dp,
                                shape = pillShape,
                                ambientColor = Color.Black.copy(alpha = 0.12f),
                                spotColor = Color.Black.copy(alpha = 0.18f)
                            )
                            .clip(pillShape)
                            .background(containerColor, pillShape)
                            .then(interactiveHighlight.modifier)
                    }
                )
        )

        // 2. 导航内容区域：手势捕获 + 滑动指示器 + 前景 Tab
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(navSectionWidthDp)
                .onGloballyPositioned { coords ->
                    navWidthPx = coords.size.width.toFloat()
                    val contentWidthPx = navWidthPx - with(density) { 8.dp.toPx() }
                    tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                }
                .pointerInput(tabWidthPx, navWidthPx, isLtr, tabsCount) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isUserDragging = true
                        isPagerCatchUpPending = false
                        val downX = down.position.x
                        interactiveHighlight.press(down.position)
                        dampedDragAnimation.press()

                        var hasMoved = false
                        val touchSlop = viewConfiguration.touchSlop
                        val currentPointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.fastFirstOrNull { it.id == currentPointerId } ?: break
                            if (change.pressed) {
                                val dragAmount = change.positionChange()
                                val totalMoveX = abs(change.position.x - downX)
                                if (!hasMoved && totalMoveX > touchSlop) {
                                    hasMoved = true
                                }

                                if (hasMoved) {
                                    change.consume()
                                }

                                interactiveHighlight.updatePosition(change.position)

                                if (tabWidthPx > 0f) {
                                    val rawDelta = if (isLtr) dragAmount.x / tabWidthPx else -dragAmount.x / tabWidthPx
                                    val newTarget = dampedDragAnimation.targetValue + rawDelta
                                    val clampedTarget = newTarget.fastCoerceIn(0f, (tabsCount - 1).toFloat())
                                    dampedDragAnimation.updateValue(clampedTarget)

                                    // 橡皮筋回弹效果
                                    val excess = (newTarget - clampedTarget) * tabWidthPx * if (isLtr) 1f else -1f
                                    if (excess != 0f || offsetAnimation.value != 0f) {
                                        animationScope.launch {
                                            offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x * 0.4f)
                                        }
                                    }
                                }
                            } else {
                                // 手指抬起
                                change.consume()
                                val upX = change.position.x
                                val targetIndex = if (!hasMoved) {
                                    // 单击手势：根据触控点直接计算点击的 Tab 项
                                    val contentStartX = with(density) { 4.dp.toPx() }
                                    val contentWidthPx = (navWidthPx - with(density) { 8.dp.toPx() }).coerceAtLeast(0f)
                                    val relativeX = (upX - contentStartX).coerceIn(0f, contentWidthPx.coerceAtLeast(1f))
                                    val tappedIndex = if (tabWidthPx > 0f) (relativeX / tabWidthPx).toInt() else 0
                                    if (isLtr) tappedIndex else (tabsCount - 1 - tappedIndex)
                                } else {
                                    // 拖拽手势：磁吸至最近的 Tab
                                    dampedDragAnimation.targetValue.fastRoundToInt()
                                }.fastCoerceIn(0, tabsCount - 1)

                                dampedDragAnimation.animateToValue(targetIndex.toFloat())
                                if (pagerProgress != null) {
                                    isPagerCatchUpPending = true
                                    pagerCatchUpDeadlineNano =
                                        System.nanoTime() + PAGER_CATCH_UP_TIMEOUT_NANO
                                }
                                onPageSelected(targetIndex)

                                val finalCenter = Offset(
                                    if (isLtr) (targetIndex + 0.5f) * tabWidthPx + with(density) { 4.dp.toPx() }
                                    else navWidthPx - (targetIndex + 0.5f) * tabWidthPx - with(density) { 4.dp.toPx() },
                                    size.height / 2f
                                )
                                interactiveHighlight.release(finalCenter)
                                animationScope.launch {
                                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                                }
                                isUserDragging = false
                                break
                            }
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // 2a. 滑动胶囊指示器
            if (tabWidthPx > 0f) {
                val tabWidthDp = with(density) { tabWidthPx.toDp() }
                if (isGlassEnabled) {
                    Box(
                        Modifier
                            .padding(start = 4.dp)
                            .graphicsLayer {
                                val progressOffset = dampedDragAnimation.value * tabWidthPx
                                translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                // 速度驱动挤压与拉伸
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            }
                            .height(56.dp)
                            .width(tabWidthDp)
                            .clip(pillShape)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = if (!isInDark) 0.68f else 0.75f
                                ),
                                pillShape
                            )
                            .drawSpecularHighlight(
                                shape = pillShape,
                                highlight = pillHighlight,
                                alpha = 0.90f
                            )
                            .innerShadow(shape = pillShape) {
                                InnerShadow(
                                    radius = 8.dp * dampedDragAnimation.pressProgress.coerceAtLeast(0.4f),
                                    color = Color.Black.copy(alpha = 0.18f),
                                    alpha = dampedDragAnimation.pressProgress.coerceAtLeast(0.4f),
                                )
                            }
                    ) {
                        // 顶部透镜光晕渐变
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(pillShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (isInDark) 0.22f else 0.35f),
                                            Color.Transparent
                                        ),
                                        startY = 0f,
                                        endY = with(density) { 28.dp.toPx() }
                                    )
                                )
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .padding(start = 4.dp)
                            .graphicsLayer {
                                val progressOffset = dampedDragAnimation.value * tabWidthPx
                                translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            }
                            .height(56.dp)
                            .width(tabWidthDp)
                            .clip(pillShape)
                            .background(MaterialTheme.colorScheme.primaryContainer, pillShape)
                    )
                }
            }

            // 2b. 前景 Tab 项（双层 Alpha 交叉淡入淡出，图标与文字保持清晰锐利）
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, destination ->
                    FloatingBottomBarTab(
                        index = index,
                        isSelected = selectedPage == index,
                        weight = { (1f - abs(dampedDragAnimation.value - index)).fastCoerceIn(0f, 1f) },
                        pressProgress = { dampedDragAnimation.pressProgress },
                        painter = destination.painter(),
                        label = destination.tabLabel,
                        accentColor = if (isGlassEnabled) MaterialTheme.colorScheme.onPrimaryContainer else accentColor,
                        contentColor = tabContentColor,
                        onSelect = {
                            if (selectedPage != index) {
                                dampedDragAnimation.animateToValue(index.toFloat())
                                if (pagerProgress != null) {
                                    isPagerCatchUpPending = true
                                    pagerCatchUpDeadlineNano =
                                        System.nanoTime() + PAGER_CATCH_UP_TIMEOUT_NANO
                                }
                            }
                            onPageSelected(index)
                        }
                    )
                }
            }
        }
    }
}




@Composable
private fun RowScope.FloatingBottomBarTab(
    index: Int,
    isSelected: Boolean,
    weight: () -> Float,
    pressProgress: () -> Float,
    painter: Painter,
    label: String,
    accentColor: Color,
    contentColor: Color,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .semantics {
                role = Role.Tab
                selected = isSelected
                onClick(label = label) { onSelect(); true }
            }
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val dynamicScale = 1f + 0.05f * weight() * pressProgress()
                scaleX = dynamicScale
                scaleY = dynamicScale
            },
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painter,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = (1f - weight()).fastCoerceIn(0f, 1f) }
            )
            Icon(
                painter = painter,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = weight().fastCoerceIn(0f, 1f) }
            )
        }
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                ),
                color = contentColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { alpha = (1f - weight()).fastCoerceIn(0f, 1f) }
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                ),
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { alpha = weight().fastCoerceIn(0f, 1f) }
            )
        }
    }
}
