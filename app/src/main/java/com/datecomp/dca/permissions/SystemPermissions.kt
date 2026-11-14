package com.datecomp.dca.permissions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService

/**
 * Efectos de IO sobre los permisos especiales del sistema. No guarda estado:
 * el estado vive en [PermissionsViewModel] y se consulta aquí cada vez que la
 * UI vuelve al primer plano.
 *
 * Toda lectura o escritura de [Settings.Secure] se limita al propio paquete
 * de DCA.
 */
object SystemPermissions {

    /** Consulta el estado real del permiso NotificationListener. */
    fun isNotificationListenerEnabled(context: Context): Boolean =
        enabledListenerComponents(context).any { it.packageName == context.packageName }

    /** Consulta la exención de optimización de batería (Doze). */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Pide al sistema re-bindear los listeners de DCA. Algunas ROMs matan el
     * proceso sin volver a bindear el servicio; esta llamada es barata y
     * segura (se ignora si ya está bindeado).
     */
    fun requestListenerRebind(context: Context) {
        enabledListenerComponents(context)
            .filter { it.packageName == context.packageName }
            .forEach { component ->
                runCatching { NotificationListenerService.requestRebind(component) }
            }
    }

    /** Abre el panel de sistema de Acceso a notificaciones. */
    fun openNotificationAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Muestra el diálogo nativo para eximir a DCA de la optimización de
     * batería. Si el sistema no soporta el intent, no hace nada.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    private fun enabledListenerComponents(context: Context): List<ComponentName> {
        val raw = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return emptyList()
        return raw.split(":")
            .mapNotNull { ComponentName.unflattenFromString(it) }
    }
}
