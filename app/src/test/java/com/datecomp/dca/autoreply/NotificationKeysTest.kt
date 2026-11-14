package com.datecomp.dca.autoreply

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Pruebas de la firma de contenido para detección de duplicados.
 * Caso crítico: mensajes NUEVOS del mismo remitente NO deben considerarse
 * duplicados aunque el sistema reutilice la misma notificación.
 */
class NotificationKeysTest {

    @Test
    fun `mensaje nuevo mismo remitente produce firma distinta`() {
        val a = NotificationKeys.contentKey("com.whatsapp", "Juan", "Hola")
        val b = NotificationKeys.contentKey("com.whatsapp", "Juan", "¿Cómo estás?")
        assertNotEquals(a, b)
    }

    @Test
    fun `mismo mensaje repesteado produce misma firma`() {
        val a = NotificationKeys.contentKey("com.whatsapp", "Juan", "Hola")
        val b = NotificationKeys.contentKey("com.whatsapp", "Juan", "Hola")
        assertEquals(a, b)
    }

    @Test
    fun `remitentes distintos con mismo texto producen firmas distintas`() {
        val a = NotificationKeys.contentKey("com.whatsapp", "Juan", "Ok")
        val b = NotificationKeys.contentKey("com.whatsapp", "Maria", "Ok")
        assertNotEquals(a, b)
    }

    @Test
    fun `apps distintas producen firmas distintas`() {
        val a = NotificationKeys.contentKey("com.whatsapp", "Juan", "Hola")
        val b = NotificationKeys.contentKey("com.telegram", "Juan", "Hola")
        assertNotEquals(a, b)
    }

    @Test
    fun `texto en blanco devuelve clave vacia`() {
        assertEquals("", NotificationKeys.contentKey("com.whatsapp", "Juan", "  "))
        assertEquals("", NotificationKeys.contentKey("com.whatsapp", "Juan", ""))
    }

    @Test
    fun `espacios en extremos no cambian la firma`() {
        val a = NotificationKeys.contentKey("com.whatsapp", " Juan ", "Hola ")
        val b = NotificationKeys.contentKey("com.whatsapp", "Juan", "Hola")
        assertEquals(a, b)
    }
}