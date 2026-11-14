package com.datecomp.dca.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Slice de guía inicial: decide si la app arranca en la guía o directo en la
 * lista de apps y registra que el usuario la completó.
 */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val store = OnboardingStore(application)

    val onboardingDone: StateFlow<Boolean> = store.onboardingDoneFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun finish() {
        viewModelScope.launch { store.setDone(true) }
    }
}
