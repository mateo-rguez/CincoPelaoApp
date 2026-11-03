package io.github.mateorguez.cincopelao

import android.animation.ObjectAnimator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import io.github.mateorguez.cincopelao.data.prefs.NotiPrefsManager
import io.github.mateorguez.cincopelao.data.prefs.getDarkModeFlow
import io.github.mateorguez.cincopelao.data.prefs.setDarkMode
import io.github.mateorguez.cincopelao.notifications.NotificationUtils
import io.github.mateorguez.cincopelao.ui.navigation.AppScaffold
import io.github.mateorguez.cincopelao.ui.notifications.SetNotifications
import io.github.mateorguez.cincopelao.ui.theme.CincoPelaoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var isThemeReady = false
    private val notiPrefs by lazy { NotiPrefsManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashView ->
            val view = splashView.view
            val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
            fadeOut.duration = 200
            fadeOut.start()

            fadeOut.doOnEnd { splashView.remove() }
        }

        NotificationUtils.createNotificationChannel(this)

        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            !isThemeReady
        }

        lifecycleScope.launch {
            notiPrefs.initializeDefaultsIfNeeded()
        }

        setContent {

            val context = LocalContext.current
            val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)

            // Notis --------------------------------

            SetNotifications(context)

            // --------------------------------------

            LaunchedEffect(isDarkTheme) {
                delay(100)
                isThemeReady = true
            }

            CincoPelaoTheme(darkTheme = isDarkTheme) {
                AppScaffold(
                    darkTheme = isDarkTheme,
                    onToggleTheme = {
                        CoroutineScope(Dispatchers.Main).launch {
                            context.setDarkMode(!isDarkTheme)
                        }
                    }
                )
            }

        }
    }
}
