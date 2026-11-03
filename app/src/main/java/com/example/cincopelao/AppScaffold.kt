package com.example.cincopelao

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
import com.example.cincopelao.DatabaseManager.currentDb
import com.exyte.animatednavbar.AnimatedNavigationBar
import com.exyte.animatednavbar.animation.balltrajectory.Teleport
import com.exyte.animatednavbar.animation.indendshape.shapeCornerRadius
import com.exyte.animatednavbar.utils.noRippleClickable
import kotlinx.coroutines.launch

@Composable
fun AppScaffold(darkTheme: Boolean, onToggleTheme: () -> Unit) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentRoute = navController
        .currentBackStackEntryAsState().value?.destination?.route

    val showFab = currentRoute == BottomNavItem.Asignaturas.route

    var completedTasksToggle by remember { mutableStateOf(false) }

    var selectedBottomNavIndex by remember { mutableStateOf(2) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onToggleTheme = onToggleTheme,
                darkTheme = darkTheme,
                onItemSelected = { index ->
                    selectedBottomNavIndex = index
                    navController.navigate(BottomNavItem.entries[index].route)
                },
                closeDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    },
                    currentRoute = currentRoute,
                    onToggleTasksClick = { completedTasksToggle = !completedTasksToggle })
            },
            bottomBar = {
                AppBottomBar(
                    navController,
                    selectedBottomNavIndex,
                    onItemSelected = { index ->
                        selectedBottomNavIndex = index
                        navController.navigate(BottomNavItem.entries[index].route)
                    }
                )
            },
            floatingActionButton = {
                val context = LocalContext.current
                if (showFab) {
                    FloatingActionButton(
                        onClick = {
                            context.startActivity(Intent(context, AddEditSubjectActivity::class.java))
                        },
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
                startDestination = BottomNavItem.Asignaturas.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    BottomNavItem.Asignaturas.route,
                    enterTransition = {
                        slideInVertically(
                            initialOffsetY = { fullHeight -> -fullHeight },
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        slideOutVertically(
                            targetOffsetY = { fullHeight -> -fullHeight },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    }
                ) {
                    val activity = LocalContext.current as? Activity
                    BackHandler {
                        activity?.finish()
                    }
                    SubjectsScreen(snackbarHostState)
                }

                composable(
                    BottomNavItem.Tareas.route,
                    enterTransition = {
                        slideInVertically(
                            initialOffsetY = { fullHeight -> -fullHeight },
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        slideOutVertically(
                            targetOffsetY = { fullHeight -> -fullHeight },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    }
                ) {
                    val activity = LocalContext.current as? Activity
                    BackHandler {
                        activity?.finish()
                    }
                    TaskScreenManager(completedTasksToggle, snackbarHostState)
                }

                composable(
                    BottomNavItem.Horarios.route,
                    enterTransition = {
                        slideInVertically(
                            initialOffsetY = { fullHeight -> -fullHeight },
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        slideOutVertically(
                            targetOffsetY = { fullHeight -> -fullHeight },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    }
                ) {
                    val activity = LocalContext.current as? Activity
                    BackHandler {
                        activity?.finish()
                    }

                    val dbManager = DatabaseManager
                    val instanceId = dbManager.instances.find { it.db == currentDb }?.id
                    ScheduleScreen(instanceId!!)
                }

                composable(BottomNavItem.Calendario.route,
                    enterTransition = { fadeIn(animationSpec = tween(600)) },
                    exitTransition = { fadeOut(animationSpec = tween(600)) }
                ) {
                    val activity = LocalContext.current as? Activity
                    BackHandler {
                        activity?.finish()
                    }
                    CalendarScreen()
                }

                composable(
                    BottomNavItem.ShareMode.route,
                    enterTransition = {
                        slideInVertically(
                            initialOffsetY = { fullHeight -> -fullHeight },
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        slideOutVertically(
                            targetOffsetY = { fullHeight -> -fullHeight },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    }
                ) {
                    val activity = LocalContext.current as? Activity
                    BackHandler {
                        activity?.finish()
                    }
                    val context = LocalContext.current
                    ShareModeScreen(
                        onTransmitClick = {
                            val intent = Intent(context, TransmitActivity::class.java)
                            context.startActivity(intent)
                        },
                        onReceiveClick = {
                            val intent = Intent(context, ReceiveActivity::class.java)
                            context.startActivity(intent)
                        }
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
    onItemSelected: (Int) -> Unit,
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
                onItemSelected(item.ordinal)
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
                val intent = Intent(context, InstancesActivity::class.java)
                context.startActivity(intent)
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
                val intent = Intent(context, NotificationsActivity::class.java)
                context.startActivity(intent)
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
    currentRoute: String?,
    onToggleTasksClick: () -> Unit) {
    var isShowingCompleted by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(text = "Cinco Pelao", fontFamily = FontFamily(Font(R.font.delight_medium))) },
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
                visible = currentRoute == "tareas",
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(onClick = {
                    isShowingCompleted = !isShowingCompleted
                    onToggleTasksClick()
                }) {
                    Crossfade(targetState = isShowingCompleted, label = "ToggleIcon", animationSpec = tween(500)) { isShowingCompleted ->
                        Icon(
                            imageVector = if (isShowingCompleted) Icons.Default.Done else Icons.Default.DoneAll,
                            contentDescription = if (isShowingCompleted) "Ver tareas actuales" else "Ver tareas completadas"
                        )
                    }
                }
            }
        }
    )
}

enum class BottomNavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Horarios("horarios", "Horarios", Icons.Default.Schedule),
    Tareas("tareas", "Tareas", Icons.AutoMirrored.Filled.List),
    Asignaturas("asignaturas", "Materias", Icons.AutoMirrored.Filled.MenuBook),
    Calendario("calendario", "Calendario", Icons.Default.CalendarToday),
    ShareMode("sharemode", "ShareMode", Icons.Default.Share)
}

enum class NavigationBarItems(val icon: ImageVector, val label: String) {
    horarios(icon = Icons.Default.Schedule, "Horarios"),
    tareas(icon = Icons.AutoMirrored.Filled.List, "Tareas"),
    asignaturas(icon = Icons.AutoMirrored.Filled.MenuBook, "Materias"),
    calendario(icon = Icons.Default.CalendarToday, "Calendario"),
    sharemode(icon = Icons.Default.Share, "ShareMode")
}

@Composable
fun AppBottomBar(
    navController: NavHostController,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    val navigationBarItems = remember { NavigationBarItems.values() }
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
            selectedIndex = selectedIndex,
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
            navigationBarItems.forEach { item ->
                val animatedColor by animateColorAsState(
                    targetValue = if (selectedIndex == item.ordinal) Color.White else Color(
                        0xFFBEC0FF
                    ),
                    animationSpec = tween(durationMillis = 300)
                )
                Box(
                    modifier = Modifier.fillMaxSize().noRippleClickable {
                        if (selectedIndex != item.ordinal) {
                            onItemSelected(item.ordinal)
                            navController.navigate(item.name)
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            modifier = Modifier.size(26.dp),
                            imageVector = item.icon,
                            contentDescription = "BottomNavBar",
                            tint = animatedColor
                        )
                        Text(
                            text = item.label,
                            color = animatedColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily(Font(R.font.delight_medium)),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskScreenManager(completedTaskToggle: Boolean, snackbarHostState: SnackbarHostState) {
    Crossfade(targetState = completedTaskToggle, label = "TasksCrossfade", animationSpec = tween(500)) { showCompleted ->
        if (showCompleted) CompletedTasksScreen(snackbarHostState) else TasksScreen()
    }
}
