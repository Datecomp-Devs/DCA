package com.datecomp.dca.ui

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.datecomp.dca.core.AppTarget
import com.datecomp.dca.data.SettingsRepository
import com.datecomp.dca.service.AutoReplyNotificationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado exclusivo de la pantalla de guía de permisos.
 */
data class GuideUiState(
    val notificationListenerGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
)

/**
 * Estado de la lista de apps y controles de DCA.
 */
data class AppsUiState(
    val targets: List<AppTarget> = emptyList(),
    val listenerEnabled: Boolean = false,
    val installedApps: List<AppTarget> = emptyList(),
    val query: String = "",
) {
    val filteredInstalled: List<AppTarget>
        get() = if (query.isBlank()) installedApps
        else installedApps.filter { it.displayName.contains(query, ignoreCase = true) }
}

/**
 * ViewModel único de la app. Coordina repositorio, permisos y UI.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    private val _guideState = MutableStateFlow(GuideUiState())
    val guideState: StateFlow<GuideUiState> = _guideState

    private val _appsState = MutableStateFlow(AppsUiState())
    val appsState: StateFlow<AppsUiState> = _appsState

    val onboardingDone: StateFlow<Boolean> = repository.onboardingDoneFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        // Migración de esquema v1→v2 (cooldown→repeat) antes de observar.
        viewModelScope.launch { repository.migrateV1ToV2() }
        viewModelScope.launch {
            repository.targetsFlow.collect { targets ->
                _appsState.value = _appsState.value.copy(targets = targets)
            }
        }
        refreshPermissionState()
        loadInstalledMessagingApps()
    }

    /** Refresca el estado de los permisos de sistema. */
    fun refreshPermissionState() {
        val context = getApplication<Application>()
        val granted = AutoReplyNotificationListener.isListenerEnabled(context)
        val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE)
            as android.os.PowerManager
        val batteryIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        _guideState.value = _guideState.value.copy(
            notificationListenerGranted = granted,
            batteryOptimizationIgnored = batteryIgnored,
        )
        _appsState.value = _appsState.value.copy(listenerEnabled = granted)
    }

    /** Actualiza el filtro de búsqueda de apps instaladas. */
    fun setQuery(query: String) {
        _appsState.value = _appsState.value.copy(query = query)
    }

    /** Abre el panel de sistema de Acceso a notificaciones. */
    fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    /**
     * Pide al sistema eximir a DCA de la optimización de batería (Doze).
     * Muestra el diálogo nativo del sistema; el usuario acepta o rechaza.
     */
    fun requestIgnoreBatteryOptimizations() {
        val context = getApplication<Application>()
        val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    /**
     * Carga TODAS las apps instaladas con launcher (excluye DCA y apps sin
     * interfaz). Se ejecuta en background porque enumerar paquetes con
     * etiquetas puede tardar con cientos de apps.
     *
     * Nota: la auto-respuesta solo funciona en apps cuyas notificaciones
     * incluyan respuesta rápida (quick reply); las demás se agregan pero el
     * sistema ignorará el intento de envío.
     */
    private fun loadInstalledMessagingApps() {
        viewModelScope.launch(Dispatchers.Default) {
            val context = getApplication<Application>()
            val pm = context.packageManager
            val self = context.packageName
            val apps = pm.getInstalledPackages(0)
                .asSequence()
                .filter { it.packageName != self }
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map {
                    AppTarget(
                        packageName = it.packageName,
                        displayName = it.applicationInfo?.loadLabel(pm)?.toString() ?: it.packageName,
                    )
                }
                .sortedBy { it.displayName.lowercase() }
                .toList()
            _appsState.value = _appsState.value.copy(installedApps = apps)
        }
    }

    /** Agrega o activa una app a la lista de DCA. */
    fun addTarget(packageName: String, displayName: String) {
        viewModelScope.launch {
            val existing = _appsState.value.targets.firstOrNull { it.packageName == packageName }
            if (existing == null) {
                repository.upsert(AppTarget(packageName = packageName, displayName = displayName, enabled = true))
            } else {
                repository.setEnabled(packageName, true)
            }
        }
    }

    /** Habilita / deshabilita sin borrar la configuración. */
    fun toggleTarget(packageName: String, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(packageName, enabled) }
    }

    /** Cambia el mensaje de auto-respuesta de una app. */
    fun updateReplyMessage(packageName: String, message: String) {
        viewModelScope.launch { repository.setReplyMessage(packageName, message) }
    }

    /** Configura el cooldown (demora) en segundos (0..3600). */
    fun updateCooldown(packageName: String, seconds: Long) {
        viewModelScope.launch { repository.setCooldown(packageName, seconds) }
    }

    /** Configura el intervalo de repetición en segundos (0..604800). */
    fun updateRepeat(packageName: String, seconds: Long) {
        viewModelScope.launch { repository.setRepeat(packageName, seconds) }
    }

    /** Activa o desactiva el chat bot automático de una app. */
    fun updateChatBotEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch { repository.setChatBotEnabled(packageName, enabled) }
    }

    /** Elimina la app de la lista DCA. */
    fun removeTarget(packageName: String) {
        viewModelScope.launch { repository.delete(packageName) }
    }

    /** Marca la guía inicial como completada. */
    fun finishOnboarding() {
        viewModelScope.launch { repository.setOnboardingDone(true) }
    }
}