package com.datecomp.dca.autoreply

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.datecomp.dca.chatbot.ChatBot
import com.datecomp.dca.chatbot.ChatBotKnowledge
import com.datecomp.dca.chatbot.ChatBotStore
import com.datecomp.dca.targets.AppTarget
import com.datecomp.dca.targets.TargetsStore
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Servicio con permiso especial NotificationListener. Única fuente de
 * eventos: entrega notificaciones de todas las apps instaladas.
 *
 * Rendimiento del hot path (onNotificationPosted):
 * - Skip temprano sincrónico para apps sin configurar: ni siquiera se lanza
 *   una coroutine ni se toca DataStore.
 * - Cache en memoria de los stores: evita lecturas de disco/JSON por
 *   notificación; se refresca solo al cambiar los datos.
 * - Lookup del target en O(1) por mapa de paquetes.
 */
class AutoReplyNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var targetsStore: TargetsStore
    private lateinit var chatBotStore: ChatBotStore

    /** Última firma respondida, POR APP (no global): evita bloqueos cruzados. */
    private val lastSeenKeys = ConcurrentHashMap<String, String>()

    /** Cache en memoria: packageName -> target. Fuente de verdad: DataStore. */
    private val targetCache = ConcurrentHashMap<String, AppTarget>()
    private val cacheLock = Any()

    /**
     * Conocimiento del chat bot, cacheado al vuelo. Se reemplaza la
     * referencia completa (inmutable después de la publicación), por lo que
     * en el hot path nunca se toca DataStore.
     */
    @Volatile
    private var chatBotKnowledge: ChatBotKnowledge = ChatBotKnowledge()

    /** Un mutex por paquete: serializa la decisión+envío por app. */
    private val packageMutexes = ConcurrentHashMap<String, Mutex>()

    override fun onCreate() {
        super.onCreate()
        android.util.Log.i("DCA", "Listener conectado: servicio creado")
        targetsStore = TargetsStore(applicationContext)
        chatBotStore = ChatBotStore(applicationContext)
        ReplyNotifier.ensureChannel(applicationContext)
        // Warm-up del cache al conectar el listener: una única lectura de
        // disco al inicio; después el hot path es puro acceso a memoria.
        scope.launch {
            targetsStore.targetsFlow.first().forEach { targetCache[it.packageName] = it }
        }
        // Mantener el cache sincronizado si el usuario edita la app en caliente.
        scope.launch {
            targetsStore.targetsFlow.collect { targets ->
                synchronized(cacheLock) {
                    targetCache.clear()
                    targets.forEach { targetCache[it.packageName] = it }
                }
            }
        }
        // Conocimiento del chat bot en caliente (ediciones del backend).
        scope.launch {
            chatBotStore.knowledgeFlow.collect { knowledge ->
                chatBotKnowledge = knowledge
            }
        }
    }

    override fun onDestroy() {
        android.util.Log.i("DCA", "Listener desconectado: servicio destruido")
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        // Ignorar nuestro propio eco y UI del sistema.
        if (pkg == applicationContext.packageName) return
        if (pkg == "com.android.systemui") return

        // Skip temprano: si la app no está en la cache configurada, no hay
        // nada que hacer. Evita coroutine + DataStore por cada notificación
        // de cualquier app del teléfono (el 99% de los casos).
        if (targetCache[pkg] == null) return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        if (title.isBlank()) return
        android.util.Log.i(
            "DCA",
            "Notificación de app configurada: $pkg · título=$title",
        )

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (text.isBlank()) return // sin cuerpo de mensaje no hay nada que responder
        // Firma por CONTENIDO (ver NotificationKeys): mensajes nuevos del mismo
        // remitente tienen firma distinta; re-posts idénticos, la misma.
        val key = NotificationKeys.contentKey(pkg, title, text)
        if (key.isBlank()) return

        scope.launch {
            // Serialización por app: los re-posts rápidos de la misma
            // notificación generan varios eventos casi simultáneos. Sin este
            // mutex, ambas coroutines evaluarían shouldReply ANTES de que la
            // primera actualice lastSeenKey (check-then-act sin atomicidad),
            // enviando el mensaje dos veces. El lock por paquete garantiza
            // que la segunda decisión vea la firma ya registrada.
            val mutex = packageMutexes.getOrPut(pkg) { Mutex() }
            mutex.withLock {
                val current = synchronized(cacheLock) { targetCache[pkg] } ?: return@withLock

                val decision = AutoReplyEngine.shouldReply(
                    target = current,
                    notification = AutoReplyEngine.IncomingNotification(packageName = pkg, sender = title),
                    nowMillis = System.currentTimeMillis(),
                    lastSeenNotificationKey = lastSeenKeys[pkg],
                    notificationKey = key,
                )

                if (decision == AutoReplyEngine.Decision.REPLY) {
                    // Demora configurable antes de responder (cooldown).
                    AutoReplyEngine.awaitCooldown(current.cooldownSeconds)

                    // Chat bot activado: el texto se genera a partir del
                    // mensaje del cliente con el mismo sistema de matching
                    // del chat bot web (keyword scoring). Si el cliente pide
                    // un humano, se responde avisando el traspaso. Si está
                    // apagado, se envía el mensaje estático configurado.
                    val outgoingMessage = when {
                        current.chatBotEnabled -> {
                            val response = ChatBot.respond(text, chatBotKnowledge)
                            ChatBot.renderText(response, chatBotKnowledge)
                                .take(AppTarget.MAX_REPLY_LENGTH)
                        }
                        else -> current.replyMessage
                    }

                    val sent = NotificationReplier.sendReply(
                        context = applicationContext,
                        sbn = sbn,
                        message = outgoingMessage,
                    )
                    android.util.Log.i(
                        "DCA",
                        if (sent) "Respuesta enviada a $pkg ($title)" else "No se pudo enviar (sin respuesta rápida): $pkg",
                    )
                    if (sent) {
                        lastSeenKeys[pkg] = key
                        // Escritura atómica a nivel de campo: actualiza el gap
                        // técnico de la app y la ventana individual del
                        // remitente (título de conversación = contacto/número),
                        // sin pisar ediciones del usuario en curso.
                        targetsStore.markReplied(pkg, title, System.currentTimeMillis())
                        ReplyNotifier.post(applicationContext, pkg, title, outgoingMessage)
                    }
                }
            }
        }
    }
}
