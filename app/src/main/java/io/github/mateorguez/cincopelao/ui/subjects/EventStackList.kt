package io.github.mateorguez.cincopelao.ui.subjects

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.ui.theme.LocalDarkTheme
import kotlinx.coroutines.launch

// Lista de eventos de una asignatura, con una pestaña por modalidad.

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
            val isDarkTheme = LocalDarkTheme.current

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
                                        val isDarkTheme = LocalDarkTheme.current
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
