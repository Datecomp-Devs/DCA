package com.datecomp.dca.core

import kotlinx.serialization.Serializable

/**
 * Motor de chat bot para mensajería: replica el sistema de respuesta del
 * chat bot del widget web de DateComp (busca la respuesta más cercana al
 * mensaje del cliente mediante keywords ponderadas por especificidad).
 *
 * Bilingüe: detecta el idioma del mensaje ([detectLocale]) y responde con
 * los textos correspondientes de [ChatBotKnowledge.textsFor].
 *
 * Sin dependencias de Android. Orden de decisión (idéntico al widget):
 * 1. Acción "hablar con una persona" (escala a humano).
 * 2. FAQ por puntuación (keywords largas pesan más; plural simple +s/+es).
 * 3. Saludo / despedida.
 * 4. Respaldo: no encontrado + opciones rápidas del menú.
 */
object ChatBot {

    // ─── Contrato ────────────────────────────────────────────────────────

    /** Qué responder: una acción especial, un nodo del menú o el fallback. */
    sealed interface Response {
        /** Idioma en que debe redactarse la respuesta. */
        val locale: ChatBotLocale

        /** El cliente pidió hablar con una persona: texto de traspaso. */
        data class HumanHandoff(override val locale: ChatBotLocale) : Response

        /** Respuesta de un nodo del menú (FAQ encontrada). */
        data class Faq(
            val node: ChatBotNode,
            val followups: List<String>,
            override val locale: ChatBotLocale,
        ) : Response

        /** Saludo / despedida. */
        data class Fixed(val text: String, override val locale: ChatBotLocale) : Response

        /** No encontrada: frase de respaldo + etiquetas sugeridas. */
        data class NotFound(
            val suggestions: List<String>,
            override val locale: ChatBotLocale,
        ) : Response
    }

    // ─── Matching (puerto 1:1 del widget web) ────────────────────────────

    /** Minúsculas + sin acentos, igual que el widget. */
    fun normalize(s: String): String = buildString(s.length) {
        var index = 0
        while (index < s.length) {
            val c = Character.toLowerCase(s[index])
            if (c == 'á') append('a')
            else if (c == 'é') append('e')
            else if (c == 'í') append('i')
            else if (c == 'ó') append('o')
            else if (c == 'ú') append('u')
            else if (c == 'ü') append('u')
            else if (c == 'ñ') append('n')
            else if (c == '¿' || c == '?' || c == '¡' || c == '!') { /* separador */ }
            else append(c)
            index++
        }
    }

    /**
     * Coincidencia de keyword contra la consulta:
     * - Frases (con espacio o guion) → substring.
     * - Palabra única → token exacto (evita falsos positivos: "persona"
     *   dentro de "personalizado", "app" dentro de "whatsapp"), con plural
     *   simple (+ "s" / + "es") para keywords de 4+ letras.
     */
    fun hasKeyword(query: String, keyword: String): Boolean {
        if (keyword.isEmpty()) return false
        if (keyword.indexOf(' ') != -1 || keyword.indexOf('-') != -1) {
            return query.indexOf(keyword) != -1
        }
        val tokens = query.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        return tokens.any { token ->
            token == keyword ||
                (keyword.length >= 4 && (token == keyword + "s" || token == keyword + "es"))
        }
    }

    /** Nodo con mayor puntuación: suma de longitudes de keywords coincidentes. */
    fun matchNode(query: String, menu: List<ChatBotNode>): ChatBotNode? {
        var best: ChatBotNode? = null
        var bestScore = 0
        for (node in menu) {
            var score = 0
            for (kw in node.keywords) {
                val nkw = normalize(kw)
                if (hasKeyword(query, nkw)) score += nkw.length
            }
            if (score > bestScore) {
                best = node
                bestScore = score
            }
        }
        return best
    }

    // ─── Detección de idioma ─────────────────────────────────────────────

    /**
     * Marcadores por idioma: solo palabras que difieren entre ES y EN.
     * Se excluyen términos neutros (app, android, firebase, stripe, etc.),
     * que no aportan señal.
     */
    private val ES_MARKERS = listOf(
        "hola", "buenas", "buenos dias", "buenas tardes", "buenas noches",
        "gracias", "por favor", "precio", "precios", "costo", "cuanto",
        "cuanto cuesta", "cuando", "donde", "como", "servicio", "servicios",
        "proyecto", "proyectos", "tienda", "pago", "pagos", "soporte",
        "horario", "atencion", "correo", "telefono", "cotizacion",
        "necesito", "quiero", "pueden", "tienen", "hacen",
    ).map { normalize(it) }

    private val EN_MARKERS = listOf(
        "hello", "hi", "hey", "good morning", "good afternoon",
        "thanks", "thank you", "please", "price", "prices", "cost",
        "how much", "how", "when", "where", "what", "service", "services",
        "project", "projects", "store", "shop", "payment", "payments",
        "support", "schedule", "hours", "email", "phone", "quote",
        "need", "want", "can you", "do you", "are you", "open", "now",
        "available",
    ).map { normalize(it) }

    /**
     * Detecta el idioma del mensaje contando marcadores exclusivos de cada
     * lengua. Empate o vacío → español (idioma principal de DateComp).
     */
    fun detectLocale(message: String): ChatBotLocale {
        val query = normalize(message)
        if (query.isBlank()) return ChatBotLocale.ES
        val es = ES_MARKERS.count { hasKeyword(query, it) }
        val en = EN_MARKERS.count { hasKeyword(query, it) }
        return if (en > es) ChatBotLocale.EN else ChatBotLocale.ES
    }

    // ─── Intenciones por idioma ──────────────────────────────────────────

    private val ES_HUMAN = listOf(
        "hablar con una persona", "hablar con alguien", "persona", "agente",
        "asesor", "humano", "representante", "atencion al cliente",
        "con una persona",
    ).map { normalize(it) }

    private val EN_HUMAN = listOf(
        "talk to a person", "talk to someone", "person", "agent",
        "human", "representative", "live person", "customer service",
    ).map { normalize(it) }

    private val ES_GREETING = listOf(
        "hola", "buenas", "buenos dias", "buenas tardes", "buenas noches",
        "saludos", "hey",
    ).map { normalize(it) }

    private val EN_GREETING = listOf(
        "hello", "hi", "hey", "good morning", "good afternoon", "good evening",
    ).map { normalize(it) }

    private val ES_FAREWELL = listOf(
        "gracias", "muchas gracias", "adios", "chau", "hasta luego",
        "hasta pronto", "bye",
    ).map { normalize(it) }

    private val EN_FAREWELL = listOf(
        "thanks", "thank you", "goodbye", "bye", "see you", "cheers",
    ).map { normalize(it) }

    private fun listFor(locale: ChatBotLocale, es: List<String>, en: List<String>): List<String> =
        if (locale == ChatBotLocale.EN) en else es

    private fun anyKeyword(query: String, keywords: List<String>): Boolean =
        keywords.any { hasKeyword(query, it) }

    // ─── Decisión ────────────────────────────────────────────────────────

    /**
     * Decide la respuesta para el mensaje del cliente, con el mismo orden
     * del widget web: acción humano → FAQ → saludo → despedida → fallback.
     * El idioma de la respuesta se detecta del propio mensaje.
     *
     * Nota de mensajería: "menu" es una keyword de la acción humana en el
     * widget web; aquí se excluye del matching de humano para que "menú",
     * "cómo te llamas", etc. no disparen el traspaso por error en WhatsApp.
     */
    fun respond(
        clientMessage: String,
        knowledge: ChatBotKnowledge = ChatBotKnowledge(),
    ): Response {
        val locale = detectLocale(clientMessage)
        val texts = knowledge.textsFor(locale)
        val query = normalize(clientMessage.trim())
        if (query.isEmpty()) return Response.NotFound(emptyList(), locale)

        // 1. Acción: hablar con una persona (sin "menu", ver kDoc).
        val humanKws = listFor(locale, ES_HUMAN, EN_HUMAN)
            .filterNot { it == "menu" || it == "menu principal" || it == "opciones" }
        if (query.isNotEmpty() && anyKeyword(query, humanKws)) {
            return Response.HumanHandoff(locale)
        }

        // 2. FAQ por puntuación → respuesta + hasta 3 followups.
        matchNode(query, texts.menu)?.let { node ->
            val followups = texts.menu
                .asSequence()
                .filter { it.id != node.id }
                .map { it.label }
                .take(ChatBotKnowledge.FALLBACK_OPTIONS)
                .toList()
            return Response.Faq(node, followups, locale)
        }

        // 3. Saludo / despedida.
        if (anyKeyword(query, listFor(locale, ES_GREETING, EN_GREETING))) {
            return Response.Fixed(texts.greeting, locale)
        }
        if (anyKeyword(query, listFor(locale, ES_FAREWELL, EN_FAREWELL))) {
            return Response.Fixed(texts.byeReply, locale)
        }

        // 4. Fallback: no encontrado + opciones.
        val suggestions = texts.menu.take(ChatBotKnowledge.FALLBACK_OPTIONS).map { it.label }
        return Response.NotFound(suggestions, locale)
    }

    /** Texto plano a enviar por mensajería para una respuesta dada. */
    fun renderText(response: Response, knowledge: ChatBotKnowledge = ChatBotKnowledge()): String {
        val texts = knowledge.textsFor(response.locale)
        return when (response) {
            is Response.HumanHandoff -> texts.humanReply
            is Response.Faq -> response.node.reply
            is Response.Fixed -> response.text
            is Response.NotFound -> buildString {
                append(texts.notFound)
                if (response.suggestions.isNotEmpty()) {
                    append("\n")
                    response.suggestions.forEachIndexed { i, s -> append("\n${i + 1}. $s") }
                }
            }
        }
    }

    /** Determina el mensaje a enviar a un cliente según la app objetivo. */
    fun replyFor(
        message: String,
        knowledge: ChatBotKnowledge = ChatBotKnowledge(),
    ): String = renderText(respond(message, knowledge), knowledge)
}