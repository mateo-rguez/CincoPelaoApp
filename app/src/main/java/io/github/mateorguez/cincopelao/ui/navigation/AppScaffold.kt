package io.github.mateorguez.cincopelao.ui.navigation

import io.github.mateorguez.cincopelao.ui.theme.NavBarUnselected
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavBackStackEntry
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.AnimatedContentTransitionScope
import io.github.mateorguez.cincopelao.ui.theme.DelightMedium
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.exyte.animatednavbar.AnimatedNavigationBar
import com.exyte.animatednavbar.animation.balltrajectory.Teleport
import com.exyte.animatednavbar.animation.indendshape.shapeCornerRadius
import com.exyte.animatednavbar.utils.noRippleClickable
import io.github.mateorguez.cincopelao.R
import io.github.mateorguez.cincopelao.ui.calendar.CalendarScreen
import io.github.mateorguez.cincopelao.ui.instances.InstancesActivity
import io.github.mateorguez.cincopelao.ui.notifications.NotificationsActivity
import io.github.mateorguez.cincopelao.ui.schedule.ScheduleScreen
import io.github.mateorguez.cincopelao.ui.sharing.ReceiveActivity
import io.github.mateorguez.cincopelao.ui.sharing.ShareModeScreen
import io.github.mateorguez.cincopelao.ui.sharing.TransmitActivity
import io.github.mateorguez.cincopelao.ui.subjects.AddEditSubjectActivity
import io.github.mateorguez.cincopelao.ui.subjects.SubjectsScreen
import io.github.mateorguez.cincopelao.ui.tasks.CompletedTasksScreen
import io.github.mateorguez.cincopelao.ui.tasks.TasksScreen
import kotlinx.coroutines.launch

/** Navega a una pestaña sin apilar pantallas repetidas y conservando el estado de cada pestaña. */
private fun NavHostController.navigateToTab(item: BottomNavItem) {
    navigate(item.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// Transiciones entre pestañas: la nueva entra desde arriba y la anterior sale hacia abajo
private val tabEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInVertically(initialOffsetY = { -it }, animationSpec = tween(300)) + fadeIn(tween(300))
}
private val tabExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(300))
}
private val tabPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(tween(300))
}
private val tabPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(300)) + fadeOut(tween(300))
}

/** Pestaña con las transiciones comunes; el botón atrás cierra la app desde cualquier pestaña. */
private fun NavGraphBuilder.tab(
    item: BottomNavItem,
    enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = tabEnter,
    exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = tabExit,
    popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = tabPopEnter,
    popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = tabPopExit,
    content: @Composable () -> Unit
) {
    composable(item.route, enterTransition = enter, exitTransition = exit, popEnterTransition = popEnter, popExitTransition = popExit) {
        val activity = LocalContext.current as? Activity
        BackHandler { activity?.finish() }
        content()
    }
}

@Composable
fun AppScaffold(darkTheme: Boolean, onToggleTheme: () -> Unit) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val selectedItem = BottomNavItem.entries.find { it.route == currentRoute } ?: START_TAB

    var showCompletedTasks by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onToggleTheme = onToggleTheme,
                darkTheme = darkTheme,
                onItemSelected = navController::navigateToTab,
                closeDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    showTasksToggle = selectedItem == BottomNavItem.Tareas,
                    showCompletedTasks = showCompletedTasks,
                    onToggleTasksClick = { showCompletedTasks = !showCompletedTasks }
                )
            },
            bottomBar = {
                AppBottomBar(
                    selectedItem = selectedItem,
                    onItemSelected = navController::navigateToTab
                )
            },
            floatingActionButton = {
                if (selectedItem == BottomNavItem.Asignaturas) {
                    FloatingActionButton(
                        onClick = { context.startActivity(Intent(context, AddEditSubjectActivity::class.java)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir asignatura")
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = START_TAB.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                tab(BottomNavItem.Asignaturas) { SubjectsScreen(snackbarHostState) }

                tab(BottomNavItem.Tareas) { TaskScreenManager(showCompletedTasks, snackbarHostState) }

                tab(BottomNavItem.Horarios) { ScheduleScreen() }

                tab(
                    BottomNavItem.Calendario,
                    enter = { fadeIn(tween(600)) },
                    exit = { fadeOut(tween(600)) },
                    popEnter = { fadeIn(tween(600)) },
                    popExit = { fadeOut(tween(600)) }
                ) { CalendarScreen() }

                tab(BottomNavItem.ShareMode) {
                    ShareModeScreen(
                        onTransmitClick = { context.startActivity(Intent(context, TransmitActivity::class.java)) },
                        onReceiveClick = { context.startActivity(Intent(context, ReceiveActivity::class.java)) }
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    onToggleTheme: () -> Unit,
    darkTheme: Boolean,
    onItemSelected: (BottomNavItem) -> Unit,
    closeDrawer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .background(MaterialTheme.colorScheme.primary)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Navegación",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }

        Spacer(Modifier.height(16.dp))

        BottomNavItem.entries.forEach { item ->
            TextButton(onClick = {
                onItemSelected(item)
                closeDrawer()
            }, modifier = Modifier.padding(start = 8.dp)) {
                Icon(item.icon, contentDescription = item.label, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(8.dp))
                Text(item.label, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Spacer(Modifier.height(24.dp))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            thickness = 1.dp,
            modifier = Modifier.padding(8.dp)
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .padding(start = 19.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DarkMode,
                contentDescription = "Modo Oscuro",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Modo Oscuro",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = darkTheme,
                onCheckedChange = { onToggleTheme() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ), modifier = Modifier.padding(end = 19.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        val context = LocalContext.current
        TextButton(
            onClick = {
                closeDrawer() // al volver, el menú ya no está abierto
                context.startActivity(Intent(context, InstancesActivity::class.java))
            },
            modifier = Modifier
                .padding(start = 8.dp)
        ) {
            Icon(Icons.Default.DataArray, contentDescription = "Instancias", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(8.dp))
            Text("Instancias", color = MaterialTheme.colorScheme.onPrimaryContainer)
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = {
                closeDrawer()
                context.startActivity(Intent(context, NotificationsActivity::class.java))
            },
            modifier = Modifier
                .padding(start = 8.dp)
        ) {
            Icon(Icons.Default.Notifications, contentDescription = "Notificaciones", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(8.dp))
            Text("Notificaciones", color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    onMenuClick: () -> Unit,
    showTasksToggle: Boolean,
    showCompletedTasks: Boolean,
    onToggleTasksClick: () -> Unit
) {
    TopAppBar(
        title = { Text(text = "Cinco Pelao", fontFamily = DelightMedium) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menú")
            }
        },
        actions = {
            AnimatedVisibility(
                visible = showTasksToggle,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(onClick = onToggleTasksClick) {
                    Crossfade(targetState = showCompletedTasks, label = "ToggleIcon", animationSpec = tween(500)) { showingCompleted ->
                        Icon(
                            imageVector = if (showingCompleted) Icons.Default.Done else Icons.Default.DoneAll,
                            contentDescription = if (showingCompleted) "Ver tareas actuales" else "Ver tareas completadas"
                        )
                    }
                }
            }
        }
    )
}

/** Pestañas de la navegación principal, en el orden en que aparecen en la barra inferior. */
enum class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    Horarios("horarios", "Horarios", Icons.Default.Schedule),
    Tareas("tareas", "Tareas", Icons.AutoMirrored.Filled.List),
    Asignaturas("asignaturas", "Materias", Icons.AutoMirrored.Filled.MenuBook),
    Calendario("calendario", "Calendario", Icons.Default.CalendarToday),
    ShareMode("sharemode", "ShareMode", Icons.Default.Share)
}

private val START_TAB = BottomNavItem.Asignaturas

@Composable
fun AppBottomBar(
    selectedItem: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit
) {
    val duration = 500

    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp + bottomPadding)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.primary)
        )
        AnimatedNavigationBar(
            modifier = Modifier
                .navigationBarsPadding()
                .height(100.dp),
            selectedIndex = selectedItem.ordinal,
            cornerRadius = shapeCornerRadius(
                topLeft = 10.dp,
                topRight = 10.dp,
                bottomLeft = 0.dp,
                bottomRight = 0.dp
            ),
            barColor = MaterialTheme.colorScheme.primary,
            ballColor = MaterialTheme.colorScheme.primary,
            ballAnimation = Teleport(tween(duration, easing = LinearOutSlowInEasing)),
        ) {
            BottomNavItem.entries.forEach { item ->
                val animatedColor by animateColorAsState(
                    targetValue = if (selectedItem == item) Color.White else NavBarUnselected,
                    animationSpec = tween(durationMillis = 300)
                )
                Box(
                    modifier = Modifier.fillMaxSize().noRippleClickable {
                        if (selectedItem != item) onItemSelected(item)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            modifier = Modifier.size(26.dp),
                            imageVector = item.icon,
                            contentDescription = null, // el texto de debajo ya lo describe
                            tint = animatedColor
                        )
                        Text(
                            text = item.label,
                            color = animatedColor,
                            fontSize = 10.sp,
                            fontFamily = DelightMedium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskScreenManager(showCompleted: Boolean, snackbarHostState: SnackbarHostState) {
    Crossfade(targetState = showCompleted, label = "TasksCrossfade", animationSpec = tween(500)) { showCompleted ->
        if (showCompleted) CompletedTasksScreen(snackbarHostState) else TasksScreen()
    }
}
