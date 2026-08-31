package com.datecomp.dca.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.datecomp.dca.R
import com.datecomp.dca.core.AppTarget
import com.datecomp.dca.core.AutoReplyEngine
import com.datecomp.dca.core.NotificationKeys
import com.datecomp.dca.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Servicio con permiso especial NotificationListener. Única fuente de
 * eventos: entrega notificaciones de todas las apps instaladas.
 *
 * Rendimiento del hot path (onNotificationPosted):
 * - Skip temprano sincrónico para apps sin configurar: ni siquiera se lanza
 *   una coroutine ni se toca DataStore.
 * - Cache en memoria del repositorio: evita lecturas de disco/JSON por
 *   notificación; se refresca solo al cambiar los datos.
 * - Lookup del target en O(1) por mapa de paquetes.
 */
class AutoReplyNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: SettingsRepository

    /** Última firma respondida, POR APP (no global): evita bloqueos cruzados. */
    private val lastSeenKeys = ConcurrentHashMap<String, String>()

    /** Cache en memoria: packageName -> target. Fuente de verdad: DataStore. */
    private val targetCache = ConcurrentHashMap<String, AppTarget>()
    private val cacheLock = Any()

    /** Un mutex por paquete: serializa la decisión+envío por app. */
    private val packageMutexes = ConcurrentHashMap<String, Mutex>()

    override fun onCreate() {
        super.onCreate()
        repository = SettingsRepository(applicationContext)
        ensureReplyChannel()
        // Warm-up del cache al conectar el listener: una única lectura de
        // disco al inicio; después el hot path es puro acceso a memoria.
        scope.launch {
            repository.targetsFlow.first().forEach { targetCache[it.packageName] = it }
        }
        // Mantener el cache sincronizado si el usuario edita la app en caliente.
        scope.launch {
            repository.targetsFlow.collect { targets ->
                synchronized(cacheLock) {
                    targetCache.clear()
                    targets.forEach { targetCache[it.packageName] = it }
                }
            }
        }
    }

    override fun onDestroy() {
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
        val target = targetCache[pkg] ?: return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        if (title.isBlank()) return

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        // Firma por CONTENIDO (ver NotificationKeys): mensajes nuevos del mismo
        // remitente tienen firma distinta; re-posts idénticos, la misma.
        val key = NotificationKeys.contentKey(pkg, title, text)
        if (key.isBlank()) return // sin cuerpo de mensaje no hay nada que responder

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
                    notification = AutoReplyEngine.IncomingNotification(packageName = pkg),
                    nowMillis = System.currentTimeMillis(),
                    lastSeenNotificationKey = lastSeenKeys[pkg],
                    notificationKey = key,
                )

                if (decision == AutoReplyEngine.Decision.REPLY) {
                    // Demora configurable antes de responder (cooldown).
                    AutoReplyEngine.awaitCooldown(current.cooldownSeconds)

                    val sent = NotificationReplier.sendReply(
                        context = applicationContext,
                        sbn = sbn,
                        message = current.replyMessage,
                    )
                    if (sent) {
                        lastSeenKeys[pkg] = key
                        // Escritura a nivel de campo sobre lo más reciente en
                        // disco: no pisa ediciones del usuario hechas durante
                        // la demora del cooldown.
                        repository.markReplied(pkg, System.currentTimeMillis())
                        postConfirmationNotification(pkg, title, current.replyMessage)
                    }
                }
            }
        }
    }

    /** Canal único, IMPORTANCE_LOW y silencioso para no molestar al usuario. */
    private fun ensureReplyChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.replier_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.replier_notification_channel_desc)
        }
        nm.createNotificationChannel(channel)
    }

    /** Confirmación discreta de que una respuesta fue enviada. */
    private fun postConfirmationNotification(pkg: String, conversationTitle: String, message: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val openAppIntent = packageManager.getLaunchIntentForPackage(pkg)
        val pending = openAppIntent?.let {
            PendingIntent.getActivity(
                this,
                pkg.hashCode(),
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reply)
            .setContentTitle("DCA respondió en $pkg")
            .setContentText("${conversationTitle.take(32)} · ${timeFmt.format(Date())} · ${message.take(48)}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_BASE + pkg.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID: String = "dca_replier_channel"
        const val NOTIF_ID_BASE: Int = 10_000

        /** Consulta el estado real del permiso NotificationListener. */
        fun isListenerEnabled(context: Context): Boolean {
            val raw = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
            return raw.split(":").any { part ->
                val cn = android.content.ComponentName.unflattenFromString(part)
                cn?.packageName == context.packageName
            }
        }
    }
}