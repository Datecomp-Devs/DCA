package com.datecomp.dca.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.datecomp.dca.R

/**
 * Receptor del broadcast BOOT_COMPLETED: tras reiniciar el dispositivo,
 * Android reconecta automáticamente los NotificationListenerService, así
 * que aquí solo confirmamos al usuario que DCA quedó activo.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Toast.makeText(
                context,
                context.getString(R.string.boot_receiver_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}