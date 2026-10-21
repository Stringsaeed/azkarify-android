package sh.saeed.azkarify.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import sh.saeed.azkarify.R

val SpaceGrotesk =
    FontFamily(
        Font(R.font.space_grotesk_regular, FontWeight.Normal),
        Font(R.font.space_grotesk_medium, FontWeight.Medium),
        Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    )

val AlanSans =
    FontFamily(
        Font(R.font.alan_regular, FontWeight.Normal),
        Font(R.font.alan_medium, FontWeight.Medium),
        Font(R.font.alan_semibold, FontWeight.SemiBold),
        Font(R.font.alan_bold, FontWeight.Bold),
    )

private val LatinType = azLatinTypography(SpaceGrotesk, AlanSans)
private val ArabicType = azArabicTypography(SpaceGrotesk, AlanSans)

/** The design system's one curve: ease-out (0.23, 1, 0.32, 1). */
val AzEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

fun <T> azTween(duration: Int = AzDuration.base): FiniteAnimationSpec<T> =
    tween(duration, easing = AzEasing)

val LocalAzColors = staticCompositionLocalOf { AzDayColors }
val LocalAzType = staticCompositionLocalOf { LatinType }

/** True when the person turned animations off; movement becomes a short fade. */
val LocalAzReduceMotion = staticCompositionLocalOf { false }

/** Shorthand for the current tokens: `Az.colors.ink`, `Az.type.headline`. */
object Az {
    val colors: AzColors
        @Composable @ReadOnlyComposable get() = LocalAzColors.current

    val type: AzTypography
        @Composable @ReadOnlyComposable get() = LocalAzType.current

    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalAzReduceMotion.current
}

fun azColors(dark: Boolean, accent: AzAccent): AzColors {
    val base = if (dark) AzNightColors else AzDayColors
    return base.copy(accent = if (dark) accent.night else accent.day)
}

/**
 * Applies the Azkarify tokens and maps them onto Material 3 so native components (bars, sheets,
 * menus, dialogs, pickers) pick up the brand without per-call colours. Dynamic colour is not
 * used: the accent is the person's choice.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AzTheme(dark: Boolean, accent: AzAccent, arabic: Boolean, content: @Composable () -> Unit) {
    val colors = remember(dark, accent) { azColors(dark, accent) }
    val type = if (arabic) ArabicType else LatinType
    val scheme = remember(colors) { colors.toColorScheme() }
    val typography = remember(type) { type.toMaterial() }
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
    CompositionLocalProvider(
        LocalAzColors provides colors,
        LocalAzType provides type,
        LocalAzReduceMotion provides reduceMotion,
    ) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = AzShapes,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

private val AzShapes =
    Shapes(
        extraSmall = RoundedCornerShape(AzRadius.xs),
        small = RoundedCornerShape(AzRadius.sm),
        medium = RoundedCornerShape(AzRadius.md),
        large = RoundedCornerShape(AzRadius.lg),
        extraLarge = RoundedCornerShape(AzRadius.xl),
    )

private fun AzColors.toColorScheme(): ColorScheme {
    val scheme = if (isDark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = paperSunk,
        onPrimaryContainer = ink,
        inversePrimary = accent,
        // The navigation indicator and selected chips sit on secondaryContainer: lime, like the
        // website's sticker.
        secondary = ink,
        onSecondary = paper,
        secondaryContainer = lime,
        onSecondaryContainer = ink,
        tertiary = forest,
        onTertiary = onForest,
        tertiaryContainer = sage,
        onTertiaryContainer = ink,
        background = paper,
        onBackground = ink,
        surface = paper,
        onSurface = ink,
        surfaceVariant = paperSunk,
        onSurfaceVariant = inkMuted,
        surfaceTint = paper,
        surfaceBright = paperRaised,
        surfaceDim = paperSunk,
        surfaceContainerLowest = paperRaised,
        surfaceContainerLow = paperRaised,
        surfaceContainer = paperRaised,
        surfaceContainerHigh = paperRaised,
        surfaceContainerHighest = paperSunk,
        inverseSurface = ink,
        inverseOnSurface = paper,
        outline = lineStrong,
        outlineVariant = line,
        scrim = stampShadow,
    )
}

private fun AzTypography.toMaterial(): Typography {
    fun TextStyle.emphasized() = copy(fontWeight = FontWeight.Bold)
    val label = footnote.copy(fontWeight = FontWeight.SemiBold)
    return Typography().copy(
        displayLarge = numeral,
        displayMedium = display,
        displaySmall = display,
        headlineLarge = title1,
        headlineMedium = title1,
        headlineSmall = title2,
        titleLarge = title3,
        titleMedium = headline,
        titleSmall = rowTitle,
        bodyLarge = body,
        bodyMedium = callout,
        bodySmall = footnote,
        labelLarge = button,
        labelMedium = label,
        labelSmall = label,
        displayLargeEmphasized = numeral.emphasized(),
        displayMediumEmphasized = display.emphasized(),
        displaySmallEmphasized = display.emphasized(),
        headlineLargeEmphasized = title1.emphasized(),
        headlineMediumEmphasized = title1.emphasized(),
        headlineSmallEmphasized = title2.emphasized(),
        titleLargeEmphasized = title3.emphasized(),
        titleMediumEmphasized = headline.emphasized(),
        titleSmallEmphasized = rowTitle.emphasized(),
        bodyLargeEmphasized = body.copy(fontWeight = FontWeight.Medium),
        bodyMediumEmphasized = callout.copy(fontWeight = FontWeight.Medium),
        bodySmallEmphasized = footnote.copy(fontWeight = FontWeight.Medium),
        labelLargeEmphasized = button.emphasized(),
        labelMediumEmphasized = label.emphasized(),
        labelSmallEmphasized = label.emphasized(),
    )
}
