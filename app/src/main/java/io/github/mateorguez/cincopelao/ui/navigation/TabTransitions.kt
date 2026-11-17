package io.github.mateorguez.cincopelao.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/*
 * Transiciones entre pestañas al estilo «eje compartido» de Material: la pestaña nueva entra
 * desde el lado en que está en la barra inferior (a la derecha → entra desde la derecha) con un
 * desplazamiento corto, mientras la anterior se desvanece rápido hacia el lado contrario. Así
 * nunca se ven las dos pantallas enteras superpuestas y el movimiento indica a dónde se va.
 */

private const val DURATION = 320
/** La saliente desaparece en este primer tramo; la entrante aparece en el resto. */
private const val FADE_OUT = 110

/** Fracción del ancho que se desplaza cada pantalla: lo justo para notar la dirección. */
private const val SLIDE_FRACTION = 6

/**
 * Sentido del movimiento al ir de [fromRoute] a [toRoute]: 1 si la nueva pestaña está a la
 * derecha en la barra, -1 si está a la izquierda y 0 si no se sabe (rutas desconocidas o iguales).
 */
fun tabDirection(fromRoute: String?, toRoute: String?): Int {
    val from = BottomNavItem.entries.indexOfFirst { it.route == fromRoute }
    val to = BottomNavItem.entries.indexOfFirst { it.route == toRoute }
    if (from < 0 || to < 0) return 0
    return to.compareTo(from)
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.direction(): Int =
    tabDirection(initialState.destination.route, targetState.destination.route)

/** Entrada de una pestaña (también al volver atrás: el sentido sale de la posición en la barra). */
val tabEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    val dir = direction()
    slideInHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { dir * it / SLIDE_FRACTION } +
        fadeIn(tween(DURATION - FADE_OUT, delayMillis = FADE_OUT, easing = LinearOutSlowInEasing))
}

/** Salida de una pestaña hacia el lado contrario al que entra la nueva. */
val tabExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    val dir = direction()
    slideOutHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { -dir * it / SLIDE_FRACTION } +
        fadeOut(tween(FADE_OUT, easing = FastOutLinearInEasing))
}
