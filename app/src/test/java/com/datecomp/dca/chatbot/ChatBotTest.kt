package com.datecomp.dca.chatbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del chat bot para mensajería: matching de keywords igual que el
 * chat bot del widget web de DateComp, casos anti-falsos-positivos, despacho
 * por prioridad (humano → FAQ → saludo → despedida → fallback), detección de
 * idioma ES/EN y validación de coherencia del cerebro para mensajería.
 */
class ChatBotTest {

    private val knowledge = ChatBotKnowledge()

    // ─── normalize ───────────────────────────────────────────────────────

    @Test
    fun `normalize quita acentos y pasa a minusculas`() {
        assertEquals("cuanto cuesta", ChatBot.normalize("¿Cuánto cuesta?"))
        assertEquals("disenar", ChatBot.normalize("DISEÑAR"))
    }

    // ─── hasKeyword: token exacto + plural, sin falsos positivos ────────

    @Test
    fun `keyword corta no aparece dentro de otra palabra`() {
        // "app" dentro de "whatsapp" NO debe matchear.
        val q = ChatBot.normalize("tengo whatsapp")
        assertFalse(ChatBot.hasKeyword(q, "app"))
        // Pero "app" como token sí.
        assertTrue(ChatBot.hasKeyword(ChatBot.normalize("hacen una app"), "app"))
    }

    @Test
    fun `keyword persona no matchea personalizado`() {
        val q = ChatBot.normalize("quiero algo personalizado")
        assertFalse(ChatBot.hasKeyword(q, "persona"))
    }

    @Test
    fun `plural simple se acepta para keywords de 4 o mas letras`() {
        // Igual que el widget web: plural solo para keywords de 4+ letras.
        assertTrue(ChatBot.hasKeyword(ChatBot.normalize("cuales son los precios"), "precio"))
        assertTrue(ChatBot.hasKeyword(ChatBot.normalize("hacen apps"), "apps"))
        // "app" (3 letras, < 4) NO matchea "apps"; el menú lo cubre con
        // la keyword explicita "apps".
        assertFalse(ChatBot.hasKeyword(ChatBot.normalize("hacen apps"), "app"))
    }

    @Test
    fun `frases se validan por substring`() {
        assertTrue(ChatBot.hasKeyword(ChatBot.normalize("me gusta el e-commerce"), "e-commerce"))
        assertTrue(ChatBot.hasKeyword(ChatBot.normalize("cuanto cuesta algo asi"), "cuanto cuesta"))
    }

    // ─── Prioridad de despacho (ES) ─────────────────────────────────────

    @Test
    fun `pide humano tiene prioridad sobre faq`() {
        val r = ChatBot.respond("quiero hablar con un agente de ventas", knowledge)
        assertTrue(r is ChatBot.Response.HumanHandoff)
        assertEquals(ChatBotLocale.ES, r.locale)
    }

    @Test
    fun `traspaso humano promete turno abierto y no pide elegir canal`() {
        val text = ChatBot.renderText(ChatBot.Response.HumanHandoff(ChatBotLocale.ES), knowledge)
        // Coherencia de mensajería: el cliente YA está en el chat.
        assertTrue(text.contains("aquí mismo"))
        assertTrue(text.contains("turno"))
        assertFalse(text.contains("Elige"))
    }

    @Test
    fun `saludo responde bienvenida`() {
        val r = ChatBot.respond("hola", knowledge)
        assertEquals(knowledge.greeting, ChatBot.renderText(r, knowledge))
    }

    @Test
    fun `despedida responde cortesia`() {
        val r = ChatBot.respond("muchas gracias", knowledge)
        assertEquals(knowledge.byeReply, ChatBot.renderText(r, knowledge))
    }

    @Test
    fun `faq gana por puntaje de keywords especificas`() {
        val r = ChatBot.respond("cuanto cuesta el proyecto", knowledge)
        assertTrue(r is ChatBot.Response.Faq)
        assertEquals("precios", (r as ChatBot.Response.Faq).node.id)
    }

    @Test
    fun `empate de puntaje lo resuelve el orden del menu como en el widget`() {
        // "cuanto cuesta" (precios, 13) empata con "pagina web"+"web"
        // (servicios, 13): gana el primer nodo del menu, igual que el
        // sort estable del chat bot web.
        val r = ChatBot.respond("cuanto cuesta una pagina web", knowledge)
        assertTrue(r is ChatBot.Response.Faq)
        assertEquals("servicios", (r as ChatBot.Response.Faq).node.id)
    }

    @Test
    fun `faq matchea con acentos y mayusculas`() {
        val r = ChatBot.respond("¿Qué tecnologías usan?", knowledge)
        assertTrue(r is ChatBot.Response.Faq)
        assertEquals("tecnologias", (r as ChatBot.Response.Faq).node.id)
    }

    @Test
    fun `metodo de pago matchea pagos`() {
        val r = ChatBot.respond("aceptan paypal", knowledge)
        assertTrue(r is ChatBot.Response.Faq)
        assertEquals("pagos", (r as ChatBot.Response.Faq).node.id)
    }

    @Test
    fun `mensaje desconocido devuelve fallback con sugerencias`() {
        val r = ChatBot.respond("abcdef xyz", knowledge)
        assertTrue(r is ChatBot.Response.NotFound)
        assertTrue((r as ChatBot.Response.NotFound).suggestions.isNotEmpty())
    }

    @Test
    fun `mensaje vacio devuelve fallback`() {
        val r = ChatBot.respond("   ", knowledge)
        assertTrue(r is ChatBot.Response.NotFound)
    }

    // ─── Cerebro adaptado a mensajería ──────────────────────────────────

    @Test
    fun `contacto entrega todos los canales y la web`() {
        val r = ChatBot.respond("como los contacto", knowledge)
        val text = ChatBot.renderText(r, knowledge)
        assertTrue(text.contains("admin@datecomp.com"))
        assertTrue(text.contains("datecomp.com"))
        assertTrue(text.contains("517-9136"))
        assertTrue(text.contains("@date_comp"))
    }

    @Test
    fun `horario explica trabajo por demanda y turno`() {
        val r = ChatBot.respond("tienen horario de atencion", knowledge)
        val text = ChatBot.renderText(r, knowledge)
        assertTrue(text.contains("demanda"))
        assertTrue(text.contains("turno"))
    }

    @Test
    fun `horario matchea en ingles`() {
        val r = ChatBot.respond("are you open now", knowledge)
        assertTrue(r is ChatBot.Response.Faq)
        assertEquals("horario", (r as ChatBot.Response.Faq).node.id)
        assertEquals(ChatBotLocale.EN, r.locale)
    }

    @Test
    fun `dca responde preguntas basicas de la app`() {
        val r = ChatBot.respond("dca no responde mis mensajes", knowledge)
        assertTrue(r is ChatBot.Response.Faq)
        assertEquals("dca", (r as ChatBot.Response.Faq).node.id)
        val text = ChatBot.renderText(r, knowledge)
        assertTrue(text.contains("Acceso a notificaciones"))
    }

    @Test
    fun `respuestas incluyen la web completa`() {
        val menu = ChatBotKnowledge.DEFAULT_MENU
        val proyectos = menu.first { it.id == "proyectos" }.reply
        assertTrue(proyectos.contains("datecomp.com"))
        val tec = menu.first { it.id == "tecnologias" }.reply
        assertTrue(tec.contains("datecomp.com"))
    }

    // ─── Bilingüe: detección y respuestas EN ────────────────────────────

    @Test
    fun `detecta espanol por defecto en mensaje neutro`() {
        assertEquals(ChatBotLocale.ES, ChatBot.detectLocale("app android 123"))
        assertEquals(ChatBotLocale.ES, ChatBot.detectLocale(""))
    }

    @Test
    fun `pregunta en ingles responde con textos en ingles`() {
        val r = ChatBot.respond("how much does a project cost", knowledge)
        assertTrue(r is ChatBot.Response.Faq)
        assertEquals(ChatBotLocale.EN, r.locale)
        val text = ChatBot.renderText(r, knowledge)
        assertTrue(text.contains("From 400 USD"))
    }

    @Test
    fun `humano en ingles`() {
        val r = ChatBot.respond("I want to talk to a person", knowledge)
        assertTrue(r is ChatBot.Response.HumanHandoff)
        assertEquals(ChatBotLocale.EN, r.locale)
        val text = ChatBot.renderText(r, knowledge)
        assertTrue(text.contains("right here"))
        assertFalse(text.contains("Elige"))
    }

    @Test
    fun `despedida en ingles`() {
        val r = ChatBot.respond("thank you", knowledge)
        val text = ChatBot.renderText(r, knowledge)
        assertEquals(knowledge.en?.byeReply, text)
    }

    // ─── Paridad ES/EN del cerebro ──────────────────────────────────────

    @Test
    fun `menu en ingles tiene los mismos ids y orden que espanol`() {
        val es = ChatBotKnowledge.DEFAULT_MENU
        val en = ChatBotKnowledge.DEFAULT_EN.menu
        assertEquals(es.map { it.id }, en.map { it.id })
    }

    @Test
    fun `todas las respuestas tienen contenido e idioma esperado`() {
        (ChatBotKnowledge.DEFAULT_MENU + ChatBotKnowledge.DEFAULT_EN.menu).forEach { node ->
            assertTrue("Nodo ${node.id} sin reply", node.reply.isNotBlank())
            assertTrue("Nodo ${node.id} sin keywords", node.keywords.isNotEmpty())
            assertFalse("Nodo ${node.id} con keyword vacía", node.keywords.any { it.isBlank() })
        }
    }

}