package com.example.cincopelao

import com.example.cincopelao.Subject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cincopelao.StatsUtils.calculateStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SubjectViewModel @Inject constructor(
    private val repository: RepositoryDB
) : ViewModel() {

    private val _subjectId = MutableStateFlow<Int?>(null)
    val subjectId: StateFlow<Int?> = _subjectId

    val subject: StateFlow<Subject?> = _subjectId
        .filterNotNull()
        .flatMapLatest { id ->
            repository.getSubjectById(id)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    fun setSubjectId(id: Int) {
        _subjectId.value = id
    }

    val events: StateFlow<List<Event>> = _subjectId
        .filterNotNull()
        .flatMapLatest { id ->
            repository.getEventsForSubject(id)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun updateEvent(updated: Event) {
        viewModelScope.launch {
            repository.updateEvent(updated)
        }
    }

    fun calculateWeightedGrade(events: List<Event>): Float? {
        val scored = events.filter {
            it.score != null && it.weight != null && it.score!! >= (it.minScore ?: 0f)/10
        }
        if (scored.isEmpty()) return null

        val weightedSum = scored.sumOf { (it.score!! * it.weight!!).toDouble() }
        return (weightedSum / 100).toFloat()
    }

    fun calculateMeanGrade(events: List<Event>): Float? {
        val scores = events.filter { it.isPuntuable }.mapNotNull { it.score }
        if (scores.isEmpty()) return null
        return scores.average().toFloat()
    }

    // Estadísticas ---------------------

    private val _stats = MutableStateFlow<Stats>(value = Stats(0f, 0f, 0f, 0f, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    fun loadStats(events: List<Event>) {
        viewModelScope.launch(Dispatchers.Default) {
            val calculated = calculateStats(events)
            _stats.value = calculated
        }
    }
}
