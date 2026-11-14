package com.datecomp.dca.chatbot

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
 * Persistido en el backend local de la app (ChatBotStore): editable en
 * caliente y sin migraciones.
 *
 * Modo de operación (definido por DateComp): sin horario fijo; se trabaja
 * por demanda de los clientes. Cuando el bot responde es porque el turno
 * del cliente ya abrió, y se le atiende hasta cumplir su solicitud.
 */
@Serializable
data class ChatBotKnowledge(
    val botName: String = "Asistente DateComp",
    val greeting: String = "¡Hola! 👋 Soy el asistente de DateComp. Puedo ayudarte con servicios, precios, tiempos, apps, tiendas online, horarios y más.\n\nEscríbeme tu pregunta o dime qué te interesa (ej. \"precios\", \"apps\", \"contacto\").",
    val byeReply: String = "¡Con gusto! 😊 Cuando necesites algo más, aquí estaré.",
    val notFound: String = "Perdona, eso no está dentro de lo que puedo responder como asistente. 🙁 Pero sí tengo respuesta para estas preguntas frecuentes — escribe el número o el tema que te interese:",
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
         * Menú español: 16 categorías (13 del chat bot web + "nuevo proyecto"
         * — la intención comercial principal — + horario de atención por
         * demanda + soporte básico de la app DCA), con vocabulario ampliado
         * (verbos, sinónimos y coloquialismos) y textos reescritos para
         * mensajería. "nuevo_proyecto" va primero: empata y gana por orden.
         */
        val DEFAULT_MENU: List<ChatBotNode> = listOf(
            ChatBotNode(
                id = "nuevo_proyecto",
                label = "🚀 Quiero un proyecto para mi empresa o negocio",
                keywords = listOf(
                    "nuevo proyecto", "proyecto para mi empresa", "proyecto para mi negocio",
                    "software para mi empresa", "software para mi negocio",
                    "pagina para mi negocio", "pagina para mi empresa", "pagina para mi emprendimiento",
                    "app para mi empresa", "app para mi negocio", "app para mi negocio",
                    "sistema para mi empresa", "sistema para mi negocio",
                    "programa para mi empresa", "programa para mi negocio",
                    "software a medida", "app a medida", "pagina a medida", "sistema a medida",
                    "tengo una idea", "tengo un proyecto", "tengo una idea de negocio",
                    "idea de software", "idea de app", "idea de negocio",
                    "necesito una app", "necesito una pagina", "necesito un software",
                    "necesito una pagina para mi negocio", "quiero una app", "quiero una pagina",
                    "quiero un software", "quiero una aplicacion", "quiero una pagina web",
                    "hacer una app", "hacer una pagina", "hacer un software",
                    "crear una app", "crear una pagina", "crear una aplicacion", "crear un software",
                    "desarrollar una app", "desarrollar una pagina", "desarrollar un software",
                    "desarrollo a medida", "programar una app",
                    "software", "programa", "programacion", "desarrollo", "aplicacion a medida",
                    "automatizar", "automatizar mi empresa", "automatizar mi negocio",
                    "digitalizar", "digitalizar mi negocio", "digitalizar mi empresa",
                    "emprendimiento", "mi negocio", "mi empresa", "empresarial",
                    "cotizar", "cotizacion gratis", "pedir cotizacion", "solicitar cotizacion",
                    "cotizacion gratis", "presupuesto gratis",
                    "contratar", "contratarlos", "contratarlos",
                    "proyecto nuevo", "empezar un proyecto", "hacer un proyecto",
                    "hagan una app", "hagan una pagina", "me hagan una app", "me podrian hacer",
                    "hacen a medida", "hacen apps a medida", "hacen paginas a medida",
                    "dejo mi informacion", "dejar mi informacion", "dejar mis datos",
                    "dejo mis datos", "mi informacion", "quiero dejar", "les dejo",
                    "mi proyecto es", "consulta", "seguimiento", "que me contacten",
                    "contactenme", "quiero asesoría", "quiero asesoria",
                ),
                reply = "¡Perfecto! Soy un asistente automático: respondo preguntas puntuales sobre DateComp, y para tu proyecto te paso con el equipo. 😊\n\nDeja aquí mismo la información de tu proyecto — qué necesitas, para qué negocio es y qué te gustaría lograr — y un miembro de nuestro equipo la revisará y te dará seguimiento a tu consulta para enviarte la cotización gratis, sin compromiso.\n\nNada de tecnicismos: hablamos tu idioma y te respaldamos en todo el proceso.",
            ),
            ChatBotNode(
                id = "servicios",
                label = "💻 ¿Qué servicios ofrecen?",
                keywords = listOf(
                    "servicio", "servicios", "pagina web", "web", "que hacen", "a que se dedican",
                    "que ofrecen", "ofrecen", "trabajan", "dedican",
                ),
                reply = "Creación de aplicaciones web o nativas para teléfono Android y computadora, según tus necesidades.\n\nTrabajamos con datos locales y bases de datos, con herramientas tanto para empresas como para usuarios individuales y emprendedores.\n\n¿Tienes un proyecto en mente? Deja aquí la información de tu proyecto y un miembro del equipo te dará seguimiento, o dime cuál te interesa y te cuento más.",
            ),
            ChatBotNode(
                id = "precios",
                label = "💰 ¿Cuánto cuesta un proyecto?",
                keywords = listOf(
                    "precio", "precios", "costo", "costos", "cuanto cuesta", "cuanto sale",
                    "cuanto me costaria", "cuanto seria", "cuanto valdria", "cuanto cobran",
                    "cuanto cobran por", "tarifa", "valor", "presupuesto", "cotizacion",
                    "cuanto por una", "invertir", "el minimo", "minimo", "cobran", "cobran por",
                ),
                reply = "Desde 400 USD por una landing page con seguridad, dominio personalizado (la titularidad del dominio no está incluida), funciones y el estilo que prefieras.\n\nIncluye soporte gratis por 1 mes al finalizar.\n\nPara tiendas, apps o sistemas a medida la cotización depende del alcance. Deja aquí mismo la información de tu proyecto y un miembro del equipo te dará seguimiento con la cotización exacta, gratis y sin compromiso.",
            ),
            ChatBotNode(
                id = "tiempo",
                label = "⏱️ ¿Cuánto tarda el desarrollo?",
                keywords = listOf(
                    "tiempo", "tarda", "tardan", "tardo", "demora", "cuanto dura", "plazo",
                    "entrega", "entregar", "rapido", "30 dias", "semanas", "cuando estaria listo",
                    "cuando lo tendria", "fecha", "tardaria",
                ),
                reply = "Depende de la magnitud del proyecto: podemos entregarlo incluso dentro de los 30 días posteriores a su aceptación.\n\nTe damos un cronograma claro al iniciar y avisamos de cada avance.",
            ),
            ChatBotNode(
                id = "tiendas",
                label = "🛒 ¿Pueden crear mi tienda online?",
                keywords = listOf(
                    "tienda", "tiendas", "ecommerce", "e-commerce", "vender", "ventas",
                    "carrito", "pagos online", "vender online", "vender por internet",
                    "tienda virtual", "catalogo", "cobrar en linea", "vender productos",
                ),
                reply = "¡Correcto! Podemos crear tu tienda online, o incluso mejorarla si ya tienes una web desactualizada, con nosotros, siempre a tu disposición.\n\nCarrito, métodos de pago, gestión de productos y pedidos, y diseño a tu marca. ¿Quieres una tienda a medida o prefieres la plataforma Stores? Puedes conocer Stores en datecomp.com, o deja tu información y un miembro del equipo te orientará con la mejor opción.",
            ),
            ChatBotNode(
                id = "apps",
                label = "📱 ¿Hacen apps para celular? ¿Para iPhone?",
                keywords = listOf(
                    "app", "apps", "aplicacion", "aplicaciones", "movil", "celular",
                    "android", "iphone", "ios", "play store", "aplicaciones moviles",
                    "aplicacion movil", "desarrollan apps", "hacen apps", "app android", "app iphone",
                ),
                reply = "¡Sí! Desarrollamos apps para Android y aplicaciones web para computadora. Para iPhone hoy creamos aplicaciones web (PWA), y estamos sumando apps nativas iOS pronto.\n\n¿La quieres para tu empresa o es un proyecto personal? Deja la información aquí y te cotizamos.",
            ),
            ChatBotNode(
                id = "tecnologias",
                label = "🚀 ¿Qué tecnologías usan?",
                keywords = listOf(
                    "tecnologia", "tecnologias", "lenguaje", "lenguajes", "stack", "herramientas",
                    "framework", "frameworks", "firebase", "node", "kotlin", "que usan",
                ),
                reply = "Las tecnologías están publicadas en datecomp.com: HTML & CSS, JavaScript, TypeScript, Node.js, Kotlin, Android, PWA, Firebase, PostgreSQL, Docker, AWS y Git.\n\nAdemás utilizamos AWS y GCP como infraestructura segura para tus datos.\n\nElegimos la tecnología según tu proyecto: siempre la más confiable y fácil de mantener.",
            ),
            ChatBotNode(
                id = "soporte",
                label = "🛡️ ¿Dan soporte y mantenimiento?",
                keywords = listOf(
                    "soporte", "soportes", "mantenimiento", "actualizacion", "actualizaciones",
                    "monitoreo", "respaldo", "soporte tecnico", "ayuda tecnica", "reparar",
                    "arreglar", "error en mi proyecto", "falla mi sistema",
                ),
                reply = "Sí, siempre que terminamos un proyecto ofrecemos soporte gratuito por 1 mes posterior a la entrega del proyecto.\n\nAdemás ofrecemos planes de mantenimiento continuo: actualizaciones y monitoreo.\n\nSi tu pregunta es sobre la app DCA, escribe \"DCA\" y te explico.",
            ),
            ChatBotNode(
                id = "hosting",
                label = "🗄️ ¿Me ayudan con hosting y bases de datos?",
                keywords = listOf(
                    "hosting", "servidor", "servidores", "base de datos", "bases de datos",
                    "alojamiento", "dominio", "dominios", "ssl", "migrar", "migracion",
                ),
                reply = "¡Correcto! Podemos ayudarte con esto y más; incluso si tienes alguna preferencia, podemos hacer que tu proyecto cumpla con ello.\n\nAlojamiento, bases de datos seguras, SSL, dominio y migraciones desde tu proveedor actual.",
            ),
                ChatBotNode(
                    id = "proyectos",
                    label = "🌟 ¿Qué proyectos han hecho?",
                    keywords = listOf(
                        "proyectos hechos", "proyectos realizados", "portafolio", "portfolio",
                        "ejemplos", "experiencia", "trabajos hechos", "que han hecho", "casos de exito",
                        "muestras", "demostraciones", "referencias", "proyectos", "sus proyectos",
                        "cuantos proyectos", "sus proyectos", "demo", "ver una demo", "demostracion",
                    ),
                reply = "Están publicados en datecomp.com, en la sección de proyectos (SP-CASH, Alarma zZz, Stores.datecomp.com); los demás son privados, protegidos por seguridad de clientes.\n\nSeguimos creciendo gracias a ustedes.\n\n¿Quieres empezar el tuyo? Deja aquí la información de tu proyecto y un miembro del equipo te dará seguimiento.",
            ),
            ChatBotNode(
                id = "empezar",
                label = "🤝 ¿Cómo empiezo mi proyecto?",
                keywords = listOf(
                    "empezar", "iniciar", "comenzar", "proceso", "como empiezo", "como iniciar",
                    "como comenzar", "como contratar", "como trabajar", "pasos", "requisitos",
                    "que necesito para", "primero", "que sigue",
                ),
                reply = "Muy fácil:\n\n1. Deja aquí mismo la información de tu proyecto (o escríbenos a admin@datecomp.com).\n2. Un miembro del equipo la revisará y te dará seguimiento a tu consulta.\n3. Si tu proyecto es aceptado, recibirás la cotización y el cronograma para arrancar.\n\nNada de tecnicismos: hablamos tu idioma y te respaldamos en todo el proceso.",
            ),
            ChatBotNode(
                id = "contacto",
                label = "📞 ¿Cómo puedo contactarlos?",
                keywords = listOf(
                    "contacto", "contactar", "contactarlos", "telefono", "numero de telefono",
                    "cual es su telefono", "cual es su numero", "su numero", "que numero",
                    "numero", "email", "correo", "correo electronico", "whatsapp",
                    "ubicacion", "donde estan", "donde queda",
                    "pagina web de ustedes", "su pagina web", "cual es su pagina web",
                    "que pagina web", "su sitio web", "cual es su sitio", "sitio web de ustedes",
                    "la pagina web", "su web", "cual es su web", "que web tienen",
                    "url", "link de su web", "link", "enlace", "pagina de ustedes",
                    "su correo", "cual es su correo", "correo de ustedes", "email de ustedes",
                    "dueño", "dueno", "propietario", "hablar con el dueño", "hablar con el dueno",
                    "hablar con alguien de datecomp", "con quien hablo", "recursos humanos",
                ),
                reply = "¡Claro! Te comparto los canales de DateComp: 😊\n\n• Web: https://datecomp.com\n• Correo: admin@datecomp.com\n• WhatsApp (aquí mismo): +1 (809) 517-9136\n• Instagram: @date_comp\n• LinkedIn: DateComp\n\nEste chat ya es nuestro canal oficial: cuando lo necesites, escribe \"hablar con una persona\" y un miembro del equipo continuará contigo aquí.",
            ),
            ChatBotNode(
                id = "pagos",
                label = "💳 ¿Qué métodos de pago aceptan?",
                keywords = listOf(
                    "pago", "pagos", "metodo", "metodos", "tarjeta", "tarjetas", "transferencia",
                    "transferencias", "paypal", "abono", "abonos", "financiamiento", "stripe",
                    "etapas", "avances", "cuotas", "como pago", "forma de pago", "formas de pago",
                    "zelle", "efectivo",
                ),
                reply = "Los pagos se realizan a través de Stripe.\n\nNormalmente dividimos el pago en etapas: un adelanto para comenzar y el resto según avances. Cuéntanos tu idea y te damos los detalles en la cotización.",
            ),
            ChatBotNode(
                id = "seguridad",
                label = "🔒 ¿Mis datos y mi web están seguros?",
                keywords = listOf(
                    "seguridad", "seguro", "datos", "privacidad", "proteccion", "hacker",
                    "hackean", "roban informacion", "confidencial", "backup", "copias",
                ),
                reply = "Correcto, utilizamos AWS y GCP como infraestructura segura para tus datos.\n\nAdemás: certificados SSL, prácticas de desarrollo seguras y monitoreo continuo.\n\nTu información y la de tus clientes están protegidas.",
            ),
            ChatBotNode(
                id = "horario",
                label = "🕐 ¿Tienen horario de atención?",
                keywords = listOf(
                    "horario", "horarios", "hora de atencion", "atienden", "disponible",
                    "disponibles", "abierto", "abren", "cierran", "cerrado", "24 horas",
                    "turno", "cuando responden", "cuando atienden", "esta activo",
                    "estan activos", "que horario tienen", "atencion al publico",
                ),
                reply = "No cerramos a una hora fija: trabajamos por demanda de los clientes, así que podemos estar activos a cualquier hora.\n\nSi te respondemos, es porque tu turno ya está abierto, y te atendemos hasta cumplir tu solicitud o proyecto.",
            ),
            ChatBotNode(
                id = "dca",
                label = "🤖 ¿Cómo funciona la app DCA?",
                keywords = listOf(
                    "dca", "permiso", "permisos", "notificacion", "notificaciones",
                    "no responde", "no respondio", "pausar", "bateria", "acceso",
                    "autorespuesta", "auto respuesta", "como funciona dca", "que es dca",
                ),
                reply = "Si tu pregunta es sobre la app DCA: responde mensajes de forma automática desde las notificaciones (usa el permiso \"Acceso a notificaciones\"), y su Chat Bot usa estas mismas preguntas frecuentes.\n\nSi una respuesta no llegó, revisa que la app esté en tu lista de DCA, que esté activada y que el Chat Bot esté encendido en su pantalla de edición.\n\nPara temas técnicos más profundos, escribe \"hablar con una persona\".",
            ),
            ChatBotNode(
                id = "sobre_nosotros",
                label = "🏢 ¿Quiénes son y cuánta experiencia tienen?",
                keywords = listOf(
                    "sobre ustedes", "sobre datecomp", "quienes son", "quienes son ustedes",
                    "empresa seria", "son confiables", "confiables", "cuanto tiempo llevan",
                    "llevan en el mercado", "tiempo en el mercado", "desde cuando",
                    "cuando empezaron", "años de experiencia", "trayectoria",
                    "cuantos clientes", "cuantos proyectos", "testimonios", "recomendaciones",
                    "redes sociales", "instagram", "linkedin",
                ),
                reply = "DateComp lleva 1 año desarrollando para usuarios privados y ahora abre sus servicios al público. 🚀\n\nCreemos en el trabajo bien hecho y en resultados que se ven. Puedes conocernos mejor en nuestras redes: Instagram (@date_comp) y LinkedIn (DateComp).\n\n¿Quieres ver ejemplos reales? Escríbenos y te compartimos referencias.",
            ),
            ChatBotNode(
                id = "garantia",
                label = "🛡️ ¿Hay garantía o contrato?",
                keywords = listOf(
                    "garantia", "garantias", "contrato", "reembolso", "devolucion",
                    "politica de reembolso", "que pasa si no me gusta", "revisiones",
                    "cambios despues de entregar",
                ),
                reply = "Al entregar tu proyecto ofrecemos 1 mes gratis de soporte para que todo funcione a tu gusto.\n\nSi necesitas cambios o ajustes posteriores, cuéntanos tu caso aquí mismo y un miembro del equipo te dará seguimiento.",
            ),
            ChatBotNode(
                id = "servicios_extra",
                label = "🎨 ¿Hacen SEO, logos o marketing digital?",
                keywords = listOf(
                    "seo", "posicionamiento", "posicionamiento web", "logo", "logos",
                    "marca", "diseño de marca", "marketing", "marketing digital",
                    "inteligencia artificial", "ia", "chatbot", "chatbots",
                ),
                reply = "Además de páginas web, apps y sistemas, trabajamos con SEO y diseño de logos cuando lo solicitas dentro de tu proyecto.\n\nPara otros servicios como marketing digital o inteligencia artificial, cuéntanos tu idea y el equipo te dirá exactamente qué podemos hacer por ti.",
            ),
            ChatBotNode(
                id = "facturacion",
                label = "🧾 ¿Emite facturas o recibos?",
                keywords = listOf(
                    "factura", "facturas", "recibo", "recibos", "comprobante",
                    "moneda", "dop", "usd", "dolares", "pesos", "boleta",
                ),
                reply = "Sí, podemos ofrecerte recibos por correo electrónico.\n\nLos pagos se realizan a través de Stripe y trabajamos en dos monedas: DOP (pesos) y USD (dólares).",
            ),
            ChatBotNode(
                id = "modalidad",
                label = "🌎 ¿Atienden de forma remota o presencial?",
                keywords = listOf(
                    "presencial", "remoto", "a distancia", "oficina", "ubicados",
                    "donde estan ubicados", "pais", "paises", "atienden en mi pais",
                    "republica dominicana", "estados unidos", "online o presencial",
                    "tienen oficina", "donde queda su oficina", "internacional",
                    "internacionales", "otro pais", "mi pais",
                ),
                reply = "Trabajamos 100% de forma remota, así que podemos atenderte sin importar dónde estés.\n\nActualmente atendemos clientes en República Dominicana y Estados Unidos.\n\n¿Estás en otro país? Escríbenos y confirmamos si podemos atenderte.",
            ),
            ChatBotNode(
                id = "urgencia",
                label = "⚡ Necesito mi proyecto urgente",
                keywords = listOf(
                    "urgente", "urgencia", "emergencia", "lo necesito ya", "para ya",
                    "cuanto antes", "apuro", "prisa", "expres",
                ),
                reply = "Entendemos la urgencia. ⏱️ Depende del alcance: podemos priorizar tu proyecto y entregarlo en los plazos más cortos que manejamos (incluso menos de 30 días).\n\nDeja aquí la información de tu proyecto con la fecha que necesitas y un miembro del equipo te dará seguimiento de inmediato.",
            ),
            ChatBotNode(
                id = "reclamos",
                label = "😟 Quiero hacer un reclamo",
                keywords = listOf(
                    "reclamo", "reclamos", "queja", "quejas", "no me gusta",
                    "inconforme", "molesto", "problema con el proyecto", "descontento",
                ),
                reply = "Lamentamos mucho tu inconveniente. 🙏\n\nEscríbenos los detalles a admin@datecomp.com y un miembro del equipo revisará tu caso para darte una solución.",
            ),
            ChatBotNode(
                id = "empleo",
                label = "💼 ¿Están contratando o hacen alianzas?",
                keywords = listOf(
                    "empleo", "vacante", "vacantes", "contratando", "contratacion",
                    "alianzas", "alianza", "socio", "colaboracion", "curriculum",
                    "hoja de vida", "postular", "busco trabajo", "busco empleo",
                    "quiero trabajar", "trabajar con ustedes",
                ),
                reply = "¡Qué gusto que quieras sumarte! 💼\n\nEnvíanos tu propuesta o CV a admin@datecomp.com y el equipo la revisará.\n\nPara alianzas o colaboraciones, escríbenos al mismo correo.",
            ),
        )

        /** Textos completos en inglés (equivalentes y validados en pareja con ES). */
        val DEFAULT_EN: ChatBotTexts = ChatBotTexts(
            botName = "DateComp Assistant",
            greeting = "Hi! 👋 I am the DateComp assistant. I can help you with services, pricing, timelines, apps, online stores, hours and more.\n\nType your question or tell me what interests you (e.g. \"pricing\", \"apps\", \"contact\").",
            byeReply = "You are welcome! 😊 Whenever you need anything else, I will be here.",
            notFound = "Sorry, that is not something I can answer as an assistant. 🙁 But I do have answers for these FAQs — type the number or topic you are interested in:",
            humanReply = "Sure! A person will reply to you right here in this chat. 👋 We work on demand: as soon as your turn opens, we assist you in this chat until your request or project is fulfilled.",
            menu = listOf(
                ChatBotNode(
                    id = "nuevo_proyecto",
                    label = "🚀 I want a project for my company or business",
                    keywords = listOf(
                        "new project", "project for my company", "project for my business",
                        "software for my business", "software for my company",
                        "page for my business", "app for my business", "app for my company",
                        "system for my business", "system for my company",
                        "custom software", "custom app", "custom website", "custom page",
                        "custom system", "tailor made", "made to order",
                        "i have an idea", "i have a project", "business idea", "app idea",
                        "software idea", "i have an idea",
                        "need an app", "need a website", "need a page", "need software",
                        "want an app", "want a website", "want a page", "want software",
                        "make an app", "make a website", "make me an app", "make me a page",
                        "build an app", "build a website", "build software",
                        "create an app", "create a website", "develop an app",
                        "develop software", "develop an app", "program an app",
                        "automate", "automate my business", "automate my company",
                        "digitalize", "go digital", "my business", "my company",
                        "for my company", "for my business", "entrepreneur",
                        "get a quote", "request a quote", "free quote", "quote me",
                        "hire you",
                        "can you build", "can you make", "can you create", "can you develop",
                        "build for me", "make for me",
                        "leave my information", "my information", "my project is",
                        "follow up", "contact me", "reach out to me", "consultation",
                        "leave my info", "my info", "information", "info",
                    ),
                    reply = "Perfect! I am an automatic assistant: I answer specific questions about DateComp, and for your project I will hand you over to the team. 😊\n\nLeave the information about your project right here — what you need, what business it is for and what you want to achieve — and a member of our team will review it and follow up on your request to send you a free, no-obligation quote.\n\nNo jargon: we speak your language and back you up through the whole process.",
                ),
                ChatBotNode(
                    id = "servicios",
                    label = "💻 What services do you offer?",
                    keywords = listOf(
                        "service", "services", "website", "web", "what do you do",
                        "what do you offer", "what do you build",
                    ),
                    reply = "We build web applications or native apps for Android phones and computers, according to your needs.\n\nWe work with local data and databases, with tools for businesses, individual users and entrepreneurs.\n\nHave a project in mind? Leave your project information right here and a member of the team will follow up, or tell me which one interests you and I can tell you more.",
                ),
                ChatBotNode(
                    id = "precios",
                    label = "💰 How much does a project cost?",
                    keywords = listOf(
                        "price", "prices", "cost", "costs", "how much", "how much is it",
                        "how much would it be", "how much do you charge", "rate", "rates",
                        "budget", "quote", "estimate", "pricing", "invest", "minimum",
                        "the minimum", "charge", "charges",
                    ),
                    reply = "From 400 USD for a landing page with security, custom domain (domain ownership not included), features and the style you prefer.\n\nIt includes free support for 1 month after delivery.\n\nFor stores, apps or custom systems the quote depends on scope. Leave the information about your project right here and a member of the team will follow up with the exact quote, free and with no obligation.",
                ),
                ChatBotNode(
                    id = "tiempo",
                    label = "⏱️ How long does development take?",
                    keywords = listOf(
                        "time", "long", "duration", "deadline", "delivery", "deliver", "weeks",
                        "fast", "30 days", "when will it be ready", "when ready", "timeline",
                        "how long would it take", "turnaround",
                    ),
                    reply = "Depending on the scope of the project, we can deliver your project even within 30 days after accepting it.\n\nWe give you a clear schedule at the start and keep you updated on progress.",
                ),
                ChatBotNode(
                    id = "tiendas",
                    label = "🛒 Can you build my online store?",
                    keywords = listOf(
                        "store", "shops", "shop", "ecommerce", "e-commerce", "sell", "sales",
                        "selling", "cart", "online store", "storefront", "catalog", "sell online",
                        "sell products", "take payments",
                    ),
                    reply = "Correct! We can build your online store, or even improve it if you already have an outdated website, always at your service.\n\nCart, payment methods, product and order management, and a design for your brand. Do you want a custom store or the Stores platform? You can check Stores at datecomp.com, or leave your information and a member of the team will guide you to the best option.",
                ),
                ChatBotNode(
                    id = "apps",
                    label = "📱 Do you build mobile apps? For iPhone?",
                    keywords = listOf(
                        "app", "apps", "application", "applications", "mobile", "android",
                        "iphone", "ios", "play store", "phone app", "smartphone",
                    ),
                    reply = "Yes! We build Android apps and web applications for computers. For iPhone we currently build web apps (PWAs), and native iOS apps are coming soon.\n\nIs it for your business or a personal project? Leave the info here and we will quote it.",
                ),
                ChatBotNode(
                    id = "tecnologias",
                    label = "🚀 What technologies do you use?",
                    keywords = listOf(
                        "technology", "technologies", "stack", "tools", "framework",
                        "frameworks", "firebase", "node", "kotlin", "language", "languages",
                        "what do you use",
                    ),
                    reply = "The technologies are published on datecomp.com: HTML & CSS, JavaScript, TypeScript, Node.js, Kotlin, Android, PWA, Firebase, PostgreSQL, Docker, AWS and Git.\n\nWe also use AWS and GCP as secure infrastructure for your data.\n\nWe pick the right technology for your project: always the most reliable and easy to maintain.",
                ),
                ChatBotNode(
                    id = "soporte",
                    label = "🛡️ Do you provide support and maintenance?",
                    keywords = listOf(
                        "support", "maintenance", "update", "updates", "monitoring", "backup",
                        "help", "technical help", "fix", "bug", "my system fails",
                        "error in my project",
                    ),
                    reply = "Yes, whenever we finish a project we offer free support for 1 month after delivery.\n\nWe also offer ongoing maintenance plans: updates and monitoring.\n\nIf your question is about the DCA app, type \"DCA\" and I will explain.",
                ),
                ChatBotNode(
                    id = "hosting",
                    label = "🗄️ Can you help with hosting and databases?",
                    keywords = listOf(
                        "hosting", "server", "servers", "database", "databases", "host my site",
                        "domain", "domains", "ssl", "migrate", "migration",
                    ),
                    reply = "Correct! We can help you with this and more; even if you have a preference, we can make your project meet it.\n\nHosting, secure databases, SSL, domain and migrations from your current provider.",
                ),
                ChatBotNode(
                    id = "proyectos",
                    label = "🌟 What projects have you built?",
                    keywords = listOf(
                        "past projects", "previous work", "portfolio", "examples",
                        "experience", "case studies", "showcase", "references", "samples",
                        "what have you built", "what have you made", "projects",
                        "show me your work", "your projects", "what have you done",
                        "demo", "see a demo", "demonstration",
                    ),
                    reply = "They are published on datecomp.com, in the projects section (SP-CASH, Alarma zZz, Stores.datecomp.com); the rest are private projects, protected by client security.\n\nWe keep growing thanks to you.\n\nWant to start yours? Leave your project information right here and a member of the team will follow up.",
                ),
                ChatBotNode(
                    id = "empezar",
                    label = "🤝 How do I start my project?",
                    keywords = listOf(
                        "start", "begin", "get started", "process", "how do i start",
                        "how to start", "how to begin", "steps", "requirements",
                        "what do i need", "first step", "what next", "hire",
                    ),
                    reply = "Very easy:\n\n1. Leave the information about your project right here (or email us at admin@datecomp.com).\n2. A member of the team will review it and follow up on your request.\n3. If your project is accepted, you will receive the quote and timeline to kick off.\n\nNo jargon: we speak your language and back you up through the whole process.",
                ),
                ChatBotNode(
                    id = "contacto",
                    label = "📞 How can I contact you?",
                    keywords = listOf(
                        "contact", "contact you", "phone", "email", "e-mail", "mail",
                        "whatsapp", "location", "where are you",
                        "owner", "talk to the owner",
                    ),
                    reply = "You are already in contact with us! 😊 This is the official chat of the DateComp assistant, and a person can continue here whenever you need.\n\nYou can also email us: admin@datecomp.com",
                ),
                ChatBotNode(
                    id = "pagos",
                    label = "💳 What payment methods do you accept?",
                    keywords = listOf(
                        "payment", "payments", "pay", "card", "cards", "transfer", "transfers",
                        "paypal", "deposit", "deposits", "financing", "stripe", "installments",
                        "how do i pay", "payment method", "payment methods", "cash", "zelle",
                    ),
                    reply = "Payments are made via Stripe.\n\nWe usually split payment into stages: a deposit to begin and the rest as we progress. Tell us your idea and we will give you the details in the quote.",
                ),
                ChatBotNode(
                    id = "seguridad",
                    label = "🔒 Is my data and my website safe?",
                    keywords = listOf(
                        "security", "secure", "safe", "data", "privacy", "protection",
                        "hacker", "hackers", "hacked", "steal information", "confidential",
                        "backups",
                    ),
                    reply = "Correct, we use AWS and GCP as secure infrastructure for your data.\n\nPlus: SSL certificates, secure development practices and continuous monitoring.\n\nYour information and your customers' information are protected.",
                ),
                ChatBotNode(
                    id = "horario",
                    label = "⏰ What are your business hours?",
                    keywords = listOf(
                        "schedule", "hours", "business hours", "available", "availability",
                        "open now", "shift", "shifts", "demand", "working hours",
                        "when do you reply", "when do you answer", "are you active",
                        "what are your hours", "opening hours",
                    ),
                    reply = "We do not close at a fixed time: we work on customer demand, so we can be active at any hour.\n\nIf we reply to you, it is because your turn is already open, and we assist you until your request or project is fulfilled.",
                ),
                ChatBotNode(
                    id = "dca",
                    label = "🤖 How does the DCA app work?",
                    keywords = listOf(
                        "dca", "permission", "permissions", "notification", "notifications",
                        "battery", "pause", "not replying", "auto reply", "how does dca work",
                        "what is dca",
                    ),
                    reply = "If your question is about the DCA app: it replies to messages automatically from notifications (it uses the \"Notification access\" permission), and its Chat Bot is powered by these same FAQs.\n\nIf a reply did not arrive, check that the app is in your DCA list, that it is enabled, and that the Chat Bot is on in its edit screen.\n\nFor deeper technical topics, type \"talk to a person\".",
                ),
                ChatBotNode(
                    id = "sobre_nosotros",
                    label = "🏢 Who are you and how much experience do you have?",
                    keywords = listOf(
                        "about you", "about datecomp", "who are you", "who are you guys",
                        "serious company", "trustworthy", "reliable", "how long have you",
                        "in the market", "how many clients", "how many projects",
                        "testimonials", "reviews", "social media", "instagram", "linkedin",
                    ),
                    reply = "DateComp has been developing for private users for 1 year and is now open to the public. 🚀\n\nWe believe in well-done work and visible results. You can get to know us better on our social media: Instagram (@date_comp) and LinkedIn (DateComp).\n\nWant to see real examples? Write to us and we will share references.",
                ),
                ChatBotNode(
                    id = "garantia",
                    label = "🛡️ Do you offer a guarantee or contract?",
                    keywords = listOf(
                        "guarantee", "guarantees", "contract", "refund", "return",
                        "refund policy", "what if i don't like it", "revisions",
                        "changes after delivery",
                    ),
                    reply = "When we deliver your project we offer 1 free month of support so everything works to your liking.\n\nIf you need later changes or adjustments, tell us your case right here and a member of the team will follow up.",
                ),
                ChatBotNode(
                    id = "servicios_extra",
                    label = "🎨 Do you do SEO, logos or digital marketing?",
                    keywords = listOf(
                        "seo", "ranking", "logo", "logos", "brand", "brand design",
                        "marketing", "digital marketing", "artificial intelligence",
                        "ai", "chatbot", "chatbots",
                    ),
                    reply = "Besides web pages, apps and systems, we work with SEO and logo design when you request them within your project.\n\nFor other services like digital marketing or artificial intelligence, tell us your idea and the team will tell you exactly what we can do for you.",
                ),
                ChatBotNode(
                    id = "facturacion",
                    label = "🧾 Do you issue invoices or receipts?",
                    keywords = listOf(
                        "invoice", "invoices", "receipt", "receipts", "proof of payment",
                        "currency", "dop", "usd", "dollars", "pesos", "bill",
                    ),
                    reply = "Yes, we can send you receipts by email.\n\nPayments are made via Stripe and we work in two currencies: DOP (pesos) and USD (dollars).",
                ),
                ChatBotNode(
                    id = "modalidad",
                    label = "🌎 Do you work remotely or in person?",
                    keywords = listOf(
                        "in person", "remote", "remotely", "office", "located",
                        "where are you located", "country", "countries", "serve my country",
                        "dominican republic", "united states", "online or in person",
                        "international", "another country", "my country", "countries served",
                    ),
                    reply = "We work 100% remotely, so we can serve you no matter where you are.\n\nWe currently serve clients in the Dominican Republic and the United States.\n\nAre you in another country? Write to us and we will confirm if we can serve you.",
                ),
                ChatBotNode(
                    id = "urgencia",
                    label = "⚡ I need my project urgently",
                    keywords = listOf(
                        "urgent", "urgency", "emergency", "need it now", "asap",
                        "rush", "in a hurry", "express", "right away",
                    ),
                    reply = "We understand the urgency. ⏱️ It depends on the scope: we can prioritize your project and deliver it within the shortest timelines we manage (even under 30 days).\n\nLeave your project information right here with the date you need and a member of the team will follow up right away.",
                ),
                ChatBotNode(
                    id = "reclamos",
                    label = "😟 I want to file a complaint",
                    keywords = listOf(
                        "complaint", "complaints", "issue", "not happy", "unhappy",
                        "upset", "dissatisfied", "problem with the project",
                    ),
                    reply = "We are very sorry for the inconvenience. 🙏\n\nWrite the details to admin@datecomp.com and a member of the team will review your case to give you a solution.",
                ),
                ChatBotNode(
                    id = "empleo",
                    label = "💼 Are you hiring or open to partnerships?",
                    keywords = listOf(
                        "job", "jobs", "vacancy", "vacancies", "hiring", "hiring now",
                        "partnership", "partnerships", "partner", "collaboration",
                        "resume", "cv", "apply", "looking for a job", "looking for work",
                        "work with you",
                    ),
                    reply = "Great that you want to join us! 💼\n\nSend us your proposal or CV to admin@datecomp.com and the team will review it.\n\nFor partnerships or collaborations, write to the same email.",
                ),
            ),
        )

        /** Opciones rápidas que se adjuntan cuando el bot no encuentra respuesta. */
        const val FALLBACK_OPTIONS: Int = 7

        /**
         * Categorías que se sugieren cuando el bot no entiende el mensaje,
         * en orden de utilidad (independiente del orden del menú para que
         * el fallback muestre siempre lo más común: negocio, precios,
         * contacto, apps…).
         */
        val FALLBACK_SUGGESTION_IDS: List<String> = listOf(
            "servicios", "precios", "nuevo_proyecto", "tiempo", "apps",
            "tiendas", "contacto", "horario", "garantia",
        )
    }
}

/** Idiomas soportados por el chat bot de mensajería. */
enum class ChatBotLocale { ES, EN }
