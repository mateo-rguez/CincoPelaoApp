package io.github.mateorguez.cincopelao.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Al aparecer, el elemento se despliega hacia abajo mientras gana opacidad. */
val ItemEnter: EnterTransition = expandVertically(tween(300)) + fadeIn(tween(300, delayMillis = 60))

/** Al irse, se desvanece y se pliega; lo de debajo sube a la vez, sin saltos. */
val ItemExit: ExitTransition = fadeOut(tween(200)) + shrinkVertically(tween(300, delayMillis = 60))

/**
 * Elementos de una columna normal (no *lazy*) que se despliegan al aparecer y se pliegan al
 * desaparecer, en vez de saltar. Los que se quitan se siguen dibujando con su último valor
 * hasta que termina su salida, en el sitio que ocupaban. Los que ya estaban la primera vez que
 * se compone no se animan (al abrir la pantalla todo aparece directamente).
 */
@Composable
fun <T> AnimatedColumnItems(
    items: List<T>,
    key: (T) -> Any,
    enter: EnterTransition = ItemEnter,
    exit: ExitTransition = ItemExit,
    content: @Composable (item: T, isFirst: Boolean) -> Unit
) {
    val states = remember { HashMap<Any, MutableTransitionState<Boolean>>() }
    val lastItems = remember { HashMap<Any, T>() }
    var order by remember { mutableStateOf(emptyList<Any>()) }
    val composed = remember { booleanArrayOf(false) }

    val currentKeys = items.map(key)
    items.forEach { lastItems[key(it)] = it }

    // Se quitan los que ya terminaron de salir y se colocan los que salen entre los actuales
    val finished = order.filter { k ->
        k !in currentKeys && states[k]?.let { it.isIdle && !it.currentState } != false
    }.toSet()
    val display = keepRemovedInPlace(order.filterNot { it in finished }, currentKeys)

    SideEffect {
        finished.forEach { states.remove(it); lastItems.remove(it) }
        display.forEach { states[it]?.targetState = it in currentKeys }
        order = display
        composed[0] = true
    }

    val firstKey = currentKeys.firstOrNull()
    display.forEach { k ->
        val item = lastItems[k] ?: return@forEach
        // Los nuevos nacen ocultos (salvo en la primera composición) y el SideEffect los muestra
        val state = states.getOrPut(k) { MutableTransitionState(!composed[0]) }
        key(k) {
            AnimatedVisibility(visibleState = state, enter = enter, exit = exit) {
                content(item, k == firstKey)
            }
        }
    }
}

/**
 * Orden en que dibujar [current] más los elementos de [previous] que ya no están (saliendo):
 * cada uno queda justo detrás del que tenía delante, para que se pliegue donde estaba.
 */
fun <K> keepRemovedInPlace(previous: List<K>, current: List<K>): List<K> {
    val currentSet = current.toSet()
    val result = current.toMutableList()
    previous.forEachIndexed { index, k ->
        if (k in currentSet) return@forEachIndexed
        val anchor = (index - 1 downTo 0).map { previous[it] }.firstOrNull { it in result }
        result.add(if (anchor == null) 0 else result.indexOf(anchor) + 1, k)
    }
    return result
}
