package io.github.mateorguez.cincopelao.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.data.local.entity.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Hoja inferior con los datos de una tarea y las acciones de borrar, editar y completar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskInfoSheet(
    task: Task,
    enableCheckbox: Boolean,
    showEdit: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCheckbox: (Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { sheetState.show() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        val formatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(text = task.title, style = MaterialTheme.typography.titleLarge)

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
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.Absolute.Right,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }

                if (showEdit) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }

                // Se actualiza al momento, sin esperar a que llegue el cambio de la base de datos
                val isChecked = remember(task) { mutableStateOf(task.isCompleted) }
                Checkbox(
                    enabled = enableCheckbox,
                    checked = isChecked.value,
                    onCheckedChange = {
                        isChecked.value = it
                        onCheckbox(it)
                    }
                )

                Button(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        }
    }
}
