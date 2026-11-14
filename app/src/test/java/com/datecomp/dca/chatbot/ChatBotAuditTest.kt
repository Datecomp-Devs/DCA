package com.datecomp.dca.chatbot

import org.junit.Test

/** Auditoría temporal: imprime la respuesta del bot para frases reales. */
class ChatBotAuditTest {

    private fun describe(r: ChatBot.Response): String = when (r) {
        is ChatBot.Response.HumanHandoff -> "HUMANO"
        is ChatBot.Response.Faq -> "FAQ[${r.node.id}]"
        is ChatBot.Response.Fixed -> "FIJA[${r.text.take(24)}]"
        is ChatBot.Response.NotFound -> "NO_ENCONTRADA"
    }

    @Test
    fun auditoria() {
        val frases = listOf(
            // Casos que fallaban antes
            "quiero un proyecto para mi empresa",
            "necesito una pagina para mi negocio",
            "hola cuanto me costaria hacer una app",
            "tengo una idea para un software",
            "necesito ayuda con mi negocio",
            "quiero cotizar",
            "hablar con el dueño",
            "necesito automatizar mi empresa",
            "I want a new project for my company",
            "can you build software for my business",
            "hello I need help",
            // Casos límite: no deben desviar del propósito comercial
            "cuantos proyectos tienen",
            "cuanto tiempo llevan",
            "hola quiero precio de una web",
            "que proyectos han hecho y cuanto cuesta",
            "hacen tiendas para mi negocio",
            "software",
            "proyectos",
            "contacto",
            "precio",
            // Flujo de captación: dejar información y esperar al equipo
            "quiero dejar mi informacion de mi proyecto",
            "les dejo mis datos para que me contacten",
            "leave my information here",
            "my project is a delivery app for my business",
            "quiero seguimiento de mi consulta",
        )
        val k = ChatBotKnowledge()
        frases.forEach { f ->
            val r = ChatBot.respond(f, k)
            val txt = ChatBot.renderText(r, k).replace("\n", " ").take(55)
            println("AUDIT | $f => ${describe(r)} | $txt")
        }
    }
}