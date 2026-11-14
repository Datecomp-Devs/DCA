package com.datecomp.dca.onboarding

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.datecomp.dca.dcaDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persistencia del estado de la guía inicial. Comparte la instancia única de
 * DataStore ([com.datecomp.dca.dcaDataStore]) con los demás slices.
 */
class OnboardingStore(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    /** Indica si el usuario ya completó la guía inicial. */
    val onboardingDoneFlow: Flow<Boolean> =
        context.dcaDataStore.data.map { prefs -> prefs[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setDone(done: Boolean) {
        context.dcaDataStore.edit { prefs -> prefs[Keys.ONBOARDING_DONE] = done }
    }
}
