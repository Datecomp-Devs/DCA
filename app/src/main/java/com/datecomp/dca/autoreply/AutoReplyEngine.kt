package com.datecomp.dca.autoreply

import com.datecomp.dca.targets.AppTarget
import kotlinx.coroutines.delay

/**
 * Motor de decisión de auto-respuesta. **Sin dependencias de Android**
 * (solo coroutines) para que sea 100% testeable en JVM.
 *
 * Semántica de tiempos:
 * - [AppTarget.cooldownSeconds]: espera desde que llega la notificación
 *   hasta enviar la respuesta (0 = inmediata).
 * - [AppTarget.repeatSeconds]: intervalo mínimo entre respuestas automáticas
 *   AL MISMO REMITENTE (0 = responder siempre). Individual: la ventana de un
 *   contacto no afecta a los demás.
 */
object AutoReplyEngine {

    /** Espera la demora de cooldown configurada (efecto cancelable). */
    suspend fun awaitCooldown(cooldownSeconds: Long) {
        if (cooldownSeconds > 0L) delay(cooldownSeconds * 1000L)
    }

    /**
     * Representa la notificación entrante tal como la entrega el sistema.
     *
     * @property packageName app de mensajería origen.
     * @property sender remitente/título de conversación (contacto o número).
     *   Base de la ventana "Repetir" individual.
     */
    data class IncomingNotification(
        val packageName: String,
        val sender: String = "",
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
     * 3a. GAP técnico por app: absorbe el doble despacho del sistema
     *     (re-posts casi simultáneos de la misma notificación).
     * 3b. Ventana "Repetir" INDIVIDUAL por remitente: si a Pepe se le
     *     respondió hace menos de [AppTarget.repeatSeconds], solo Pepe
     *     queda en espera; Lele tiene su propia ventana.
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

        // 3a. GAP técnico por app (persistido, no configurable): frena
        //     re-posts del sistema a <2s de la última respuesta, sea quien
        //     sea el remitente. No es la ventana del usuario.
        val hasRepliedBefore = target.lastReplyTimestampMillis > 0L
        if (hasRepliedBefore &&
            nowMillis - target.lastReplyTimestampMillis < MIN_REPLY_GAP_MILLIS
        ) {
            return Decision.SKIP_REPEAT_NOT_DUE
        }

        // 3b. "Repetir" individual POR REMITENTE (contacto o número).
        //     Remitentes distintos tienen ventanas independientes.
        val repeatMillis = target.repeatSeconds * 1000L
        val sender = notification.sender.trim()
        if (repeatMillis > 0L && sender.isNotBlank()) {
            val senderLast = target.lastReplyBySender[sender] ?: 0L
            if (senderLast > 0L && nowMillis - senderLast < repeatMillis) {
                return Decision.SKIP_REPEAT_NOT_DUE
            }
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
     * del sistema. No es configurable a propósito. NO es la ventana
     * "Repetir" del usuario: esa es individual por remitente.
     */
    const val MIN_REPLY_GAP_MILLIS: Long = 2_000L
}