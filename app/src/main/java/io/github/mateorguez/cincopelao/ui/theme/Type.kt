package io.github.mateorguez.cincopelao.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.mateorguez.cincopelao.R

/** Fuente Delight Medium (títulos de barras, encabezados). */
val DelightMedium = FontFamily(Font(R.font.delight_medium))

/** Fuente Delight Regular (texto destacado de tarjetas y listas). */
val DelightRegular = FontFamily(Font(R.font.delight_regular))

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
