package io.github.mateorguez.cincopelao.ui.notifications

import io.github.mateorguez.cincopelao.ui.theme.appColors
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsOff
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.mateorguez.cincopelao.notifications.ReminderSettings
import io.github.mateorguez.cincopelao.ui.theme.CincoPelaoAppTheme
import io.github.mateorguez.cincopelao.ui.theme.DelightMedium
import kotlinx.coroutines.launch

/** Ajustes de los recordatorios: con cuántos días de antelación y en qué franja horaria. */
@AndroidEntryPoint
class NotificationsActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CincoPelaoAppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(text = "Notificaciones", fontFamily = DelightMedium) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                                }
                            },
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        NotificationsScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(modifier: Modifier = Modifier, viewModel: NotificationsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

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
            divider = { HorizontalDivider(color = contentColor.copy(alpha = 0.3f), thickness = 1.dp) },
            modifier = Modifier.shadow(0.dp)
        ) {
            listOf("Eventos", "Tareas").forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { page ->
            val kind = if (page == 0) ReminderKind.Events else ReminderKind.Tasks
            // Hasta que se leen los ajustes guardados no se muestra nada (ni se puede guardar)
            state.form(kind)?.let { form ->
                NotificationConfigPage(
                    title = if (kind == ReminderKind.Events) "Recordatorios para eventos" else "Recordatorios para tareas",
                    settings = form.edited,
                    hasUnsavedChanges = form.hasUnsavedChanges,
                    onAddDay = { viewModel.addDay(kind, it) },
                    onRemoveDay = { viewModel.removeDay(kind, it) },
                    onStartHourChange = { viewModel.setStartHour(kind, it) },
                    onEndHourChange = { viewModel.setEndHour(kind, it) },
                    onSave = { viewModel.save(kind) }
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

@Composable
private fun NotificationConfigPage(
    title: String,
    settings: ReminderSettings,
    hasUnsavedChanges: Boolean,
    onAddDay: (Int) -> Unit,
    onRemoveDay: (Int) -> Unit,
    onStartHourChange: (Int) -> Unit,
    onEndHourChange: (Int) -> Unit,
    onSave: () -> Unit
) {
    var showAddDayDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)) {
            items(settings.daysBefore, key = { it }) { day ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = if (day == 1) "$day día antes" else "$day días antes")
                        IconButton(onClick = { onRemoveDay(day) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = { showAddDayDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Añadir día")
        }

        Spacer(Modifier.height(16.dp))
        Text("Rango horario para las notificaciones", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TimePickerButton(label = "Desde", hour = settings.startHour, onHourSelected = onStartHourChange)
            TimePickerButton(label = "Hasta", hour = settings.endHour, onHourSelected = onEndHourChange)
        }

        Spacer(Modifier.height(32.dp))

        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Guardar configuración")
        }

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
            onConfirm = { day ->
                onAddDay(day)
                showAddDayDialog = false
            }
        )
    }
}

@Composable
private fun TimePickerButton(label: String, hour: Int, onHourSelected: (Int) -> Unit) {
    val context = LocalContext.current
    Button(
        onClick = { TimePickerDialog(context, { _, selectedHour, _ -> onHourSelected(selectedHour) }, hour, 0, true).show() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("$label: ${"%02d:00".format(hour)}")
    }
}

@Composable
private fun AddDayDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
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
            TextButton(onClick = {
                val value = inputValue.toIntOrNull()
                if (value != null && value > 0) onConfirm(value) else onDismiss()
            }) {
                Text("Añadir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
