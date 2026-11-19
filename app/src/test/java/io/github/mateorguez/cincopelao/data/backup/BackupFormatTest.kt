package io.github.mateorguez.cincopelao.data.backup

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.sharing.protocol.DataToSend
import io.github.mateorguez.cincopelao.ui.backup.backupInfoSummary
import io.github.mateorguez.cincopelao.ui.backup.importedText
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupFormatTest {

    private val teleco = BackupInstance(
        name = "Teleco",
        titulation = "Grado en Teleco",
        data = DataToSend(
            subjects = listOf(Subject(subjectId = 1, name = "Cálculo", abbrev = "CAL")),
            events = listOf(
                Event(eventId = 2, subjectId = 1, name = "Parcial", type = "Examen", date = 10L, isPuntuable = true, weight = 40f, score = 6.5f),
                Event(eventId = 3, subjectId = 1, name = "Prácticas", type = "Ejercicio", date = 20L, noPuntuableDone = true)
            ),
            tasks = listOf(
                Task(taskId = 4, title = "Memoria", subjectId = 1, dueDate = 30L),
                Task(taskId = 5, title = "Índice", parentTaskId = 4, isCompleted = true, completedAt = 25L),
                Task(taskId = 6, title = "Vieja", isCompleted = true, archived = true)
            )
        ),
        schedule = byteArrayOf(1, 2, 3, 4)
    )
    private val vacia = BackupInstance(name = "Vacía", titulation = null, data = DataToSend(), schedule = null)

    private fun write(vararg instances: BackupInstance): ByteArray =
        ByteArrayOutputStream().also { BackupFormat.write(it, instances.toList(), createdAt = 123L, appVersion = "2.0") }.toByteArray()

    private fun read(bytes: ByteArray) = BackupFormat.read(ByteArrayInputStream(bytes))

    private fun zip(vararg entries: Pair<String, String>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
        }
    }.toByteArray()

    private fun assertFails(bytes: ByteArray, expected: String) {
        try {
            read(bytes)
            fail("debería fallar")
        } catch (e: BackupFormatException) {
            assertTrue(e.message, e.message.orEmpty().contains(expected))
        }
    }

    @Test
    fun `una copia se lee igual que se escribio, con notas, hechas y archivadas`() {
        val contents = read(write(teleco, vacia))

        assertEquals(123L, contents.manifest.createdAt)
        assertEquals("2.0", contents.manifest.appVersion)
        assertEquals(listOf("Teleco", "Vacía"), contents.instances.map { it.name })
        val leida = contents.instances[0]
        assertEquals("Grado en Teleco", leida.titulation)
        assertEquals(teleco.data, leida.data)
        assertArrayEquals(teleco.schedule, leida.schedule)
        assertNull(contents.instances[1].schedule)
        assertNull(contents.instances[1].titulation)
    }

    @Test
    fun `el indice resume cada instancia`() {
        val info = read(write(teleco)).manifest.instances.single()

        assertEquals(BackupInstanceInfo("1", "Teleco", "Grado en Teleco", subjects = 1, events = 2, tasks = 3, hasSchedule = true), info)
        assertEquals("Grado en Teleco · 1 asignatura · 2 eventos · 3 tareas · horario", backupInfoSummary(info))
        assertEquals("Vacía", backupInfoSummary(BackupInstanceInfo("2", "x")))
    }

    @Test
    fun `un archivo que no es una copia se rechaza con un mensaje claro`() {
        assertFails("hola".toByteArray(), "No es una copia")
        assertFails(zip("otra.txt" to "x"), "No es una copia")
        assertFails(zip("copia.json" to """{"format":"otra","createdAt":1,"appVersion":"1","instances":[]}"""), "No es una copia")
    }

    @Test
    fun `una copia danada o de una version mas nueva se rechaza`() {
        assertFails(zip("copia.json" to "{roto"), "dañada")
        assertFails(
            zip("copia.json" to """{"format":"cincopelao-copia","version":99,"createdAt":1,"appVersion":"9","instances":[]}"""),
            "más nueva"
        )
        assertFails(
            zip("copia.json" to """{"format":"cincopelao-copia","version":1,"createdAt":1,"appVersion":"2","instances":[{"folder":"1","name":"Teleco"}]}"""),
            "Faltan los datos de «Teleco»"
        )
    }

    @Test
    fun `los campos desconocidos se ignoran`() {
        val bytes = zip(
            "copia.json" to """{"format":"cincopelao-copia","version":1,"createdAt":1,"appVersion":"2","futuro":true,"instances":[{"folder":"1","name":"A","nuevo":3}]}""",
            "1/datos.json" to """{"subjects":[],"events":[],"tasks":[],"extra":[]}"""
        )
        assertEquals("A", read(bytes).instances.single().name)
    }

    @Test
    fun `los nombres repetidos se numeran`() {
        assertEquals("Teleco", importedName("Teleco", listOf("Otra")))
        assertEquals("Teleco (2)", importedName("Teleco", listOf("teleco ")))
        assertEquals("Teleco (3)", importedName("Teleco", listOf("Teleco", "Teleco (2)")))
        assertEquals("Instancia importada", importedName("  ", emptyList()))
    }

    @Test
    fun `el aviso de importacion nombra las instancias`() {
        assertEquals("Se ha importado «A». Las tienes en Instancias.", importedText(listOf("A")))
        assertEquals("Se han importado «A», «B» y «C». Las tienes en Instancias.", importedText(listOf("A", "B", "C")))
    }
}
