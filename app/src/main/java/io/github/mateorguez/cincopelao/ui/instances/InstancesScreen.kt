package io.github.mateorguez.cincopelao.ui.instances

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import kotlin.math.abs
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mateorguez.cincopelao.data.instances.DatabaseInstance
import io.github.mateorguez.cincopelao.ui.theme.DelightRegular
import io.github.mateorguez.cincopelao.ui.theme.appColors

/** Letras, números y espacios (ya no se usa como nombre de archivo, así que puede tener espacios). */
private fun sanitizeInstanceName(input: String) = input.filter { it.isLetterOrDigit() || it == ' ' }

/** Clave de la fila «Nueva instancia» cuando es ella la que está abierta como formulario. */
private const val NEW_INSTANCE = "nueva"

private val CardShape = RoundedCornerShape(12.dp)

/** Fracción del ancho que hay que deslizar para pedir el borrado. */
private const val SWIPE_THRESHOLD = 0.35f

/** Espera a que la tarjeta y el teclado terminen de animarse antes de desplazar la lista. */
private const val FORM_SETTLE_MS = 350L

/** Cómo se ve cada elemento: su fila, el formulario o la confirmación de borrar. */
private enum class ItemStage { ROW, FORM, CONFIRM_DELETE }

/**
 * Lista de instancias. Cada tarjeta muestra su resumen y, al editarla (o al pulsar «Nueva
 * instancia»), se transforma en el formulario en su sitio; desde ahí también se elimina. Solo
 * hay un formulario abierto a la vez.
 */
@Composable
fun InstancesScreen(
    onInstanceSelected: () -> Unit,
    viewModel: InstancesViewModel = hiltViewModel()
) {
    val instances by viewModel.instances.collectAsStateWithLifecycle()
    val currentInstanceId by viewModel.currentInstanceId.collectAsStateWithLifecycle()
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()

    // Id de la instancia abierta como formulario, NEW_INSTANCE para la nueva o null si ninguna
    var openId by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = openId != null) { openId = null }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(instances, key = { it.id }) { instance ->
            InstanceItem(
                instance = instance,
                summary = summaries[instance.id],
                isSelected = instance.id == currentInstanceId,
                isOpen = openId == instance.id,
                onClick = {
                    viewModel.select(instance)
                    onInstanceSelected()
                },
                onOpen = { openId = instance.id },
                onClose = { openId = null },
                onSave = { name, titulation ->
                    viewModel.update(instance, name, titulation)
                    openId = null
                },
                onDelete = {
                    openId = null
                    viewModel.delete(instance)
                }
            )
        }

        item(key = NEW_INSTANCE) {
            NewInstanceItem(
                isOpen = openId == NEW_INSTANCE,
                onOpen = { openId = NEW_INSTANCE },
                onClose = { openId = null },
                onSave = { name, titulation ->
                    viewModel.create(name, titulation)
                    openId = null
                }
            )
        }

        // Pista para descubrir el deslizamiento (solo si hay alguna instancia que se pueda borrar)
        if (instances.size > 1) {
            item(key = "pista") { SwipeHint() }
        }
    }
}

/** Texto discreto bajo la lista que explica cómo eliminar una instancia. */
@Composable
private fun SwipeHint() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        Icon(Icons.Default.Swipe, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Desliza una instancia hacia un lado para eliminarla", style = MaterialTheme.typography.bodySmall, color = color)
    }
}

/** Transición común: el contenido se funde mientras la tarjeta cambia de tamaño con un muelle. */
private fun AnimatedContentTransitionScope<*>.stageTransition(): ContentTransform =
    (fadeIn(tween(150, delayMillis = 90)) togetherWith fadeOut(tween(90)))
        .using(SizeTransform(clip = true) { _, _ -> spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow) })

/** Tarjeta de una instancia; se borra deslizándola o desde su formulario (salvo la activa). */
@Composable
private fun InstanceItem(
    instance: DatabaseInstance,
    summary: String?,
    isSelected: Boolean,
    isOpen: Boolean,
    onClick: () -> Unit,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onSave: (name: String, titulation: String?) -> Unit,
    onDelete: () -> Unit
) {
    val accent = MaterialTheme.appColors.accent
    var confirmingDelete by remember { mutableStateOf(false) }
    LaunchedEffect(isOpen) { if (!isOpen) confirmingDelete = false }
    val stage = when {
        confirmingDelete -> ItemStage.CONFIRM_DELETE
        isOpen -> ItemStage.FORM
        else -> ItemStage.ROW
    }

    // La tarjeta nunca se va de la pantalla: al soltarla pasado el umbral vuelve a su sitio y se
    // convierte en la confirmación (la activa solo vuelve, no se puede borrar)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled && !isSelected) confirmingDelete = true
            false
        },
        positionalThreshold = { total -> total * SWIPE_THRESHOLD }
    )

    SwipeToDismissBox(
        state = dismissState,
        gesturesEnabled = stage == ItemStage.ROW,
        backgroundContent = { SwipeBackground(dismissState, deletable = !isSelected) }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(if (isSelected) 2.dp else 0.dp, if (isSelected) accent else Color.Transparent, CardShape),
            shape = CardShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            AnimatedContent(targetState = stage, transitionSpec = { stageTransition() }, label = "instancia") { shown ->
                when (shown) {
                    ItemStage.ROW -> InstanceRow(instance.name, summary, isSelected, onClick, onOpen)
                    ItemStage.FORM -> InstanceForm(
                        title = "Editar instancia",
                        initialName = instance.name,
                        initialTitulation = instance.titulation.orEmpty(),
                        // La activa no se puede borrar: primero hay que cambiar a otra
                        onDelete = if (isSelected) null else ({ confirmingDelete = true }),
                        onCancel = onClose,
                        onSave = onSave
                    )
                    ItemStage.CONFIRM_DELETE -> ConfirmDelete(
                        name = instance.name,
                        onCancel = { confirmingDelete = false },
                        onConfirm = onDelete
                    )
                }
            }
        }
    }
}

/**
 * Lo que asoma detrás de la tarjeta al deslizarla. El rojo se va intensificando hasta el umbral
 * y la papelera, en el lado que se descubre, crece al pasarlo (con una vibración suave). En la
 * activa el fondo es gris y explica por qué no se puede borrar.
 */
@Composable
private fun SwipeBackground(state: SwipeToDismissBoxState, deletable: Boolean) {
    var width by remember { mutableFloatStateOf(1f) }
    val offset = runCatching { state.requireOffset() }.getOrDefault(0f)
    val fraction = (abs(offset) / width).coerceIn(0f, 1f)
    val armed = fraction >= SWIPE_THRESHOLD

    val view = LocalView.current
    LaunchedEffect(armed) {
        if (armed) {
            val feedback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            view.performHapticFeedback(feedback)
        }
    }

    val scheme = MaterialTheme.colorScheme
    val strength = (fraction / SWIPE_THRESHOLD).coerceIn(0f, 1f)
    val background = if (deletable) lerp(scheme.surfaceVariant, scheme.error, strength) else scheme.surfaceVariant.copy(alpha = 0.6f)
    val content = if (deletable) lerp(scheme.onSurfaceVariant, scheme.onError, strength) else scheme.onSurfaceVariant
    val iconScale by animateFloatAsState(
        targetValue = if (armed && deletable) 1.3f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "papelera"
    )
    val fromStart = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd

    Row(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            .background(if (offset == 0f) Color.Transparent else background, CardShape)
            .padding(horizontal = 24.dp),
        horizontalArrangement = if (fromStart) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (deletable) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = content,
                modifier = Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }
            )
        } else {
            if (!fromStart) Text("No se puede eliminar la activa", style = MaterialTheme.typography.bodyMedium, color = content)
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = content,
                modifier = Modifier.padding(horizontal = 8.dp).size(20.dp)
            )
            if (fromStart) Text("No se puede eliminar la activa", style = MaterialTheme.typography.bodyMedium, color = content)
        }
    }
}

/** Nombre, marca de activa y resumen; el lápiz abre el formulario. */
@Composable
private fun InstanceRow(name: String, summary: String?, isSelected: Boolean, onClick: () -> Unit, onEdit: () -> Unit) {
    val accent = MaterialTheme.appColors.accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = DelightRegular),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isSelected) {
                    Spacer(Modifier.width(10.dp))
                    ActiveBadge(accent)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                summary ?: " ", // reserva la línea mientras se calcula, para que no salte
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = accent)
        }
    }
}

/** Pastilla «✓ Activa» en el color de acento. */
@Composable
private fun ActiveBadge(accent: Color) {
    Row(
        modifier = Modifier
            .background(accent.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Check, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("Activa", style = MaterialTheme.typography.labelMedium, color = accent)
    }
}

/** Última fila de la lista: «＋ Nueva instancia» con borde discontinuo, que se abre en el formulario. */
@Composable
private fun NewInstanceItem(isOpen: Boolean, onOpen: () -> Unit, onClose: () -> Unit, onSave: (String, String?) -> Unit) {
    val outline = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Borde discontinuo solo mientras es la fila; el formulario es una tarjeta normal
                if (!isOpen) drawRoundRect(
                    color = outline,
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            },
        shape = CardShape,
        color = if (isOpen) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        AnimatedContent(targetState = isOpen, transitionSpec = { stageTransition() }, label = "nueva instancia") { open ->
            if (open) {
                InstanceForm(
                    title = "Nueva instancia",
                    initialName = "",
                    initialTitulation = "",
                    onDelete = null,
                    onCancel = onClose,
                    onSave = onSave
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Nueva instancia", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** Formulario de nombre (obligatorio) y titulación (opcional), dentro de la propia tarjeta. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InstanceForm(
    title: String,
    initialName: String,
    initialTitulation: String,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit,
    onSave: (name: String, titulation: String?) -> Unit
) {
    // Con TextFieldValue para dejar el cursor al final del nombre al editar
    var name by remember { mutableStateOf(TextFieldValue(initialName, TextRange(initialName.length))) }
    var titulation by remember { mutableStateOf(initialTitulation) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    // La lista solo desplaza hasta el campo con el foco: con el teclado abierto quedaban ocultos
    // la titulación y los botones. Cada vez que el teclado cambia de alto (y cuando la tarjeta
    // termina de crecer) se pide mostrar el formulario entero.
    val bringIntoView = remember { BringIntoViewRequester() }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottom) {
        delay(FORM_SETTLE_MS)
        bringIntoView.bringIntoView()
    }

    Column(
        Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.copy(text = sanitizeInstanceName(it.text)) },
            label = { Text("Nombre") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().padding(end = 8.dp).focusRequester(focus)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = titulation,
            onValueChange = { titulation = it },
            label = { Text("Titulación (opcional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onDelete != null) {
                TextButton(onClick = onDelete) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("Cancelar") }
            TextButton(enabled = name.text.isNotBlank(), onClick = { onSave(name.text.trim(), titulation.trim()) }) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Confirmación de borrado en la propia tarjeta. */
@Composable
private fun ConfirmDelete(name: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp)) {
        Text("¿Eliminar «$name»?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            "Se borrarán sus asignaturas, eventos, tareas y horario. No se puede deshacer.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.align(Alignment.End)) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            // Borrar una instancia no se puede deshacer: se pide además el desbloqueo del móvil
            TextButton(onClick = { requestDeviceAuth(context, "Eliminar instancia", "Confirma que quieres eliminar «$name»", onConfirm) }) {
                Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
}
