package io.github.mateorguez.cincopelao.ui.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Instancia activa, para que las pestañas empiecen de cero (scroll incluido) al cambiarla. */
@HiltViewModel
class ActiveInstanceViewModel @Inject constructor(instances: InstanceRepository) : ViewModel() {
    val instanceId: StateFlow<String> = instances.currentInstanceId
}
