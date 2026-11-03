package com.example.cincopelao

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CompletedTasksScreen(snackbarHostState: SnackbarHostState) {
    val mainViewModel: MainViewModel = hiltViewModel()
    val subjects by mainViewModel.allSubjects.collectAsState(initial = emptyList())
    val allTasks by mainViewModel.allTasks.collectAsState(initial = emptyList())

    // Filtrar tareas archivadas
    val archivedTasks = allTasks.filter { it.archived }
    var visibleArchivedTasks by remember { mutableStateOf(archivedTasks) }
    LaunchedEffect(archivedTasks) {
        visibleArchivedTasks = archivedTasks
    }
    val visibilityMap = remember { mutableStateMapOf<Long, Boolean>() }
    LaunchedEffect(archivedTasks) {
        archivedTasks.forEach { visibilityMap[it.taskId] = true }
    }
    val coroutineScope = rememberCoroutineScope()

    var showTaskInfo by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var taskToShow by remember { mutableStateOf<Task?>(null) }

    val taskToShowState by produceState<Task?>(initialValue = null, taskToShow) {
        if (taskToShow != null) {
            val flow = if (taskToShow!!.subjectId != null) {
                mainViewModel.getTasksBySubject(taskToShow!!.subjectId!!)
            } else {
                mainViewModel.getGeneralTasks()
            }

            flow.collect { tasks ->
                value = tasks.find { it.taskId == taskToShow!!.taskId }
            }
        }
    }

    val context = LocalContext.current
    val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)
    val brushColors = if (isDarkTheme) {
        listOf(
            Color.White.copy(alpha = 0.3f),
            Color.White.copy(alpha = 0.05f)
        )
    } else {
        listOf(
            Color(0xFF001794).copy(alpha = 0.3f),
            Color(0xFF001794).copy(alpha = 0.05f)
        )
    }

    val scope = rememberCoroutineScope()

    if (showTaskInfo && taskToShowState != null) {
        TaskInfo(
            task = taskToShowState!!,
            enableCheckbox = false,
            showEdit = false,
            onDismiss = { showTaskInfo = false },
            onEdit = { /* nada */ },
            onDelete = {
                taskToDelete = taskToShowState!!
                showDeleteDialog = true
            },
            onCheckbox = { checked ->
                mainViewModel.updateTask(
                    taskToShowState!!.copy(
                        isCompleted = checked,
                        completedAt = if (checked) System.currentTimeMillis() else null
                    )
                )
            }
        )
    }

    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showContent = true
    }
    AnimatedVisibility(
        visible = (visibleArchivedTasks.isEmpty() && showContent),
        enter = fadeIn(tween(500)) + slideInVertically { it / 2 },
        exit = fadeOut(tween(300)) + slideOutVertically { it / 2 }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "😘",
                    fontSize = 64.sp,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                )
                Text(
                    text = "No hay tareas completadas por ahora.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Línea divisoria
        item {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )
        }

        // Sección "Tareas Completadas"
        item {
            var isExpanded by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                isExpanded = true
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp, horizontal = 12.dp)
                        .height(43.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tareas Completadas",
                        fontFamily = FontFamily(Font(R.font.delight_regular)),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (archivedTasks.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Eliminar todas",
                                tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Contenido expandido
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (visibleArchivedTasks.isNotEmpty()) {
                            visibleArchivedTasks.sortedByDescending { it.completedAt ?: Long.MAX_VALUE }.forEach { task ->
                                val subtasks = visibleArchivedTasks.filter { it.parentTaskId == task.taskId }
                                val subjectAbbrev = subjects.find { it.subjectId.toLong() == task.subjectId }?.abbrev

                                val modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)

                                if (subtasks.isNotEmpty()) {
                                    ArchivedParentTaskBlock(
                                        parentTask = task,
                                        subtasks = subtasks,
                                        subjectAbbrev = subjectAbbrev,
                                        showInfoSheet = { clickedTask ->
                                            taskToShow = clickedTask
                                            showTaskInfo = true
                                        },
                                        onRestore = {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                                                ReminderScheduler.clearAllScheduledReminders(context)
                                            }
                                            mainViewModel.updateTask(task.copy(archived = false))
                                            subtasks.forEach {
                                                mainViewModel.updateTask(it.copy(archived = false))
                                            }
                                        },
                                        onDelete = {
                                            scope.launch {
                                                val deletedParentTask = task
                                                val deletedSubtasks = subtasks.toList()
                                                subtasks.forEach { mainViewModel.deleteTask(it) }
                                                mainViewModel.deleteTask(task)
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Tarea y subtareas eliminadas",
                                                    actionLabel = "Deshacer",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    mainViewModel.insertTask(deletedParentTask)
                                                    deletedSubtasks.forEach { mainViewModel.insertTask(it) }
                                                }
                                            }
                                        }

                                    )
                                } else if (task.parentTaskId == null) {
                                    Box(modifier = modifier.fillMaxWidth()) {
                                        // Fondo
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .blur(16.dp)
                                                .border(
                                                    width = 1.dp,
                                                    brush = Brush.linearGradient(colors = brushColors),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                        )

                                        // Contenido
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clip(RoundedCornerShape(12.dp)),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            ArchivedTaskRow(
                                                task = task,
                                                subjectAbbrev = subjectAbbrev,
                                                modifier = Modifier
                                                    .padding(horizontal = 8.dp)
                                                    .animateItem(fadeInSpec = null, fadeOutSpec = null),
                                                onClick = {
                                                    taskToShow = task
                                                    showTaskInfo = true
                                                },
                                                onRestore = {
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                                                        ReminderScheduler.clearAllScheduledReminders(context)
                                                    }
                                                    mainViewModel.updateTask(task.copy(archived = false))
                                                },
                                                onDelete = {
                                                    scope.launch {
                                                        mainViewModel.deleteTask(task)
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "Tarea eliminada",
                                                            actionLabel = "Deshacer",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            mainViewModel.insertTask(task)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmacion para eliminar todas
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteAllDialog = false
            },
            title = { Text("Eliminar todas las tareas archivadas") },
            text = { Text("¿Estás seguro de que quieres eliminar todas las tareas archivadas?") },
            confirmButton = {
                TextButton(onClick = {
                    archivedTasks.forEach{ mainViewModel.deleteTask(it) }
                    showDeleteAllDialog = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = {showDeleteAllDialog = false}) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de confirmación para eliminar
    if (showDeleteDialog && taskToDelete != null) {
        val subtasks by mainViewModel.getSubTasks(taskToDelete!!.taskId).collectAsState(initial = emptyList())
        val hasSubtasks = subtasks.isNotEmpty()

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                taskToDelete = null
            },
            title = { Text("Eliminar tarea") },
            text = {
                if (hasSubtasks) {
                    Text("Esta tarea tiene subtareas. ¿Qué te gustaría hacer?")
                } else {
                    Text("¿Estás seguro de que quieres eliminar esta tarea?")
                }
            },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (hasSubtasks) {
                        TextButton(onClick = {
                            subtasks.forEach {
                                mainViewModel.updateTask(it.copy(parentTaskId = null))
                            }
                            mainViewModel.deleteTask(taskToDelete!!)
                            showDeleteDialog = false
                            taskToDelete = null
                        }) {
                            Text("Eliminar solo esta")
                        }

                        TextButton(onClick = {
                            mainViewModel.deleteTask(taskToDelete!!)
                            showDeleteDialog = false
                            taskToDelete = null
                        }) {
                            Text("Eliminar tarea y subtareas")
                        }
                    } else {
                        TextButton(onClick = {
                            mainViewModel.deleteTask(taskToDelete!!)
                            showDeleteDialog = false
                            taskToDelete = null
                        }) {
                            Text("Eliminar")
                        }
                    }

                    TextButton(onClick = {
                        showDeleteDialog = false
                        taskToDelete = null
                    }) {
                        Text("Cancelar")
                    }
                }
            },
            dismissButton = {}
        )
    }
}

@Composable
fun ArchivedParentTaskBlock(
    parentTask: Task,
    subtasks: List<Task>,
    subjectAbbrev: String?,
    showInfoSheet: (Task) -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {

    var showTaskInfo by remember { mutableStateOf(false) }
    var taskToShow by remember { mutableStateOf<Task?>(null) }

    if (showTaskInfo) {
        showInfoSheet(taskToShow!!)
        showTaskInfo = false
    }

    val context = LocalContext.current
    val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)
    val brushColors = if (isDarkTheme) {
        listOf(
            Color.White.copy(alpha = 0.3f),
            Color.White.copy(alpha = 0.05f)
        )
    } else {
        listOf(
            Color(0xFF001794).copy(alpha = 0.3f),
            Color(0xFF001794).copy(alpha = 0.05f)
        )
    }

    Box {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .blur(16.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = brushColors
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                )
                .clip(RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Título + resumen
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = {
                        taskToShow = parentTask
                        showTaskInfo = true
                    })
                    .animateContentSize()
            ) {
                Column(modifier = Modifier
                    .weight(1f)
                    .height(48.dp)) {
                    Text(
                        text = parentTask.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val subjectText = subjectAbbrev?.let { "[$it] " } ?: ""
                    val completedAtText = parentTask.completedAt?.let {
                        val now = System.currentTimeMillis()
                        val diff = now - it

                        val days = TimeUnit.MILLISECONDS.toDays(diff)
                        when {
                            days < 1 -> "hoy"
                            days < 7 -> "hace $days día${if (days > 1) "s" else ""}"
                            else -> {
                                val formatter = "el " + SimpleDateFormat("d MMM", Locale.getDefault())
                                formatter.format(Date(it))
                            }
                        }
                    } ?: "fecha desconocida"

                    Text(
                        text = "${subjectText}Completada $completedAtText",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                val context = LocalContext.current
                val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)
                IconButton(onClick = { onRestore() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Archivar tarea",
                        tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { onDelete() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar tarea",
                        tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

            // Lista de subtareas (TaskRow)
            Column {
                val sortedSubtasks = subtasks.sortedBy { it.dueDate ?: Long.MAX_VALUE }
                sortedSubtasks.forEachIndexed { index, subtask ->
                    ArchivedTaskRow(
                        task = subtask,
                        subjectAbbrev = null,
                        modifier = Modifier.padding(horizontal = 0.dp),
                        onClick = {
                            taskToShow = subtask
                            showTaskInfo = true
                        },
                        onRestore = { /* todo ñam */ },
                        onDelete = { /* todo ñom */ }
                    )

                    if (index < sortedSubtasks.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedTaskRow(
    task: Task,
    modifier: Modifier,
    subjectAbbrev: String?,
    onClick: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(modifier)
            .clickable { onClick() }
            .animateContentSize()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(8.dp))
            Column {

                Text(task.title)

                val subjectText = subjectAbbrev?.let { "[$it] " } ?: ""
                val completedAtText = task.completedAt?.let {
                    val now = System.currentTimeMillis()
                    val diff = now - it

                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    when {
                        days < 1 -> "hoy"
                        days < 7 -> "hace $days día${if (days > 1) "s" else ""}"
                        else -> {
                            val formatter = "el " + SimpleDateFormat("d MMM", Locale.getDefault())
                            formatter.format(Date(it))
                        }
                    }
                } ?: "fecha desconocida"

                Text(
                    text = "${subjectText}Completada $completedAtText",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        val context = LocalContext.current
        val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)
        if (task.parentTaskId == null) {
            Row {
                IconButton(onClick = { onRestore() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Archivar tarea",
                        tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { onDelete() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar tarea",
                        tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

