package io.github.mateorguez.cincopelao.ui.navigation

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import io.github.mateorguez.cincopelao.R
import io.github.mateorguez.cincopelao.ui.appearance.AppearanceActivity
import io.github.mateorguez.cincopelao.ui.backup.BackupActivity
import io.github.mateorguez.cincopelao.ui.instances.InstancesActivity
import io.github.mateorguez.cincopelao.ui.notifications.NotificationsActivity
import io.github.mateorguez.cincopelao.ui.theme.appColors

private const val GITHUB_USER = "mateo-rguez"

/**
 * Menú lateral: las pestañas (las mismas de la barra inferior), y tras un divisor el modo
 * oscuro (atajo; el resto del tema está en Apariencia), Apariencia, Instancias y
 * Notificaciones. Al pie, versión y autor.
 */
@Composable
fun AppDrawer(
    onToggleTheme: () -> Unit,
    darkTheme: Boolean,
    onItemSelected: (BottomNavItem) -> Unit,
    closeDrawer: () -> Unit
) {
    val context = LocalContext.current
    // Abre una pantalla y cierra el menú, para que al volver ya no esté abierto
    fun open(activity: Class<*>) {
        closeDrawer()
        context.startActivity(Intent(context, activity))
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .background(MaterialTheme.appColors.bar)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.appColors.bar)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Menú",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.appColors.onBar
            )
        }

        Spacer(Modifier.height(12.dp))

        BottomNavItem.entries.forEach { item ->
            DrawerRow(item.icon, item.label, onClick = {
                onItemSelected(item)
                closeDrawer()
            })
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        DrawerRow(Icons.Default.DarkMode, "Modo oscuro", onClick = onToggleTheme) {
            Switch(
                modifier = Modifier.scale(0.85f),
                checked = darkTheme,
                onCheckedChange = { onToggleTheme() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
        DrawerRow(Icons.Default.Palette, "Apariencia", onClick = { open(AppearanceActivity::class.java) })
        DrawerRow(Icons.Default.DataArray, "Instancias", onClick = { open(InstancesActivity::class.java) })
        DrawerRow(Icons.Default.Notifications, "Notificaciones", onClick = { open(NotificationsActivity::class.java) })
        DrawerRow(Icons.Default.SettingsBackupRestore, "Copia de seguridad", onClick = { open(BackupActivity::class.java) })

        Spacer(Modifier.weight(1f))

        DrawerFooter(versionName = remember(context) { context.appVersionName() })
    }
}

/** Fila del menú: icono, texto y, si hace falta, un control al final (el interruptor). */
@Composable
private fun DrawerRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Pie discreto: nombre y versión de la app, y el autor con el logo de GitHub. */
@Composable
private fun DrawerFooter(versionName: String?) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = listOfNotNull("Cinco Pelao", versionName?.let { "versión $it" }).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = muted
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_github),
                contentDescription = "GitHub",
                tint = muted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(text = GITHUB_USER, style = MaterialTheme.typography.labelMedium, color = muted)
        }
    }
}

private fun Context.appVersionName(): String? =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull()
