package com.example.phoenx.data.config

import com.example.phoenx.domain.model.SubscriptionTier
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Grille de paliers d'abonnement, lue à distance depuis Firestore
 * (appConfig/subscriptionTiers) — jamais codée en dur (principe de malléabilité,
 * voir claude/decisions-23sept2026-architecture-abonnements-malleabilite.md).
 *
 * Structure attendue du document Firestore (à remplir en Console Firebase le
 * jour où la grille tarifaire sera décidée — rien à coder pour changer un
 * chiffre) :
 * {
 *   "DECOUVERTE": { "maxRecipients": 3, "maxStorageMb": 500 },
 *   "SOLIDAIRE":  { "maxRecipients": 10, "maxStorageMb": 5000 },
 *   "ESSENCE":    { ... },
 *   "LIGNEE":     { ... },
 *   "PRESTIGE":   { ... },
 *   "entitlementMapping": { "id_revenuecat_essence": "ESSENCE", ... }
 * }
 *
 * Tant que la grille n'est pas remplie, limitFor() renvoie null pour toute clé —
 * à traiter comme "pas de restriction connue", jamais comme une erreur.
 */
data class SubscriptionConfig(
    private val rawLimits: Map<String, Map<String, Any?>> = emptyMap(),
    val entitlementMapping: Map<String, String> = emptyMap()
) {
    fun limitFor(tier: SubscriptionTier, key: String): Long? {
        return (rawLimits[tier.name]?.get(key) as? Number)?.toLong()
    }

    fun rawLimitsFor(tier: SubscriptionTier): Map<String, Any?> = rawLimits[tier.name] ?: emptyMap()

    /** Traduit un identifiant d'entitlement RevenueCat en palier PHOEN-X. */
    fun tierForEntitlement(entitlementId: String): SubscriptionTier? {
        return entitlementMapping[entitlementId]?.let { SubscriptionTier.fromFirestoreValue(it) }
    }

    companion object {
        private const val ENTITLEMENT_MAPPING_KEY = "entitlementMapping"

        @Suppress("UNCHECKED_CAST")
        fun fromSnapshot(snapshot: DocumentSnapshot?): SubscriptionConfig {
            if (snapshot == null || !snapshot.exists()) return SubscriptionConfig()
            val data = snapshot.data ?: return SubscriptionConfig()

            val limits = data
                .filterKeys { it != ENTITLEMENT_MAPPING_KEY }
                .mapNotNull { (tierName, value) ->
                    (value as? Map<String, Any?>)?.let { tierName to it }
                }
                .toMap()

            val mapping = (data[ENTITLEMENT_MAPPING_KEY] as? Map<String, Any?>)
                ?.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }
                ?.toMap()
                ?: emptyMap()

            return SubscriptionConfig(rawLimits = limits, entitlementMapping = mapping)
        }
    }
}
