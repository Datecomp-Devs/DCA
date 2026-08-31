package com.datecomp.dca.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.datecomp.dca.core.AppTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "dca_settings")

/**
 * Repositorio único de DCA (única capa de IO de disco). Persiste la lista de
 * [AppTarget]s como JSON en DataStore. Aislada para poder sustituirla o
 * mockearla en pruebas.
 *
 * Errores de deserialización se degradan a lista vacía para no romper la app
 * con datos corruptos.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val TARGETS = androidx.datastore.preferences.core.stringPreferencesKey("app_targets")
        val ONBOARDING_DONE = androidx.datastore.preferences.core.booleanPreferencesKey("onboarding_done")
        val MIGRATED_V2 = androidx.datastore.preferences.core.booleanPreferencesKey("migrated_v2")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Flujo reactivo de la lista de apps objetivo. */
    val targetsFlow: Flow<List<AppTarget>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.TARGETS]
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { json.decodeFromString<List<AppTarget>>(raw) }.getOrElse { emptyList() }
        }
    }

    /** Indica si el usuario ya completó la guía inicial. */
    val onboardingDoneFlow: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[Keys.ONBOARDING_DONE] ?: false }

    /** Inserta o actualiza un target (por packageName). */
    suspend fun upsert(target: AppTarget) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.TARGETS]
                ?.let { runCatching { json.decodeFromString<MutableList<AppTarget>>(it) }.getOrNull() }
                ?: mutableListOf()
            val index = current.indexOfFirst { it.packageName == target.packageName }
            if (index >= 0) current[index] = target else current.add(target)
            prefs[Keys.TARGETS] = json.encodeToString(current.toList())
        }
    }

    /** Elimina un target por packageName. */
    suspend fun delete(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.TARGETS]
                ?.let { runCatching { json.decodeFromString<MutableList<AppTarget>>(it) }.getOrNull() }
                ?: mutableListOf()
            current.removeAll { it.packageName == packageName }
            prefs[Keys.TARGETS] = json.encodeToString(current.toList())
        }
    }

    /** Persiste el resultado de [AutoReplyEngine.markReplied]; tolera fallos silenciosos. */
    suspend fun markReplied(target: AppTarget) {
        runCatching { upsert(target) }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.ONBOARDING_DONE] = done }
    }

    /**
     * Migración v1 → v2: en v1, `cooldownSeconds` significaba "intervalo
     * mínimo entre respuestas" (semántica actual de `repeatSeconds`). Esta
     * migración traslada el valor conservado a su campo correcto y pone la
     * demora en 0, preservando la intención original del usuario.
     * Se ejecuta una sola vez, al arranque del proceso.
     */
    suspend fun migrateV1ToV2() {
        context.dataStore.edit { prefs ->
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