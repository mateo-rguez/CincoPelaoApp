package io.github.mateorguez.cincopelao.ui.subjects

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import org.junit.Assert.assertEquals
import org.junit.Test

class SubjectFormValidationTest {

    private val subject = Subject(name = "Cálculo", course = "1º", abbrev = "CAL")
    private fun event(weight: Float?) = Event(subjectId = 0, name = "e", type = "Examen", isPuntuable = weight != null, weight = weight, date = 1L)

    @Test
    fun `faltan datos o no hay eventos`() {
        assertEquals(SubjectFormValidation.Incomplete, validateSubjectForm(subject.copy(abbrev = ""), listOf(event(100f))))
        assertEquals(SubjectFormValidation.Incomplete, validateSubjectForm(subject, emptyList()))
    }

    @Test
    fun `los pesos deben sumar casi 100`() {
        assertEquals(SubjectFormValidation.WeightsNotHundred, validateSubjectForm(subject, listOf(event(60f), event(30f))))
        assertEquals(SubjectFormValidation.Valid, validateSubjectForm(subject, listOf(event(60f), event(40f), event(null))))
        assertEquals(SubjectFormValidation.Valid, validateSubjectForm(subject, listOf(event(33f), event(33f), event(33f))))
    }
}
