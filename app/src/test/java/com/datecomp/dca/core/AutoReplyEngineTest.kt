package com.datecomp.dca.core

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del motor de decisión de auto-respuesta. Lógica pura en JVM:
 * cubre habilitación, repetición, duplicados, contrato inválido y la
 * espera de cooldown (demora).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutoReplyEngineTest {

    private fun target(
        enabled: Boolean = true,
        cooldownSeconds: Long = 0L,
        repeatSeconds: Long = 15L,
        lastReply: Long = 0L,
        message: String = "Fuera de línea",
    ) = AppTarget(
        packageName = "com.test.app",
        displayName = "Test App",
        enabled = enabled,
        cooldownSeconds = cooldownSeconds,
        repeatSeconds = repeatSeconds,
        replyMessage = message,
        lastReplyTimestampMillis = lastReply,
    )

    private val notification = AutoReplyEngine.IncomingNotification(packageName = "com.test.app")

    private val pepe = AutoReplyEngine.IncomingNotification(packageName = "com.test.app", sender = "Pepe")
    private val lele = AutoReplyEngine.IncomingNotification(packageName = "com.test.app", sender = "Lele")

    @Test
    fun `responde cuando app habilitada y sin historial`() {
        val decision = AutoReplyEngine.shouldReply(
            target = target(),
            notification = notification,
            nowMillis = 1_000L,
            lastSeenNotificationKey = null,
            notificationKey = "k1",
        )
        assertEquals(AutoReplyEngine.Decision.REPLY, decision)
    }

    @Test
    fun `salta cuando app no existe`() {
        val decision = AutoReplyEngine.shouldReply(
            target = null,
            notification = notification,
            nowMillis = 1_000L,
            lastSeenNotificationKey = null,
            notificationKey = "k1",
        )
        assertEquals(AutoReplyEngine.Decision.SKIP_APP_NOT_ENABLED, decision)
    }

    @Test
    fun `salta cuando app deshabilitada`() {
        val decision = AutoReplyEngine.shouldReply(
            target = target(enabled = false),
            notification = notification,
            nowMillis = 1_000L,
            lastSeenNotificationKey = null,
            notificationKey = "k1",
        )
        assertEquals(AutoReplyEngine.Decision.SKIP_APP_NOT_ENABLED, decision)
    }

    @Test
    fun `salta cuando repeticion del remitente aun no vence`() {
        // Ventana individual: el remitente fue respondido hace 5s con
        // Repetir=15s → bloqueado para ESE remitente.
        val target = target(repeatSeconds = 15L).let {
            AutoReplyEngine.markReplied(it, "Test App", nowMillis = 10_000L)
        }
        val decision = AutoReplyEngine.shouldReply(
            target = target,
            notification = AutoReplyEngine.IncomingNotification("com.test.app", sender = "Test App"),
            nowMillis = 15_000L, // 5s después con repetición de 15s
            lastSeenNotificationKey = null,
            notificationKey = "k2",
        )
        assertEquals(AutoReplyEngine.Decision.SKIP_REPEAT_NOT_DUE, decision)
    }

    @Test
    fun `responde justo al vencer la repeticion`() {
        val lastReply = 10_000L
        val now = lastReply + 15_000L // exactamente el intervalo
        val decision = AutoReplyEngine.shouldReply(
            target = target(lastReply = lastReply),
            notification = notification,
            nowMillis = now,
            lastSeenNotificationKey = null,
            notificationKey = "k2",
        )
        assertEquals(AutoReplyEngine.Decision.REPLY, decision)
    }

    @Test
    fun `repeticion cero responde tras el gap interno`() {
        // Gap interno MIN_REPLY_GAP_MILLIS = 2s: 3s después responde.
        val decision = AutoReplyEngine.shouldReply(
            target = target(repeatSeconds = 0L, lastReply = 10_000L),
            notification = notification,
            nowMillis = 13_001L,
            lastSeenNotificationKey = null,
            notificationKey = "k-different",
        )
        assertEquals(AutoReplyEngine.Decision.REPLY, decision)
    }

    @Test
    fun `gap interno bloquea re-post con repeticion cero`() {
        // Caso anti doble-envio: re-post a 1ms con Repetir=0 (Siempre).
        // El gap interno de 2s debe frenarlo aunque la firma difiera.
        val decision = AutoReplyEngine.shouldReply(
            target = target(repeatSeconds = 0L, lastReply = 10_000L),
            notification = notification,
            nowMillis = 11_000L,
            lastSeenNotificationKey = "otra-firma",
            notificationKey = "firma-distinta",
        )
        assertEquals(AutoReplyEngine.Decision.SKIP_REPEAT_NOT_DUE, decision)
    }

    @Test
    fun `gap interno no supera el repetir del usuario`() {
        // Repetir 1h > gap 2s: manda el intervalo del usuario POR REMITENTE.
        // Pepe fue respondido hace 5s: solo Pepe esta bloqueado.
        val target = target(repeatSeconds = 3_600L).let {
            AutoReplyEngine.markReplied(it, "Pepe", nowMillis = 10_000L)
        }
        val decision = AutoReplyEngine.shouldReply(
            target = target,
            notification = pepe,
            nowMillis = 15_000L, // 5s después: fuera del gap, dentro de 1h
            lastSeenNotificationKey = null,
            notificationKey = "k2",
        )
        assertEquals(AutoReplyEngine.Decision.SKIP_REPEAT_NOT_DUE, decision)
    }

    @Test
    fun `repetir es individual por remitente pepe vs lele`() {
        // Escenario del usuario: Repetir 1 día. Pepe respondido en T0;
        // Lele escribe 5 minutos despues y DEBE recibir respuesta.
        val base = target(repeatSeconds = 86_400L)
        val afterPepe = AutoReplyEngine.markReplied(base, "Pepe", nowMillis = 10_000L)

        // Pepe a los 5 min: bloqueado (su ventana de 1 dia no vencio).
        val pepeBlocked = AutoReplyEngine.shouldReply(
            target = afterPepe,
            notification = pepe,
            nowMillis = 10_000L + 300_000L,
            lastSeenNotificationKey = null,
            notificationKey = "k-pepe2",
        )
        assertEquals(AutoReplyEngine.Decision.SKIP_REPEAT_NOT_DUE, pepeBlocked)

        // Lele a los 5 min: su ventana es propia y esta vacia → REPLY.
        val leleOk = AutoReplyEngine.shouldReply(
            target = afterPepe,
            notification = lele,
            nowMillis = 10_000L + 300_000L,
            lastSeenNotificationKey = null,
            notificationKey = "k-lele1",
        )
        assertEquals(AutoReplyEngine.Decision.REPLY, leleOk)
    }

    @Test
    fun `remitente con numero de telefono tambien tiene ventana propia`() {
        val base = target(repeatSeconds = 3_600L)
        val afterNumber = AutoReplyEngine.markReplied(base, "+573001234567", nowMillis = 1_000L)
        // Mismo numero dentro de la ventana: bloqueado.
        val same = AutoReplyEngine.shouldReply(
            target = afterNumber,
            notification = AutoReplyEngine.IncomingNotification("com.test.app", "+573001234567"),
            nowMillis = 60_000L,
            lastSeenNotificationKey = null,
            notificationKey = "kn1",
        )
        assertEquals(AutoReplyEngine.Decision.SKIP_REPEAT_NOT_DUE, same)
        // Numero distinto a la vez: responde.
        val other = AutoReplyEngine.shouldReply(
            target = afterNumber,
            notification = AutoReplyEngine.IncomingNotification("com.test.app", "+573009876543"),
            nowMillis = 60_000L,
            lastSeenNotificationKey = null,
            notificationKey = "kn2",
        )
        assertEquals(AutoReplyEngine.Decision.REPLY, other)
    }

    @Test
    fun `markReplied poda el historial de remitentes viejos`() {
        val base = target(repeatSeconds = 0L)
        // 250 remitentes antiguos (fuera del TTL de 7 dias).
        val old = (1..250).associate { "old$it" to (it * 1000L) }
        val loaded = base.copy(lastReplyBySender = old)
        val after = AutoReplyEngine.markReplied(loaded, "nuevo", nowMillis = 30L * 86_400_000L)
        assertEquals(1, after.lastReplyBySender.size)
    }

    @Test
    fun `salta notificacion duplicada`() {
        val decision = AutoReplyEngine.shouldReply(
            target = target(),
            notification = notification,
            nowMillis = 1_000L,
            lastSeenNotificationKey = "dup-key",
            notificationKey = "dup-key",
        )
        assertEquals(AutoReplyEngine.Decision.SKIP_DUPLICATE_DETECTION, decision)
    }

    @Test
    fun `salta con contrato invalido mensaje vacio`() {
        val decision = AutoReplyEngine.shouldReply(
            target = target(message = ""),
            notification = notification,
            nowMillis = 1_000L,
            lastSeenNotificationKey = null,
            notificationKey = "k1",
        )
        assertEquals(AutoReplyEngine.Decision.SKIP_TARGET_INVALID, decision)
    }

    @Test
    fun `salta con repeticion negativa contrato invalido`() {
        val badTarget = target().copy(repeatSeconds = -5L)
        val decision = AutoReplyEngine.shouldReply(
            target = badTarget,
            notification = notification,
            nowMillis = 1_000L,
            lastSeenNotificationKey = null,
            notificationKey = "k1",
        )
        assertEquals(AutoReplyEngine.Decision.SKIP_TARGET_INVALID, decision)
    }

    @Test
    fun `cooldown largo no bloquea decision de repeticion`() {
        // La demora es un efecto post-decision: no debe afectar shouldReply.
        // (lastReply 5s atrás pasa el gap interno de 2s.)
        val decision = AutoReplyEngine.shouldReply(
            target = target(cooldownSeconds = 60L, repeatSeconds = 0L, lastReply = 5_000L),
            notification = notification,
            nowMillis = 7_500L,
            lastSeenNotificationKey = null,
            notificationKey = "k9",
        )
        assertTrue(decision == AutoReplyEngine.Decision.REPLY)
    }
}