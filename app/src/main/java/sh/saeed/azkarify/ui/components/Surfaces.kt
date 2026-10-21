package sh.saeed.azkarify.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzAccent
import sh.saeed.azkarify.ui.theme.AzBorder
import sh.saeed.azkarify.ui.theme.AzColors
import sh.saeed.azkarify.ui.theme.AzDuration
import sh.saeed.azkarify.ui.theme.AzRadius
import sh.saeed.azkarify.ui.theme.AzSpace
import sh.saeed.azkarify.ui.theme.AzStamp
import sh.saeed.azkarify.ui.theme.azTween
import sh.saeed.azkarify.ui.theme.stampLine

/** Moment pastels for toned cards. They keep their meaning in Night. */
enum class AzTone {
    Quiet,
    Sage,
    Peach,
    Clay,
    Forest,
    Lime,
}

fun AzColors.toneBackground(tone: AzTone) =
    when (tone) {
        AzTone.Quiet -> paperRaised
        AzTone.Sage -> sage
        AzTone.Peach -> peach
        AzTone.Clay -> clay
        AzTone.Forest -> forest
        AzTone.Lime -> lime
    }

fun AzColors.toneContent(tone: AzTone) = if (tone == AzTone.Forest) onForest else ink

/** Secondary text on a ground: `ink-muted` on paper, `ink-soft` on pastels. */
fun AzColors.toneSecondary(tone: AzTone) =
    when (tone) {
        AzTone.Quiet -> inkMuted
        AzTone.Forest -> onForest.copy(alpha = 0.82f)
        else -> inkSoft
    }

/**
 * The container for grouped content on brand screens: quiet (outlined), toned, or [stamped]
 * (the one interactive hero card per screen). With [onClick] the whole card is one button.
 */
@Composable
fun AzCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    tone: AzTone = AzTone.Quiet,
    stamped: Boolean = false,
    compact: Boolean = false,
    outline: Color? = null,
    contentPadding: PaddingValues = PaddingValues(if (compact) AzSpace.md else AzSpace.lg),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(AzSpace.sm),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Az.colors
    val shape = RoundedCornerShape(if (compact) AzRadius.md else AzRadius.lg)
    val interaction = remember { MutableInteractionSource() }
    val border =
        outline
            ?: when {
                stamped -> colors.stampLine
                tone == AzTone.Quiet -> colors.lineStrong
                else -> Color.Transparent
            }
    Column(
        modifier
            .fillMaxWidth()
            .then(
                if (stamped)
                    Modifier.azStamp(AzStamp.md, shape, interaction.takeIf { onClick != null })
                else Modifier
            )
            .clip(shape)
            .background(colors.toneBackground(tone))
            .border(AzBorder.hairline, border, shape)
            .then(
                if (onClick == null) Modifier
                else
                    Modifier.clickable(
                        interaction,
                        if (stamped) null else ripple(),
                        role = Role.Button,
                        onClick = onClick,
                    )
            )
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
    ) {
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides colors.toneContent(tone)
        ) {
            content()
        }
    }
}

enum class AzWellState {
    Idle,
    On,
    Spark,
    Lime,
}

/** A circle holding one symbol. Decorative: the row's label says what it is. */
@Composable
fun AzIconWell(
    @DrawableRes symbol: Int,
    modifier: Modifier = Modifier,
    state: AzWellState = AzWellState.Idle,
    small: Boolean = false,
) {
    val colors = Az.colors
    val (background, content) =
        when (state) {
            AzWellState.Idle -> colors.paperSunk to colors.ink
            AzWellState.On -> colors.forest to colors.onForest
            AzWellState.Spark -> colors.spark to Color(0xFF252923)
            AzWellState.Lime -> colors.lime to colors.ink
        }
    Box(
        modifier
            .size(if (small) 36.dp else 44.dp)
            .background(background, CircleShape)
            .then(
                if (state == AzWellState.Lime) Modifier.border(AzBorder.hairline, colors.ink, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        AzIcon(symbol, null, size = if (small) 20.dp else 22.dp, tint = content)
    }
}

/** A rounded group of rows separated by hairlines, for short, non-lazy groups. */
@Composable
fun AzListGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = Az.colors
    val shape = RoundedCornerShape(AzRadius.md)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.paperRaised)
            .border(AzBorder.hairline, colors.lineStrong, shape),
        content = content,
    )
}

/**
 * The same group look for rows of a lazy list: each item draws its slice of the group's ground
 * and outline, so long lists (the full Hisn) stay lazy.
 */
fun Modifier.azGroupItem(index: Int, count: Int, colors: AzColors): Modifier {
    val first = index == 0
    val last = index == count - 1
    return drawWithCache {
        val r = AzRadius.md.toPx()
        val w = size.width
        val h = size.height
        val stroke = AzBorder.hairline.toPx()
        val half = stroke / 2
        val ground =
            Path().apply {
                addRoundRect(
                    RoundRect(
                        0f,
                        0f,
                        w,
                        h,
                        topLeftCornerRadius = if (first) CornerRadius(r) else CornerRadius.Zero,
                        topRightCornerRadius = if (first) CornerRadius(r) else CornerRadius.Zero,
                        bottomLeftCornerRadius = if (last) CornerRadius(r) else CornerRadius.Zero,
                        bottomRightCornerRadius = if (last) CornerRadius(r) else CornerRadius.Zero,
                    )
                )
            }
        val outline =
            Path().apply {
                moveTo(half, if (first) r else 0f)
                if (last) {
                    lineTo(half, h - r)
                    quadraticTo(half, h - half, r, h - half)
                    lineTo(w - r, h - half)
                    quadraticTo(w - half, h - half, w - half, h - r)
                } else {
                    lineTo(half, h)
                    moveTo(w - half, h)
                }
                if (first) {
                    lineTo(w - half, r)
                    quadraticTo(w - half, half, w - r, half)
                    lineTo(r, half)
                    quadraticTo(half, half, half, r)
                } else lineTo(w - half, 0f)
            }
        onDrawBehind {
            drawPath(ground, colors.paperRaised)
            drawPath(outline, colors.lineStrong, style = Stroke(stroke))
        }
    }
}

/**
 * One row of a list group: optional leading element, title and subtitle, one trailing
 * accessory. [divider] draws the hairline above it, inset to the text when [inset].
 */
@Composable
fun AzListRow(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    subtitle: String? = null,
    subtitleFirst: Boolean = false,
    selected: Boolean = false,
    divider: Boolean = false,
    inset: Dp = 72.dp,
    titleMaxLines: Int = 3,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = Az.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val dividerStart = if (leading == null) AzSpace.md else inset
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(
                if (selected) Modifier.background(lerp(colors.paperRaised, colors.accent, 0.10f))
                else Modifier
            )
            .then(
                if (divider)
                    Modifier.drawBehind {
                        val start = dividerStart.toPx()
                        val x0 = if (rtl) 0f else start
                        val x1 = if (rtl) size.width - start else size.width
                        drawLine(colors.line, Offset(x0, 0f), Offset(x1, 0f), AzBorder.hairline.toPx())
                    }
                else Modifier
            )
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = AzSpace.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AzSpace.sm),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (subtitle != null && subtitleFirst)
                Text(subtitle, style = Az.type.footnote, color = colors.inkMuted)
            Text(
                title,
                style = Az.type.rowTitle,
                color = colors.ink,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null && !subtitleFirst)
                Text(subtitle, style = Az.type.footnote, color = colors.inkMuted)
        }
        trailing?.invoke(this)
    }
}

/** The row chevron: `ink-subtle`, mirrored in right-to-left layouts. */
@Composable
fun AzChevron() {
    AzIcon(AzSymbol.chevron, null, size = 20.dp, tint = Az.colors.inkSubtle)
}

/**
 * An 8dp pill track. Fill `accent`, complete `forest`; the fill animates in the draw phase
 * only and fills from the right in right-to-left layouts. Always pair it with text.
 */
@Composable
fun AzProgressTrack(
    fraction: Float,
    modifier: Modifier = Modifier,
    complete: Boolean = false,
    onTone: Boolean = false,
    thickness: Dp = 8.dp,
) {
    val colors = Az.colors
    val target = fraction.coerceIn(0f, 1f)
    val animated =
        animateFloatAsState(
            target,
            azTween(if (Az.reduceMotion) 0 else AzDuration.slow),
            label = "track",
        )
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val track = if (onTone) colors.paper.copy(alpha = 0.6f) else colors.paperSunk
    val fill = if (complete) colors.forest else colors.accent
    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) }
            .drawBehind {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(track, cornerRadius = r)
                val w = size.width * animated.value
                if (w > 0f)
                    drawRoundRect(
                        fill,
                        topLeft = Offset(if (rtl) size.width - w else 0f, 0f),
                        size = Size(w, size.height),
                        cornerRadius = r,
                    )
            }
    )
}

enum class AzPillTone {
    Accent,
    Muted,
    Solid,
}

/** A small outlined tag. Never interactive. */
@Composable
fun AzPill(text: String, modifier: Modifier = Modifier, tone: AzPillTone = AzPillTone.Muted) {
    val colors = Az.colors
    val type = Az.type
    val color =
        when (tone) {
            AzPillTone.Accent -> colors.accent
            AzPillTone.Muted -> colors.inkMuted
            AzPillTone.Solid -> colors.paper
        }
    Box(
        modifier
            .height(24.dp)
            .then(if (tone == AzPillTone.Solid) Modifier.background(colors.ink, CircleShape) else Modifier)
            .border(AzBorder.hairline, if (tone == AzPillTone.Solid) colors.ink else color, CircleShape)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (type.latin) text.uppercase() else text,
            style = if (type.latin) type.eyebrow.copy(letterSpacing = 0.06.em) else type.footnote.copy(fontSize = 12.sp),
            color = color,
            maxLines = 1,
        )
    }
}

/**
 * The tilted lime sticker for the one thing that is next or newly done. At most one per screen.
 */
@Composable
fun AzSticker(
    text: String,
    modifier: Modifier = Modifier,
    ornament: AzOrnamentShape? = null,
    ornamentColor: Color = Az.colors.spark,
    large: Boolean = false,
) {
    val colors = Az.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val shape = RoundedCornerShape(AzRadius.xs)
    val angle = (if (large) -8f else -4f) * if (rtl) -1 else 1
    Row(
        modifier
            .graphicsLayer { rotationZ = angle }
            .azStamp(if (large) AzStamp.md.copy(y = AzStamp.md.x) else AzStamp.sm, shape)
            .background(colors.lime, shape)
            .border(AzBorder.stamp, colors.stampLine, shape)
            .padding(
                horizontal = if (large) 18.dp else 14.dp,
                vertical = if (large) 14.dp else 7.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AzSpace.xs),
    ) {
        ornament?.let { AzOrnament(it, ornamentColor, size = if (large) 22.dp else 14.dp) }
        Text(
            text,
            style =
                if (large) Az.type.headline.copy(fontSize = 20.sp, lineHeight = 23.sp)
                else Az.type.footnote.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
            color = colors.ink,
        )
    }
}

/** The day's points in the toolbar; opens the points sheet. */
@Composable
fun AzPointsBadge(points: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Az.colors
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$label, $points" }
            .padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .height(36.dp)
                .background(colors.paperRaised, CircleShape)
                .border(AzBorder.hairline, colors.stampLine, CircleShape)
                .padding(start = AzSpace.xs, end = AzSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AzOrnament(AzOrnamentShape.Star, colors.spark, size = 16.dp)
            Text(points, style = Az.type.headline.copy(fontFeatureSettings = "tnum"), color = colors.ink)
        }
    }
}

/** An uppercase (English) label above a group. */
@Composable
fun AzEyebrow(text: String, modifier: Modifier = Modifier, color: Color = Az.colors.sparkInk) {
    val type = Az.type
    Text(if (type.latin) text.uppercase() else text, modifier, style = type.eyebrow, color = color)
}

/** Eyebrow, title and an optional trailing action above a group. */
@Composable
fun AzSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            eyebrow?.let { AzEyebrow(it) }
            Text(title, style = Az.type.title3, color = Az.colors.ink)
        }
        action?.let { AzButton(it, onAction, variant = AzButtonVariant.Text) }
    }
}

/** What a screen shows with nothing to list. Never blames the person. */
@Composable
fun AzEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    ornament: AzOrnamentShape? = AzOrnamentShape.Crescent,
    @DrawableRes symbol: Int? = null,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = Az.colors
    Column(
        modifier.fillMaxWidth().padding(horizontal = AzSpace.lg, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            symbol != null -> AzIcon(symbol, null, size = 56.dp, tint = colors.inkSubtle)
            ornament != null -> AzOrnament(ornament, colors.clay, size = 56.dp)
        }
        Text(
            title,
            Modifier.padding(top = AzSpace.xs),
            style = Az.type.title3,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            body,
            Modifier.widthIn(max = 280.dp),
            style = Az.type.body,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
        )
        action?.let {
            AzButton(it, onAction, Modifier.padding(top = 6.dp), AzButtonVariant.Secondary, small = true)
        }
    }
}

@Immutable data class AzSwatch(val accent: AzAccent, val name: String)

/** Accent choices as a row of discs; a radio group. */
@Composable
fun AzSwatchRow(
    swatches: List<AzSwatch>,
    selected: AzAccent,
    onSelect: (AzAccent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Az.colors
    Row(
        modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(AzSpace.sm),
    ) {
        swatches.forEach { swatch ->
            val on = swatch.accent == selected
            val fill = if (colors.isDark) swatch.accent.night else swatch.accent.day
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .selectable(on, role = Role.RadioButton, onClick = { onSelect(swatch.accent) })
                    .semantics { contentDescription = swatch.name },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .border(if (on) 2.dp else AzBorder.hairline, if (on) colors.ink else colors.lineStrong, CircleShape)
                        .padding(if (on) 4.dp else 3.dp)
                        .background(fill, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (on) AzIcon(AzSymbol.check, null, size = 20.dp, tint = colors.onAccent)
                }
            }
        }
    }
}

/** Native switch tinted `forest`: a switch shows a state, not the accent. */
@Composable
fun AzSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = Az.colors
    Switch(
        checked,
        onCheckedChange,
        modifier,
        colors =
            SwitchDefaults.colors(
                checkedTrackColor = colors.forest,
                checkedThumbColor = colors.paper,
                checkedBorderColor = colors.forest,
                uncheckedTrackColor = colors.paperSunk,
                uncheckedThumbColor = colors.inkMuted,
                uncheckedBorderColor = colors.lineStrong,
            ),
    )
}

/** Native segmented choice (Theme only). Selected segment `lime` with an `ink` label. */
@Composable
fun <T> AzSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Az.colors
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.lime,
                        activeContentColor = colors.ink,
                        activeBorderColor = colors.lineStrong,
                        inactiveContainerColor = colors.paperRaised,
                        inactiveContentColor = colors.ink,
                        inactiveBorderColor = colors.lineStrong,
                    ),
                label = { Text(label, style = Az.type.callout, maxLines = 1) },
            )
        }
    }
}

/** Page dots for pagers: the active page is an 18×6 `spark` pill. */
@Composable
fun AzPageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val colors = Az.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) {
            Box(
                Modifier
                    .size(width = if (it == current) 18.dp else 6.dp, height = 6.dp)
                    .background(if (it == current) colors.spark else colors.lineStrong, CircleShape)
            )
        }
    }
}

/** The arch frame for hero art: `clay`, a half-circle top, two hairline insets. */
@Composable
fun AzJourneyArch(
    modifier: Modifier = Modifier,
    width: Dp = 150.dp,
    height: Dp = 200.dp,
    content: @Composable () -> Unit,
) {
    val colors = Az.colors
    val inset = lerp(colors.clay, colors.ink, 0.25f)
    val inner = lerp(colors.clay, colors.ink, 0.18f)
    Box(
        modifier
            .width(width)
            .height(height)
            .drawBehind {
                fun arch(pad: Float, bottom: Float) =
                    RoundRect(
                        pad,
                        pad,
                        size.width - pad,
                        size.height - pad,
                        topLeftCornerRadius = CornerRadius(size.width / 2 - pad),
                        topRightCornerRadius = CornerRadius(size.width / 2 - pad),
                        bottomLeftCornerRadius = CornerRadius(bottom),
                        bottomRightCornerRadius = CornerRadius(bottom),
                    )
                val lg = AzRadius.lg.toPx()
                drawPath(Path().apply { addRoundRect(arch(0f, lg)) }, colors.clay)
                val stroke = Stroke(AzBorder.hairline.toPx())
                val a = 8.dp.toPx()
                val b = 16.dp.toPx()
                drawPath(Path().apply { addRoundRect(arch(a, lg - a + 2.dp.toPx())) }, inset, style = stroke)
                drawPath(Path().apply { addRoundRect(arch(b, (lg - b).coerceAtLeast(0f) + 4.dp.toPx())) }, inner, style = stroke)
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
