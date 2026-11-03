package com.example.cincopelao

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.room.util.TableInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksScreen() {
    val mainViewModel: MainViewModel = hiltViewModel()
    val subjects by mainViewModel.allSubjects.collectAsState(initial = emptyList())
    val generalTasksRaw by mainViewModel.getGeneralTasks().collectAsState(initial = emptyList())

    val tasksBySubjectRaw = subjects.associate { subject ->
        subject.subjectId to mainViewModel
            .getTasksBySubject(subject.subjectId.toLong())
            .collectAsState(initial = emptyList())
    }

    // Filtrar tareas archivadas
    val generalTasks = generalTasksRaw.filterNot { it.archived }
    val tasksBySubject = tasksBySubjectRaw.mapValues { (_, state) ->
        derivedStateOf { state.value.filterNot { it.archived } }
    }

    val expandedStates = remember { mutableStateMapOf<Int, Boolean>() }

    val coroutineScope = rememberCoroutineScope()

    var showForm by remember { mutableStateOf(false) }
    var showTaskInfo by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var taskBeingEdited by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var taskToShow by remember { mutableStateOf<Task?>(null) }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }

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

    val allTasks = generalTasks + tasksBySubject.values.flatMap { it.value }
    val isParent = !(taskToShowState != null && allTasks.any { it.parentTaskId == taskToShowState!!.taskId })
    if (showTaskInfo && taskToShowState != null) {
        TaskInfo(
            task = taskToShowState!!,
            enableCheckbox = isParent,
            showEdit = true,
            onDismiss = { showTaskInfo = false },
            onEdit = {
                selectedSubject = subjects.find { it.subjectId.toLong() == taskToShow!!.subjectId }
                taskBeingEdited = taskToShowState!!
                showForm = true
            },
            onDelete = {
                taskToDelete = taskToShowState!!
                showDeleteDialog = true
            },
            onCheckbox = { checked ->
                CoroutineScope(Dispatchers.IO).launch {
                    WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                    ReminderScheduler.clearAllScheduledReminders(context)
                    mainViewModel.updateTask(
                        taskToShowState!!.copy(
                            isCompleted = checked,
                            completedAt = if (checked) System.currentTimeMillis() else null
                        )
                    )
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {

        item {
            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), thickness = 0.5.dp)
        }

        // Tareas generales
        item {
            val generalId = -1
            val isExpanded = expandedStates[generalId] ?: generalTasks.isNotEmpty()

            Column(
                modifier = Modifier/*.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )*/
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedStates[generalId] = !isExpanded
                        }
                        .padding(vertical = 2.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tareas Generales",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily(Font(R.font.delight_regular))
                        ),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = {
                        selectedSubject = null
                        showForm = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir tarea general")
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expandir o colapsar"
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column() {
                        if (generalTasks.isEmpty()) {
                            Row (
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text("No hay tareas generales.", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            generalTasks.sortedBy { it.dueDate ?: Long.MAX_VALUE }.forEach { task ->


                                val subtasks = generalTasks.filter { it.parentTaskId == task.taskId }
                                if (subtasks.isNotEmpty()) {
                                    ParentTaskBlock(
                                        parentTask = task,
                                        subtasks = subtasks,
                                        onCheckedChange = { subtask, checked ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                                                ReminderScheduler.clearAllScheduledReminders(context)
                                                mainViewModel.updateTask(
                                                    subtask.copy(
                                                        isCompleted = checked,
                                                        completedAt = if (checked) System.currentTimeMillis() else null
                                                    )
                                                )
                                            }
                                        },
                                        showInfoSheet = { task ->
                                            taskToShow = task
                                            showTaskInfo = true
                                        },
                                        onArchive = {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                                                ReminderScheduler.clearAllScheduledReminders(context)
                                            }
                                            mainViewModel.updateTask(
                                                task.copy(
                                                    archived = true
                                                )
                                            )
                                            subtasks.forEach { subtask ->
                                                mainViewModel.updateTask(
                                                    subtask.copy(
                                                        archived = true
                                                    )
                                                )
                                            }
                                        }
                                    )
                                } else if (task.parentTaskId == null) {
                                    Box {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .blur(16.dp)
                                                .border(
                                                    width = 1.dp,
                                                    brush = Brush.linearGradient(
                                                        colors = brushColors
                                                    ),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                        )
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(
                                                        alpha = 0.5f
                                                    ),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clip(RoundedCornerShape(12.dp)),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TaskRow(
                                                task = task,
                                                modifier = Modifier.padding(horizontal = 8.dp),
                                                onCheckedChange = { checked ->
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                                                        ReminderScheduler.clearAllScheduledReminders(
                                                            context
                                                        )
                                                        mainViewModel.updateTask(
                                                            task.copy(
                                                                isCompleted = checked,
                                                                completedAt = if (checked) System.currentTimeMillis() else null
                                                            )
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    taskToShow = task
                                                    showTaskInfo = true
                                                },
                                                onArchive = {
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                                                        ReminderScheduler.clearAllScheduledReminders(context)
                                                    }
                                                    mainViewModel.updateTask(
                                                        task.copy(
                                                            archived = true
                                                        )
                                                    )
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

        item {
            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), thickness = 0.5.dp)
        }

        // Tareas por asignatura
        items(subjects.size) { index ->
            val subject = subjects[index]
            val subjectTasks = tasksBySubject[subject.subjectId]?.value ?: emptyList()
            val isExpanded = expandedStates[subject.subjectId] ?: subjectTasks.isNotEmpty()

            Column(
                modifier = Modifier/*.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )*/
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedStates[subject.subjectId] = !isExpanded
                        }
                        .padding(vertical = 2.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily(Font(R.font.delight_regular))
                        ),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Pequeño espacio antes de los iconos
                    IconButton(onClick = {
                        selectedSubject = subject
                        showForm = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir tarea")
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expandir o colapsar"
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column() {
                        if (subjectTasks.isEmpty()) {
                            Row (
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text("No hay tareas para ${subject.name}.", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            subjectTasks.sortedBy { it.dueDate ?: Long.MAX_VALUE }.forEach { task ->


                                val subtasks = subjectTasks.filter { it.parentTaskId == task.taskId }
                                if (subtasks.isNotEmpty()) {
                                    ParentTaskBlock(
                                        parentTask = task,
                                        subtasks = subtasks,
                                        onCheckedChange = { subtask, checked ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                                                ReminderScheduler.clearAllScheduledReminders(context)
                                                mainViewModel.updateTask(
                                                    subtask.copy(
                                                        isCompleted = checked,
                                                        completedAt = if (checked) System.currentTimeMillis() else null
                                                    )
                                                )
                                            }
                                        },
                                        showInfoSheet = { task ->
                                            taskToShow = task
                                            showTaskInfo = true
                                        },
                                        onArchive = {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                                                ReminderScheduler.clearAllScheduledReminders(context)
                                            }
                                            mainViewModel.updateTask(task.copy(
                                                archived = true
                                            ))
                                            subtasks.forEach { subtask ->
                                                mainViewModel.updateTask(subtask.copy(
                                                    archived = true
                                                ))
                                            }
                                        }
                                    )
                                } else if (task.parentTaskId == null) {
                                    Box {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .blur(16.dp)
                                                .border(
                                                    width = 1.dp,
                                                    brush = Brush.linearGradient(
                                                        colors = brushColors
                                                    ),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                        )
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(
                                                        alpha = 0.5f
                                                    ),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clip(RoundedCornerShape(12.dp)),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TaskRow(
                                                task = task,
                                                modifier = Modifier.padding(horizontal = 8.dp),
                                                onCheckedChange = { checked ->
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                                                        ReminderScheduler.clearAllScheduledReminders(
                                                            context
                                                        )
                                                        mainViewModel.updateTask(
                                                            task.copy(
                                                                isCompleted = checked,
                                                                completedAt = if (checked) System.currentTimeMillis() else null
                                                            )
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    taskToShow = task
                                                    showTaskInfo = true
                                                },
                                                onArchive = {
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                                                        ReminderScheduler.clearAllScheduledReminders(context)
                                                    }
                                                    mainViewModel.updateTask(task.copy(
                                                        archived = true
                                                    ))
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
            if (index < subjects.size - 1) {
                Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }

        item {
            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), thickness = 0.5.dp)
        }
    }

    // Bottom sheet del formulario
    if (showForm) {
        TaskForm(
            task = taskBeingEdited,
            subject = selectedSubject,
            onSave = { title, description, dueDateMillis, parentTaskId ->
                CoroutineScope(Dispatchers.IO).launch {
                    WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                    ReminderScheduler.clearAllScheduledReminders(context)
                }
                val updatedTask = taskBeingEdited?.copy(
                    title = title,
                    description = description,
                    dueDate = dueDateMillis,
                    parentTaskId = parentTaskId
                ) ?: Task(
                    title = title,
                    description = description,
                    dueDate = dueDateMillis,
                    subjectId = selectedSubject?.subjectId?.toLong(),
                    parentTaskId = parentTaskId
                )

                if (taskBeingEdited != null) {
                    mainViewModel.updateTask(updatedTask)
                } else {
                    mainViewModel.insertTask(updatedTask)
                }

                showForm = false
                taskBeingEdited = null
                selectedSubject = null
            },
            onDismiss = {
                showForm = false
                taskBeingEdited = null
                selectedSubject = null
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
                            CoroutineScope(Dispatchers.IO).launch {
                                WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                                ReminderScheduler.clearAllScheduledReminders(context)
                            }
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
                            CoroutineScope(Dispatchers.IO).launch {
                                WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                                ReminderScheduler.clearAllScheduledReminders(context)
                            }
                            mainViewModel.deleteTask(taskToDelete!!)
                            showDeleteDialog = false
                            taskToDelete = null
                        }) {
                            Text("Eliminar tarea y subtareas")
                        }
                    } else {
                        TextButton(onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                WorkManager.getInstance(context).cancelAllWorkByTag("NOTIFICATION_WORK")
                                ReminderScheduler.clearAllScheduledReminders(context)
                            }
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

fun daysUntil(timestampMillis: Long): Int {
    val now = Calendar.getInstance()
    val due = Calendar.getInstance().apply {
        timeInMillis = timestampMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    now.set(Calendar.HOUR_OF_DAY, 0)
    now.set(Calendar.MINUTE, 0)
    now.set(Calendar.SECOND, 0)
    now.set(Calendar.MILLISECOND, 0)

    val diffMillis = due.timeInMillis - now.timeInMillis
    return (diffMillis / (1000 * 60 * 60 * 24)).toInt()
}

@Composable
fun ParentTaskBlock(
    parentTask: Task,
    subtasks: List<Task>,
    onCheckedChange: (Task, Boolean) -> Unit,
    showInfoSheet: (Task) -> Unit,
    onArchive: () -> Unit,
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
                    val daysLeft = parentTask.dueDate?.let { daysUntil(it) }
                    val dueText = if (daysLeft != null) {
                        when {
                            daysLeft < 0 -> "Para hace ${-daysLeft} días"
                            daysLeft == 0 -> "Hoy"
                            daysLeft == 1 -> "Mañana"
                            else -> "En $daysLeft días"
                        }
                    } else null
                    val completed = subtasks.count { it.isCompleted }
                    val total = subtasks.size
                    Text(
                        text = dueText?.let {
                            "$it, $completed de $total completadas"
                        } ?: "$completed de $total completadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                val context = LocalContext.current
                val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)
                AnimatedVisibility(
                    visible = parentTask.isCompleted,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(onClick = { onArchive() }) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Archivar tarea",
                            tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

            // Lista de subtareas (TaskRow)
            Column {
                val sortedSubtasks = subtasks.sortedBy { it.dueDate ?: Long.MAX_VALUE }
                sortedSubtasks.forEachIndexed { index, subtask ->
                    TaskRow(
                        task = subtask,
                        modifier = Modifier.padding(horizontal = 0.dp),
                        onCheckedChange = { checked -> onCheckedChange(subtask, checked) },
                        onClick = {
                            taskToShow = subtask
                            showTaskInfo = true
                        },
                        onArchive = { /* nada */ }
                    )

                    if (index < sortedSubtasks.lastIndex) {
                        Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun ScribbleStrikeThroughText(
    text: String,
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val animationProgress by animateFloatAsState(
        targetValue = if (isChecked) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    val textColor by animateColorAsState(
        targetValue = if (isChecked) Color.Gray else MaterialTheme.colorScheme.onBackground
    )

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Box(modifier = modifier) {
        Text(
            text = text,
            style = textStyle,
            color = textColor,
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier.align(Alignment.CenterStart)
        )

        Canvas(modifier = Modifier.matchParentSize()) {
            val layoutResult = textLayoutResult ?: return@Canvas

            val waveAmplitude = 10f
            val waveLength = 50f

            val delayPerLine = 1f
            val totalDuration = 1f + delayPerLine * (layoutResult.lineCount - 1)

            for (lineIndex in 0 until layoutResult.lineCount) {
                val start = delayPerLine * lineIndex
                val lineProgress = ((animationProgress * totalDuration) - start).coerceIn(0f, 1f)

                val lineStart = layoutResult.getLineLeft(lineIndex)
                val lineEnd = layoutResult.getLineRight(lineIndex)
                val lineTop = layoutResult.getLineTop(lineIndex).toFloat()
                val lineBottom = layoutResult.getLineBottom(lineIndex).toFloat()
                val heightCenter = (lineTop + lineBottom) / 2f
                val length = (lineEnd - lineStart) * lineProgress

                val path = Path().apply {
                    moveTo(lineStart, heightCenter)
                    var x = lineStart
                    var up = true
                    while (x < lineStart + length) {
                        val controlY = if (up) heightCenter - waveAmplitude else heightCenter + waveAmplitude
                        val nextX = (x + waveLength).coerceAtMost(lineStart + length)
                        quadraticTo(x + waveLength / 2, controlY, nextX, heightCenter)
                        x = nextX
                        up = !up
                    }
                }

                drawPath(
                    path = path,
                    color = textColor,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskRow(
    task: Task,
    modifier: Modifier,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onArchive: () -> Unit
) {

    val textColor by animateColorAsState(
        targetValue = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onBackground
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(modifier)
            .clickable { onClick() }
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onCheckedChange
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                ScribbleStrikeThroughText(
                    text = task.title,
                    isChecked = task.isCompleted
                )

                val daysLeft = task.dueDate?.let { daysUntil(it) }

                if (daysLeft != null) {
                    val dueText = when {
                        daysLeft < 0 -> "Para hace ${-daysLeft} días"
                        daysLeft == 0 -> "Hoy"
                        daysLeft == 1 -> "Mañana"
                        else -> "En $daysLeft días"
                    }

                    Text(
                        text = dueText,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                }
            }
        }

        val context = LocalContext.current
        val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)
        AnimatedVisibility(
            visible = task.isCompleted && task.parentTaskId == null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            IconButton(onClick = { onArchive() }) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = "Archivar tarea",
                    tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskInfo(
    task: Task,
    enableCheckbox: Boolean,
    showEdit: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCheckbox: (Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(Unit) {
        sheetState.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        val formatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Fecha límite: ${task.dueDate?.let { formatter.format(Date(it)) } ?: "No especificada"}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (task.description.isNullOrBlank()) "Sin descripción" else task.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Absolute.Right,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón eliminar
                IconButton(onClick = {onDelete()}) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }

                // Botón editar
                if (showEdit) {
                    IconButton(onClick = {onEdit()}) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }

                // Done Checkbox
                val isChecked = remember(task) { mutableStateOf(task.isCompleted) }
                Checkbox(
                    enabled = enableCheckbox,
                    checked = isChecked.value,
                    onCheckedChange = {
                        isChecked.value = it
                        onCheckbox(it)
                    }
                )

                // Botón cerrar
                Button(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskForm(
    task: Task? = null,
    subject: Subject?,
    onSave: (title: String, description: String, dueDateMillis: Long?, parentTaskId: Long?) -> Unit,
    onDismiss: () -> Unit
) {

    val mainViewModel: MainViewModel = hiltViewModel()
    val tasksInSubjectFlow = if (subject != null) {
        mainViewModel.getTasksBySubject(subject.subjectId.toLong())
    } else {
        mainViewModel.getGeneralTasks()
    }
    val tasksInSubject by tasksInSubjectFlow.collectAsState(initial = emptyList())

    val beingEdited = task != null

    // Form states
    var title by remember { mutableStateOf(task?.title.orEmpty()) }
    var description by remember { mutableStateOf(task?.description.orEmpty()) }
    var selectedDateMillis by remember { mutableStateOf(task?.dueDate) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedParentTask by remember { mutableStateOf<Task?>(null) }
    var expandedParentMenu by remember { mutableStateOf(false) }

    LaunchedEffect(task, tasksInSubject) {
        title = task?.title.orEmpty()
        description = task?.description.orEmpty()
        selectedDateMillis = task?.dueDate
        selectedParentTask = tasksInSubject.find { it.taskId == task?.parentTaskId }
    }

    // Flags y estados auxiliares
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Para detectar cambios
    val formHasChanges by remember {
        derivedStateOf {
            title.isNotBlank() || description.isNotBlank() || selectedDateMillis != null
        }
    }
    val formHasChangesState by rememberUpdatedState(formHasChanges)

    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = {
            if (it == SheetValue.Hidden && formHasChangesState) false else true
        }
    )

    var showDiscardDialog by remember { mutableStateOf(false) }

    // Modal Bottom Sheet
    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título
            Text(
                text = if (subject == null && !beingEdited) "Nueva tarea general" else if (subject != null && !beingEdited) "Nueva tarea (${subject.abbrev})"
                else if (subject == null && beingEdited) "Editar tarea general" else "Editar tarea (${subject!!.abbrev})",
                style = MaterialTheme.typography.titleLarge
            )

            // Campo título
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título de la tarea") },
                modifier = Modifier.fillMaxWidth()
            )

            // Campo descripción
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción (opcional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 200.dp),
                maxLines = 5,
                singleLine = false,
                keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences)
            )

            // Campo fecha (read-only con date picker)
            OutlinedTextField(
                value = selectedDateMillis?.let { formatter.format(Date(it)) } ?: "",
                onValueChange = {},
                label = { Text("Fecha (opcional)") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Elegir fecha")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            // Selector de fecha
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        selectedDateMillis = datePickerState.selectedDateMillis
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Aceptar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancelar")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // Dropdown para tarea madre
            ExposedDropdownMenuBox(
                expanded = expandedParentMenu,
                onExpandedChange = { expandedParentMenu = !expandedParentMenu }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedParentTask?.title ?: "",
                    onValueChange = {},
                    label = { Text("Tarea madre (opcional)") },
                    placeholder = { Text("Sin tarea madre") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedParentMenu) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedParentMenu,
                    onDismissRequest = { expandedParentMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sin tarea madre") },
                        onClick = {
                            selectedParentTask = null
                            expandedParentMenu = false
                        }
                    )

                    tasksInSubject
                        .filter { it.taskId != task?.taskId && it.parentTaskId == null && !it.archived }
                        .forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.title) },
                                onClick = {
                                    selectedParentTask = t
                                    expandedParentMenu = false
                                }
                            )
                        }
                }
            }


            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        if (formHasChanges) {
                            showDiscardDialog = true
                        } else {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text("Cancelar")
                }

                TextButton(
                    enabled = title.isNotBlank(),
                    onClick = {
                        onSave(title, description, selectedDateMillis, selectedParentTask?.taskId)
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                ) {
                    Text("Guardar")
                }
            }
        }
    }

    // Diálogo de confirmación
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { /* tocar fuera del dialog = nada */ },
            title = { Text("Descartar cambios") },
            text = { Text("Tienes cambios sin guardar. ¿Descartar?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    coroutineScope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                }) {
                    Text("Descartar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Seguir editando")
                }
            }
        )
    }
}