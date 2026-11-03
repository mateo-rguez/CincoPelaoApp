package io.github.mateorguez.cincopelao.ui.subjects

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.mateorguez.cincopelao.ui.theme.CincoPelaoAppTheme

@AndroidEntryPoint
class SubjectActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // El ViewModel lee el id de la asignatura de los extras del intent
            val viewModel: SubjectViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val navController = rememberNavController()

            CincoPelaoAppTheme {
                NavHost(navController = navController, startDestination = ROUTE_DETAIL) {
                    composable(
                        route = ROUTE_DETAIL,
                        enterTransition = { slideIntoContainer(SlideDirection.Left, tween(200)) },
                        exitTransition = { slideOutOfContainer(SlideDirection.Left, tween(200)) },
                        popEnterTransition = { slideIntoContainer(SlideDirection.Right, tween(200)) },
                        popExitTransition = { slideOutOfContainer(SlideDirection.Right, tween(200)) }
                    ) {
                        SubjectDetailScreen(
                            state = state,
                            onBackClick = { finish() },
                            onEventChanged = viewModel::updateEvent,
                            onNavigateToStats = { navController.navigate(ROUTE_STATS) }
                        )
                    }
                    composable(
                        route = ROUTE_STATS,
                        enterTransition = { slideIntoContainer(SlideDirection.Left, tween(200)) },
                        exitTransition = { slideOutOfContainer(SlideDirection.Left, tween(200)) },
                        popEnterTransition = { slideIntoContainer(SlideDirection.Right, tween(200)) },
                        popExitTransition = { slideOutOfContainer(SlideDirection.Right, tween(200)) }
                    ) {
                        SubjectStatsScreen(state = state, onBackClick = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    companion object {
        /** Extra con el id (Long) de la asignatura a mostrar. */
        const val EXTRA_SUBJECT_ID = "subjectId"

        private const val ROUTE_DETAIL = "subject_screen"
        private const val ROUTE_STATS = "stats_screen"

        fun intent(context: Context, subjectId: Long): Intent =
            Intent(context, SubjectActivity::class.java).putExtra(EXTRA_SUBJECT_ID, subjectId)
    }
}
