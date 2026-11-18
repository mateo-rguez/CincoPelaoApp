package io.github.mateorguez.cincopelao.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Qué nota sale en grande en la cabecera de una asignatura; la otra va debajo. */
enum class HeaderGrade {
    WEIGHTED, MEAN;

    val other: HeaderGrade get() = if (this == WEIGHTED) MEAN else WEIGHTED
}

private val HEADER_GRADE_KEY = stringPreferencesKey("subject_header_grade")

/** Cómo se muestra el detalle de las asignaturas. Común a todas las asignaturas e instancias. */
@Singleton
class SubjectDisplayRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val headerGrade: Flow<HeaderGrade> = context.dataStore.data.map { prefs ->
        HeaderGrade.entries.firstOrNull { it.name == prefs[HEADER_GRADE_KEY] } ?: HeaderGrade.WEIGHTED
    }

    suspend fun setHeaderGrade(grade: HeaderGrade) {
        context.dataStore.edit { it[HEADER_GRADE_KEY] = grade.name }
    }
}
