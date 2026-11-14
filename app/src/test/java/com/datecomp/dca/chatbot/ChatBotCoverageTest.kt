package com.datecomp.dca.chatbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Batería de cobertura: frases reales de clientes y su categoría esperada. */
class ChatBotCoverageTest {

    private fun expectedId(r: ChatBot.Response): String = when (r) {
        is ChatBot.Response.Faq -> r.node.id
        is ChatBot.Response.NotFound -> "NOT_FOUND"
        is ChatBot.Response.HumanHandoff -> "HUMANO"
        is ChatBot.Response.Fixed -> "FIJA"
    }

    @Test
    fun cobertura_responde_la_categoria_esperada() {
        val k = ChatBotKnowledge()
        // frase -> id de categoría esperado
        val casos = listOf(
            "cuanto cuesta una app" to "precios",
            "cuanto cuesta una tienda online" to "precios",
            "cuanto cuesta un sistema" to "precios",
            "tengo poco presupuesto" to "precios",
            "cual es el minimo que cobran" to "precios",
            "hay garantia" to "garantia",
            "tienen contrato" to "garantia",
            "puedo pedir reembolso" to "garantia",
            "son una empresa seria" to "sobre_nosotros",
            "son confiables" to "sobre_nosotros",
            "cuanto tiempo llevan en el mercado" to "sobre_nosotros",
            "quienes son ustedes" to "sobre_nosotros",
            "cuantos clientes tienen" to "sobre_nosotros",
            "tienen testimonios" to "sobre_nosotros",
            "hacen marketing digital" to "servicios_extra",
            "hacen seo" to "servicios_extra",
            "hacen logos o marca" to "servicios_extra",
            "hacen chatbots con inteligencia artificial" to "servicios_extra",
            "me dan factura" to "facturacion",
            "emiten recibo" to "facturacion",
            "son de republica dominicana" to "modalidad",
            "tienen oficina" to "modalidad",
            "es presencial o remoto" to "modalidad",
            "atienden clientes internacionales" to "modalidad",
            "trabajan con clientes de otro pais" to "modalidad",
            "lo necesito urgente" to "urgencia",
            "tengo una emergencia" to "urgencia",
            "estan contratando" to "empleo",
            "hacen alianzas" to "empleo",
            "quiero trabajar con ustedes" to "empleo",
            "quiero hacer un reclamo" to "reclamos",
            "no me gusta mi proyecto" to "reclamos",
            "tienen referencias de clientes" to "proyectos",
            "puedo ver una demo" to "proyectos",
            // Desvíos detectados en captura real
            "cual es su pagina web" to "contacto",
            "numero de telefono" to "contacto",
            "cual es su telefono" to "contacto",
            "me pasan su sitio web" to "contacto",
            "url" to "contacto",
            "link de su web" to "contacto",
            // Núcleo
            "quiero un proyecto para mi empresa" to "nuevo_proyecto",
            "cuanto tarda" to "tiempo",
            "android" to "apps",
            "precio" to "precios",
            "contacto" to "contacto",
            "hacen paginas web" to "servicios",
        )
        casos.forEach { (frase, esperado) ->
            val r = ChatBot.respond(frase, k)
            assertEquals("'$frase' debía dar '$esperado'", esperado, expectedId(r))
        }
    }

    @Test
    fun cobertura_no_queda_en_fallback() {
        val k = ChatBotKnowledge()
        val frases = listOf(
            "cuanto cuesta una app", "hay garantia", "quienes son ustedes", "hacen seo",
            "me dan factura", "es presencial o remoto", "lo necesito urgente",
            "estan contratando", "quiero hacer un reclamo", "puedo ver una demo",
            "cual es su pagina web", "numero de telefono", "quiero un proyecto para mi empresa",
        )
        frases.forEach { f ->
            assertTrue("'$f' no debería ser NotFound", ChatBot.respond(f, k) !is ChatBot.Response.NotFound)
        }
    }
}