package io.github.mateorguez.cincopelao.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mateorguez.cincopelao.data.backup.BackupFormat
import io.github.mateorguez.cincopelao.ui.tasks.TaskCardFrame
import io.github.mateorguez.cincopelao.ui.tasks.TaskCheckbox
import io.github.mateorguez.cincopelao.ui.theme.appColors

/**
 * Copia de seguridad: exportar las instancias marcadas a un archivo e importar las de un
 * archivo como instancias nuevas. Los archivos se eligen con el selector del sistema, así que
 * no hace falta ningún permiso.
 */
@Composable
fun BackupScreen(viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupFormat.MIME_TYPE)) { uri ->
        uri?.let(viewModel::export)
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::open)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(Icons.Default.Upload, "Exportar")
        Hint("Guarda en un archivo las instancias marcadas, con todo: notas, tareas hechas y horario.")

        BackupCard {
            state.exportItems.forEachIndexed { i, item ->
                if (i > 0) RowDivider()
                CheckRow(
                    title = item.name,
                    subtitle = item.summary,
                    checked = item.id in state.exportSelected,
                    enabled = state.work == null,
                    onToggle = { viewModel.toggleExport(item.id) }
                )
            }
        }

        WorkButton(
            text = when (state.exportCount) {
                0 -> "Exportar"
                1 -> "Exportar 1 instancia"
                else -> "Exportar ${state.exportCount} instancias"
            },
            working = state.work == BackupWork.EXPORTING,
            enabled = state.canExport,
            onClick = { createFile.launch(viewModel.suggestedFileName()) }
        )
        Message(state.exportMessage)

        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

        SectionTitle(Icons.Default.Download, "Importar")
        Hint("Las instancias de la copia se añaden como nuevas; no se sobrescribe nada de lo que tienes.")

        val preview = state.importPreview
        if (preview == null) {
            WorkButton(
                text = "Elegir archivo",
                icon = Icons.Default.FolderOpen,
                working = state.work == BackupWork.READING,
                enabled = state.work == null,
                outlined = true,
                onClick = { openFile.launch(arrayOf(BackupFormat.MIME_TYPE, "application/x-zip-compressed", "application/octet-stream")) }
            )
        } else {
            BackupCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        preview.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Copia del ${preview.createdAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.appColors.muted
                    )
                }
                preview.instances.forEachIndexed { i, info ->
                    RowDivider()
                    CheckRow(
                        title = info.name,
                        subtitle = backupInfoSummary(info),
                        checked = i in preview.selected,
                        enabled = state.work == null,
                        onToggle = { viewModel.toggleImport(i) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = viewModel::cancelImport,
                    enabled = state.work == null,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancelar") }
                WorkButton(
                    text = if (preview.selected.size == 1) "Importar 1" else "Importar ${preview.selected.size}",
                    working = state.work == BackupWork.IMPORTING,
                    enabled = state.work == null && preview.selected.isNotEmpty(),
                    onClick = viewModel::importSelected,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Message(state.importMessage)
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.appColors.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.appColors.muted)
}

/** Tarjeta con el estilo de Tareas: fondo de la paleta, sombra teñida y brillo en el borde. */
@Composable
private fun BackupCard(content: @Composable () -> Unit) {
    // Sin margen propio (lo pone la columna) y con una columna interior para que las filas
    // queden pegadas a sus divisores, sin el hueco entre hijos de TaskCardFrame
    TaskCardFrame(outerPadding = PaddingValues(0.dp)) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            Column(Modifier.fillMaxWidth().animateContentSize()) { content() }
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f), thickness = 1.dp)
}

/** Fila con casilla redonda: nombre de la instancia y su resumen. */
@Composable
private fun CheckRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TaskCheckbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.appColors.muted)
        }
    }
}

/** Botón que muestra un indicador mientras su operación está en marcha. */
@Composable
private fun WorkButton(
    text: String,
    working: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    icon: ImageVector? = null,
    outlined: Boolean = false
) {
    val content: @Composable () -> Unit = {
        if (working) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            icon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text)
        }
    }
    if (outlined) {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) { content() }
    } else {
        Button(onClick = onClick, enabled = enabled, modifier = modifier) { content() }
    }
}

/** Resultado de exportar o importar, con su icono. */
@Composable
private fun Message(message: BackupMessage?) {
    AnimatedVisibility(visible = message != null) {
        val shown = message ?: return@AnimatedVisibility
        val color = if (shown.isError) MaterialTheme.colorScheme.error else MaterialTheme.appColors.done
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                if (shown.isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(shown.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
