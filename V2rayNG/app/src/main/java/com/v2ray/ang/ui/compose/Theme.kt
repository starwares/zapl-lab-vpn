package com.v2ray.ang.ui.compose

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * "Minimal Trust" palette (Minimalism & Swiss Style, VPN & Privacy profile of ui-ux-pro-max):
 * navy for trust and primary actions, green reserved for the connected/success state, slate
 * neutrals, off-white instead of pure white backgrounds. Text pairs meet WCAG AA (4.5:1),
 * outlines 3:1, in both schemes.
 */
private val LightColor = lightColorScheme(
    primary = Color(0xFF1E3A5F), // Navy
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE7F4),
    onPrimaryContainer = Color(0xFF0B1F36),
    secondary = Color(0xFF2F5D8C), // Steel blue: headers, switches, progress
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2ECF7),
    onSecondaryContainer = Color(0xFF0F2A47),
    tertiary = Color(0xFF15803D), // Connected / success green
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCFCE7),
    onTertiaryContainer = Color(0xFF14532D),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFF8FAFC), // Slate 50
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F172A), // Slate 900
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569), // Slate 600
    outline = Color(0xFF64748B), // Slate 500
    outlineVariant = Color(0xFFE2E8F0), // Slate 200: dividers, card borders
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color(0xFFF1F5F9),
    inversePrimary = Color(0xFFA9C7EA),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF1E3A5F),
    surfaceContainerLowest = Color(0xFFFFFFFF), // Cards
    surfaceContainerLow = Color(0xFFF1F5F9),
    surfaceContainer = Color(0xFFEEF2F7),
    surfaceContainerHigh = Color(0xFFE8EDF3),
    surfaceContainerHighest = Color(0xFFE2E8F0),
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFFA9C7EA), // Light navy
    onPrimary = Color(0xFF0B2545),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFDCE7F4),
    secondary = Color(0xFF8DB4E2),
    onSecondary = Color(0xFF0B2545),
    secondaryContainer = Color(0xFF243B55),
    onSecondaryContainer = Color(0xFFD6E4F5),
    tertiary = Color(0xFF4ADE80), // Connected / success green
    onTertiary = Color(0xFF052E16),
    tertiaryContainer = Color(0xFF14532D),
    onTertiaryContainer = Color(0xFFBBF7D0),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF0F172A), // Slate 900
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8), // Slate 400
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF243044), // Dividers, card borders
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = Color(0xFF1E3A5F),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFFA9C7EA),
    surfaceContainerLowest = Color(0xFF0B1220),
    surfaceContainerLow = Color(0xFF131C2E),
    surfaceContainer = Color(0xFF1A2436), // Cards
    surfaceContainerHigh = Color(0xFF1F2A40),
    surfaceContainerHighest = Color(0xFF283449),
)

/** System font with a tighter, heavier heading scale; sizes stay in sp so user font scaling applies. */
private val AppTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        bodyLarge = base.bodyLarge.copy(letterSpacing = 0.sp),
        bodyMedium = base.bodyMedium.copy(letterSpacing = 0.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// Semantic Colors, resolved from the active scheme so they follow light, dark and dynamic color.
val colorPing: Color @Composable get() = MaterialTheme.colorScheme.tertiary
val colorPingRed: Color @Composable get() = MaterialTheme.colorScheme.error
val colorConfigType: Color @Composable get() = MaterialTheme.colorScheme.secondary

/** Card container: white on the off-white light background, one step above the background in dark. */
val colorCard: Color
    @Composable get() = if (LocalDarkTheme.current) MaterialTheme.colorScheme.surfaceContainer
    else MaterialTheme.colorScheme.surfaceContainerLowest

// Toast Colors 70%
val toastNormalBgLight = Color(0xB31E293B) // Slate 800
val toastNormalBgDark = Color(0xB3334155) // Slate 700
val toastSuccessBg = Color(0xB315803D) // Green 700
val toastErrorBg = Color(0xB3B91C1C) // Red 700
val toastInfoBg = Color(0xB31E3A5F) // Navy
val toastIconCircleBg = Color(0x33FFFFFF) // Semi-transparent White
val toastTextColor = Color.White // White

object ThemeManager {
    /** Off by default so the Zapl Lab palette is shown; users can still opt in to wallpaper colors. */
    const val DYNAMIC_COLOR_DEFAULT = false

    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, DYNAMIC_COLOR_DEFAULT)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
        _dynamicColorEnabled.value =
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, DYNAMIC_COLOR_DEFAULT)
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColor
        else -> LightColor
    }
    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
