package io.github.mateorguez.cincopelao.ui.instances

import io.github.mateorguez.cincopelao.ui.theme.DelightRegular
import io.github.mateorguez.cincopelao.ui.theme.DelightMedium
import io.github.mateorguez.cincopelao.ui.theme.LocalDarkTheme
import io.github.mateorguez.cincopelao.data.instances.DatabaseInstance
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.MainActivity
import io.github.mateorguez.cincopelao.R
import io.github.mateorguez.cincopelao.ui.theme.CincoPelaoAppTheme
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class InstancesActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val context = LocalContext.current
            val viewModel: InstancesViewModel = hiltViewModel()
            val instances by viewModel.instances.collectAsState()
            val currentInstanceId by viewModel.currentInstanceId.collectAsState()

            CincoPelaoAppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Instancias",
                                    fontFamily = DelightMedium
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
                            InstanceScreen(
                                instances = instances,
                                currentInstanceId = currentInstanceId,
                                onCreate = viewModel::create,
                                onUpdate = viewModel::update,
                                onDelete = viewModel::delete,
                                onInstanceSelected = { instance ->
                                    viewModel.select(instance)
                                    val intent = Intent(context, MainActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                    finish()
                                },
                                onCancel = { finish() }
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
fun InstanceScreen(
    instances: List<DatabaseInstance>,
    currentInstanceId: String,
    onCreate: (name: String) -> Unit,
    onUpdate: (instance: DatabaseInstance, name: String, titulation: String?) -> Unit,
    onDelete: (DatabaseInstance) -> Unit,
    onInstanceSelected: (DatabaseInstance) -> Unit,
    onCancel: () -> Unit
) {

    val context = LocalContext.current
    val isDarkTheme = LocalDarkTheme.current

    var showNewDialog by remember { mutableStateOf(false) }
    var setName by remember { mutableStateOf("") }
    if (showNewDialog) {
        AlertDialog(
            onDismissRequest = {
                setName = ""
                showNewDialog = false
            },
            title = { Text(text = "Nueva Instancia") },
            text = {
                Column {
                    Text("Introduce un nombre para esta instancia:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = setName,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isLetterOrDigit() }
                            setName = filtered
                        },
                        placeholder = { Text("Nombre") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreate(setName)
                        setName = ""
                        showNewDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                Button(onClick = {
                    setName = ""
                    showNewDialog = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var instanceToEdit by remember { mutableStateOf<DatabaseInstance?>(null) }
    var instanceName by remember { mutableStateOf(instanceToEdit?.name ?: "") }
    var instanceTitulation by remember { mutableStateOf(instanceToEdit?.titulation ?: "") }
    LaunchedEffect(instanceToEdit) {
        instanceName = instanceToEdit?.name ?: ""
        instanceTitulation = instanceToEdit?.titulation ?: ""
    }
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
            },
            title = { Text(text = "Editar Instancia") },
            text = {
                Column {
                    Text("Introduce un nombre para esta instancia:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = instanceName,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isLetterOrDigit() }
                            instanceName = filtered
                        },
                        placeholder = { Text("Nombre") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Titulación de esta instancia:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = instanceTitulation,
                        onValueChange = { newValue ->
                            instanceTitulation = newValue
                        },
                        placeholder = { Text("Titulación (opcional)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (instanceName.isNotBlank()) {
                            onUpdate(instanceToEdit!!, instanceName, instanceTitulation)
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showEditDialog = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(instances, key = { it.id }) { instance ->

                val dismissState = rememberSwipeToDismissBoxState()
                val coroutineScope = rememberCoroutineScope()
                var showDeleteDialogForThisInstance by remember { mutableStateOf(false) }

                val isSelected = instance.id == currentInstanceId

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = !isSelected,
                    enableDismissFromEndToStart = !isSelected,
                    backgroundContent = {
                        Box(modifier = Modifier.background(Color.Red))
                    }
                ) {

                    val isSelected = instance.id == currentInstanceId

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onInstanceSelected(instance) }
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected && !isDarkTheme) MaterialTheme.colorScheme.primary else if (isSelected) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                instance.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = DelightRegular,
                                    color = if (isSelected && !isDarkTheme)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            Spacer(Modifier.weight(1f))

                            IconButton(onClick = {
                                instanceToEdit = instance
                                showEditDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = if (!isDarkTheme) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }
                        }
                    }

                    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                        showDeleteDialogForThisInstance = true
                    }

                    if (showDeleteDialogForThisInstance) {
                        AlertDialog(
                            onDismissRequest = {
                                coroutineScope.launch { dismissState.snapTo(SwipeToDismissBoxValue.Settled) }
                                showDeleteDialogForThisInstance = false
                            },
                            title = { Text("Eliminar instancia") },
                            text = { Text("¿Deseas eliminar ${instance.name}?") },
                            confirmButton = {
                                Button(onClick = {
                                    showDeleteDialogForThisInstance = false
                                    onDelete(instance)
                                }) { Text("Eliminar") }
                            },
                            dismissButton = {
                                Button(onClick = {
                                    coroutineScope.launch { dismissState.snapTo(SwipeToDismissBoxValue.Settled) }
                                    showDeleteDialogForThisInstance = false
                                }) { Text("Cancelar") }
                            }
                        )
                    }

                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNewDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Añadir nueva instancia",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                        )
                    }
                }
            }

        }

        Button(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 16.dp)
        ) {
            Text("Cancelar")
        }
    }

}
