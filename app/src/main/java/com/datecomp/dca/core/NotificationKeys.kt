package com.datecomp.dca.core

/**
 * Construcción de la firma de una notificación para detección de duplicados.
 *
 * La firma se basa en el CONTENIDO del mensaje (remitente + texto), no en la
 * identidad de la notificación del sistema: cuando la app objetivo recibe un
 * mensaje nuevo de la misma conversación, ACTUALIZA su notificación (mismo
 * sbn.key, texto nuevo), por lo que usar sbn.key hacía que mensajes nuevos
 * del mismo remitente fueran descartados erróneamente como duplicados.
 */
object NotificationKeys {

    /**
     * Firma de contenido: "paquete|remitente|texto".
     *
     * - Mensaje nuevo del mismo remitente (texto distinto)→ firma distinta.
     * - Mismo mensaje re-posteado → misma firma (duplicado).
     * - Remitente distinto con el mismo texto → firma distinta.
     *
     * Devuelve "" cuando no hay cuerpo de mensaje: sin cuerpo no hay nada
     * que responder y la dedupe debe quedar desactivada (clave en blanco).
     */
    fun contentKey(
        packageName: String,
        conversationTitle: String,
        messageBody: String,
    ): String {
        val body = messageBody.trim()
        if (body.isBlank()) return ""
        return "$packageName|${conversationTitle.trim()}|$body"
    }
}