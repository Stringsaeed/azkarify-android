package sh.saeed.azkarify.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import sh.saeed.azkarify.data.AppState
import sh.saeed.azkarify.ui.components.AzIcon
import sh.saeed.azkarify.ui.components.AzSymbol
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzAccent
import sh.saeed.azkarify.ui.theme.AzBorder
import sh.saeed.azkarify.ui.theme.AzRadius
import sh.saeed.azkarify.ui.theme.AzTheme

@Composable
fun AzkarTheme(state: AppState, content: @Composable () -> Unit) {
    val dark =
        when (state.theme) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }
    AzTheme(dark, AzAccent.of(state.accent), arabic = state.language == "ar") {
        content()
    }
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
    val colors = Az.colors
    val itemColors =
        MenuDefaults.itemColors(
            textColor = if (selected) colors.accent else colors.ink,
            leadingIconColor = colors.accent,
            trailingIconColor = colors.accent,
            disabledTextColor = colors.inkMuted.copy(alpha = 0.5f),
            disabledLeadingIconColor = colors.inkMuted.copy(alpha = 0.5f),
        )

    val itemModifier =
        if (selected) {
            modifier
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .background(
                    color = colors.paperSunk,
                    shape = RoundedCornerShape(10.dp),
                )
        } else {
            modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        }

    DropdownMenuItem(
        text = {
            ProvideTextStyle(
                value =
                    Az.type.body.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
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
                    { AzIcon(AzSymbol.check, null, tint = colors.accent) }
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
