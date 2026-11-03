package io.github.mateorguez.cincopelao.ui.subjects

import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.mateorguez.cincopelao.R
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.prefs.getDarkModeFlow
import io.github.mateorguez.cincopelao.notifications.ReminderScheduler
import io.github.mateorguez.cincopelao.ui.notifications.SetNotifications
import io.github.mateorguez.cincopelao.ui.theme.CincoPelaoTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SubjectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val subjectId = intent.getIntExtra("subjectId", -1)

        setContent {
            val context = LocalContext.current
            val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)

            val subjectViewModel: SubjectViewModel = hiltViewModel()
            subjectViewModel.setSubjectId(subjectId)

            // Notis --------------------------------

            SetNotifications(context)

            // --------------------------------------

            val events by subjectViewModel.events.collectAsState()
            val weightedScore = remember(events) { subjectViewModel.calculateWeightedGrade(events) }
            val meanScore = remember(events) { subjectViewModel.calculateMeanGrade(events) }

            val navController = rememberNavController()

            CincoPelaoTheme(darkTheme = isDarkTheme) {
                NavHost(
                    navController = navController,
                    startDestination = "subject_screen"
                ) {
                    composable(route = "subject_screen",
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(200)
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(200)
                            )
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(200)
                            )
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(200)
                            )
                        }) {
                        SubjectScreen(
                            subjectId = subjectId,
                            events = events,
                            weightedScore = weightedScore,
                            meanScore = meanScore,
                            onBackClick = { finish() },
                            onEventChanged = { updatedEvent ->
                                subjectViewModel.updateEvent(updatedEvent)
                            },
                            onNavigateToStats = {
                                navController.navigate("stats_screen")
                            },
                            viewModel = subjectViewModel
                        )
                    }
                    composable(route = "stats_screen",
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(200)
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(200)
                            )
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(200)
                            )
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(200)
                            )
                        }) {
                        SubjectStatsScreen(
                            events = events,
                            onBackClick = {
                                navController.popBackStack()
                            },
                            viewModel = subjectViewModel
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectScreen(
    subjectId: Int,
    events: List<Event>,
    weightedScore: Float?,
    meanScore: Float?,
    onBackClick: () -> Unit,
    onEventChanged: (Event) -> Unit,
    onNavigateToStats: () -> Unit,
    viewModel: SubjectViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            SubjectTopBar(
                onMenuClick = onBackClick,
                onNavigateToStats = {
                    if (events.any { it.isPuntuable }) {
                        onNavigateToStats()
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Para acceder a las estadísticas, tiene que haber al menos un evento puntuable.")
                        }
                    }
                },
                viewModel = viewModel)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding)) {

            HeaderSection(grade = weightedScore, meanScore = meanScore)

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )

            EventStackList(
                events = events,
                subjectId = subjectId,
                onEventChanged = onEventChanged
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectTopBar(
    onMenuClick: () -> Unit,
    onNavigateToStats: () -> Unit,
    viewModel: SubjectViewModel
    ) {
    val selectedSubject by viewModel.subject.collectAsState()

    TopAppBar(
        title = { Text(text = "${selectedSubject?.name ?: ""}", fontFamily = FontFamily(Font(R.font.delight_medium))) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        },
        actions = {
            IconButton(onClick = { onNavigateToStats() }) {
                Icon(Icons.Default.QueryStats, contentDescription = "Estadísticas")
            }
        }
    )
}

@Composable
fun HeaderSection(
    grade: Float?,
    meanScore: Float?,
    modifier: Modifier = Modifier
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val headerHeight = screenHeight * 0.20f

    val gradeText = when {
        grade == null -> "Sin nota"
        grade % 1f == 0f -> "%.1f".format(grade)
        else -> "%.2f".format(grade)
    }

    val context = LocalContext.current
    val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)

    val targetGlowColor = when {
        meanScore == null -> if (isDarkTheme) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        meanScore == 10f -> if (isDarkTheme) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        meanScore >= 6f -> if (isDarkTheme) Color.Blue.copy(alpha = 0.3f) else Color.Yellow.copy(alpha = 0.5f)
        meanScore >= 4f -> if (isDarkTheme) Color(0xFFC57009).copy(alpha = 0.3f).copy(alpha = 0.3f) else Color(0xFFEF6D05).copy(alpha = 0.5f)
        else -> if (isDarkTheme) Color(0xFFFF0000).copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f)
    }

    val glowColor by animateColorAsState(
        targetValue = targetGlowColor,
        animationSpec = tween(durationMillis = 200),
        label = "GlowColorAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .height(headerHeight)
            .drawBehind {
                val radius = size.height * 0.8f

                // Luz izquierda
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = Offset(-size.width * 0.15f, size.height / 2),
                        radius = radius
                    ),
                    center = Offset(-size.width * 0.15f, size.height / 2),
                    radius = radius
                )

                // Luz derecha
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = Offset(size.width * 1.15f, size.height / 2),
                        radius = radius
                    ),
                    center = Offset(size.width * 1.15f, size.height / 2),
                    radius = radius
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Número animado, centrado
        AnimatedNumber(
            number = gradeText,
            modifier = Modifier
        )

        // Texto etéreo en la parte inferior (overlay)
        val status = when {
            meanScore == null -> "¡Suerte con los exámenes!"
            meanScore >= 9f -> "¡Excelente! Tu nota media es de %.2f".format(meanScore)
            meanScore >= 6f -> "Tu nota media es buena: %.2f".format(meanScore)
            meanScore >= 4f -> "Tu nota media es pocha: %.2f".format(meanScore)
            else -> "Tu nota media es baja: %.2f".format(meanScore)
        }

        if (status.isNotEmpty()) {
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedNumber(
    number: String,
    modifier: Modifier = Modifier
) {
    val previousNumber = remember { mutableStateOf(number) }

    Row(modifier = modifier) {
        for (i in number.indices) {
            val prevChar = previousNumber.value.getOrNull(i)
            val newChar = number[i]

            AnimatedContent(targetState = newChar, transitionSpec = {
                if (targetState > initialState) {
                    slideInVertically { -it } + fadeIn() togetherWith
                            slideOutVertically { it } + fadeOut()
                } else {
                    slideInVertically { it } + fadeIn() togetherWith
                            slideOutVertically { -it } + fadeOut()
                }
            }) { char ->
                Text(char.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily(Font(R.font.delight_medium))
                    ),
                    textAlign = TextAlign.Center, fontSize = 80.sp)
            }
        }
    }

    LaunchedEffect(number) {
        previousNumber.value = number
    }
}


@Composable
fun EventBox(
    event: Event,
    subjectId: Int,
    onScoreChange: (Float?) -> Unit,
    onDoneChange: (Boolean) -> Unit,
    onEventUpdate: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEditEventDialog by remember { mutableStateOf(false) }

    if (showEditEventDialog) {
        EventDialog(
            subjectId = subjectId,
            onDismiss = { showEditEventDialog = false },
            onEventCreated = { },
            onEventUpdated = { newEvent ->
                CoroutineScope(Dispatchers.IO).launch {
                    WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                    ReminderScheduler.clearAllScheduledReminders(context)
                }
                onEventUpdate(newEvent)
            },
            event = event
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp, 5.dp, 20.dp, 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(0.45f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily(Font(R.font.delight_medium))
                        ),
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.clickable{showEditEventDialog = true}
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { showEditEventDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                event.weight?.let {
                    Text("Ponderación: ${"%.0f".format(it)}%", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Nota mínima: ${
                            if (event.minScore == null || event.minScore == 0f) "N/A"
                            else "${"%.0f%%".format(event.minScore)}"
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                val formattedDate = remember(event.date) {
                    val formatter = SimpleDateFormat("d MMM", Locale.getDefault())
                    val raw = formatter.format(Date(event.date))
                    raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }

                Text("${event.type} ${event.modal ?: ""}", style = MaterialTheme.typography.bodySmall)
                Text("Fecha: $formattedDate", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(8.dp))

                val (statusText, statusColor) = getEventStatus(event)

                if(event.isPuntuable) {
                    EventStatusTag(
                        text = statusText,
                        backgroundColor = statusColor
                    )
                }
            }

            if (event.isPuntuable) {
                Column(
                    modifier = Modifier.weight(0.55f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val formattedScore = event.score?.let { String.format("%.2f", it) } ?: ""
                    var scoreText by remember { mutableStateOf(formattedScore) }

                    fun modifyScore(delta: Float) {
                        val current = scoreText.replace(',', '.').toFloatOrNull()
                        val updated: Float? = when {
                            current == null -> (delta).coerceIn(0f, 10f)
                            current == 0f && delta < 0f -> null
                            else -> (current + delta).coerceIn(0f, 10f)
                        }
                        if (updated == null) {
                            scoreText = ""
                            onScoreChange(null)
                        } else {
                            scoreText = "%.2f".format(updated)
                            onScoreChange(updated)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { modifyScore(1.0f) }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir 1")
                        }
                        IconButton(onClick = { modifyScore(0.1f) }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir 0.1")
                        }
                        IconButton(onClick = { modifyScore(0.01f) }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir 0.01")
                        }
                    }

                    OutlinedTextField(
                        value = scoreText,
                        onValueChange = { input ->
                            val sanitized = input.replace(',', '.')
                            val floatValue = sanitized.toFloatOrNull()
                            if (input.isBlank()) {
                                scoreText = ""
                                onScoreChange(null)
                            } else if (floatValue != null && floatValue in 0f..10f) {
                                scoreText = input
                                onScoreChange(floatValue)
                            }
                        },
                        placeholder = { Text("Nota") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            val floatValue = scoreText.replace(',', '.').toFloatOrNull()
                            if (floatValue == 0f) {
                                IconButton(
                                    onClick = {
                                        scoreText = ""
                                        onScoreChange(null)
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar Nota")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { modifyScore(-1.0f) }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar 1")
                        }
                        IconButton(onClick = { modifyScore(-0.1f) }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar 0.1")
                        }
                        IconButton(onClick = { modifyScore(-0.01f) }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar 0.01")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (statusText, statusColor) = getEventStatus(event)
                        var checked by remember { mutableStateOf(event.noPuntuableDone) }
                        EventStatusTag(
                            text = statusText,
                            backgroundColor = statusColor
                        )

                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                checked = it
                                onDoneChange(it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EventStatusTag(
    text: String,
    backgroundColor: Color,
    contentColor: Color = Color.White
) {
    // Animar el color de fondo y del texto
    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(durationMillis = 500)
    )

    val animatedContentColor by animateColorAsState(
        targetValue = contentColor,
        animationSpec = tween(durationMillis = 200)
    )

    // Animar el cambio de texto con AnimatedContent
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            fadeIn(tween(200)) togetherWith fadeOut(tween(200))
        },
        label = "StatusTextTransition"
    ) { animatedText ->
        Box(
            modifier = Modifier
                .background(
                    color = animatedBackgroundColor,
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = animatedText,
                color = animatedContentColor,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

fun getEventStatus(event: Event): Pair<String, Color> {
    return if (!event.isPuntuable) {
        if (event.noPuntuableDone) {
            "Hecho" to Color(0xFF4CAF50)
        } else {
            "Por hacer" to Color.Gray
        }
    } else {
        when {
            event.score == null -> "Sin nota" to Color.Gray
            event.minScore != null && event.score < event.minScore / 10 -> "Nota mínima" to Color(0xFF6E1800)
            event.score < 5 -> "Suspenso" to Color(0xFFD32F2F)
            event.score < 6 -> "Suficiente" to Color(0xFF4CAF50)
            event.score < 7 -> "Aceptable" to Color(0xFFFFA000)
            event.score < 9 -> "Notable" to Color(0xFF8D7728)
            event.score >= 9 -> "Sobresaliente" to Color(0xFF1976D2)
            else -> "Bien" to Color(0xFFFFA000)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventStackList(
    events: List<Event>,
    subjectId: Int,
    onEventChanged: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val modalityOrder = listOf("Teoría", "Prácticas", "Seminario", "Taller", null)

    val availableModalities = remember(events) {
        events.map { it.modal }
            .distinct()
            .sortedBy { modalityOrder.indexOf(it) }
    }

    val pagerState = rememberPagerState { availableModalities.size }
    val coroutineScope = rememberCoroutineScope()
    val hasTabs = availableModalities.size > 1

    fun getModalLabel(modal: String?): String = modal ?: "Sin modalidad"

    // Creamos flingBehavior para controlar la física del scroll y evitar stutter
    val flingBehavior = PagerDefaults.flingBehavior(state = pagerState)

    Column(modifier = modifier.fillMaxWidth()) {
        if (hasTabs) {
            val context = LocalContext.current
            val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)

            val contentColor = if (isDarkTheme) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.primary
            }

            val containerColor = MaterialTheme.colorScheme.background

            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = containerColor,
                contentColor = contentColor,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                            .height(3.dp),
                        color = contentColor
                    )
                },
                divider = {
                    HorizontalDivider(
                        color = contentColor.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                },
                modifier = Modifier.shadow(0.dp)
            ) {
                availableModalities.forEachIndexed { index, modal ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                val current = pagerState.currentPage
                                if (kotlin.math.abs(current - index) > 1) {
                                    // Salto largo sin animacion
                                    pagerState.scrollToPage(index)
                                } else {
                                    // Salto corto
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        },
                        text = {
                            Text(
                                getModalLabel(modal),
                                color = contentColor
                            )
                        },
                        modifier = Modifier
                            .shadow(0.dp)
                            .height(50.dp)
                    )
                }
            }
        }

        HorizontalPager(
            pageSize = PageSize.Fill,
            state = pagerState,
            flingBehavior = flingBehavior,
            pageSpacing = 8.dp,
            modifier = Modifier.fillMaxSize()
        ) { page ->

            // Animación visual simple con escala y alpha para suavizar transición
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val scale = 1f - kotlin.math.abs(pageOffset) * 0.05f
            val alpha = 1f - kotlin.math.abs(pageOffset) * 0.3f

            Box(
                Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .fillMaxSize()
            ) {
                val selectedModal = availableModalities[page]
                val filteredEvents = if (hasTabs) {
                    events.filter { it.modal == selectedModal }
                } else {
                    events
                }

                if (filteredEvents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay eventos en esta categoría",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(top = 0.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        val puntuables = filteredEvents.filter { it.isPuntuable }
                        val noPuntuables = filteredEvents.filterNot { it.isPuntuable }

                        itemsIndexed(puntuables, key = { _, event -> event.eventId }) { index, event ->

                            EventBox(
                                event = event,
                                subjectId = subjectId,
                                onScoreChange = { newScore -> onEventChanged(event.copy(score = newScore)) },
                                onEventUpdate = { newEvent -> onEventChanged(newEvent) },
                                onDoneChange = { newDone -> onEventChanged(event.copy(noPuntuableDone = newDone)) }
                            )

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        }

                        if (noPuntuables.isNotEmpty()) {
                            item {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.background)
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val context = LocalContext.current
                                        val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)
                                        val contentColor = if (isDarkTheme) {
                                            MaterialTheme.colorScheme.onBackground
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }

                                        Text(
                                            "No puntuables",
                                            color = contentColor,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }

                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            itemsIndexed(noPuntuables, key = { _, event -> event.eventId }) { _, event ->
                                Spacer(modifier = Modifier.height(8.dp))

                                EventBox(
                                    event = event,
                                    subjectId = subjectId,
                                    onScoreChange = { newScore -> onEventChanged(event.copy(score = newScore)) },
                                    onEventUpdate = { newEvent -> onEventChanged(newEvent) },
                                    onDoneChange = { newDone -> onEventChanged(event.copy(noPuntuableDone = newDone)) }
                                )

                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
