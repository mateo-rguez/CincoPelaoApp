package io.github.mateorguez.cincopelao.ui.subjects

import io.github.mateorguez.cincopelao.data.prefs.HeaderGrade

/** Textos de la cabecera de una asignatura: la nota grande y la frase de debajo. */
data class HeaderTexts(val big: String, val status: String)

/** Nombre de cada nota, para la etiqueta que aparece al abrir y al cambiarla. */
val HeaderGrade.label: String
    get() = when (this) {
        HeaderGrade.WEIGHTED -> "Nota ponderada"
        HeaderGrade.MEAN -> "Nota media"
    }

/** Probabilidad de que la etiqueta, al abrir, recuerde que se puede tocar. */
const val HEADER_TAP_HINT_CHANCE = 0.25

/** Etiqueta de la nota grande; con [tapHint], recuerda además que se puede cambiar. */
fun headerLabel(shown: HeaderGrade, tapHint: Boolean): String =
    if (tapHint) "${shown.label} · toca para cambiar" else shown.label

/** Nota con dos decimales, o con uno si es entera (7,0 en vez de 7,00). */
private fun Float.gradeText(): String = if (this % 1f == 0f) "%.1f".format(this) else "%.2f".format(this)

/**
 * La nota elegida ([shown]) va en grande y la otra en la frase de debajo, para no perder
 * ninguna. La valoración (excelente, buena…) es siempre la de la media, como el brillo.
 */
fun headerTexts(weighted: Float?, mean: Float?, shown: HeaderGrade): HeaderTexts {
    val big = when (shown) {
        HeaderGrade.WEIGHTED -> weighted
        HeaderGrade.MEAN -> mean
    }
    val status = when {
        mean == null -> "¡Suerte con los exámenes!"
        shown == HeaderGrade.WEIGHTED -> when {
            mean >= 9f -> "¡Excelente! Tu nota media es de %.2f".format(mean)
            mean >= 6f -> "Tu nota media es buena: %.2f".format(mean)
            mean >= 4f -> "Tu nota media es pocha: %.2f".format(mean)
            else -> "Tu nota media es baja: %.2f".format(mean)
        }
        else -> {
            val opinion = when {
                mean >= 9f -> "¡Excelente media!"
                mean >= 6f -> "Tu media es buena."
                mean >= 4f -> "Tu media es pocha."
                else -> "Tu media es baja."
            }
            weighted?.let { "$opinion Ponderada: %.2f".format(it) } ?: opinion
        }
    }
    return HeaderTexts(big = big?.gradeText() ?: "Sin nota", status = status)
}
