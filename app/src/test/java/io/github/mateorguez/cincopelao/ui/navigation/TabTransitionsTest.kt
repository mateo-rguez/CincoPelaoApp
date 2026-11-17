package io.github.mateorguez.cincopelao.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class TabTransitionsTest {

    @Test
    fun `una pestaña a la derecha en la barra entra desde la derecha`() {
        assertEquals(1, tabDirection(BottomNavItem.Asignaturas.route, BottomNavItem.Calendario.route))
        assertEquals(1, tabDirection(BottomNavItem.Horarios.route, BottomNavItem.ShareMode.route))
    }

    @Test
    fun `una pestaña a la izquierda entra desde la izquierda`() {
        assertEquals(-1, tabDirection(BottomNavItem.Asignaturas.route, BottomNavItem.Tareas.route))
        assertEquals(-1, tabDirection(BottomNavItem.ShareMode.route, BottomNavItem.Horarios.route))
    }

    @Test
    fun `sin sentido si la ruta es la misma o desconocida`() {
        assertEquals(0, tabDirection(BottomNavItem.Tareas.route, BottomNavItem.Tareas.route))
        assertEquals(0, tabDirection(null, BottomNavItem.Tareas.route))
        assertEquals(0, tabDirection(BottomNavItem.Tareas.route, "otra"))
    }
}
