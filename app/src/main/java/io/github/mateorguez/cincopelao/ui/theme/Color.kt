package io.github.mateorguez.cincopelao.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta base -----------------------------------------------------------------------------------

val AzulFuerte = Color(0xFF2823BC)
val AzulOscuro = Color(0xFF11122F)
/** Botones y controles de Clásico oscuro: el azul de siempre, aclarado para que se lea sobre fondo oscuro. */
val AzulClaroOscuro = Color(0xFF9AA6FF)
val Blanco = Color(0xFFFFFFFF)
val Negro = Color(0xFF000000)
val SurfaceDark = Color(0xFF121212)
val PrimaryContainerDark = Color(0xFF4F378B)
val PrimaryContainerLight = Color(0xFFEADDFF)

// Barra de navegación inferior -------------------------------------------------------------------

val NavBarUnselected = Color(0xFFBEC0FF)

// Tareas: tarjetas y etiqueta de la fecha límite ------------------------------------------------

val TaskCardTintLight = Color(0xFF001794)
val TaskCardLight = Color(0xFFF1F2FB)
val DueOverdueLight = Color(0xFF9F1D17)
val DueOverdueDark = Color(0xFFFF9E96)
val DueSoonLight = Color(0xFF884000)
val DueSoonDark = Color(0xFFFFB74D)

// Asignaturas: degradado de las tarjetas ---------------------------------------------------------

val SubjectCardGradientStart = Color(0xFFE4D4FF)
val SubjectCardGradientEnd = Color(0xFF76EAEE)

// Asignaturas: brillo de la cabecera según la nota media -----------------------------------------

val GlowRegularDark = Color(0xFFC57009)
val GlowRegularLight = Color(0xFFEF6D05)
val GlowFailDark = Color(0xFFFF0000)
val GlowPass = Color(0xFFFFD54F)

// Estado de un evento (etiquetas de nota) --------------------------------------------------------

val StatusDone = Color(0xFF4CAF50)
val StatusBelowMinimum = Color(0xFF6E1800)
val StatusFail = Color(0xFFD32F2F)
val StatusPass = Color(0xFF4CAF50)
val StatusFair = Color(0xFFFFA000)
val StatusGood = Color(0xFF8D7728)
val StatusExcellent = Color(0xFF1976D2)

// Calendario -------------------------------------------------------------------------------------

val CalendarSelectedDayDark = Color(0xFF3A3767)
// Tipos del calendario en Clásico: exámenes en rosa, otros eventos en azul y tareas en verde.
// En claro, tonos oscuros y en oscuro, claros, para que la etiqueta y los puntos se lean
// (los comprueba PalettesTest).
val TimelineExamPointLight = Color(0xFFAD1457)
val TimelineEventPointLight = Color(0xFF1458B0)
val TimelineTaskPointLight = Color(0xFF216A27)
val TimelineExamPointDark = Color(0xFFF48FB1)
val TimelineEventPointDark = Color(0xFF90CAF9)
val TimelineTaskPointDark = Color(0xFFA5D6A7)
/** Casilla de un día del calendario en Clásico claro: lavanda muy suave, del tono de la app. */
val CalendarDayLight = Color(0xFFECEDF8)

// Compartición: consola técnica (siempre oscura, como un terminal) y estados
val TerminalBackground = Color(0xFF0D1117)
val TerminalText = Color(0xFF7EE787)
val TerminalDim = Color(0xFF8B949E)
val TerminalAccent = Color(0xFF79C0FF)
val TerminalError = Color(0xFFFF7B72)
/** Acento de iconos y datos en modo oscuro: AzulFuerte apenas contrasta con el fondo AzulOscuro. */
val ShareAccentDark = Color(0xFF9D99FF)
