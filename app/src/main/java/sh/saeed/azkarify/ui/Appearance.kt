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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import sh.saeed.azkarify.data.AppState
import sh.saeed.azkarify.ui.theme.AlanSans
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzAccent
import sh.saeed.azkarify.ui.theme.AzBorder
import sh.saeed.azkarify.ui.theme.AzRadius
import sh.saeed.azkarify.ui.theme.AzColors
import sh.saeed.azkarify.ui.theme.AzDayColors
import sh.saeed.azkarify.ui.theme.AzTheme

data class Palette(
    val accent: Color,
    val background: Color,
    val text: Color,
    val secondary: Color,
    val dark: Boolean,
)

val LocalPalette = staticCompositionLocalOf { palette(AzDayColors) }

/** Screens not yet moved to the design system components read these roles from the tokens. */
fun palette(colors: AzColors) =
    Palette(colors.accent, colors.paper, colors.ink, colors.inkMuted, colors.isDark)

@Composable
fun AzkarTheme(state: AppState, content: @Composable () -> Unit) {
    val dark =
        when (state.theme) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }
    AzTheme(dark, AzAccent.of(state.accent), arabic = state.language == "ar") {
        val colors = Az.colors
        val p = remember(colors) { palette(colors) }
        CompositionLocalProvider(LocalPalette provides p, content = content)
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = Az.colors
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AzRadius.md),
        colors = CardDefaults.outlinedCardColors(containerColor = colors.paperRaised),
        border = BorderStroke(AzBorder.hairline, colors.lineStrong),
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
    val colors = Az.colors
    val containerColor = colors.paperRaised
    val border = BorderStroke(AzBorder.hairline, colors.lineStrong)

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
                    color = lerp(Az.colors.paperRaised, p.accent, 0.10f),
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
    HorizontalDivider(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 8.dp),
        color = Az.colors.line,
    )
}

/** The botanical scenery is retired; screens keep a plain paper ground until they move to tokens. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun BotanicalBackground(variant: Int = 0) {
    Box(Modifier.fillMaxSize().background(LocalPalette.current.background))
}
