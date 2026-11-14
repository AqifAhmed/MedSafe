package com.medsafe.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.medsafe.app.data.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = ClayPrimary,
    onPrimary = ClayOnPrimary,
    primaryContainer = ClayPrimaryContainer,
    onPrimaryContainer = ClayOnPrimaryContainer,
    inversePrimary = ClayInversePrimary,
    secondary = ClaySecondary,
    onSecondary = ClayOnSecondary,
    secondaryContainer = ClaySecondaryContainer,
    onSecondaryContainer = ClayOnSecondaryContainer,
    tertiary = ClayTertiary,
    onTertiary = ClayOnTertiary,
    tertiaryContainer = ClayTertiaryContainer,
    onTertiaryContainer = ClayOnTertiaryContainer,
    background = ClayBackground,
    onBackground = ClayOnBackground,
    surface = ClaySurface,
    onSurface = ClayOnSurface,
    surfaceVariant = ClaySurfaceVariant,
    onSurfaceVariant = ClayOnSurfaceVariant,
    surfaceTint = ClayPrimary,
    inverseSurface = ClayInverseSurface,
    inverseOnSurface = ClayInverseOnSurface,
    error = ClayError,
    onError = ClayOnError,
    errorContainer = ClayErrorContainer,
    onErrorContainer = ClayOnErrorContainer,
    outline = ClayOutline,
    outlineVariant = ClayOutlineVariant,
    surfaceBright = ClaySurfaceContainerLowest,
    surfaceDim = ClaySurfaceContainerHigh,
    surfaceContainerLowest = ClaySurfaceContainerLowest,
    surfaceContainerLow = ClaySurfaceContainerLow,
    surfaceContainer = ClaySurfaceContainer,
    surfaceContainerHigh = ClaySurfaceContainerHigh,
    surfaceContainerHighest = ClaySurfaceContainerHighest,
    scrim = androidx.compose.ui.graphics.Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = ClayDarkPrimary,
    onPrimary = ClayDarkOnPrimary,
    primaryContainer = ClayDarkPrimaryContainer,
    onPrimaryContainer = ClayDarkOnPrimaryContainer,
    inversePrimary = ClayDarkInversePrimary,
    secondary = ClayDarkSecondary,
    onSecondary = ClayDarkOnSecondary,
    secondaryContainer = ClayDarkSecondaryContainer,
    onSecondaryContainer = ClayDarkOnSecondaryContainer,
    tertiary = ClayDarkTertiary,
    onTertiary = ClayDarkOnTertiary,
    tertiaryContainer = ClayDarkTertiaryContainer,
    onTertiaryContainer = ClayDarkOnTertiaryContainer,
    background = ClayDarkBackground,
    onBackground = ClayDarkOnBackground,
    surface = ClayDarkSurface,
    onSurface = ClayDarkOnSurface,
    surfaceVariant = ClayDarkSurfaceVariant,
    onSurfaceVariant = ClayDarkOnSurfaceVariant,
    surfaceTint = ClayDarkPrimary,
    inverseSurface = ClayDarkInverseSurface,
    inverseOnSurface = ClayDarkInverseOnSurface,
    error = ClayDarkError,
    onError = ClayDarkOnError,
    errorContainer = ClayDarkErrorContainer,
    onErrorContainer = ClayDarkOnErrorContainer,
    outline = ClayDarkOutline,
    outlineVariant = ClayDarkOutlineVariant,
    surfaceBright = ClayDarkSurfaceContainerHighest,
    surfaceDim = ClayDarkSurfaceContainerLowest,
    surfaceContainerLowest = ClayDarkSurfaceContainerLowest,
    surfaceContainerLow = ClayDarkSurfaceContainerLow,
    surfaceContainer = ClayDarkSurfaceContainer,
    surfaceContainerHigh = ClayDarkSurfaceContainerHigh,
    surfaceContainerHighest = ClayDarkSurfaceContainerHighest,
    scrim = androidx.compose.ui.graphics.Color.Black,
)

/** Body copy starts at 16sp because the audience includes older users. */
val MedSafeTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)

val MedSafeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MedSafeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MedSafeTypography,
        shapes = MedSafeShapes,
        content = content,
    )
}
