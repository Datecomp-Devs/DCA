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
    fun `salta cuando repeticion aun no vence`() {
        val lastReply = 10_000L
        val now = lastReply + 5_000L // 5s después con repetición de 15s
        val decision = AutoReplyEngine.shouldReply(
            target = target(lastReply = lastReply),
            notification = notification,
            nowMillis = now,
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
        // Repetir 1h > gap 2s: manda el intervalo del usuario.
        val decision = AutoReplyEngine.shouldReply(
            target = target(repeatSeconds = 3_600L, lastReply = 10_000L),
            notification = notification,
            nowMillis = 15_000L, // 5s después: fuera del gap, dentro de 1h
            lastSeenNotificationKey = null,
            notificationKey = "k2",
        )
        assertEquals(AutoReplyEngine.Decision.SKIP_REPEAT_NOT_DUE, decision)
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
    fun `markReplied actualiza timestamp`() {
        val updated = AutoReplyEngine.markReplied(target(), nowMillis = 99_999L)
        assertEquals(99_999L, updated.lastReplyTimestampMillis)
    }

    @Test
    fun `awaitCooldown espera la demora configurada`() = runTest {
        val start = testScheduler.currentTime
        AutoReplyEngine.awaitCooldown(3L)
        assertEquals(3_000L, testScheduler.currentTime - start)
    }

    @Test
    fun `awaitCooldown con cero no espera`() = runTest {
        val start = testScheduler.currentTime
        AutoReplyEngine.awaitCooldown(0L)
        assertEquals(0L, testScheduler.currentTime - start)
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