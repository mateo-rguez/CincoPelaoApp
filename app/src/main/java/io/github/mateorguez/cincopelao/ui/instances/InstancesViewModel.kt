package io.github.mateorguez.cincopelao.ui.instances

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.instances.DatabaseInstance
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InstancesViewModel @Inject constructor(
    private val repository: InstanceRepository
) : ViewModel() {

    val instances: StateFlow<List<DatabaseInstance>> = repository.instances
    val currentInstanceId: StateFlow<String> = repository.currentInstanceId

    fun select(instance: DatabaseInstance) = repository.select(instance.id)

    fun create(name: String) {
        repository.create(name, setCurrent = false)
    }

    fun update(instance: DatabaseInstance, name: String, titulation: String?) =
        repository.update(instance.id, name, titulation)

    fun delete(instance: DatabaseInstance) {
        viewModelScope.launch { repository.delete(instance.id) }
    }
}
