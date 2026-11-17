package io.github.mateorguez.cincopelao

import io.github.mateorguez.cincopelao.ui.theme.ThemeViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.Manifest
import android.animation.ObjectAnimator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import io.github.mateorguez.cincopelao.ui.schedule.ScheduleImageCache
import javax.inject.Inject
import io.github.mateorguez.cincopelao.notifications.NotificationUtils
import io.github.mateorguez.cincopelao.ui.navigation.AppScaffold
import io.github.mateorguez.cincopelao.ui.theme.CincoPelaoTheme
import io.github.mateorguez.cincopelao.data.prefs.ThemeMode
import io.github.mateorguez.cincopelao.data.prefs.ThemeSettings
import androidx.compose.foundation.isSystemInDarkTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var isThemeReady = false

    @Inject lateinit var scheduleImageCache: ScheduleImageCache

    // Desde Android 13 hay que pedir permiso para mostrar notificaciones
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashView ->
            val view = splashView.view
            val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
            fadeOut.duration = 200
            fadeOut.start()

            fadeOut.doOnEnd { splashView.remove() }
        }

        super.onCreate(savedInstanceState)
        // El horario (sobre todo si es un PDF) tarda en prepararse: se adelanta para que la
        // pestaña lo muestre al momento la primera vez
        scheduleImageCache.preload()

        splashScreen.setKeepOnScreenCondition {
            !isThemeReady
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationUtils.hasNotificationPermission(this)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val savedSettings by themeViewModel.settings.collectAsState()

            // La pantalla de carga se mantiene hasta saber qué tema usar, para no mostrar el equivocado
            LaunchedEffect(savedSettings) {
                if (savedSettings != null) isThemeReady = true
            }
            val settings = savedSettings ?: ThemeSettings()
            val isDarkTheme = settings.isDark(isSystemInDarkTheme())

            CincoPelaoTheme(darkTheme = isDarkTheme, palette = settings.palette, titleFont = settings.titleFont) {
                AppScaffold(
                    darkTheme = isDarkTheme,
                    onToggleTheme = {
                        themeViewModel.setMode(if (isDarkTheme) ThemeMode.LIGHT else ThemeMode.DARK)
                    }
                )
            }
        }
    }
}
