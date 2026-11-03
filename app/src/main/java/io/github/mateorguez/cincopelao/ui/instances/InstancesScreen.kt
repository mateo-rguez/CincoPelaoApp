package io.github.mateorguez.cincopelao.ui.instances

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mateorguez.cincopelao.data.instances.DatabaseInstance
import io.github.mateorguez.cincopelao.ui.theme.DelightRegular
import io.github.mateorguez.cincopelao.ui.theme.LocalDarkTheme
import kotlinx.coroutines.launch

/** Letras, números y espacios (ya no se usa como nombre de archivo, así que puede tener espacios). */
private fun sanitizeInstanceName(input: String) = input.filter { it.isLetterOrDigit() || it == ' ' }

@Composable
fun InstancesScreen(
    onInstanceSelected: () -> Unit,
    onCancel: () -> Unit,
    viewModel: InstancesViewModel = hiltViewModel()
) {
    val instances by viewModel.instances.collectAsStateWithLifecycle()
    val currentInstanceId by viewModel.currentInstanceId.collectAsStateWithLifecycle()

    var showNewDialog by remember { mutableStateOf(false) }
    var instanceToEdit by remember { mutableStateOf<DatabaseInstance?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(instances, key = { it.id }) { instance ->
                InstanceCard(
                    instance = instance,
                    isSelected = instance.id == currentInstanceId,
                    onClick = {
                        viewModel.select(instance)
                        onInstanceSelected()
                    },
                    onEdit = { instanceToEdit = instance },
                    onDelete = { viewModel.delete(instance) }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showNewDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(text = "Añadir nueva instancia", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                    }
                }
            }
        }

        Button(onClick = onCancel, modifier = Modifier.align(Alignment.End).padding(top = 16.dp)) {
            Text("Cancelar")
        }
    }

    if (showNewDialog) {
        InstanceDialog(
            title = "Nueva Instancia",
            initialName = "",
            initialTitulation = null,
            onDismiss = { showNewDialog = false },
            onSave = { name, _ ->
                viewModel.create(name)
                showNewDialog = false
            }
        )
    }

    instanceToEdit?.let { instance ->
        InstanceDialog(
            title = "Editar Instancia",
            initialName = instance.name,
            initialTitulation = instance.titulation,
            onDismiss = { instanceToEdit = null },
            onSave = { name, titulation ->
                viewModel.update(instance, name, titulation)
                instanceToEdit = null
            }
        )
    }
}

/** Tarjeta de una instancia; se borra deslizándola (salvo la activa). */
@Composable
private fun InstanceCard(
    instance: DatabaseInstance,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDarkTheme = LocalDarkTheme.current
    val accentColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
    val dismissState = rememberSwipeToDismissBoxState()
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Al terminar de deslizar se pide confirmación
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) showDeleteDialog = true
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isSelected,
        enableDismissFromEndToStart = !isSelected,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onError)
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) accentColor else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    instance.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = DelightRegular,
                        color = if (isSelected && !isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = accentColor)
                }
            }
        }
    }

    if (showDeleteDialog) {
        fun cancel() {
            showDeleteDialog = false
            coroutineScope.launch { dismissState.snapTo(SwipeToDismissBoxValue.Settled) }
        }
        AlertDialog(
            onDismissRequest = ::cancel,
            title = { Text("Eliminar instancia") },
            text = { Text("¿Deseas eliminar ${instance.name}?") },
            confirmButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("Eliminar") }
            },
            dismissButton = {
                Button(onClick = ::cancel) { Text("Cancelar") }
            }
        )
    }
}

/** Diálogo para crear o editar una instancia (nombre obligatorio y titulación opcional). */
@Composable
private fun InstanceDialog(
    title: String,
    initialName: String,
    initialTitulation: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, titulation: String?) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var titulation by remember { mutableStateOf(initialTitulation.orEmpty()) }
    // La titulación solo se edita en instancias existentes (como hasta ahora)
    val isEditing = initialName.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                Text("Introduce un nombre para esta instancia:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = sanitizeInstanceName(it) },
                    placeholder = { Text("Nombre") },
                    singleLine = true
                )
                if (isEditing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Titulación de esta instancia:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = titulation,
                        onValueChange = { titulation = it },
                        placeholder = { Text("Titulación (opcional)") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), titulation.trim()) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
