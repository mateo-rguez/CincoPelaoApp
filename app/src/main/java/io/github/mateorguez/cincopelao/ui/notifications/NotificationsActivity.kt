package io.github.mateorguez.cincopelao.ui.notifications

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.mateorguez.cincopelao.R
import io.github.mateorguez.cincopelao.data.local.DatabaseManager.currentDb
import io.github.mateorguez.cincopelao.data.prefs.NotiPrefsManager
import io.github.mateorguez.cincopelao.data.prefs.getDarkModeFlow
import io.github.mateorguez.cincopelao.notifications.ReminderScheduler
import io.github.mateorguez.cincopelao.notifications.scheduleReminders
import io.github.mateorguez.cincopelao.ui.MainViewModel
import io.github.mateorguez.cincopelao.ui.theme.CincoPelaoTheme
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)

            val scope = rememberCoroutineScope()

            val prefs = remember { NotiPrefsManager(context) }

            // Notis --------------------------------

            SetNotifications(context)

            // --------------------------------------

            CincoPelaoTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Notificaciones",
                                    fontFamily = FontFamily(Font(R.font.delight_medium))
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menú")
                                }
                            },
                        )
                    },
                    content = { paddingValues ->

                        val context = LocalContext.current

                        Box(modifier = Modifier.padding(paddingValues)) {
                            NotificationsScreen(
                                onSaveEvents = { days, startHour, endHour ->
                                    scope.launch {
                                        WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                                        ReminderScheduler.clearAllScheduledReminders(context)
                                        prefs.saveEventConfig(days, startHour, endHour)
                                        Toast.makeText(context, "Configuración de eventos guardada", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onSaveTasks = { days, startHour, endHour ->
                                    scope.launch {
                                        WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                                        ReminderScheduler.clearAllScheduledReminders(context)
                                        prefs.saveTaskConfig(days, startHour, endHour)
                                        Toast.makeText(context, "Configuración de tareas guardada", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    modifier: Modifier = Modifier,
    onSaveEvents: (daysBefore: List<Int>, startHour: Int, endHour: Int) -> Unit,
    onSaveTasks: (daysBefore: List<Int>, startHour: Int, endHour: Int) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = {2})
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { NotiPrefsManager(context) }

    // --- Estados externalizados para que persistan entre páginas ---
    var eventDays by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var eventStartHour by rememberSaveable { mutableStateOf(8) }
    var eventEndHour by rememberSaveable { mutableStateOf(22) }

    var taskDays by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var taskStartHour by rememberSaveable { mutableStateOf(8) }
    var taskEndHour by rememberSaveable { mutableStateOf(22) }

    // Inicializar con DataStore una sola vez
    LaunchedEffect(Unit) {
        eventDays = prefs.eventDaysFlow.first()
        eventStartHour = prefs.eventStartHourFlow.first()
        eventEndHour = prefs.eventEndHourFlow.first()

        taskDays = prefs.taskDaysFlow.first()
        taskStartHour = prefs.taskStartHourFlow.first()
        taskEndHour = prefs.taskEndHourFlow.first()
    }

    Column(modifier = modifier.fillMaxSize()) {
        val tabs = listOf("Eventos", "Tareas")
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
        )  {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) { page ->
            when (page) {
                0 -> NotificationConfigPage(
                    title = "Recordatorios para eventos",
                    daysBeforeState = eventDays,
                    startHourState = eventStartHour,
                    endHourState = eventEndHour,
                    onDaysBeforeChange = { eventDays = it },
                    onStartHourChange = { eventStartHour = it },
                    onEndHourChange = { eventEndHour = it },
                    isTaskConfig = false,
                    onSave = onSaveEvents
                )
                1 -> NotificationConfigPage(
                    title = "Recordatorios para tareas",
                    daysBeforeState = taskDays,
                    startHourState = taskStartHour,
                    endHourState = taskEndHour,
                    onDaysBeforeChange = { taskDays = it },
                    onStartHourChange = { taskStartHour = it },
                    onEndHourChange = { taskEndHour = it },
                    isTaskConfig = true,
                    onSave = onSaveTasks
                )
            }
        }
    }
}

@Composable
fun NotificationConfigPage(
    title: String,
    daysBeforeState: List<Int>,
    startHourState: Int,
    endHourState: Int,
    onDaysBeforeChange: (List<Int>) -> Unit,
    onStartHourChange: (Int) -> Unit,
    onEndHourChange: (Int) -> Unit,
    onSave: (List<Int>, Int, Int) -> Unit,
    isTaskConfig: Boolean // ← Para saber si estamos configurando eventos o tareas
) {
    val context = LocalContext.current
    val prefs = remember { NotiPrefsManager(context) }

    var showAddDayDialog by remember { mutableStateOf(false) }

    // Recuperamos los valores guardados en DataStore
    val storedDays by if (isTaskConfig) {
        prefs.taskDaysFlow.collectAsState(initial = emptyList())
    } else {
        prefs.eventDaysFlow.collectAsState(initial = emptyList())
    }

    val storedStartHour by if (isTaskConfig) {
        prefs.taskStartHourFlow.collectAsState(initial = 8)
    } else {
        prefs.eventStartHourFlow.collectAsState(initial = 8)
    }

    val storedEndHour by if (isTaskConfig) {
        prefs.taskEndHourFlow.collectAsState(initial = 22)
    } else {
        prefs.eventEndHourFlow.collectAsState(initial = 22)
    }

    // Variable reactiva que detecta cambios sin guardar
    val hasUnsavedChanges by remember(
        daysBeforeState,
        startHourState,
        endHourState,
        storedDays,
        storedStartHour,
        storedEndHour
    ) {
        derivedStateOf {
            daysBeforeState.sortedDescending() != storedDays.sortedDescending() ||
                    startHourState != storedStartHour ||
                    endHourState != storedEndHour
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 250.dp)
        ) {
            items(daysBeforeState) { day ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = if (day == 1) "$day día antes" else "$day días antes")
                        IconButton(onClick = { onDaysBeforeChange(daysBeforeState - day) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { showAddDayDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Añadir día")
        }

        Spacer(Modifier.height(16.dp))

        Text("Rango horario para las notificaciones", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            TimePickerButton(
                label = "Desde",
                hour = startHourState,
                onHourSelected = { selectedStart ->
                    if (selectedStart >= endHourState) {
                        Toast.makeText(
                            context,
                            "La hora de inicio no puede ser igual o posterior a la de fin",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        onStartHourChange(selectedStart)
                    }
                }
            )

            TimePickerButton(
                label = "Hasta",
                hour = endHourState,
                onHourSelected = { selectedEnd ->
                    if (selectedEnd <= startHourState) {
                        Toast.makeText(
                            context,
                            "La hora de fin no puede ser anterior o igual a la de inicio",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        onEndHourChange(selectedEnd)
                    }
                }
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { onSave(daysBeforeState, startHourState, endHourState) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar configuración")
        }

        // Si hay cambios sin guardar, mostramos aviso
        if (hasUnsavedChanges) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "⚠️ Tienes cambios sin guardar",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    if (showAddDayDialog) {
        AddDayDialog(
            onDismiss = { showAddDayDialog = false },
            onConfirm = { newDay ->
                if (newDay > 0 && newDay !in daysBeforeState) {
                    onDaysBeforeChange((daysBeforeState + newDay).sortedDescending())
                }
                showAddDayDialog = false
            }
        )
    }
}


@Composable
fun TimePickerButton(
    label: String,
    hour: Int,
    onHourSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val hourFormatted = String.format("%02d:00", hour)

    Button(
        onClick = {
            TimePickerDialog(
                context,
                { _, selectedHour, _ -> onHourSelected(selectedHour) },
                hour,
                0,
                true
            ).show()
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("$label: $hourFormatted")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDayDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var inputValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir día") },
        text = {
            OutlinedTextField(
                value = inputValue,
                onValueChange = { if (it.all { c -> c.isDigit() }) inputValue = it },
                label = { Text("Días antes") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = inputValue.toIntOrNull()
                    if (value != null && value > 0) onConfirm(value) else onDismiss()
                }
            ) {
                Text("Añadir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

class NotificationConfigState(
    initialDays: List<Int>,
    initialStartHour: Int,
    initialEndHour: Int
) {
    var daysBefore by mutableStateOf(initialDays.sortedDescending())
    var startHour by mutableStateOf(initialStartHour)
    var endHour by mutableStateOf(initialEndHour)
}

@Composable
fun SetNotifications(context: Context) {
    val mainViewModel: MainViewModel = hiltViewModel()

    val allSubjects by mainViewModel.allSubjects.collectAsState(initial = emptyList())
    val allEvents by mainViewModel.allEvents.collectAsState(initial = emptyList())
    val allTasks by mainViewModel.allTasks.collectAsState(initial = emptyList())
    val notiPrefs = remember { NotiPrefsManager(context) }

    val eventDays by notiPrefs.eventDaysFlow.collectAsState(initial = emptyList())
    val eventStartHour by notiPrefs.eventStartHourFlow.collectAsState(initial = 8)
    val eventEndHour by notiPrefs.eventEndHourFlow.collectAsState(initial = 22)

    val taskDays by notiPrefs.taskDaysFlow.collectAsState(initial = emptyList())
    val taskStartHour by notiPrefs.taskStartHourFlow.collectAsState(initial = 8)
    val taskEndHour by notiPrefs.taskEndHourFlow.collectAsState(initial = 22)

    LaunchedEffect(allSubjects, allEvents, allTasks, eventDays, eventStartHour, eventEndHour, taskDays, taskStartHour, taskEndHour) {
        val now = System.currentTimeMillis()
        val upcomingEvents = allEvents.filter { it.date > now }
        val upcomingTasks = allTasks.filter { task ->
            (task.dueDate?.let { it > now } ?: false) && !task.archived && !task.isCompleted
        }

        scheduleReminders(context, notiPrefs, upcomingEvents, upcomingTasks, allSubjects)
    }
}