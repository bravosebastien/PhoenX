package com.example.phoenx.data.subscription

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Source unique de vérité côté application pour l'état d'abonnement du
 * compte connecté. Interroge la Cloud Function getMyBillingState (jamais de
 * lecture directe de Firestore ici — le serveur reste la seule autorité,
 * voir functions/src/billing.ts).
 */
@Singleton
class BillingRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                refresh()
            } else {
                _state.value = BillingState(isLoading = false)
            }
        }
    }

    fun refresh() {
        if (auth.currentUser == null) return
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, loadError = null)
            try {
                val result = functions.getHttpsCallable("getMyBillingState").call().await()
                @Suppress("UNCHECKED_CAST")
                val raw = result.data as? Map<String, Any?> ?: emptyMap()
                _state.value = parseBillingState(raw)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, loadError = e.message ?: "Erreur inconnue")
            }
        }
    }

    fun hasReachedLimit(key: String, currentCount: Int): Boolean {
        val limit = _state.value.currentTier?.limits?.get(key) ?: return false
        if (limit < 0) return false // -1 = illimité, convention côté serveur
        return currentCount >= limit
    }
}
