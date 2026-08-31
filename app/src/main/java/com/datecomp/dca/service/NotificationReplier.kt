package com.datecomp.dca.service

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
 *
 * Aislado como objeto puro de envío para que el listener sea solo
 * orquestador y esto sea fácil de auditar y mantener.
 */
object NotificationReplier {

    private const val FALLBACK_KEY = "dca_fallback_reply_key"

    /**
     * Responde a la notificación [sbn] (la misma que disparó la regla) usando
     * su primera acción compatible con RemoteInput.
     *
     * @return true si el PendingIntent se disparó con éxito.
     */
    fun sendReply(context: Context, sbn: StatusBarNotification, message: String): Boolean {
        if (message.isBlank()) return false
        val notifications = sbn.notification ?: return false
        val action = notifications.actions
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