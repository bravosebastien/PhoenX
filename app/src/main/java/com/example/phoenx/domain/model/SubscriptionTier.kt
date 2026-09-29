package com.example.phoenx.domain.model

/**
 * Paliers d'abonnement PHOEN-X — doit toujours correspondre exactement aux
 * clés du document Firestore appConfig/tiers (functions/src/billingDefaults.ts,
 * defaultTiersConfig). Aucun prix, aucun quota codé en dur ici : tout vient de
 * la réponse de la Cloud Function getMyBillingState (voir BillingRepository).
 */
enum class SubscriptionTier {
    DECOUVERTE,
    ESSENCE,
    LIGNEE,
    PRESTIGE,
    CONTINUITE;

    companion object {
        val DEFAULT = DECOUVERTE

        fun fromFirestoreValue(value: String?): SubscriptionTier {
            if (value.isNullOrBlank()) return DEFAULT
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                DEFAULT
            }
        }
    }
}
