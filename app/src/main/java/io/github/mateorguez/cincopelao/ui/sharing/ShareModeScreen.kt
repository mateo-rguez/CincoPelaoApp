package io.github.mateorguez.cincopelao.ui.sharing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.R
import io.github.mateorguez.cincopelao.ui.common.EmptyState

/**
 * Pestaña de compartir: el logo con ondas en el círculo común de bienvenida, qué se comparte y
 * una tarjeta con las dos opciones, cada una con lo que hace.
 */
@Composable
fun ShareModeScreen(
    onTransmitClick: () -> Unit,
    onReceiveClick: () -> Unit
) {
    val accent = shareAccent()
    EmptyState(
        title = "Comparte con tus compañeros",
        text = "Pasa asignaturas, eventos, tareas y horario a un móvil cercano. No hace falta conexión a internet.",
        accent = accent,
        waves = true,
        illustration = {
            Icon(
                painter = painterResource(id = R.drawable.sharemode),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(128.dp)
            )
        }
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ShareOption(
                    icon = Icons.Default.Upload,
                    title = "Transmitir",
                    description = "Envía tus datos a un móvil cercano",
                    accent = accent,
                    onClick = onTransmitClick
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                )
                ShareOption(
                    icon = Icons.Default.Download,
                    title = "Recibir",
                    description = "Los datos llegan como una instancia nueva",
                    accent = accent,
                    onClick = onReceiveClick
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            Icon(Icons.Default.Lock, contentDescription = null, tint = muted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Tus notas y tu progreso no se envían", style = MaterialTheme.typography.bodySmall, color = muted)
        }
    }
}

/** Una opción de la tarjeta: icono en un círculo de acento, título, qué hace y una flecha. */
@Composable
private fun ShareOption(icon: ImageVector, title: String, description: String, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(accent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
