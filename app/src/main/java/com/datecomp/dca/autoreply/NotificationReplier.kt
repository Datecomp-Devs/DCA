package com.datecomp.dca.autoreply

import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification

/**
 * Efecto de IO: envía la respuesta automática usando la acción de respuesta
 * rápida (RemoteInput) que expone la propia notificación entrante. Es la
 * misma vía que usa la respuesta "inline" del sistema Android, por lo que
 * funciona con cualquier app que ofrezca quick-reply (WhatsApp, Telegram,
 * Signal, Messenger, etc.).
 */
object NotificationReplier {

    /**
     * Responde a la notificación [sbn] (la misma que disparó la regla) usando
     * su primera acción compatible con RemoteInput.
     *
     * @return true si el PendingIntent se disparó con éxito.
     */
    fun sendReply(context: Context, sbn: StatusBarNotification, message: String): Boolean {
        if (message.isBlank()) return false
        val notification = sbn.notification ?: return false
        val action = notification.actions
            ?.firstOrNull { a -> a.remoteInputs != null && a.remoteInputs.isNotEmpty() }
            ?: return false
        val remoteInput = action.remoteInputs.firstOrNull() ?: return false

        val fillIn = Intent()
        val results = android.os.Bundle().apply {
            putCharSequence(remoteInput.resultKey, message)
        }
        RemoteInput.addResultsToIntent(arrayOf(remoteInput), fillIn, results)

        return runCatching {
            action.actionIntent.send(context, 0, fillIn)
        }.isSuccess
    }
}