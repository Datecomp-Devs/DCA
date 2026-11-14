package com.datecomp.dca.targets

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.datecomp.dca.dcaDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persistencia de las apps objetivo y de su configuración.
 *
 * INVARIANTE DE CONCURRENCIA: todas las mutaciones son read-modify-write
 * DENTRO de `dataStore.edit` (atómico), aplicando cambios a nivel de campo
 * sobre el valor más reciente en disco. Ninguna escritura reemplaza el
 * objeto completo desde un snapshot en memoria: así las escrituras del
 * listener (marca de tiempo) nunca pisan ediciones simultáneas del usuario
 * (mensaje, cooldown, repetir), ni viceversa.
 */
class TargetsStore(private val context: Context) {

    private object Keys {
        val TARGETS = stringPreferencesKey("app_targets")
        val MIGRATED_V2 = booleanPreferencesKey("migrated_v2")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Flujo reactivo de la lista de apps objetivo. */
    val targetsFlow: Flow<List<AppTarget>> = context.dcaDataStore.data.map { prefs ->
        val raw = prefs[Keys.TARGETS]
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { json.decodeFromString<List<AppTarget>>(raw) }.getOrElse { emptyList() }
        }
    }

    /**
     * Edición atómica de UNA app: lee el target más reciente en disco dentro
     * de `edit`, aplica [transform] y persiste. Si el paquete no existe, no
     * hace nada (a menos que [createIfMissing] reciba una semilla).
     */
    private suspend fun editTarget(
        packageName: String,
        createIfMissing: AppTarget? = null,
        transform: (AppTarget) -> AppTarget,
    ) {
        context.dcaDataStore.edit { prefs ->
            val raw = prefs[Keys.TARGETS]
            val current = raw
                ?.let { runCatching { json.decodeFromString<List<AppTarget>>(it) }.getOrNull() }
                .orEmpty()
            val index = current.indexOfFirst { it.packageName == packageName }
            val updated = when {
                index >= 0 -> current.toMutableList().also {
                    it[index] = transform(current[index])
                }
                createIfMissing != null -> current + transform(createIfMissing)
                else -> return@edit
            }
            prefs[Keys.TARGETS] = json.encodeToString(updated)
        }
    }

    /** Inserta o reemplaza por completo (usado solo para altas nuevas). */
    suspend fun upsert(target: AppTarget) {
        context.dcaDataStore.edit { prefs ->
            val raw = prefs[Keys.TARGETS]
            val current = raw
                ?.let { runCatching { json.decodeFromString<List<AppTarget>>(it) }.getOrNull() }
                .orEmpty()
            val merged = current.indexOfFirst { it.packageName == target.packageName }
                .let { idx ->
                    if (idx >= 0) {
                        // Merge con lo último en disco: conserva timestamp real.
                        current.toMutableList().also {
                            it[idx] = target.copy(
                                lastReplyTimestampMillis = current[idx].lastReplyTimestampMillis,
                            )
                        }
                    } else {
                        current + target
                    }
                }
            prefs[Keys.TARGETS] = json.encodeToString(merged)
        }
    }

    /** Habilita / deshabilita sin tocar los demás campos. */
    suspend fun setEnabled(packageName: String, enabled: Boolean) =
        editTarget(packageName) { it.copy(enabled = enabled) }

    /** Cambia solo el mensaje de auto-respuesta. */
    suspend fun setReplyMessage(packageName: String, message: String) =
        editTarget(packageName) { it.copy(replyMessage = message.take(AppTarget.MAX_REPLY_LENGTH)) }

    /** Cambia solo la demora (cooldown). */
    suspend fun setCooldown(packageName: String, seconds: Long) =
        editTarget(packageName) {
            it.copy(
                cooldownSeconds = seconds.coerceIn(
                    AppTarget.MIN_COOLDOWN_SECONDS,
                    AppTarget.MAX_COOLDOWN_SECONDS,
                ),
            )
        }

    /** Cambia solo el intervalo de repetición. */
    suspend fun setRepeat(packageName: String, seconds: Long) =
        editTarget(packageName) {
            it.copy(
                repeatSeconds = seconds.coerceIn(
                    AppTarget.MIN_REPEAT_SECONDS,
                    AppTarget.MAX_REPEAT_SECONDS,
                ),
            )
        }

    /** Activa o desactiva el chat bot para una app, sin tocar lo demás. */
    suspend fun setChatBotEnabled(packageName: String, enabled: Boolean) =
        editTarget(packageName) { it.copy(chatBotEnabled = enabled) }

    /**
     * Registra una respuesta con persistencia atómica a nivel de campo sobre
     * el valor más reciente en disco: actualiza el gap técnico de la app y
     * la ventana individual del remitente ([AppTarget.markReplied]).
     * Nunca pisa ediciones del usuario hechas durante la demora del cooldown.
     */
    suspend fun markReplied(packageName: String, sender: String, timestampMillis: Long) =
        editTarget(packageName) { it.markReplied(sender, timestampMillis) }

    /** Elimina un target por packageName. */
    suspend fun delete(packageName: String) {
        context.dcaDataStore.edit { prefs ->
            val raw = prefs[Keys.TARGETS]
            val current = raw
                ?.let { runCatching { json.decodeFromString<List<AppTarget>>(it) }.getOrNull() }
                .orEmpty()
            prefs[Keys.TARGETS] = json.encodeToString(current.filterNot { it.packageName == packageName })
        }
    }

    /**
     * Migración v1 → v2: en v1, `cooldownSeconds` significaba "intervalo
     * mínimo entre respuestas" (semántica actual de `repeatSeconds`).
     * Se ejecuta una sola vez, al arranque del proceso.
     */
    suspend fun migrateV1ToV2() {
        context.dcaDataStore.edit { prefs ->
            if (prefs[Keys.MIGRATED_V2] == true) return@edit
            val raw = prefs[Keys.TARGETS]
            if (!raw.isNullOrBlank()) {
                val migrated = runCatching {
                    json.decodeFromString<List<AppTarget>>(raw).map { t ->
                        if (t.repeatSeconds == 0L && t.cooldownSeconds > 0L) {
                            t.copy(repeatSeconds = t.cooldownSeconds, cooldownSeconds = 0L)
                        } else t
                    }
                }.getOrNull()
                if (migrated != null) {
                    prefs[Keys.TARGETS] = json.encodeToString(migrated)
                }
            }
            prefs[Keys.MIGRATED_V2] = true
        }
    }
}
