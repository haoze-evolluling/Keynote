package com.haoze.keynote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.Build

// ─── Dark Mode ────────────────────────────────────────────────────────────────

enum class DarkModePreference { SYSTEM, LIGHT, DARK }

@Immutable
data class DarkModeManager(val preference: DarkModePreference = DarkModePreference.SYSTEM) {
    @Composable
    fun isDarkMode(): Boolean = when (preference) {
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
        DarkModePreference.LIGHT -> false
        DarkModePreference.DARK -> true
    }
}

val LocalDarkModeManager = staticCompositionLocalOf<DarkModeManager> { DarkModeManager() }

fun Int.toDarkModePreference(): DarkModePreference = when (this) {
    1 -> DarkModePreference.LIGHT
    2 -> DarkModePreference.DARK
    else -> DarkModePreference.SYSTEM
}

fun DarkModePreference.toInt(): Int = when (this) {
    DarkModePreference.SYSTEM -> 0
    DarkModePreference.LIGHT -> 1
    DarkModePreference.DARK -> 2
}

// ─── Colors ───────────────────────────────────────────────────────────────────

@Immutable
data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val background: Color,
    val onBackground: Color,
    val transparent: Color,
    val shadow: Color,
    val dialogContainer: Color,
    val unspecified: Color,
    val chartColors: List<Color>,
    val priorityLow: Color,
    val priorityMedium: Color,
    val priorityHigh: Color,
    val statTotalSpending: Color,
    val statBillCount: Color,
    val statDailyAvg: Color,
    val statTopCategory: Color
)

val LocalAppColors = staticCompositionLocalOf<AppColors> { error("No AppColors provided") }

private val FallbackLightScheme = lightColorScheme(
    primary = Color(0xFF6650A4), onPrimary = Color(0xFFFFFFFF), primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71), onSecondary = Color(0xFFFFFFFF), secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260), onTertiary = Color(0xFFFFFFFF), tertiaryContainer = Color(0xFFFFD8E4), onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFFFBFE), onBackground = Color(0xFF1C1B1F), surface = Color(0xFFFFFBFE), onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC), onSurfaceVariant = Color(0xFF49454F), outline = Color(0xFF79747E), outlineVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF7F2FA), surfaceContainer = Color(0xFFF3EDF7), surfaceContainerHigh = Color(0xFFECE6F0), surfaceContainerHighest = Color(0xFFE6E0E9)
)

private val FallbackDarkScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF381E72), primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC), onSecondary = Color(0xFF332D41), secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8), onTertiary = Color(0xFF492532), tertiaryContainer = Color(0xFF633B48), onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF1C1B1F), onBackground = Color(0xFFE6E1E5), surface = Color(0xFF1C1B1F), onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F), onSurfaceVariant = Color(0xFFCAC4D0), outline = Color(0xFF938F99), outlineVariant = Color(0xFF49454F),
    surfaceContainerLowest = Color(0xFF0F0D13), surfaceContainerLow = Color(0xFF1C1B1F), surfaceContainer = Color(0xFF201F23), surfaceContainerHigh = Color(0xFF2B2930), surfaceContainerHighest = Color(0xFF36343B)
)

fun ColorScheme.toAppColors(darkTheme: Boolean) = AppColors(
    primary, onPrimary, primaryContainer, onPrimaryContainer, secondary, onSecondary, secondaryContainer, onSecondaryContainer,
    surface, onSurface, surfaceVariant, onSurfaceVariant, error, onError, errorContainer, onErrorContainer, tertiary, onTertiary,
    tertiaryContainer, onTertiaryContainer, outline, outlineVariant, background, onBackground, Color.Transparent,
    Color.Black.copy(alpha = if (darkTheme) 0.45f else 0.12f), surfaceContainerHigh, Color.Unspecified,
    chartColors = listOf(primary, tertiary, secondary, error, primaryContainer, tertiaryContainer, secondaryContainer, outline, outlineVariant, surfaceVariant),
    priorityLow = if (darkTheme) Color(0xFF81C995) else Color(0xFF2E7D32),
    priorityMedium = if (darkTheme) Color(0xFFFFB74D) else Color(0xFFB06000),
    priorityHigh = if (darkTheme) Color(0xFFFF8A80) else Color(0xFFBA1A1A),
    statTotalSpending = error, statBillCount = tertiary, statDailyAvg = if (darkTheme) Color(0xFF81C995) else Color(0xFF2E7D32), statTopCategory = primary
)

// ─── Spacing ──────────────────────────────────────────────────────────────────

object SpacingTokens {
    val screenPadding = 16.dp
    val contentSpacing = 12.dp
    val smallSpacing = 8.dp
    val tinySpacing = 4.dp
    // ── Bluke 设计语言：卡片式布局 / 细线镂空 / 统一圆角 ──
    val cardGap = 2.dp                // 分组条目之间的细线镂空间隙
    val groupedOuterRadius = 28.dp    // 分组首尾条目的外圆角
    val groupedInnerRadius = 4.dp     // 分组中间条目的内圆角
    val listCardRadius = 28.dp        // 列表独立卡片圆角（DeviceRow 同款）
    val cardRadius = 24.dp            // 大内容卡片圆角
    val pillRadius = 20.dp            // Pill 按钮圆角
    val statusPillRadius = 12.dp      // 状态 pill / 嵌套小块圆角
    val tagRadius = 6.dp              // 小标签 / 徽章圆角
    val iconCircleSize = 40.dp        // 列表条目圆形图标容器
    val iconCircleInner = 20.dp       // 圆形容器内图标尺寸
    val itemPaddingHorizontal = 16.dp // 条目水平内边距
    val itemPaddingVertical = 14.dp   // 条目垂直内边距
    val sectionSpacing = 16.dp        // 区块之间间距
    val iconSmall = 18.dp
    val iconMedium = 22.dp
    val iconLarge = 24.dp
    val actionRowHeight = 36.dp
    val chipHeight = 24.dp
    val cardElevation = 0.dp
    val shadowElevation = 8.dp
    val borderWidth = 1.dp
    val borderWidthThick = 2.dp
    val chartStrokeWidth = 2.dp
    val chartGridStrokeWidth = 1.dp
    val chartPointRadius = 4.dp
    val chartInnerPointRadius = 2.dp
    val chartBarWidth = 24.dp
    val chartBarSpacing = 12.dp
    val chartPaddingLeft = 60.dp
    val chartPaddingRight = 16.dp
    val chartPaddingTop = 16.dp
    val chartPaddingBottom = 30.dp
    val chartPointSpacing = 48.dp
    val chartMinWidth = 300.dp
    val chartHeight = 200.dp
    val donutChartSize = 200.dp
    val donutChartStrokeWidth = 40.dp
}

// ─── Modal / Typography ───────────────────────────────────────────────────────

object ModalTokens {
    val titleTextStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp)
    val bodyTextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
    val buttonTextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
    val labelTextStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
    val menuItemPaddingVertical = 8.dp
    val menuDividerPaddingVertical = 4.dp
}

val Typography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp)
)

@Composable
fun DialogContent(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = verticalArrangement, horizontalAlignment = horizontalAlignment, content = content)
}

// ─── Shapes ───────────────────────────────────────────────────────────────────

// Bluke 圆角体系：徽章 6 → 状态块 12 → 大卡片 24 → 列表卡片/卡片组 28
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// ─── Theme Composable ─────────────────────────────────────────────────────────

@Composable
fun KeyNoteTheme(
    darkModeManager: DarkModeManager = DarkModeManager(),
    content: @Composable () -> Unit
) {
    val darkTheme = darkModeManager.isDarkMode()
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> FallbackDarkScheme
        else -> FallbackLightScheme
    }
    val colors = colorScheme.toAppColors(darkTheme)

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalDarkModeManager provides darkModeManager
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
