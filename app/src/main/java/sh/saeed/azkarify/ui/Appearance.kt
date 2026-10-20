package sh.saeed.azkarify.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import sh.saeed.azkarify.R
import sh.saeed.azkarify.data.AppState

val AlanSans = FontFamily(Font(R.font.alan_regular), Font(R.font.alan_bold, FontWeight.Bold))

data class Palette(
    val accent: Color,
    val background: Color,
    val text: Color,
    val secondary: Color,
    val dark: Boolean,
)

val LocalPalette = staticCompositionLocalOf { palette("brown", false) }

fun palette(name: String, dark: Boolean): Palette {
    val pair =
        when (name) {
            "saffron" -> Color(.61f, .30f, 0f) to Color(1f, .73f, .34f)
            "teal" -> Color(.12f, .39f, .39f) to Color(.48f, .87f, .82f)
            "blue" -> Color(.16f, .32f, .59f) to Color(.57f, .74f, 1f)
            "green" -> Color(.19f, .42f, .29f) to Color(.59f, .84f, .63f)
            else -> Color(.52f, .25f, .14f) to Color(.91f, .62f, .43f)
        }
    val accent = if (dark) pair.second else pair.first
    val weight = if (dark) .10f else .04f
    val base = if (dark) 0f else 1f - weight
    return Palette(
        accent,
        Color(
            base + accent.red * weight,
            base + accent.green * weight,
            base + accent.blue * weight,
        ),
        if (dark) Color(.95f, .95f, .95f) else Color(.08f, .08f, .08f),
        if (dark) Color(.65f, .65f, .65f) else Color(.45f, .43f, .42f),
        dark,
    )
}

@Composable
fun AzkarTheme(state: AppState, content: @Composable () -> Unit) {
    val dark =
        when (state.theme) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }
    val p = remember(state.accent, dark) { palette(state.accent, dark) }
    val base = Typography()
    fun TextStyle.font() = copy(fontFamily = AlanSans)
    val type = remember {
        base.copy(
            displayLarge = base.displayLarge.font(),
            displayMedium = base.displayMedium.font(),
            displaySmall = base.displaySmall.font(),
            headlineLarge = base.headlineLarge.font(),
            headlineMedium = base.headlineMedium.font(),
            headlineSmall = base.headlineSmall.font(),
            titleLarge = base.titleLarge.font(),
            titleMedium = base.titleMedium.font(),
            titleSmall = base.titleSmall.font(),
            bodyLarge = base.bodyLarge.font(),
            bodyMedium = base.bodyMedium.font(),
            bodySmall = base.bodySmall.font(),
            labelLarge = base.labelLarge.font(),
            labelMedium = base.labelMedium.font(),
            labelSmall = base.labelSmall.font(),
            displayLargeEmphasized = base.displayLargeEmphasized.font(),
            displayMediumEmphasized = base.displayMediumEmphasized.font(),
            displaySmallEmphasized = base.displaySmallEmphasized.font(),
            headlineLargeEmphasized = base.headlineLargeEmphasized.font(),
            headlineMediumEmphasized = base.headlineMediumEmphasized.font(),
            headlineSmallEmphasized = base.headlineSmallEmphasized.font(),
            titleLargeEmphasized = base.titleLargeEmphasized.font(),
            titleMediumEmphasized = base.titleMediumEmphasized.font(),
            titleSmallEmphasized = base.titleSmallEmphasized.font(),
            bodyLargeEmphasized = base.bodyLargeEmphasized.font(),
            bodyMediumEmphasized = base.bodyMediumEmphasized.font(),
            bodySmallEmphasized = base.bodySmallEmphasized.font(),
            labelLargeEmphasized = base.labelLargeEmphasized.font(),
            labelMediumEmphasized = base.labelMediumEmphasized.font(),
            labelSmallEmphasized = base.labelSmallEmphasized.font(),
        )
    }
    val scheme =
        if (dark)
            darkColorScheme(
                primary = p.accent,
                primaryContainer = lerp(p.background, p.accent, .18f),
                onPrimaryContainer = p.text,
                secondary = p.accent,
                secondaryContainer = lerp(p.background, p.accent, .12f),
                onSecondary = if (dark) p.background else Color.White,
                onSecondaryContainer = p.text,
                tertiary = p.accent,
                tertiaryContainer = lerp(p.background, p.accent, .14f),
                onTertiary = if (dark) p.background else Color.White,
                onTertiaryContainer = p.text,
                background = p.background,
                surface = p.background,
                onBackground = p.text,
                onSurface = p.text,
                surfaceContainer = p.background,
                surfaceContainerHigh = lerp(p.background, p.accent, .08f),
                surfaceContainerHighest = lerp(p.background, p.accent, .10f),
                onSurfaceVariant = p.secondary,
                onPrimary = p.background,
            )
        else
            lightColorScheme(
                primary = p.accent,
                primaryContainer = lerp(p.background, p.accent, .18f),
                onPrimaryContainer = p.text,
                secondary = p.accent,
                secondaryContainer = lerp(p.background, p.accent, .12f),
                onSecondary = if (dark) p.background else Color.White,
                onSecondaryContainer = p.text,
                tertiary = p.accent,
                tertiaryContainer = lerp(p.background, p.accent, .14f),
                onTertiary = if (dark) p.background else Color.White,
                onTertiaryContainer = p.text,
                background = p.background,
                surface = p.background,
                onBackground = p.text,
                onSurface = p.text,
                surfaceContainer = p.background,
                surfaceContainerHigh = lerp(p.background, p.accent, .08f),
                surfaceContainerHighest = lerp(p.background, p.accent, .10f),
                onSurfaceVariant = p.secondary,
                onPrimary = Color.White,
            )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            typography = type,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = p.background),
        border = BorderStroke(1.dp, p.accent.copy(alpha = if (p.dark) .24f else .16f)),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 2.dp),
        content = content,
    )
}

@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPalette.current
    val containerColor = lerp(p.background, p.accent, if (p.dark) 0.12f else 0.08f)
    val border =
        BorderStroke(width = 1.dp, color = lerp(p.secondary, p.accent, 0.3f).copy(alpha = 0.25f))

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier =
            modifier
                .background(containerColor, shape = RoundedCornerShape(16.dp))
                .padding(vertical = 4.dp),
        offset = offset,
        shape = RoundedCornerShape(16.dp),
        containerColor = containerColor,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = border,
        content = content,
    )
}

@Composable
fun AppDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val p = LocalPalette.current
    val itemColors =
        MenuDefaults.itemColors(
            textColor = if (selected) p.accent else p.text,
            leadingIconColor = p.accent,
            trailingIconColor = p.accent,
            disabledTextColor = p.secondary.copy(alpha = 0.5f),
            disabledLeadingIconColor = p.secondary.copy(alpha = 0.5f),
        )

    val itemModifier =
        if (selected) {
            modifier
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .background(
                    color = lerp(p.background, p.accent, if (p.dark) 0.22f else 0.14f),
                    shape = RoundedCornerShape(10.dp),
                )
        } else {
            modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        }

    DropdownMenuItem(
        text = {
            ProvideTextStyle(
                value =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = AlanSans,
                    )
            ) {
                text()
            }
        },
        onClick = onClick,
        modifier = itemModifier,
        leadingIcon = leadingIcon,
        trailingIcon =
            trailingIcon
                ?: if (selected) {
                    { Icon(Icons.Outlined.Check, contentDescription = null, tint = p.accent) }
                } else null,
        enabled = enabled,
        colors = itemColors,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
fun AppMenuDivider(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    HorizontalDivider(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 8.dp),
        color = lerp(p.background, p.accent, 0.25f).copy(alpha = 0.35f),
    )
}

@Composable
fun BotanicalBackground(variant: Int = 0) {
    val p = LocalPalette.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        Modifier.fillMaxSize().background(p.background).drawWithCache {
            fun variation(index: Int, salt: Int): Float {
                val seed = variant.toULong() * 2654435761uL + (index * 97 + salt * 31).toULong()
                return (((seed xor (seed shr 13)) * 1274126177uL) % 1000uL).toFloat() / 1000f
            }
            data class Leaf(
                val path: Path,
                val brush: Brush,
                val offset: Offset,
                val size: Offset,
                val rotation: Float,
            )
            data class Sprig(
                val stem: Path,
                val position: Offset,
                val size: Offset,
                val mirrored: Boolean,
                val rotation: Float,
                val leaves: List<Leaf>,
            )
            val stemColor = p.accent.copy(alpha = if (p.dark) .15f else .12f)
            val stemStyle = Stroke(1.5.dp.toPx())
            val sprigs =
                (0..5).map { index ->
                    val left = (index % 2 == 0) != rtl
                    val w = (112 + variation(index, 3) * 34).dp.toPx()
                    val h = w * 1.8f
                    val x = if (left) 4.dp.toPx() else size.width - 4.dp.toPx()
                    val y = size.height * (.10f + (index / 2) * .35f + variation(index, 7) * .09f)
                    val stem =
                        Path().apply {
                            moveTo(w * .60f, h)
                            cubicTo(w * .46f, h * .66f, w * .60f, h * .31f, w * .44f, h * .05f)
                        }
                    val leaves =
                        (0..6).map { leaf ->
                            val lw = w * .30f
                            val lh = h * (if (leaf == 6) .20f else .25f)
                            val lx =
                                w * (if (leaf == 6) .44f else if (leaf % 2 == 0) .32f else .68f)
                            val ly = h * (.85f - leaf * .115f)
                            val path =
                                Path().apply {
                                    moveTo(lw * .5f, lh)
                                    cubicTo(
                                        -lw * .16f,
                                        lh * .68f,
                                        lw * .08f,
                                        lh * .27f,
                                        lw * .43f,
                                        0f,
                                    )
                                    cubicTo(
                                        lw * 1.03f,
                                        lh * .25f,
                                        lw * 1.12f,
                                        lh * .72f,
                                        lw * .5f,
                                        lh,
                                    )
                                    close()
                                }
                            val brush =
                                Brush.linearGradient(
                                    listOf(
                                        p.accent.copy(alpha = if (p.dark) .0975f else .078f),
                                        p.accent.copy(alpha = if (p.dark) .15f else .12f),
                                    ),
                                    start = Offset.Zero,
                                    end = Offset(lw, lh),
                                )
                            Leaf(
                                path,
                                brush,
                                Offset(lx, ly),
                                Offset(lw, lh),
                                if (leaf == 6) -8f else if (leaf % 2 == 0) -52f else 48f,
                            )
                        }
                    Sprig(
                        stem,
                        Offset(x, y),
                        Offset(w, h),
                        !left,
                        (if (left) 1 else -1) * (12 + variation(index, 11) * 18),
                        leaves,
                    )
                }
            onDrawBehind {
                for (sprig in sprigs) {
                    rotate(sprig.rotation, sprig.position) {
                        scale(if (sprig.mirrored) -1f else 1f, 1f, sprig.position) {
                            translate(
                                sprig.position.x - sprig.size.x / 2,
                                sprig.position.y - sprig.size.y / 2,
                            ) {
                                drawPath(sprig.stem, stemColor, style = stemStyle)
                                for (leaf in sprig.leaves) {
                                    rotate(leaf.rotation, leaf.offset) {
                                        translate(
                                            leaf.offset.x - leaf.size.x / 2,
                                            leaf.offset.y - leaf.size.y / 2,
                                        ) {
                                            drawPath(leaf.path, leaf.brush)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
