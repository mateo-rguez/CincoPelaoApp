package com.example.cincopelao

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cincopelao.DatabaseManager
import com.example.cincopelao.DatabaseManager.currentDb
import com.example.cincopelao.ui.theme.CincoPelaoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File

class InstancesActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val context = LocalContext.current
            val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)

            CincoPelaoTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Instancias",
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
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Menú")
                                }
                            },
                        )
                    },
                    content = { paddingValues ->

                        val context = LocalContext.current

                        Box(modifier = Modifier.padding(paddingValues)) {
                            InstanceScreen(
                                instances = DatabaseManager.instances,
                                currentDb = currentDb,
                                onInstanceSelected = { instance ->
                                    DatabaseManager.setCurrentDb(context, instance)
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun InstanceScreen(
    instances: List<DatabaseManager.DatabaseInstance>,
    currentDb: AppDatabase?,
    onInstanceSelected: (DatabaseManager.DatabaseInstance) -> Unit,
    onCancel: () -> Unit
) {

    val context = LocalContext.current
    val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)

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
                        val dbManager = DatabaseManager
                        dbManager.createInstance(context, setName, false)
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
    var instanceToEdit by remember { mutableStateOf<DatabaseManager.DatabaseInstance?>(null) }
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
                        val dbManager = DatabaseManager
                        if (instanceName.isNotBlank()) {
                            dbManager.renameInstance(context, instanceToEdit!!, instanceName, instanceTitulation)
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
            items(instances) { instance ->

                val dismissState = rememberDismissState()
                val coroutineScope = rememberCoroutineScope()
                var showDeleteDialogForThisInstance by remember { mutableStateOf(false) }

                val isSelected = instance.db == currentDb

                SwipeToDismiss(
                    state = dismissState,
                    directions = if (!isSelected) setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart) else emptySet(),
                    background = {
                        Box(modifier = Modifier.background(Color.Red))
                    }
                ) {

                    val isSelected = instance.db == currentDb

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
                                    fontFamily = FontFamily(Font(R.font.delight_regular)),
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

                    if (dismissState.isDismissed(DismissDirection.StartToEnd) || dismissState.isDismissed(DismissDirection.EndToStart)) {
                        showDeleteDialogForThisInstance = true
                    }

                    if (showDeleteDialogForThisInstance) {
                        AlertDialog(
                            onDismissRequest = {
                                coroutineScope.launch { dismissState.snapTo(DismissValue.Default) }
                                showDeleteDialogForThisInstance = false
                            },
                            title = { Text("Eliminar instancia") },
                            text = { Text("¿Deseas eliminar ${instance.name}?") },
                            confirmButton = {
                                Button(onClick = {
                                    showDeleteDialogForThisInstance = false
                                    deleteInstance(context, instance.id)
                                }) { Text("Eliminar") }
                            },
                            dismissButton = {
                                Button(onClick = {
                                    coroutineScope.launch { dismissState.snapTo(DismissValue.Default) }
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

fun deleteInstance(context: Context, instanceId: String) {
    try {
        val instance = DatabaseManager.instances.find { it.id == instanceId }
        instance!!.db.close()

        context.deleteDatabase(instanceId)

        val scheduleDir = File(context.filesDir, "SCHEDULE_${instance.id}")
        if (scheduleDir.exists()) {
            scheduleDir.deleteRecursively()
        }

        DatabaseManager.deleteInstanceFromList(instance.id)

        DatabaseManager.saveInstances(context)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}