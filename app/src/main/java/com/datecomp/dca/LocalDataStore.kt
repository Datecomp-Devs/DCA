package com.datecomp.dca

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Instancia única del DataStore de DCA (`dca_settings`).
 *
 * DataStore prohíbe tener dos instancias activas sobre el mismo archivo, así
 * que esta instancia es compartida por los slices que persisten ajustes
 * ([com.datecomp.dca.targets.TargetsStore],
 * [com.datecomp.dca.chatbot.ChatBotStore] y
 * [com.datecomp.dca.onboarding.OnboardingStore]). Cada slice declara sus
 * propias claves y operaciones sobre esta instancia.
 */
val Context.dcaDataStore: DataStore<Preferences> by preferencesDataStore(name = "dca_settings")
