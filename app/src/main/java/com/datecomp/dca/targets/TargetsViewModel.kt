package com.datecomp.dca.targets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Estado de la lista de apps objetivo y del buscador de apps instaladas.
 */
data class TargetsUiState(
    val targets: List<AppTarget> = emptyList(),
    val installedApps: List<AppTarget> = emptyList(),
    val query: String = "",
) {
    val filteredInstalled: List<AppTarget>
        get() = if (query.isBlank()) installedApps
        else installedApps.filter { it.displayName.contains(query, ignoreCase = true) }
}

/**
 * Slice de apps objetivo: coordina persistencia, apps instaladas y acciones
 * del usuario sobre la lista y el editor.
 */
class TargetsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = TargetsStore(application)

    private val _state = MutableStateFlow(TargetsUiState())
    val state: StateFlow<TargetsUiState> = _state

    init {
        // Migración de esquema v1→v2 (cooldown→repeat) antes de observar.
        viewModelScope.launch { store.migrateV1ToV2() }
        viewModelScope.launch {
            store.targetsFlow.collect { targets ->
                _state.value = _state.value.copy(targets = targets)
            }
        }
        loadInstalledApps()
    }

    /** Actualiza el filtro de búsqueda de apps instaladas. */
    fun setQuery(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    /**
     * Carga TODAS las apps instaladas con launcher (excluye DCA y apps sin
     * interfaz). Se ejecuta en background porque enumerar paquetes con
     * etiquetas puede tardar con cientos de apps.
     */
    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.Default) {
            val apps = InstalledApps.list(getApplication())
            _state.value = _state.value.copy(installedApps = apps)
        }
    }

    /** Agrega o activa una app a la lista de DCA. */
    fun addTarget(packageName: String, displayName: String) {
        viewModelScope.launch {
            val existing = _state.value.targets.firstOrNull { it.packageName == packageName }
            if (existing == null) {
                store.upsert(
                    AppTarget(
                        packageName = packageName,
                        displayName = displayName,
                        enabled = true,
                    ),
                )
            } else {
                store.setEnabled(packageName, true)
            }
        }
    }

    /** Habilita / deshabilita sin borrar la configuración. */
    fun toggleTarget(packageName: String, enabled: Boolean) {
        viewModelScope.launch { store.setEnabled(packageName, enabled) }
    }

    /** Cambia el mensaje de auto-respuesta de una app. */
    fun updateReplyMessage(packageName: String, message: String) {
        viewModelScope.launch { store.setReplyMessage(packageName, message) }
    }

    /** Configura el cooldown (demora) en segundos (0..3600). */
    fun updateCooldown(packageName: String, seconds: Long) {
        viewModelScope.launch { store.setCooldown(packageName, seconds) }
    }

    /** Configura el intervalo de repetición en segundos (0..604800). */
    fun updateRepeat(packageName: String, seconds: Long) {
        viewModelScope.launch { store.setRepeat(packageName, seconds) }
    }

    /** Activa o desactiva el chat bot automático de una app. */
    fun updateChatBotEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch { store.setChatBotEnabled(packageName, enabled) }
    }

    /** Elimina la app de la lista DCA. */
    fun removeTarget(packageName: String) {
        viewModelScope.launch { store.delete(packageName) }
    }
}
