package com.datecomp.dca.permissions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Estado de los permisos especiales que la UI necesita mostrar. La guía
 * inicial y la lista de apps comparten esta misma instancia para que el
 * estado "Activo / Inactivo" sea consistente en toda la app.
 */
data class PermissionsUiState(
    val notificationListenerGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
)

/**
 * Slice de permisos: consulta y solicita los accesos del sistema. No conoce
 * la lógica de auto-respuesta; los efectos concretos viven en
 * [SystemPermissions].
 */
class PermissionsViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PermissionsUiState())
    val state: StateFlow<PermissionsUiState> = _state

    init {
        refresh()
    }

    /**
     * Relee el estado real de los permisos. Se llama al arrancar y cada vez
     * que la UI vuelve al primer plano (p. ej. tras volver de Ajustes).
     */
    fun refresh() {
        val context = getApplication<Application>()
        val granted = SystemPermissions.isNotificationListenerEnabled(context)
        _state.value = PermissionsUiState(
            notificationListenerGranted = granted,
            batteryOptimizationIgnored = SystemPermissions.isBatteryOptimizationIgnored(context),
        )
        // Auto-reparación del listener: en algunas ROMs el sistema mata el
        // proceso y no vuelve a bindear el servicio. Pedir el rebind al abrir
        // la app repara el caso sin intervención del usuario.
        if (granted) SystemPermissions.requestListenerRebind(context)
    }

    fun openNotificationAccessSettings() {
        SystemPermissions.openNotificationAccessSettings(getApplication())
    }

    fun requestIgnoreBatteryOptimizations() {
        SystemPermissions.requestIgnoreBatteryOptimizations(getApplication())
    }
}
