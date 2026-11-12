package com.arthurnagy.dexlens

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.arthurnagy.dexlens.core.navigation.EntryProviderInstaller
import dagger.hilt.android.AndroidEntryPoint
import org.orbitmvi.orbit.compose.collectAsState
import javax.inject.Inject

/**
 * The single activity. Field injection is allowed only here and in other framework entry points
 * (@AndroidEntryPoint); everything else uses constructor injection.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** Every feature's Nav 3 entries, contributed by its impl module's `@IntoSet` entry module. */
    @Inject
    lateinit var entryProviderInstallers: Set<@JvmSuppressWildcards EntryProviderInstaller>

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate: swaps the launch theme (Theme.DexLens.Starting) for Theme.DexLens.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // The splash stays up until the stored theme is read (a DataStore read: milliseconds), so a theme picked
        // in-app never flashes the system's first; the app's first frame waits behind it.
        splashScreen.setKeepOnScreenCondition { viewModel.container.stateFlow.value.loading }
        splashScreen.setOnExitAnimationListener { splash ->
            splash.view
                .animate()
                .alpha(0f)
                .setDuration(SPLASH_FADE_MS)
                .withEndAction { splash.remove() }
                .start()
        }
        setContent {
            val state by viewModel.collectAsState()
            // Nothing renders until the stored theme is read: drawing in the default one first would
            // flash the wrong theme on a cold start.
            if (!state.loading) {
                val darkTheme =
                    when (state.themeMode) {
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK -> true
                        ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    }
                // The system bars draw over the edge-to-edge content, so their icons follow the app's theme —
                // a dark theme picked in-app on a light system gets light icons. enableEdgeToEdge()'s defaults
                // read the system's night mode instead; re-applied whenever the resolved theme changes.
                DisposableEffect(darkTheme) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                        navigationBarStyle = SystemBarStyle.auto(lightScrim, darkScrim) { darkTheme },
                    )
                    useLightCaptionBar(!darkTheme)
                    onDispose {}
                }
                AppTheme(themeMode = state.themeMode) {
                    AppNavigation(entryProviderInstallers = entryProviderInstallers)
                }
            }
        }
    }

    /**
     * Desktop windowing (Android 15+, Googlebook / tablets): the caption bar's Back / Minimize / Maximize / Close
     * buttons draw over the edge-to-edge content — match their tint to the app's theme, not the system's.
     */
    private fun useLightCaptionBar(light: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.insetsController?.setSystemBarsAppearance(
                if (light) WindowInsetsController.APPEARANCE_LIGHT_CAPTION_BARS else 0,
                WindowInsetsController.APPEARANCE_LIGHT_CAPTION_BARS,
            )
        }
    }

    private companion object {
        const val SPLASH_FADE_MS = 200L

        // enableEdgeToEdge()'s default navigation-bar scrims (behind three-button navigation on API 26–28).
        val lightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val darkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
