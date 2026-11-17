package io.github.mateorguez.cincopelao.ui.sharing

import io.github.mateorguez.cincopelao.ui.theme.appColors
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mateorguez.cincopelao.sharing.session.SetupIssue
import io.github.mateorguez.cincopelao.ui.theme.TerminalAccent
import io.github.mateorguez.cincopelao.ui.theme.TerminalBackground
import io.github.mateorguez.cincopelao.ui.theme.TerminalDim
import io.github.mateorguez.cincopelao.ui.theme.TerminalError
import io.github.mateorguez.cincopelao.ui.theme.TerminalText

/** Color de iconos y datos destacados: legible tanto en tema claro como oscuro. */
@Composable
fun shareAccent(): Color = MaterialTheme.appColors.shareAccent

/** Botón secundario con el acento legible (el primary del tema apenas se ve en modo oscuro). */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, colors = ButtonDefaults.outlinedButtonColors(contentColor = shareAccent())) { Text(text) }
}

/** Icono animado, título y subtítulo de la fase actual. */
@Composable
fun PhaseHeader(icon: @Composable () -> Unit, title: String, subtitle: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        icon()
        Spacer(Modifier.size(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Spacer(Modifier.size(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Qué falta para poder compartir y cómo arreglarlo. */
@Composable
fun SetupCard(
    issues: List<SetupIssue>,
    onGrantPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onFix: (SetupIssue) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Antes de empezar",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            issues.forEach { issue ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "• ${issue.description}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    when (issue) {
                        SetupIssue.PERMISSIONS -> Button(onClick = onGrantPermissions) { Text("Conceder") }
                        SetupIssue.UNSUPPORTED -> Unit
                        else -> OutlinedButton(onClick = { onFix(issue) }) { Text("Activar") }
                    }
                }
            }
            if (SetupIssue.PERMISSIONS in issues) {
                Text(
                    "¿Los denegaste antes? Actívalos en los ajustes de la app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                OutlinedButton(onClick = onOpenAppSettings) { Text("Abrir ajustes de la app") }
            }
        }
    }
}

/** Código de emparejamiento grande: tiene que coincidir en los dos móviles. */
@Composable
fun VerificationCode(code: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = code.toList().joinToString(" "),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            color = shareAccent()
        )
        Text(
            "Código de emparejamiento: el otro móvil debe mostrar el mismo",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TransferProgress(progress: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.size(4.dp))
        Text("${(progress * 100).toInt()} %", fontFamily = FontFamily.Monospace)
    }
}

/**
 * Panel plegable con los datos técnicos de la sesión y una consola con el registro.
 * Nunca contiene secretos (la contraseña del grupo no se guarda en el registro).
 */
@Composable
fun TechDetailsCard(tech: TechInfo, log: List<LogLine>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Terminal, contentDescription = null, tint = shareAccent())
            Spacer(Modifier.width(8.dp))
            Text("Detalles técnicos", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Ocultar detalles" else "Ver detalles"
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                TechSection.entries.filter { tech[it].orEmpty().isNotEmpty() }.forEachIndexed { index, section ->
                    val rows = tech[section].orEmpty()
                    if (rows.isNotEmpty()) {
                        Text(
                            section.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 13.sp,
                            color = shareAccent(),
                            // Más aire entre apartados que entre filas
                            modifier = Modifier.padding(top = if (index == 0) 8.dp else 18.dp, bottom = 4.dp)
                        )
                        rows.forEach { (label, value) -> TechRow(label, value) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        "CONSOLA",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 13.sp,
                        color = shareAccent(),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { clipboard.setText(AnnotatedString(log.joinToString("\n") { it.format() })) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar registro", modifier = Modifier.size(18.dp))
                    }
                }
                Console(log)
            }
        }
    }
}

@Composable
private fun TechRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            label,
            modifier = Modifier.width(112.dp),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Console(log: List<LogLine>) {
    val scroll = rememberScrollState()
    LaunchedEffect(log.size) { scroll.animateScrollTo(scroll.maxValue) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp, max = 240.dp)
            .background(TerminalBackground, RoundedCornerShape(8.dp))
            .verticalScroll(scroll)
            .padding(8.dp)
    ) {
        Column {
            if (log.isEmpty()) Text("$ _", color = TerminalDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            log.forEach { line ->
                val color = when (line.tag) {
                    "ERR" -> TerminalError
                    "APP", "DB" -> TerminalAccent
                    else -> TerminalText
                }
                Text(line.format(), color = color, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
    }
}
