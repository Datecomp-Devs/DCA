package com.datecomp.dca.core

import kotlinx.serialization.Serializable

/**
 * Nodo de conocimiento del chat bot: una categoría de preguntas frecuentes
 * con sus keywords de detección y la respuesta a enviar por mensajería.
 */
@Serializable
data class ChatBotNode(
    val id: String,
    val label: String,
    val keywords: List<String> = emptyList(),
    val reply: String,
)

/**
 * Textos completos (sistema + menú) para un idioma del chat bot.
 */
@Serializable
data class ChatBotTexts(
    val botName: String,
    val greeting: String,
    val byeReply: String,
    val notFound: String,
    val humanReply: String,
    val menu: List<ChatBotNode>,
)

/**
 * Conocimiento del chat bot de mensajería de DateComp. Adaptado del chat bot
 * web de DateCompWebSide al entorno de apps de mensajería (WhatsApp, etc.):
 * sin botones ni enlaces elegibles, con respuestas de traspaso humano,
 * contacto no circular (el cliente YA está conversando con nosotros) y URLs
 * completas clicables. Bilingüe: ES por defecto + EN, con detección
 * automática del idioma (ChatBot.detectLocale).
 *
 * Persistido en el backend de la app (SettingsRepository): editable en
 * caliente y sin migraciones.
 *
 * Modo de operación (definido por DateComp): sin horario fijo; se trabaja
 * por demanda de los clientes. Cuando el bot responde es porque el turno
 * del cliente ya abrió, y se le atiende hasta cumplir su solicitud.
 */
@Serializable
data class ChatBotKnowledge(
    val botName: String = "Asistente DateComp",
    val greeting: String = "¡Hola! 👋 Soy el asistente de DateComp. ¿En qué puedo ayudarte? Escríbeme tu pregunta.",
    val byeReply: String = "¡Con gusto! 😊 Si necesitas algo más, aquí estaré.",
    val notFound: String = "Lo siento, no tengo una respuesta exacta para eso. Puedo ayudarte con estas opciones:",
    val humanLabel: String = "💬 Hablar con una persona",
    val humanReply: String = "¡Claro! Una persona te responderá aquí mismo. 👋 Trabajamos por demanda: cuando tu turno abra, te atendemos en este chat hasta cumplir tu solicitud o proyecto.",
    val menuReply: String = "¡Con gusto! Aquí tienes el menú para elegir:",
    val menu: List<ChatBotNode> = DEFAULT_MENU,
    /** Textos completos en inglés; si es null, EN cae a ES. */
    val en: ChatBotTexts? = DEFAULT_EN,
) {
    /** Textos efectivos para un idioma (EN sin datos vuelve a ES). */
    fun textsFor(locale: ChatBotLocale): ChatBotTexts = when (locale) {
        ChatBotLocale.EN -> en ?: esTexts()
        ChatBotLocale.ES -> esTexts()
    }

    private fun esTexts() = ChatBotTexts(
        botName = botName,
        greeting = greeting,
        byeReply = byeReply,
        notFound = notFound,
        humanReply = humanReply,
        menu = menu,
    )

    companion object {

        /**
         * Menú español: 15 categorías (13 del chat bot web + horario de
         * atención por demanda + soporte básico de la app DCA), con textos
         * reescritos para mensajería.
         */
        val DEFAULT_MENU: List<ChatBotNode> = listOf(
            ChatBotNode(
                id = "servicios",
                label = "💻 ¿Qué servicios ofrecen?",
                keywords = listOf("servicio", "servicios", "pagina web", "web", "aplicacion", "sistema", "automatizacion"),
                reply = "Creación de aplicaciones web o nativas para teléfono Android y computadora, según tus necesidades.\n\nTrabajamos con datos locales y bases de datos, con herramientas tanto para empresas como para usuarios individuales y emprendedores.\n\n¿Te interesa alguno en especial? Puedo contarte más.",
            ),
            ChatBotNode(
                id = "precios",
                label = "💰 ¿Cuánto cuesta un proyecto?",
                keywords = listOf("precio", "precios", "costo", "cuanto cuesta", "tarifa", "valor", "presupuesto", "cotizacion", "costos"),
                reply = "Desde 400 USD por una landing page con seguridad, dominio personalizado (la titularidad del dominio no está incluida), funciones y el estilo que prefieras.\n\nIncluye soporte gratis por 1 mes al finalizar.\n\nPara tiendas, apps o sistemas a medida te damos una cotización según el alcance. Escríbenos tu idea aquí mismo y te la enviamos gratis, sin compromiso.",
            ),
            ChatBotNode(
                id = "tiempo",
                label = "⏱️ ¿Cuánto tarda el desarrollo?",
                keywords = listOf("tiempo", "tarda", "tardan", "tardo", "demora", "cuanto dura", "plazo", "entrega", "rapido", "30 dias"),
                reply = "Depende de la magnitud del proyecto: podemos entregarlo incluso dentro de los 30 días posteriores a su aceptación.\n\nTe damos un cronograma claro al iniciar y avisamos de cada avance.",
            ),
            ChatBotNode(
                id = "tiendas",
                label = "🛒 ¿Pueden crear mi tienda online?",
                keywords = listOf("tienda", "tiendas", "ecommerce", "e-commerce", "vender", "ventas", "carrito", "pagos online"),
                reply = "¡Correcto! Podemos crear tu tienda online, o incluso mejorarla si ya tienes una web desactualizada, con nosotros, siempre a tu disposición.\n\nCarrito, métodos de pago, gestión de productos y pedidos, y diseño a tu marca. ¿Quieres una tienda a medida o prefieres la plataforma Stores? Puedes conocer Stores en datecomp.com.",
            ),
            ChatBotNode(
                id = "apps",
                label = "📱 ¿Hacen apps para celular? ¿Para iPhone?",
                keywords = listOf("app", "apps", "aplicacion", "movil", "celular", "android", "iphone", "ios", "play store"),
                reply = "¡Sí! (Para iPhone solo creamos PWA, aplicaciones web).\n\nNuestro equipo trabaja arduamente para integrar aplicaciones nativas para iPhone en nuestros servicios.\n\nDesarrollamos apps nativas para Android y aplicaciones web para computadora.",
            ),
            ChatBotNode(
                id = "tecnologias",
                label = "🚀 ¿Qué tecnologías usan?",
                keywords = listOf("tecnologia", "tecnologias", "lenguaje", "stack", "herramientas", "firebase", "node", "kotlin"),
                reply = "Las tecnologías están publicadas en datecomp.com: HTML & CSS, JavaScript, TypeScript, Node.js, Kotlin, Android, PWA, Firebase, PostgreSQL, Docker, AWS y Git.\n\nAdemás utilizamos AWS y GCP como infraestructura segura para tus datos.\n\nElegimos la tecnología según tu proyecto: siempre la más confiable y fácil de mantener.",
            ),
            ChatBotNode(
                id = "soporte",
                label = "🛡️ ¿Dan soporte y mantenimiento?",
                keywords = listOf("soporte", "mantenimiento", "actualizacion", "monitoreo", "respaldo", "soporte tecnico"),
                reply = "Sí, siempre que terminamos un proyecto ofrecemos soporte gratuito por 1 mes posterior a la entrega del proyecto.\n\nAdemás ofrecemos planes de mantenimiento continuo: actualizaciones y monitoreo.",
            ),
            ChatBotNode(
                id = "hosting",
                label = "🗄️ ¿Me ayudan con hosting y bases de datos?",
                keywords = listOf("hosting", "servidor", "base de datos", "alojamiento", "dominio", "ssl"),
                reply = "¡Correcto! Podemos ayudarte con esto y más; incluso si tienes alguna preferencia, podemos hacer que tu proyecto cumpla con ello.\n\nAlojamiento, bases de datos seguras, SSL, dominio y migraciones desde tu proveedor actual.",
            ),
            ChatBotNode(
                id = "proyectos",
                label = "🌟 ¿Qué proyectos han hecho?",
                keywords = listOf("proyecto", "proyectos", "trabajos", "portafolio", "ejemplos", "clientes"),
                reply = "Están publicados en datecomp.com, en la sección de proyectos (SP-CASH, Alarma zZz, Stores.datecomp.com); los demás son privados, protegidos por seguridad de clientes.\n\nSeguimos creciendo gracias a ustedes.",
            ),
            ChatBotNode(
                id = "empezar",
                label = "🤝 ¿Cómo empiezo mi proyecto?",
                keywords = listOf("empezar", "iniciar", "comenzar", "proceso", "como empiezo", "como iniciar", "como comenzar", "como contratar", "como trabajar", "pasos"),
                reply = "Puedes contactarnos escribiéndonos al correo o al número de teléfono vía WhatsApp, y coordinar una llamada de 30 minutos hablando sobre tu proyecto y si será aceptado para empezar su desarrollo.\n\nNada de tecnicismos: hablamos tu idioma y te respaldamos en todo el proceso.",
            ),
            ChatBotNode(
                id = "contacto",
                label = "📞 ¿Cómo puedo contactarlos?",
                keywords = listOf("contacto", "contactar", "telefono", "email", "correo", "whatsapp", "ubicacion", "donde estan"),
                reply = "¡Ya estás en contacto con nosotros! 😊 Este es el chat oficial del asistente de DateComp, y una persona puede continuar aquì cuando lo necesites.\n\nTambién puedes escribirnos por correo: admin@datecomp.com",
            ),
            ChatBotNode(
                id = "pagos",
                label = "💳 ¿Qué métodos de pago aceptan?",
                keywords = listOf("pago", "pagos", "metodo", "metodos", "tarjeta", "transferencia", "paypal", "abono", "financiamiento", "stripe", "etapas", "avances", "cuotas"),
                reply = "Los pagos se realizan a través de Stripe.\n\nNormalmente dividimos el pago en etapas: un adelanto para comenzar y el resto según avances. Cuéntanos tu idea y te damos los detalles en la cotización.",
            ),
            ChatBotNode(
                id = "seguridad",
                label = "🔒 ¿Mis datos y mi web están seguros?",
                keywords = listOf("seguridad", "seguro", "datos", "privacidad", "proteccion", "hacker"),
                reply = "Correcto, utilizamos AWS y GCP como infraestructura segura para tus datos.\n\nAdemás: certificados SSL, prácticas de desarrollo seguras y monitoreo continuo.\n\nTu información y la de tus clientes están protegidas.",
            ),
            ChatBotNode(
                id = "horario",
                label = "🕐 ¿Tienen horario de atención?",
                keywords = listOf("horario", "horarios", "hora", "atienden", "disponible", "disponibles", "abierto", "abren", "cierran", "cerrado", "24 horas", "turno", "ahora"),
                reply = "No cerramos a una hora fija: trabajamos por demanda de los clientes, así que podemos estar activos a cualquier hora.\n\nSi te respondemos, es porque tu turno ya está abierto, y te atendemos hasta cumplir tu solicitud o proyecto.",
            ),
            ChatBotNode(
                id = "dca",
                label = "🤖 ¿Cómo funciona la app DCA?",
                keywords = listOf("dca", "permiso", "permisos", "notificacion", "notificaciones", "no responde", "no respondio", "pausar", "bateria", "acceso", "autorespuesta", "auto respuesta"),
                reply = "Si tu pregunta es sobre la app DCA: responde mensajes de forma automática desde las notificaciones (usa el permiso \"Acceso a notificaciones\"), y su Chat Bot usa estas mismas preguntas frecuentes.\n\nSi una respuesta no llegó, revisa que la app esté en tu lista de DCA, que esté activada y que el Chat Bot esté encendido en su pantalla de edición.\n\nPara temas técnicos más profundos, escribe \"hablar con una persona\".",
            ),
        )

        /** Textos completos en inglés (equivalentes y validados en pareja con ES). */
        val DEFAULT_EN: ChatBotTexts = ChatBotTexts(
            botName = "DateComp Assistant",
            greeting = "Hi! 👋 I am the DateComp assistant. How can I help you? Type your question.",
            byeReply = "You are welcome! 😊 If you need anything else, I will be here.",
            notFound = "Sorry, I do not have an exact answer for that. But I can help you with these options:",
            humanReply = "Sure! A person will reply to you right here in this chat. 👋 We work on demand: as soon as your turn opens, we assist you in this chat until your request or project is fulfilled.",
            menu = listOf(
                ChatBotNode(
                    id = "servicios",
                    label = "💻 What services do you offer?",
                    keywords = listOf("service", "services", "website", "web", "system", "automation"),
                    reply = "We build web applications or native apps for Android phones and computers, according to your needs.\n\nWe work with local data and databases, with tools for businesses, individual users and entrepreneurs.\n\nInterested in one in particular? I can tell you more.",
                ),
                ChatBotNode(
                    id = "precios",
                    label = "💰 How much does a project cost?",
                    keywords = listOf("price", "prices", "cost", "how much", "quote", "budget", "estimate"),
                    reply = "From 400 USD for a landing page with security, custom domain (domain ownership not included), features and the style you prefer.\n\nIt includes free support for 1 month after delivery.\n\nFor stores, apps or custom systems we give you a quote based on scope. Tell us your idea right here and we will send you a free, no-obligation quote.",
                ),
                ChatBotNode(
                    id = "tiempo",
                    label = "⏱️ How long does development take?",
                    keywords = listOf("time", "long", "duration", "deadline", "delivery", "weeks", "fast", "30 days"),
                    reply = "Depending on the scope of the project, we can deliver your project even within 30 days after accepting it.\n\nWe give you a clear schedule at the start and keep you updated on progress.",
                ),
                ChatBotNode(
                    id = "tiendas",
                    label = "🛒 Can you build my online store?",
                    keywords = listOf("store", "shop", "ecommerce", "e-commerce", "sell", "sales", "cart", "online store"),
                    reply = "Correct! We can build your online store, or even improve it if you already have an outdated website, always at your service.\n\nCart, payment methods, product and order management, and a design for your brand. Do you want a custom store or the Stores platform? You can check Stores at datecomp.com.",
                ),
                ChatBotNode(
                    id = "apps",
                    label = "📱 Do you build mobile apps? For iPhone?",
                    keywords = listOf("app", "apps", "application", "mobile", "android", "iphone", "ios", "play store"),
                    reply = "Yes! (For iPhone we only build PWAs, web apps).\n\nOur team works hard to add native iPhone apps to our services.\n\nWe build native Android apps and web applications for computers.",
                ),
                ChatBotNode(
                    id = "tecnologias",
                    label = "🚀 What technologies do you use?",
                    keywords = listOf("technology", "technologies", "stack", "tools", "firebase", "node", "kotlin", "language"),
                    reply = "The technologies are published on datecomp.com: HTML & CSS, JavaScript, TypeScript, Node.js, Kotlin, Android, PWA, Firebase, PostgreSQL, Docker, AWS and Git.\n\nWe also use AWS and GCP as secure infrastructure for your data.\n\nWe pick the right technology for your project: always the most reliable and easy to maintain.",
                ),
                ChatBotNode(
                    id = "soporte",
                    label = "🛡️ Do you provide support and maintenance?",
                    keywords = listOf("support", "maintenance", "update", "monitoring", "backup", "help"),
                    reply = "Yes, whenever we finish a project we offer free support for 1 month after delivery.\n\nWe also offer ongoing maintenance plans: updates and monitoring.",
                ),
                ChatBotNode(
                    id = "hosting",
                    label = "🗄️ Can you help with hosting and databases?",
                    keywords = listOf("hosting", "server", "database", "domain", "ssl"),
                    reply = "Correct! We can help you with this and more; even if you have a preference, we can make your project meet it.\n\nHosting, secure databases, SSL, domain and migrations from your current provider.",
                ),
                ChatBotNode(
                    id = "proyectos",
                    label = "🌟 What projects have you built?",
                    keywords = listOf("project", "projects", "work", "portfolio", "examples", "clients"),
                    reply = "They are published on datecomp.com, in the projects section (SP-CASH, Alarma zZz, Stores.datecomp.com); the rest are private projects, protected by client security.\n\nWe keep growing thanks to you.",
                ),
                ChatBotNode(
                    id = "empezar",
                    label = "🤝 How do I start my project?",
                    keywords = listOf("start", "begin", "process", "how do i start", "how to start", "how to begin", "get started", "steps", "work with"),
                    reply = "You can contact us by email or by phone number via WhatsApp, and schedule a 30-minute call to talk about your project and whether it will be accepted to start development.\n\nNo jargon: we speak your language and back you up through the whole process.",
                ),
                ChatBotNode(
                    id = "contacto",
                    label = "📞 How can I contact you?",
                    keywords = listOf("contact", "phone", "email", "whatsapp", "location", "where"),
                    reply = "You are already in contact with us! 😊 This is the official chat of the DateComp assistant, and a person can continue here whenever you need.\n\nYou can also email us: admin@datecomp.com",
                ),
                ChatBotNode(
                    id = "pagos",
                    label = "💳 What payment methods do you accept?",
                    keywords = listOf("payment", "payments", "pay", "card", "transfer", "paypal", "deposit", "installments", "stripe"),
                    reply = "Payments are made via Stripe.\n\nWe usually split payment into stages: a deposit to begin and the rest as we progress. Tell us your idea and we will give you the details in the quote.",
                ),
                ChatBotNode(
                    id = "seguridad",
                    label = "🔒 Is my data and my website safe?",
                    keywords = listOf("security", "secure", "safe", "data", "privacy", "protection", "hacker"),
                    reply = "Correct, we use AWS and GCP as secure infrastructure for your data.\n\nPlus: SSL certificates, secure development practices and continuous monitoring.\n\nYour information and your customers' information are protected.",
                ),
                ChatBotNode(
                    id = "horario",
                    label = "⏰ What are your business hours?",
                    keywords = listOf("schedule", "hours", "available", "availability", "open now", "shift", "shifts", "demand", "working hours"),
                    reply = "We do not close at a fixed time: we work on customer demand, so we can be active at any hour.\n\nIf we reply to you, it is because your turn is already open, and we assist you until your request or project is fulfilled.",
                ),
                ChatBotNode(
                    id = "dca",
                    label = "🤖 How does the DCA app work?",
                    keywords = listOf("dca", "permission", "permissions", "notification", "notifications", "battery", "pause", "not replying", "auto reply"),
                    reply = "If your question is about the DCA app: it replies to messages automatically from notifications (it uses the \"Notification access\" permission), and its Chat Bot is powered by these same FAQs.\n\nIf a reply did not arrive, check that the app is in your DCA list, that it is enabled, and that the Chat Bot is on in its edit screen.\n\nFor deeper technical topics, type \"talk to a person\".",
                ),
            ),
        )

        /** Opciones rápidas que se adjuntan cuando el bot no encuentra respuesta. */
        const val FALLBACK_OPTIONS: Int = 3
    }
}

/** Idiomas soportados por el chat bot de mensajería. */
enum class ChatBotLocale { ES, EN }