package com.example.phoenx.domain.liveness

import com.example.phoenx.domain.usecase.ActivationProtocolManager
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * LivenessManager (Signature PHOEN-X 5.0)
 * Gère la Preuve de Vie passive. Zéro Anxiété : l'app confirme la vie 
 * simplement parce que l'utilisateur l'utilise ou bouge.
 */
@Singleton
class LivenessManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val protocolManager: ActivationProtocolManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Confirme la présence du Créateur silencieusement.
     * Appelé à chaque lancement d'app ou dépôt de souvenir.
     */
    fun confirmPassivePresence() {
        val userId = auth.currentUser?.uid ?: return
        scope.launch {
            try {
                protocolManager.confirmProofOfLife(userId)
            } catch (e: Exception) {
                // Silencieux pour ne pas interrompre l'expérience utilisateur
            }
        }
    }
}
