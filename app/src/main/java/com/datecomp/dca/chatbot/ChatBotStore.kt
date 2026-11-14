package com.datecomp.dca.chatbot

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.datecomp.dca.dcaDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persistencia del conocimiento del chat bot. Comparte la instancia única de
 * DataStore ([com.datecomp.dca.dcaDataStore]) con los demás slices.
 */
class ChatBotStore(private val context: Context) {

    private object Keys {
        val CHAT_BOT_KNOWLEDGE = stringPreferencesKey("chat_bot_knowledge")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Conocimiento del chat bot: por defecto el del widget web de DateComp. */
    val knowledgeFlow: Flow<ChatBotKnowledge> = context.dcaDataStore.data.map { prefs ->
        val raw = prefs[Keys.CHAT_BOT_KNOWLEDGE]
        if (raw.isNullOrBlank()) {
            ChatBotKnowledge()
        } else {
            runCatching { json.decodeFromString<ChatBotKnowledge>(raw) }
                .getOrElse { ChatBotKnowledge() }
        }
    }

    /**
     * Reemplaza el conocimiento del chat bot (edición avanzada desde el
     * backend de la app). Rechaza JSON mayor a 256 KB por seguridad.
     */
    suspend fun setKnowledge(knowledge: ChatBotKnowledge) {
        val encoded = json.encodeToString(knowledge)
        require(encoded.length <= MAX_KNOWLEDGE_CHARS) { "Conocimiento demasiado grande" }
        context.dcaDataStore.edit { prefs -> prefs[Keys.CHAT_BOT_KNOWLEDGE] = encoded }
    }

    /** Tope del conocimiento persistido: 256 KB de JSON. */
    private companion object {
        const val MAX_KNOWLEDGE_CHARS: Int = 256 * 1024
    }
}
