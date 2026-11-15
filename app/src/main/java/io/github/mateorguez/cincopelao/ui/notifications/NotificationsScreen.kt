package io.github.mateorguez.cincopelao.ui.notifications

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import io.github.mateorguez.cincopelao.notifications.ReminderSettings
import io.github.mateorguez.cincopelao.ui.theme.appColors
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Ajustes de los recordatorios de eventos y de tareas, en dos pestañas. Todo se guarda al momento. */
@Composable
fun NotificationsScreen(modifier: Modifier = Modifier, viewModel: NotificationsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    // Se vuelve a comprobar al volver de los ajustes del sistema
    var notificationsEnabled by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (!notificationsEnabled) {
            NotificationsDisabledBanner(onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            })
        }

        val contentColor = MaterialTheme.appColors.accent
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = contentColor,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                        .height(3.dp),
                    color = contentColor
                )
            },
            divider = { HorizontalDivider(color = contentColor.copy(alpha = 0.3f), thickness = 1.dp) }
        ) {
            listOf("Eventos", "Tareas").forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(state = pagerState, verticalAlignment = Alignment.Top) { page ->
            val kind = if (page == 0) ReminderKind.Events else ReminderKind.Tasks
            // Hasta que se leen los ajustes guardados no se muestra nada
            state.settings(kind)?.let { settings ->
                ReminderSettingsPage(
                    kind = kind,
                    settings = settings,
                    onToggleDay = { viewModel.toggleDay(kind, it) },
                    onAddDay = { viewModel.addDay(kind, it) },
                    onHoursChange = { start, end -> viewModel.setHours(kind, start, end) }
                )
            }
        }
    }
}

/** Aviso cuando el sistema tiene las notificaciones de la app desactivadas. */
@Composable
private fun NotificationsDisabledBanner(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Las notificaciones están desactivadas, así que no recibirás recordatorios.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onOpenSettings) { Text("Activar") }
        }
    }
}

/** Una pestaña: frase que lo resume, pastillas de días y franja horaria. */
@Composable
private fun ReminderSettingsPage(
    kind: ReminderKind,
    settings: ReminderSettings,
    onToggleDay: (Int) -> Unit,
    onAddDay: (Int) -> Unit,
    onHoursChange: (Int, Int) -> Unit
) {
    var showAddDayDialog by remember { mutableStateOf(false) }
    // Mientras se arrastra el slider se muestra sin guardar; se guarda al soltar
    var dragging by remember { mutableStateOf<ClosedFloatingPointRange<Float>?>(null) }
    val range = dragging ?: settings.startHour.toFloat()..settings.endHour.toFloat()
    val shown = settings.copy(startHour = range.start.roundToInt(), endHour = range.endInclusive.roundToInt())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        SummaryCard(reminderSummaryParts(kind, shown))

        SectionTitle(if (kind == ReminderKind.Events) "Avisar antes de cada evento" else "Avisar antes de cada tarea")
        // Rejilla de 4 columnas que ocupa todo el ancho; «+ Otro» (null) al final
        val pills: List<DayChip?> = dayChips(settings.daysBefore) + null
        Column(verticalArrangement = Arrangement.spacedBy(PillGap)) {
            pills.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(PillGap)) {
                    row.forEach { chip ->
                        if (chip != null) {
                            DayPill(dayLabel(chip.day), chip.selected, onClick = { onToggleDay(chip.day) }, modifier = Modifier.weight(1f))
                        } else {
                            DayPill("Otro", selected = false, icon = Icons.Default.Add, onClick = { showAddDayDialog = true }, modifier = Modifier.weight(1f))
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        SectionTitle("Franja horaria")
        Row(horizontalArrangement = Arrangement.spacedBy(PillGap)) {
            HourBox("Desde", hourLabel(shown.startHour), Modifier.weight(1f))
            HourBox("Hasta", hourLabel(shown.endHour), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        RangeSlider(
            value = range,
            onValueChange = { new ->
                // Al menos una hora de franja
                val start = new.start.roundToInt().coerceIn(0, 23)
                val end = new.endInclusive.roundToInt().coerceIn(start + 1, 24)
                dragging = start.toFloat()..end.toFloat()
            },
            onValueChangeFinished = {
                dragging?.let { onHoursChange(it.start.roundToInt(), it.endInclusive.roundToInt()) }
                dragging = null
            },
            valueRange = 0f..24f,
            steps = 23
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "6", "12", "18", "24").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showAddDayDialog) {
        AddDayDialog(
            onDismiss = { showAddDayDialog = false },
            onConfirm = { day ->
                onAddDay(day)
                showAddDayDialog = false
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 22.dp, bottom = 10.dp)
    )
}

private val PillGap = 8.dp
private val PillShape = RoundedCornerShape(14.dp)

/** Tarjeta con la campana y la frase que resume los ajustes, con días y horas resaltados. */
@Composable
private fun SummaryCard(parts: List<SummaryPart>) {
    val highlight = MaterialTheme.appColors.accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.appColors.taskCard, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = buildAnnotatedString {
                parts.forEach { part ->
                    if (part.highlight) {
                        withStyle(SpanStyle(color = highlight, fontWeight = FontWeight.Bold)) { append(part.text) }
                    } else {
                        append(part.text)
                    }
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/** Pastilla de un día: rellena del color de los controles cuando está activada. */
@Composable
private fun DayPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "fondo"
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground, label = "texto"
    )
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(PillShape)
            .background(container)
            .then(if (selected) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, PillShape))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(it, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/** Una de las dos horas de la franja, en grande. */
@Composable
private fun HourBox(label: String, hour: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.appColors.taskCard, PillShape)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = hour,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/** Días de antelación escritos a mano, para lo que no está en las pastillas. */
@Composable
private fun AddDayDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var inputValue by remember { mutableStateOf("") }
    val value = inputValue.toIntOrNull()
    val valid = value != null && value in 1..MAX_REMINDER_DAYS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Otro día") },
        text = {
            OutlinedTextField(
                value = inputValue,
                onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) inputValue = it },
                label = { Text("Días antes") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { value?.let(onConfirm) }) { Text("Añadir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
