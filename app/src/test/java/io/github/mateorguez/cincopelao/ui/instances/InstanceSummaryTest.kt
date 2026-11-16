package io.github.mateorguez.cincopelao.ui.instances

import org.junit.Assert.assertEquals
import org.junit.Test

class InstanceSummaryTest {

    @Test
    fun `junta todo lo que hay con puntos`() {
        assertEquals("Teleco · 6 asignaturas · 12 tareas · horario", instanceSummaryText("Teleco", 6, 12, true))
    }

    @Test
    fun `usa el singular con uno`() {
        assertEquals("1 asignatura · 1 tarea", instanceSummaryText(null, 1, 1, false))
    }

    @Test
    fun `omite lo vacio y la titulacion en blanco`() {
        assertEquals("3 asignaturas · horario", instanceSummaryText("  ", 3, 0, true))
    }

    @Test
    fun `una instancia sin nada se marca como vacia`() {
        assertEquals("Vacía", instanceSummaryText("", 0, 0, false))
    }
}
