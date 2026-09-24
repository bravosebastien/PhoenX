package com.example.phoenx.data.subscription

import com.example.phoenx.data.config.SubscriptionConfig
import com.example.phoenx.domain.model.SubscriptionTier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Moteur de droits central (voir Partie 2 de
 * decisions-23sept2026-architecture-abonnements-malleabilite.md) : source
 * unique de vérité, interrogée par toute l'app, pour "ce compte a-t-il le
 * droit de X ?". Lit le palier actif du compte (users/{uid}.subscriptionTier)
 * et la grille de règles à distance (appConfig/subscriptionTiers).
 *
 * IMPORTANT — sécurité : ceci est une vérification de CONFORT côté application
 * (griser un bouton, afficher un message). Le champ subscriptionTier est
 * verrouillé en écriture côté client (voir firestore.rules) et n'est modifié
 * que par la Cloud Function revenueCatWebhook. Mais toute action sensible
 * limitée par palier doit AUSSI être vérifiée côté serveur (Cloud Function /
 * règle Firestore) avant d'être considérée comme réellement protégée — une
 * vérification uniquement ici serait contournable (app modifiée, requête
 * directe). Voir Partie 4 du document de décisions.
 */
@Singleton
class EntitlementsManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    private val _currentTier = MutableStateFlow(SubscriptionTier.DEFAULT)
    val currentTier: StateFlow<SubscriptionTier> = _currentTier.asStateFlow()

    private val _subscriptionExpiresAtMillis = MutableStateFlow<Long?>(null)
    val subscriptionExpiresAtMillis: StateFlow<Long?> = _subscriptionExpiresAtMillis.asStateFlow()

    private val _config = MutableStateFlow(SubscriptionConfig())
    val config: StateFlow<SubscriptionConfig> = _config.asStateFlow()

    private var userListener: ListenerRegistration? = null
    private var listenedUid: String? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != listenedUid) {
                listenedUid = uid
                attachUserListener(uid)
            }
        }
        attachConfigListener()
    }

    private fun attachUserListener(uid: String?) {
        userListener?.remove()
        if (uid == null) {
            _currentTier.value = SubscriptionTier.DEFAULT
            _subscriptionExpiresAtMillis.value = null
            return
        }
        userListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                _currentTier.value = SubscriptionTier.fromFirestoreValue(snapshot?.getString("subscriptionTier"))
                _subscriptionExpiresAtMillis.value = snapshot?.getTimestamp("subscriptionExpiresAt")?.toDate()?.time
            }
    }

    private fun attachConfigListener() {
        db.collection("appConfig").document("subscriptionTiers")
            .addSnapshotListener { snapshot, _ ->
                _config.value = SubscriptionConfig.fromSnapshot(snapshot)
            }
    }

    /**
     * true si la limite `key` du palier actuel est définie ET atteinte/dépassée
     * par `currentCount`. Si la limite n'est pas (encore) définie dans la
     * configuration à distance, renvoie toujours false — pas de restriction
     * tant que la grille n'a pas été remplie en Console Firebase.
     */
    fun hasReachedLimit(key: String, currentCount: Int): Boolean {
        val limit = _config.value.limitFor(_currentTier.value, key) ?: return false
        return currentCount >= limit
    }

    fun limitFor(key: String): Long? = _config.value.limitFor(_currentTier.value, key)
}
