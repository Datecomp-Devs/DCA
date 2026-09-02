package com.datecomp.dca.core

import org.junit.Test

/** Auditoría de cobertura: verifica frases reales de clientes. */
class ChatBotCoverageTest {

    private fun describe(r: ChatBot.Response): String = when (r) {
        is ChatBot.Response.HumanHandoff -> "HUMANO"
        is ChatBot.Response.Faq -> "FAQ[${r.node.id}]"
        is ChatBot.Response.Fixed -> "FIJA"
        is ChatBot.Response.NotFound -> "NOT_FOUND"
    }

    @Test
    fun cobertura() {
        val k = ChatBotKnowledge()
        val frases = listOf(
            // Precios específicos
            "cuanto cuesta una app",
            "cuanto cuesta una tienda online",
            "cuanto cuesta un sistema",
            "tengo poco presupuesto",
            "cual es el minimo que cobran",
            // Garantía / contrato / confianza
            "hay garantia",
            "tienen contrato",
            "puedo pedir reembolso",
            "son una empresa seria",
            "son confiables",
            "cuanto tiempo llevan en el mercado",
            "quienes son ustedes",
            "cuantos clientes tienen",
            "tienen testimonios",
            // Servicios adicionales
            "hacen marketing digital",
            "hacen seo",
            "hacen logos o marca",
            "hacen chatbots con inteligencia artificial",
            // Facturación
            "me dan factura",
            "emiten recibo",
            // Modalidad / ubicación
            "son de republica dominicana",
            "tienen oficina",
            "es presencial o remoto",
            "atienden clientes internacionales",
            "trabajan con clientes de otro pais",
            // Urgencia
            "lo necesito urgente",
            "tengo una emergencia",
            // Empleo / alianzas
            "estan contratando",
            "hacen alianzas",
            "quiero trabajar con ustedes",
            // Reclamos
            "quiero hacer un reclamo",
            "no me gusta mi proyecto",
            // Referencias
            "tienen referencias de clientes",
            "puedo ver una demo",
            // Núcleo (debe seguir bien)
            "quiero un proyecto para mi empresa",
            "cuanto tarda",
            "precio",
            "contacto",
        )
        var notFound = 0
        frases.forEach { f ->
            val r = ChatBot.respond(f, k)
            if (r is ChatBot.Response.NotFound) notFound++
            println("COVERAGE | ${describe(r)} | $f")
        }
        println("COVERAGE | RESULTADO: ${frases.size - notFound}/${frases.size} respondidas, $notFound en fallback")
    }
}