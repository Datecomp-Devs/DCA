package com.datecomp.dca

import android.app.Application

/**
 * Punto de entrada de la aplicación DCA.
 *
 * Mantenido minimalista a propósito: el estado vive en el repositorio
 * (objetDataStore) y el servicio de notificación se reconecta por el
 * sistema cuando el usurio concede el permiso.
 */
class DcaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: DcaApp
            private set
    }
}