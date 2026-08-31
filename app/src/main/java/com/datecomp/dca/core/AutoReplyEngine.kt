package com.datecomp.dca.core

import kotlinx.coroutines.delay

/**
 * Motor de decisión de auto-respuesta. **Sin dependencias de Android**
 * (solo coroutines) para que sea 100% testeable en JVM.
 *
 * Semántica de tiempos:
 * - [AppTarget.cooldownSeconds]: espera desde que llega la notificación
 *   hasta enviar la respuesta (0 = inmediata).
 * - [AppTarget.repeatSeconds]: intervalo mínimo entre respuestas automáticas
 *   a la misma app (0 = responder cada notificación).
 */
object AutoReplyEngine {

    /** Espera la demora de cooldown configurada (efecto cancelable). */
    suspend fun awaitCooldown(cooldownSeconds: Long) {
        if (cooldownSeconds > 0L) delay(cooldownSeconds * 1000L)
    }

    /**
     * Representa la notificación entrante tal como la entrega el sistema.
     */
    data class IncomingNotification(
        val packageName: String,
        val isGroupConversation: Boolean = false,
    )

    enum class Decision {
        REPLY,
        SKIP_APP_NOT_ENABLED,
        SKIP_TARGET_INVALID,
        SKIP_REPEAT_NOT_DUE,
        SKIP_DUPLICATE_DETECTION,
    }

    /**
     * Decide si la notificación debe disparar una respuesta.
     *
     * Reglas (en orden):
     * 1. La app debe estar habilitada.
     * 2. El target debe pasar la validación de contrato.
     * 3. Debe haber transcurrido el intervalo mínimo de repetición.
     * 4. La notificación no debe ser un duplicado re-posteado.
     *
     * El cooldown (demora) NO decide aquí: se aplica como espera
     * ([awaitCooldown]) tras una decisión REPLY, antes de enviar.
     */
    fun shouldReply(
        target: AppTarget?,
        notification: IncomingNotification,
        nowMillis: Long,
        lastSeenNotificationKey: String?,
        notificationKey: String,
    ): Decision {
        // 1. App no registrada o deshabilitada: ignorar silenciosamente.
        if (target == null || !target.enabled) return Decision.SKIP_APP_NOT_ENABLED

        // 2. Contrato inválido: no responder con datos corruptos.
        if (!target.isValid()) return Decision.SKIP_TARGET_INVALID

        // 3. Intervalo mínimo entre respuestas a esta app. Es el mayor entre
        //    el "Repetir" elegido por el usuario y un GAP interno no
        //    configurable que absorbe re-posts casi simultáneos del mismo
        //    mensaje (las apps re-publican la notificación con variaciones
        //    mínimas de texto en milisegundos). Se usa lastReplyTimestampMillis
        //    (persistido) para que la protección sobreviva reinicios y
        //    reconexiones del listener. 0 = nunca respondido.
        val repeatMillis = target.repeatSeconds * 1000L
        val minGapMillis = maxOf(repeatMillis, MIN_REPLY_GAP_MILLIS)
        val hasRepliedBefore = target.lastReplyTimestampMillis > 0L
        if (hasRepliedBefore && nowMillis - target.lastReplyTimestampMillis < minGapMillis) {
            return Decision.SKIP_REPEAT_NOT_DUE
        }

        // 4. Duplicado: la misma firma de contenido ya fue respondida.
        if (notificationKey.isNotBlank() && notificationKey == lastSeenNotificationKey) {
            return Decision.SKIP_DUPLICATE_DETECTION
        }

        return Decision.REPLY
    }

    /**
     * GAP interno anti-duplicados: ventana mínima entre dos respuestas a la
     * misma app aunque "Repetir" sea 0 (Siempre). Absorbe el doble despacho
     * del sistema. No es configurable a propósito.
     */
    const val MIN_REPLY_GAP_MILLIS: Long = 2_000L

    /**
     * Devuelve una copia del target con la marca de respuesta actualizada.
     * El repositorio debe persistir el resultado de esta función.
     */
    fun markReplied(target: AppTarget, nowMillis: Long): AppTarget =
        target.copy(lastReplyTimestampMillis = nowMillis)
}