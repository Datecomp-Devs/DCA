package com.datecomp.dca.chatbot

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica que la charla fuera del negocio NO se desvíe a una FAQ y que el
 * bot responda claramente que no tiene respuesta, ofreciendo opciones.
 */
class OffTopicTest {

    private val knowledge = ChatBotKnowledge()

    @Test
    fun `charla casual no cae en faq ni en horario`() {
        listOf(
            "cual es tu color favorito",
            "dime un chiste",
            "que hora es",
            "estas enamorada",
            "cantame una cancion",
        ).forEach { f ->
            val r = ChatBot.respond(f, knowledge)
            assertTrue("'$f' no debería responder FAQ", r !is ChatBot.Response.Faq)
            assertTrue("'$f' debería ser NotFound", r is ChatBot.Response.NotFound)
        }
    }

    @Test
    fun `fallback recomienda opciones y hablar con persona`() {
        val text = ChatBot.renderText(ChatBot.respond("dime un chiste", knowledge), knowledge)
        assertTrue(text.contains("no") || text.contains("No"))
        assertTrue(text.contains("hablar con una persona"))
        // Enumera al menos 3 opciones numeradas.
        assertTrue(text.contains("1."))
    }
}