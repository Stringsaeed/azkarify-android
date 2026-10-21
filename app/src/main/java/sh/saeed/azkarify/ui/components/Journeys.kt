package sh.saeed.azkarify.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import sh.saeed.azkarify.ui.theme.Az
import sh.saeed.azkarify.ui.theme.AzBorder
import sh.saeed.azkarify.ui.theme.AzSpace
import sh.saeed.azkarify.ui.theme.AzStamp
import sh.saeed.azkarify.ui.theme.stampLine

/**
 * One journey on Today: symbol, title, time or subtitle, progress. The whole card is one
 * button. The next prayer's card has an `ink` outline and the "Next" sticker on its top edge.
 */
@Composable
fun AzJourneyCard(
    title: String,
    detail: String,
    @DrawableRes symbol: Int,
    fraction: Float,
    progressLabel: String,
    accessibilityValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    complete: Boolean = false,
    detailIsTime: Boolean = false,
    nextLabel: String? = null,
) {
    val colors = Az.colors
    Box(modifier.padding(top = if (nextLabel != null) 10.dp else 0.dp)) {
        AzCard(
            Modifier.semantics(mergeDescendants = true) { stateDescription = accessibilityValue },
            onClick = onClick,
            compact = true,
            outline = if (nextLabel != null) colors.ink else null,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AzSpace.sm),
            ) {
                AzIconWell(
                    if (complete) AzSymbol.check else symbol,
                    state =
                        when {
                            complete -> AzWellState.On
                            nextLabel != null -> AzWellState.Spark
                            else -> AzWellState.Idle
                        },
                )
                Column(Modifier.weight(1f)) {
                    Text(title, style = Az.type.headline, color = colors.ink)
                    Text(
                        detail,
                        style =
                            if (detailIsTime) Az.type.callout.copy(fontFeatureSettings = "tnum")
                            else Az.type.footnote,
                        color = if (detailIsTime) colors.ink else colors.inkMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AzChevron()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AzSpace.sm),
            ) {
                AzProgressTrack(fraction, Modifier.weight(1f), complete = complete)
                Text(
                    progressLabel,
                    style = Az.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = if (complete) colors.forest else colors.ink,
                )
            }
        }
        if (nextLabel != null)
            AzSticker(
                nextLabel,
                Modifier.align(Alignment.TopEnd).padding(end = AzSpace.md).offset(y = (-14).dp).zIndex(1f),
                ornament = AzOrnamentShape.Star,
            )
    }
}

enum class AzStepState {
    Todo,
    Next,
    Done,
}

/**
 * The numbered circle of a journey's timeline. Tapping completes or reopens the step;
 * disabled while a choice is still needed.
 */
@Composable
fun AzStepMarker(
    number: String,
    state: AzStepState,
    accessibilityLabel: String,
    accessibilityState: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = Az.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(48.dp)
            .clickable(interaction, null, enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .semantics {
                contentDescription = accessibilityLabel
                stateDescription = accessibilityState
                selected = state == AzStepState.Done
            },
        contentAlignment = Alignment.Center,
    ) {
        val fill =
            when (state) {
                AzStepState.Done -> colors.forest
                AzStepState.Next -> colors.lime
                AzStepState.Todo -> colors.paperRaised
            }
        Box(
            Modifier
                .size(40.dp)
                .then(
                    if (state == AzStepState.Next) Modifier.azStamp(AzStamp.sm, CircleShape, interaction)
                    else Modifier
                )
                .background(fill, CircleShape)
                .border(
                    AzBorder.stamp,
                    if (state == AzStepState.Done) colors.forest else colors.stampLine,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (state == AzStepState.Done) AzIcon(AzSymbol.check, null, size = 20.dp, tint = colors.onForest)
            else
                Text(
                    number,
                    style = Az.type.headline.copy(fontSize = Az.type.callout.fontSize),
                    color = if (enabled) colors.ink else colors.inkSubtle,
                )
        }
    }
}

enum class AzRail {
    None,
    Dashed,
    Solid,
}

/** One step of a journey timeline: the marker and its rail, then the step's content. */
@Composable
fun AzStepRow(
    marker: @Composable () -> Unit,
    rail: AzRail,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Az.colors
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AzSpace.sm)) {
        Column(Modifier.width(48.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            marker()
            if (rail != AzRail.None)
                Box(
                    Modifier
                        .weight(1f)
                        .width(2.dp)
                        .padding(vertical = 4.dp)
                        .drawBehind {
                            val stroke = AzBorder.stamp.toPx()
                            drawLine(
                                if (rail == AzRail.Solid) colors.forest else colors.lineStrong,
                                Offset(size.width / 2, 0f),
                                Offset(size.width / 2, size.height),
                                stroke,
                                pathEffect =
                                    if (rail == AzRail.Dashed)
                                        PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                                    else null,
                            )
                        }
                )
        }
        Column(Modifier.weight(1f).padding(top = AzSpace.xs, bottom = AzSpace.lg), content = content)
    }
}

@Immutable
data class AzPrayerTime(val name: String, val time: String, val past: Boolean, val next: Boolean)

/**
 * The Next prayer card on Today: the coming prayer large, all five times beneath. A `sage`
 * stamped card; tapping it opens the prayer location.
 */
@Composable
fun AzPrayerStrip(
    eyebrow: String,
    nextName: String,
    remaining: String,
    times: List<AzPrayerTime>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Az.colors
    AzCard(
        modifier,
        onClick = onClick,
        tone = AzTone.Sage,
        stamped = true,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AzIcon(AzSymbol.location, null, size = 14.dp, tint = colors.inkSoft)
            AzEyebrow(eyebrow, color = colors.inkSoft)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(nextName, Modifier.weight(1f), style = Az.type.title2, color = colors.ink)
            Text(remaining, style = Az.type.callout, color = colors.inkSoft)
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            times.forEach { prayer ->
                Column(
                    Modifier
                        .weight(1f)
                        .then(
                            if (prayer.next)
                                Modifier
                                    .background(colors.lime, RoundedCornerShape(14.dp))
                                    .border(AzBorder.hairline, colors.ink, RoundedCornerShape(14.dp))
                            else Modifier
                        )
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val tone = if (prayer.past) colors.inkSoft else colors.ink
                    Text(
                        prayer.name,
                        style = Az.type.footnote.copy(fontWeight = if (prayer.next) FontWeight.SemiBold else null),
                        color = tone,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        prayer.time,
                        style = Az.type.headline.copy(fontFeatureSettings = "tnum"),
                        color = tone,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** True in right-to-left layouts. */
@Composable
internal fun isRtl() = LocalLayoutDirection.current == LayoutDirection.Rtl
