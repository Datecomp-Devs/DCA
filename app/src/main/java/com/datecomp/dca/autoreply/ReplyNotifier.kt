package com.datecomp.dca.autoreply

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.datecomp.dca.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Efecto de IO: canal y notificación de confirmación de DCA. Separado del
 * listener para que la entrada de notificaciones no mezcle la orquestación
 * de respuestas con la presentación de resultados.
 */
object ReplyNotifier {

    const val CHANNEL_ID: String = "dca_replier_channel"
    private const val NOTIF_ID_BASE: Int = 10_000

    /** Canal único, IMPORTANCE_LOW y silencioso para no molestar al usuario. */
    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.replier_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.replier_notification_channel_desc)
        }
        nm.createNotificationChannel(channel)
    }

    /** Confirmación discreta de que una respuesta fue enviada. */
    fun post(context: Context, pkg: String, conversationTitle: String, message: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val openAppIntent = context.packageManager.getLaunchIntentForPackage(pkg)
        val pending = openAppIntent?.let {
            PendingIntent.getActivity(
                context,
                pkg.hashCode(),
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reply)
            .setContentTitle("DCA respondió en $pkg")
            .setContentText(
                "${conversationTitle.take(32)} · ${timeFmt.format(Date())} · ${message.take(48)}",
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_BASE + pkg.hashCode(), notification)
    }
}
