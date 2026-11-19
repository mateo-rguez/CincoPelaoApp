package io.github.mateorguez.cincopelao.notifications

/**
 * Frases de los recordatorios. Para cambiar o añadir una, basta con tocar estas listas: cada
 * recordatorio elige una de forma estable (siempre la misma para el mismo aviso).
 *
 * Huecos que se rellenan:
 * - `{nombre}`: nombre del evento o título de la tarea.
 * - `{asig}`: abreviatura de la asignatura.
 * - `{el_tipo}`: tipo del evento con su artículo («el examen», «la presentación»).
 * - `{dias}`: días que faltan.
 *
 * Una frase cuyo hueco no se puede rellenar (una tarea sin asignatura, un tipo de evento sin
 * artículo conocido) no se usa para ese aviso; por eso cada lista necesita frases sin
 * `{asig}` ni `{el_tipo}`.
 */
internal object ReminderPhrases {

    /** Títulos de los avisos de eventos. */
    val eventTitles = listOf(
        "Recuerda: {nombre} de {asig}",
        "Ojo, que viene {el_tipo} de {asig}",
        "{nombre} ({asig}) a la vista 👀",
        "Que no se te olvide {el_tipo} de {asig}",
        "Vete pensando en {nombre}",
        "{asig}: {nombre} se acerca",
        "Se acerca {el_tipo} de {asig}",
        "Que no se te pase: {nombre}"
    )

    /** Títulos de los avisos de tareas. */
    val taskTitles = listOf(
        "Pendiente: {nombre}",
        "No te olvides de {nombre}",
        "Queda poco para {nombre}",
        "Tarea de {asig}: {nombre}",
        "{asig} te reclama: {nombre}",
        "Vete pensando en {nombre}",
        "Esto no se hace solo: {nombre}"
    )

    /** Texto del aviso el día antes. */
    val messagesTomorrow = listOf(
        "Es mañana, ¿todo listo? 💪",
        "Mañana es el día. Tú puedes 💪",
        "Último día, a darlo todo 🔥",
        "Mañana toca. Repaso rápido y a dormir pronto 😴",
        "24 horas y contando ⏳"
    )

    /** Texto del aviso cuando faltan 2 o 3 días. */
    val messagesSoon = listOf(
        "Quedan {dias} unidades de día, tocó cookear 🐐",
        "Faltan {dias} días: momento de ponerse serio 📚",
        "{dias} días. Ya no vale el «mañana empiezo» 😬",
        "Quedan {dias} días, a meterle caña 🚀"
    )

    /** Texto del aviso cuando faltan de 4 a 6 días. */
    val messagesThisWeek = listOf(
        "Faltan {dias} días, habrá que ir espabilando 🙄",
        "Quedan {dias} días: buen momento para empezar 🙂",
        "{dias} días por delante. Un poco cada día y listo ✅",
        "Faltan {dias} días. Tu yo del futuro te lo agradecerá 🙏"
    )

    /** Texto del aviso cuando falta una semana o más. */
    val messagesLater = listOf(
        "Aún quedan {dias} días, hay tiempo 🥱",
        "Quedan {dias} días, sin agobios ☕",
        "Faltan {dias} días. Apúntatelo y sigue con lo tuyo 📝",
        "{dias} días de margen, pero no te confíes 😏"
    )

    /** Texto del botón que marca la tarea como hecha desde su aviso. */
    val doneActions = listOf(
        "Está hecha ✅",
        "Ya la hice",
        "Completada",
        "Hecha y bien hecha",
        "Tachada ✍️",
        "Lista 💪"
    )

    /** Artículo de cada tipo de evento (los de `EventSheet`). */
    private val articleByType = mapOf(
        "examen" to "el",
        "proyecto" to "el",
        "entregable" to "el",
        "trabajo" to "el",
        "presentación" to "la",
        "ejercicio" to "el"
    )

    /** «el examen», «la presentación»; `null` si el tipo no es uno de los conocidos. */
    fun typeWithArticle(type: String): String? {
        val lower = type.trim().lowercase()
        return articleByType[lower]?.let { "$it $lower" }
    }

    /** Frases de mensaje que tocan según los días que faltan. */
    fun messagesFor(offsetDays: Int): List<String> = when {
        offsetDays <= 1 -> messagesTomorrow
        offsetDays <= 3 -> messagesSoon
        offsetDays < 7 -> messagesThisWeek
        else -> messagesLater
    }

    private val placeholder = Regex("""\{(\w+)\}""")

    /** Nombres de los huecos que usa una frase. */
    fun placeholdersOf(template: String): Set<String> =
        placeholder.findAll(template).map { it.groupValues[1] }.toSet()

    /**
     * Rellena los huecos de una sola pasada, así un valor que contenga un nombre de hueco
     * (una tarea llamada «{asig}», un evento «Prototipo») no se vuelve a sustituir.
     */
    fun fill(template: String, values: Map<String, String>): String =
        placeholder.replace(template) { values[it.groupValues[1]] ?: it.value }

    /**
     * Elige de forma determinista una frase que se pueda rellenar con [values]: el mismo
     * [key] da siempre la misma frase mientras la lista no cambie.
     */
    fun pick(templates: List<String>, values: Map<String, String>, key: String): String {
        val usable = templates.filter { values.keys.containsAll(placeholdersOf(it)) }
        val template = usable[Math.floorMod(key.hashCode(), usable.size)]
        return fill(template, values)
    }

    /** Pone en minúscula la primera letra salvo que sea una sigla («TFG», «SO»). */
    fun lowerFirst(text: String): String {
        val trimmed = text.trim()
        if (trimmed.length >= 2 && trimmed[0].isUpperCase() && trimmed[1].isUpperCase()) return trimmed
        return trimmed.replaceFirstChar { it.lowercaseChar() }
    }
}
