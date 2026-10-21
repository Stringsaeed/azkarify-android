package sh.saeed.azkarify.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzBorder
import sh.saeed.azkarify.ui.theme.AzSpace
import sh.saeed.azkarify.ui.theme.AzStamp
import sh.saeed.azkarify.ui.theme.stampLine

enum class AzButtonVariant {
    /** `ink` fill: the one main action on a screen or sheet. */
    Primary,
    /** Stamped `paper-raised`: the alternative beside it, or an action inside a card. */
    Secondary,
    /** `accent` fill: the accent-tinted confirmation in a sheet. */
    Accent,
    /** `accent` label, no container: low-emphasis actions. */
    Text,
}

/** Pill button. Label in sentence case, three words at most. */
@Composable
fun AzButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AzButtonVariant = AzButtonVariant.Primary,
    small: Boolean = false,
    enabled: Boolean = true,
    @DrawableRes leading: Int? = null,
    @DrawableRes trailing: Int? = null,
) {
    val colors = Az.colors
    val height = if (small) 40.dp else 52.dp
    val content: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.let { AzIcon(it, null, size = 20.dp) }
            Text(text, style = Az.type.button)
            trailing?.let { AzIcon(it, null, size = 20.dp) }
        }
    }
    val padding = PaddingValues(horizontal = if (small) 16.dp else 24.dp)
    if (variant == AzButtonVariant.Text) {
        TextButton(
            onClick,
            modifier.heightIn(min = 48.dp),
            enabled = enabled,
            shape = CircleShape,
            colors =
                ButtonDefaults.textButtonColors(
                    contentColor = colors.accent,
                    disabledContentColor = colors.inkSubtle,
                ),
            contentPadding = PaddingValues(horizontal = AzSpace.sm),
        ) {
            content()
        }
        return
    }
    val interaction = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val stamped = variant == AzButtonVariant.Secondary
    val (container, label, border) =
        when (variant) {
            AzButtonVariant.Primary -> Triple(colors.ink, colors.paper, colors.ink)
            AzButtonVariant.Accent -> Triple(colors.accent, colors.onAccent, colors.accent)
            else -> Triple(colors.paperRaised, colors.ink, colors.stampLine)
        }
    Button(
        onClick = {
            if (stamped) haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        modifier =
            modifier
                .heightIn(min = height)
                .then(
                    if (stamped && enabled) Modifier.azStamp(AzStamp.sm, CircleShape, interaction)
                    else Modifier
                ),
        enabled = enabled,
        shape = CircleShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = label,
                disabledContainerColor = container.copy(alpha = 0.4f),
                disabledContentColor = label.copy(alpha = 0.7f),
            ),
        border = BorderStroke(AzBorder.hairline, if (enabled) border else Color.Transparent),
        contentPadding = padding,
        interactionSource = interaction,
    ) {
        content()
    }
}

enum class AzIconButtonVariant {
    Plain,
    Filled,
    Outline,
    Ink,
}

/** A circular button for one symbol on custom surfaces; 48dp target. */
@Composable
fun AzIconButton(
    @DrawableRes symbol: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AzIconButtonVariant = AzIconButtonVariant.Plain,
    tint: Color = Color.Unspecified,
    enabled: Boolean = true,
) {
    val colors = Az.colors
    val container =
        when (variant) {
            AzIconButtonVariant.Plain -> Color.Transparent
            AzIconButtonVariant.Filled -> colors.paperSunk
            AzIconButtonVariant.Outline -> colors.paperRaised
            AzIconButtonVariant.Ink -> colors.ink
        }
    val content =
        when {
            tint.isSpecified -> tint
            variant == AzIconButtonVariant.Ink -> colors.paper
            else -> colors.ink
        }
    IconButton(
        onClick,
        modifier
            .size(48.dp)
            .then(
                if (variant == AzIconButtonVariant.Outline)
                    Modifier.padding(2.dp).border(AzBorder.hairline, colors.ink, CircleShape)
                else Modifier
            ),
        enabled = enabled,
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor = container,
                contentColor = content,
                disabledContentColor = colors.inkSubtle,
            ),
    ) {
        AzIcon(symbol, label, size = 24.dp)
    }
}

/** The repetition count on a zikr: opens the counter set to that number. */
@Composable
fun AzCountButton(
    label: String,
    accessibilityLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Az.colors
    val interaction = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        modifier =
            modifier
                .heightIn(min = 40.dp)
                .azStamp(AzStamp.sm, CircleShape, interaction)
                .semantics { contentDescription = accessibilityLabel },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = colors.peach, contentColor = colors.ink),
        border = BorderStroke(AzBorder.hairline, colors.stampLine),
        contentPadding = PaddingValues(start = 6.dp, end = 14.dp),
        interactionSource = interaction,
    ) {
        Box(
            Modifier.size(28.dp).background(colors.ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("×", style = Az.type.footnote, color = colors.paper)
        }
        Text(label, Modifier.padding(start = AzSpace.xs), style = Az.type.callout.copy(fontWeight = FontWeight.SemiBold))
    }
}

/** Pill-shaped quick filter. Selected is `ink` with `paper` text, never accent. */
@Composable
fun AzChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes symbol: Int? = null,
) {
    val colors = Az.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = Az.type.callout) },
        modifier = modifier.heightIn(min = 40.dp),
        leadingIcon =
            when {
                symbol != null -> ({ AzIcon(symbol, null, size = 18.dp) })
                selected -> ({ AzIcon(AzSymbol.check, null, size = 18.dp) })
                else -> null
            },
        shape = CircleShape,
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = colors.paperRaised,
                labelColor = colors.ink,
                iconColor = colors.ink,
                selectedContainerColor = colors.ink,
                selectedLabelColor = colors.paper,
                selectedLeadingIconColor = colors.paper,
            ),
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = colors.lineStrong,
                selectedBorderColor = colors.ink,
                borderWidth = AzBorder.hairline,
                selectedBorderWidth = AzBorder.hairline,
            ),
    )
}
