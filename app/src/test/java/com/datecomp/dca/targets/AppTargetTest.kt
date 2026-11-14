package com.datecomp.dca.targets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contrato y transiciones puras del modelo de app objetivo. */
class AppTargetTest {

    private val base = AppTarget(
        packageName = "com.test.app",
        displayName = "Test App",
    )

    @Test
    fun `markReplied poda el historial de remitentes viejos`() {
        // 250 remitentes antiguos (fuera del TTL de 7 días).
        val old = (1..250).associate { "old$it" to (it * 1000L) }
        val loaded = base.copy(lastReplyBySender = old)
        val after = loaded.markReplied("nuevo", nowMillis = 30L * 86_400_000L)
        assertEquals(1, after.lastReplyBySender.size)
        assertTrue(after.lastReplyBySender.containsKey("nuevo"))
    }

    @Test
    fun `markReplied actualiza el timestamp de la app`() {
        val after = base.markReplied("Pepe", nowMillis = 1_000L)
        assertEquals(1_000L, after.lastReplyTimestampMillis)
        assertEquals(1_000L, after.lastReplyBySender["Pepe"])
    }

    @Test
    fun `target con chat bot activo es valido sin mensaje`() {
        val target = AppTarget(
            packageName = "com.whatsapp",
            displayName = "WhatsApp",
            enabled = true,
            chatBotEnabled = true,
            replyMessage = "",
        )
        assertTrue(target.isValid())
    }

    @Test
    fun `target sin chat bot requiere mensaje`() {
        val target = AppTarget(
            packageName = "com.whatsapp",
            displayName = "WhatsApp",
            enabled = true,
            chatBotEnabled = false,
            replyMessage = "",
        )
        assertFalse(target.isValid())
    }
}
