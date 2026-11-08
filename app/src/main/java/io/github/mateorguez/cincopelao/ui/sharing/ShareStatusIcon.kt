package io.github.mateorguez.cincopelao.ui.sharing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Tune
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mateorguez.cincopelao.sharing.session.SharePhase
import io.github.mateorguez.cincopelao.ui.theme.StatusDone

/** Escenas del icono: varias fases comparten animación. */
private enum class IconScene(val description: String) {
    SETUP("Faltan permisos o ajustes"),
    NAMING("Listo para recibir"),
    ADVERTISING("Anunciándose por Bluetooth"),
    SCANNING("Buscando por Bluetooth"),
    READING_BEACON("Leyendo la baliza Bluetooth"),
    WIFI("Conectando por Wi‑Fi Direct"),
    HANDSHAKE("Intercambiando saludo"),
    CHOOSING("Eligiendo datos"),
    TRANSFER("Transfiriendo datos"),
    IMPORTING("Guardando"),
    DONE("Completado"),
    FAILED("Error")
}

private fun sceneOf(phase: SharePhase): IconScene = when (phase) {
    is SharePhase.NeedsSetup -> IconScene.SETUP
    SharePhase.Idle -> IconScene.NAMING
    SharePhase.Advertising -> IconScene.ADVERTISING
    SharePhase.Scanning -> IconScene.SCANNING
    SharePhase.ReadingBeacon -> IconScene.READING_BEACON
    SharePhase.ConnectingWifi -> IconScene.WIFI
    SharePhase.Handshaking -> IconScene.HANDSHAKE
    SharePhase.Choosing, SharePhase.AwaitingApproval -> IconScene.CHOOSING
    is SharePhase.Transferring -> IconScene.TRANSFER
    SharePhase.Importing -> IconScene.IMPORTING
    SharePhase.Received, SharePhase.Done -> IconScene.DONE
    is SharePhase.Failed -> IconScene.FAILED
}

private val ICON_SIZE = 140.dp

/**
 * Icono animado que cuenta lo que está pasando: ondas BLE al anunciarse, barrido de radar al
 * buscar, barras de Wi‑Fi encendiéndose al conectar, paquetes yendo y viniendo en el saludo,
 * anillo con el progreso real al transferir, check que se dibuja al terminar...
 *
 * Todo con Compose: sin librerías ni archivos externos. Si algún día se usan Lottie, basta con
 * cambiar el contenido de cada escena aquí.
 */
@Composable
fun ShareStatusIcon(phase: SharePhase, modifier: Modifier = Modifier) {
    val scene = sceneOf(phase)
    val progress = (phase as? SharePhase.Transferring)?.progress ?: 0f
    AnimatedContent(
        targetState = scene,
        transitionSpec = {
            (fadeIn(tween(300)) + scaleIn(initialScale = 0.85f, animationSpec = tween(300)))
                .togetherWith(fadeOut(tween(200)) + scaleOut(targetScale = 1.1f, animationSpec = tween(200)))
        },
        label = "escena del icono",
        modifier = modifier.semantics { contentDescription = scene.description }
    ) { target ->
        Box(Modifier.size(ICON_SIZE), contentAlignment = Alignment.Center) {
            when (target) {
                IconScene.SETUP -> SetupScene()
                IconScene.NAMING -> NamingScene()
                IconScene.ADVERTISING -> AdvertisingScene()
                IconScene.SCANNING -> ScanningScene()
                IconScene.READING_BEACON -> ReadingBeaconScene()
                IconScene.WIFI -> WifiScene()
                IconScene.HANDSHAKE -> HandshakeScene()
                IconScene.CHOOSING -> ChoosingScene()
                IconScene.TRANSFER -> TransferScene(progress)
                IconScene.IMPORTING -> ImportingScene()
                IconScene.DONE -> DoneScene()
                IconScene.FAILED -> FailedScene()
            }
        }
    }
}

@Composable
private fun CenterIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color = shareAccent(), modifier: Modifier = Modifier) {
    Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(56.dp))
}

/** Deslizadores de ajustes que laten suave: falta algo por activar. */
@Composable
private fun SetupScene() {
    val scale by rememberInfiniteTransition(label = "ajustes").animateFloat(
        1f, 1.1f, infiniteRepeatable(tween(1_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "latido"
    )
    CenterIcon(Icons.Default.Tune, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
}

/**
 * Avión de papel flotando en el sitio: sube y baja con un ligero cabeceo, con una sombra que se
 * encoge al subir y estelas que aparecen detrás. Es la espera tranquila antes de hacerse visible.
 */
@Composable
private fun NamingScene() {
    val color = shareAccent()
    val transition = rememberInfiniteTransition(label = "avión")
    // Una vuelta completa (0..2π) cada 2,8 s: seno para la altura, coseno para el cabeceo
    val phase by transition.animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2_800, easing = LinearEasing)), label = "vuelo"
    )
    val trail by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1_400, easing = LinearEasing)), label = "estela"
    )
    val lift = sin(phase)              // −1 abajo … 1 arriba
    val pitch = cos(phase) * 6f        // grados: morro arriba al subir, abajo al bajar
    val planeSize = 64.dp
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            // Sombra en el "suelo": más pequeña y clara cuanto más alto vuela
            val shadowWidth = size.width * (0.34f - 0.06f * lift)
            drawOval(
                color.copy(alpha = 0.16f - 0.06f * lift),
                topLeft = Offset(center.x - shadowWidth / 2, size.height * 0.80f),
                size = Size(shadowWidth, 6.dp.toPx())
            )
            // Estelas detrás del avión (a su izquierda), que se alargan y se desvanecen
            val planeY = center.y - lift * 10.dp.toPx()
            listOf(-9.dp.toPx() to 0f, 7.dp.toPx() to 0.5f).forEach { (dy, delay) ->
                val t = (trail + delay) % 1f
                val endX = center.x - 30.dp.toPx() - t * 8.dp.toPx()
                val length = 14.dp.toPx() + t * 14.dp.toPx()
                drawLine(
                    color.copy(alpha = (1f - t) * 0.55f),
                    Offset(endX - length, planeY + dy),
                    Offset(endX, planeY + dy),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.Send, // el icono de "enviar" de Material es un avión de papel
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(planeSize)
                .graphicsLayer {
                    translationY = -lift * 10.dp.toPx()
                    rotationZ = -pitch
                }
        )
    }
}

/** Ondas que salen del icono de Bluetooth: el receptor se está anunciando. */
@Composable
private fun AdvertisingScene() {
    val color = shareAccent()
    val t by rememberInfiniteTransition(label = "anuncio").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2_400, easing = LinearEasing)), label = "ondas"
    )
    Canvas(Modifier.fillMaxSize()) {
        val maxRadius = size.minDimension / 2
        repeat(3) { i ->
            val phase = (t + i / 3f) % 1f
            drawCircle(
                color = color.copy(alpha = (1f - phase) * 0.6f),
                radius = maxRadius * (0.35f + 0.65f * phase),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
    CenterIcon(Icons.Default.Bluetooth)
}

/** Barrido de radar: el emisor busca receptores. */
@Composable
private fun ScanningScene() {
    val color = shareAccent()
    val angle by rememberInfiniteTransition(label = "radar").animateFloat(
        0f, 360f, infiniteRepeatable(tween(2_000, easing = LinearEasing)), label = "barrido"
    )
    Canvas(Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2
        listOf(0.45f, 0.72f, 1f).forEach {
            drawCircle(color.copy(alpha = 0.18f), radius * it - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
        }
        rotate(angle) {
            drawArc(
                brush = Brush.sweepGradient(
                    0f to Color.Transparent,
                    0.75f to Color.Transparent,
                    1f to color.copy(alpha = 0.45f)
                ),
                startAngle = 0f, sweepAngle = 360f, useCenter = true
            )
            drawLine(color, center, Offset(center.x + radius, center.y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
    CenterIcon(Icons.AutoMirrored.Filled.BluetoothSearching)
}

/** Puntos que convergen hacia el centro: se están leyendo datos por GATT. */
@Composable
private fun ReadingBeaconScene() {
    val color = MaterialTheme.colorScheme.tertiary
    val t by rememberInfiniteTransition(label = "gatt").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1_200, easing = LinearEasing)), label = "lectura"
    )
    Canvas(Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2
        repeat(8) { i ->
            val angle = Math.toRadians(i * 45.0)
            val phase = (t + i * 0.125f) % 1f
            val r = radius * (1f - phase * 0.6f)
            drawCircle(
                color.copy(alpha = phase),
                radius = 4.dp.toPx(),
                center = Offset(center.x + (r * kotlin.math.cos(angle)).toFloat(), center.y + (r * kotlin.math.sin(angle)).toFloat())
            )
        }
    }
    CenterIcon(Icons.Default.Bluetooth)
}

/** Barras de Wi‑Fi que se encienden de abajo arriba. */
@Composable
private fun WifiScene() {
    val on = shareAccent()
    val off = on.copy(alpha = 0.18f)
    val step by rememberInfiniteTransition(label = "wifi").animateFloat(
        0f, 4f, infiniteRepeatable(tween(1_600, easing = LinearEasing)), label = "barras"
    )
    Canvas(Modifier.size(100.dp)) {
        val lit = step.toInt() // 0..3: cuántos elementos están encendidos
        val base = Offset(size.width / 2, size.height * 0.82f)
        drawCircle(if (lit >= 0) on else off, radius = 7.dp.toPx(), center = base)
        listOf(0.32f, 0.56f, 0.8f).forEachIndexed { i, fraction ->
            val r = size.width * fraction * 0.75f
            drawArc(
                color = if (lit > i) on else off,
                startAngle = 225f, sweepAngle = 90f, useCenter = false,
                topLeft = Offset(base.x - r, base.y - r), size = Size(r * 2, r * 2),
                style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

/** Dos móviles y un paquete que va y vuelve: HELLO ↔ HELLO. */
@Composable
private fun HandshakeScene() {
    val color = shareAccent()
    val packet = MaterialTheme.colorScheme.tertiary
    val t by rememberInfiniteTransition(label = "saludo").animateFloat(
        0f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "paquete"
    )
    Box(Modifier.fillMaxSize()) {
        Icon(Icons.Default.Smartphone, null, tint = color, modifier = Modifier.size(48.dp).align(Alignment.CenterStart))
        Icon(Icons.Default.Smartphone, null, tint = color, modifier = Modifier.size(48.dp).align(Alignment.CenterEnd))
        Canvas(Modifier.fillMaxSize()) {
            val start = size.width * 0.3f
            val end = size.width * 0.7f
            drawLine(color.copy(alpha = 0.2f), Offset(start, center.y), Offset(end, center.y), strokeWidth = 2.dp.toPx())
            drawCircle(packet, radius = 6.dp.toPx(), center = Offset(start + (end - start) * t, center.y))
        }
    }
}

/** Lista que "respira" con un anillo discontinuo girando: alguien está decidiendo. */
@Composable
private fun ChoosingScene() {
    val color = shareAccent()
    val transition = rememberInfiniteTransition(label = "elegir")
    val scale by transition.animateFloat(1f, 1.08f, infiniteRepeatable(tween(1_200), RepeatMode.Reverse), label = "pulso")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(10_000, easing = LinearEasing)), label = "anillo")
    Canvas(Modifier.fillMaxSize()) {
        rotate(rotation) {
            val dash = 12
            repeat(dash) { i ->
                drawArc(
                    color.copy(alpha = 0.35f), startAngle = i * 360f / dash, sweepAngle = 360f / dash / 2, useCenter = false,
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()), size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                    style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
    CenterIcon(Icons.AutoMirrored.Filled.FactCheck, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
}

/** Anillo con el progreso real de la transferencia y el porcentaje en el centro. */
@Composable
private fun TransferScene(progress: Float) {
    val color = shareAccent()
    val shown by animateFloatAsState(progress, animationSpec = tween(250), label = "progreso")
    val glow by rememberInfiniteTransition(label = "flujo").animateFloat(
        0f, 360f, infiniteRepeatable(tween(1_500, easing = LinearEasing)), label = "brillo"
    )
    Canvas(Modifier.fillMaxSize()) {
        val stroke = 10.dp.toPx()
        drawProgressRing(color.copy(alpha = 0.15f), 360f, stroke)
        drawProgressRing(color, 360f * shown, stroke)
        // Un destello que recorre el anillo para que se vea vivo aunque el progreso vaya lento
        rotate(glow) {
            drawArc(
                color.copy(alpha = 0.35f), startAngle = -90f, sweepAngle = 30f, useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke / 3, cap = StrokeCap.Round)
            )
        }
    }
    Text(
        "${(shown * 100).toInt()}%",
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        color = color
    )
}

private fun DrawScope.drawProgressRing(color: Color, sweep: Float, stroke: Float) = drawArc(
    color, startAngle = -90f, sweepAngle = sweep, useCenter = false,
    topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke),
    style = Stroke(stroke, cap = StrokeCap.Round)
)

/** Arco indeterminado alrededor del icono de guardar. */
@Composable
private fun ImportingScene() {
    val color = shareAccent()
    val transition = rememberInfiniteTransition(label = "guardar")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1_100, easing = LinearEasing)), label = "giro")
    val sweep by transition.animateFloat(40f, 220f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "arco")
    Canvas(Modifier.fillMaxSize()) {
        rotate(rotation) { drawProgressRing(color, sweep, 6.dp.toPx()) }
    }
    CenterIcon(Icons.Default.Save)
}

/** Círculo que aparece con rebote y un check que se dibuja trazo a trazo. */
@Composable
private fun DoneScene() {
    val color = StatusDone
    val circle = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        circle.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f))
        check.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(color.copy(alpha = 0.15f), radius = size.minDimension / 2 * circle.value)
        drawCircle(color, radius = size.minDimension / 2 * circle.value - 3.dp.toPx(), style = Stroke(5.dp.toPx()))
        val path = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.52f)
            lineTo(size.width * 0.44f, size.height * 0.67f)
            lineTo(size.width * 0.73f, size.height * 0.36f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * check.value, partial, true)
        drawPath(partial, color, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Aviso que se sacude una vez. */
@Composable
private fun FailedScene() {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        shake.animateTo(
            0f,
            keyframes {
                durationMillis = 500
                -14f at 60; 14f at 140; -10f at 220; 10f at 300; -4f at 380; 0f at 500
            }
        )
    }
    CenterIcon(
        Icons.Default.ErrorOutline,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(96.dp).graphicsLayer { translationX = shake.value * density }
    )
}
