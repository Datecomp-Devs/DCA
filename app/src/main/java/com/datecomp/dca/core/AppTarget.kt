package com.datecomp.dca.core

import kotlinx.serialization.Serializable

/**
 * Modelo de dominio: una aplicación de mensajería objetivo del usuario.
 *
 * @property packageName Identificador único del paquete (ej. "com.whatsapp").
 * @property displayName Nombre visible en la UI.
 * @property enabled Si está activada para auto-responder.
 * @property cooldownSeconds Segundos de espera desde que llega la
 *   notificación antes de enviar la respuesta automática (0 = inmediata).
 * @property repeatSeconds Tiempo mínimo en segundos entre respuestas
 *   automáticas AL MISMO REMITENTE de esta app (0 = responder siempre).
 *   Ej.: 86400 = un remitente recibe una respuesta por día; otro remitente
 *   de la misma app tiene su propia ventana independiente.
 * @property replyMessage Mensaje de auto-respuesta personalizado.
 * @property lastReplyTimestampMillis Última respuesta a ESTA APP (cualquier
 *   remitente). Uso exclusivo del gap técnico anti-doble-envío.
 * @property lastReplyBySender Última respuesta POR REMITENTE
 *   (clave = remitente/título de conversación, valor = epoch millis).
 *   Base de la ventana "Repetir" individual por contacto/numero.
 */
@Serializable
data class AppTarget(
    val packageName: String,
    val displayName: String,
    val enabled: Boolean = false,
    val cooldownSeconds: Long = 0L,
    val repeatSeconds: Long = 0L,
    val replyMessage: String = DEFAULT_REPLY,
    val lastReplyTimestampMillis: Long = 0L,
    val lastReplyBySender: Map<String, Long> = emptyMap(),
) {
    /** Validación de contrato: paquete no vacío, tiempos >= 0, mensaje 1..500. */
    fun isValid(): Boolean =
        packageName.isNotBlank() &&
            displayName.isNotBlank() &&
            cooldownSeconds >= 0L &&
            repeatSeconds >= 0L &&
            replyMessage.length in 1..MAX_REPLY_LENGTH

    companion object {
        const val DEFAULT_REPLY: String = "Hola! Ahora no puedo contestar. Te escribo en un rato."
        const val MAX_REPLY_LENGTH: Int = 500

        /** Demora máxima antes de responder: 1 hora. */
        const val MAX_COOLDOWN_SECONDS: Long = 3_600L
        const val MIN_COOLDOWN_SECONDS: Long = 0L

        /** Repetición máxima: 7 días. */
        const val MAX_REPEAT_SECONDS: Long = 604_800L
        const val MIN_REPEAT_SECONDS: Long = 0L
    }
}