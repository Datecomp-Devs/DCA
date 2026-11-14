package com.datecomp.dca.targets

import android.content.Context

/**
 * Enumeración de apps instaladas con launcher (efecto PackageManager).
 *
 * Excluye DCA y las apps sin interfaz. La auto-respuesta solo funciona en
 * apps cuyas notificaciones incluyan respuesta rápida (quick reply), pero la
 * app se puede agregar igualmente: el sistema ignora el envío si no la
 * soporta.
 */
object InstalledApps {

    fun list(context: Context): List<AppTarget> {
        val pm = context.packageManager
        val self = context.packageName
        return pm.getInstalledPackages(0)
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
    }
}
