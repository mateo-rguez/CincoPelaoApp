package io.github.mateorguez.cincopelao.ui.subjects

import io.github.mateorguez.cincopelao.ui.theme.appColors
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.ui.theme.DelightMedium
import kotlinx.coroutines.launch

// Detalle de una asignatura: cabecera con la nota y lista de eventos.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    state: SubjectDetailUiState,
    onBackClick: () -> Unit,
    onEventChanged: (Event) -> Unit,
    onNavigateToStats: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            SubjectTopBar(
                title = state.subject?.name.orEmpty(),
                onMenuClick = onBackClick,
                onNavigateToStats = {
                    if (state.hasGradedEvents) {
                        onNavigateToStats()
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Para acceder a las estadísticas, tiene que haber al menos un evento puntuable.")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding)) {

            HeaderSection(grade = state.weightedGrade, meanScore = state.meanGrade)

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )

            EventStackList(
                events = state.events,
                subjectId = state.subject?.subjectId ?: -1L,
                onEventChanged = onEventChanged
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectTopBar(
    title: String,
    onMenuClick: () -> Unit,
    onNavigateToStats: () -> Unit
) {
    TopAppBar(
        title = { Text(text = title, fontFamily = DelightMedium) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.appColors.bar,
            titleContentColor = MaterialTheme.appColors.onBar,
            navigationIconContentColor = MaterialTheme.appColors.onBar,
            actionIconContentColor = MaterialTheme.appColors.onBar
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

    val appColors = MaterialTheme.appColors

    val targetGlowColor = when {
        meanScore == null -> appColors.glowNoScore
        meanScore == 10f -> appColors.glowPerfect
        meanScore >= 6f -> appColors.glowPass
        meanScore >= 4f -> appColors.glowRegular
        else -> appColors.glowFail
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
                        fontFamily = DelightMedium
                    ),
                    textAlign = TextAlign.Center, fontSize = 80.sp)
            }
        }
    }

    LaunchedEffect(number) {
        previousNumber.value = number
    }
}
