package com.example.phoenx.domain.model

/**
 * Paliers d'abonnement PHOEN-X.
 *
 * v1 — MÉCANIQUE UNIQUEMENT. Les prix et les quotas exacts de chaque palier ne
 * sont pas encore décidés (voir claude/decisions-23sept2026-architecture-abonnements-malleabilite.md
 * dans le Projet Claude "Phoen-X"). Cet enum sert uniquement à identifier le palier
 * actif d'un compte — aucun prix, aucun quota ne doit jamais être codé en dur ici
 * ni ailleurs : tout vit dans Firestore (appConfig/subscriptionTiers), lu par
 * SubscriptionConfig / EntitlementsManager.
 */
enum class SubscriptionTier {
    DECOUVERTE, // Palier gratuit par défaut
    SOLIDAIRE,
    ESSENCE,
    LIGNEE,
    PRESTIGE;

    companion object {
        val DEFAULT = DECOUVERTE

        /**
         * Convertit la valeur brute stockée dans users/{uid}.subscriptionTier.
         * Toute valeur absente, vide ou inconnue retombe sur le palier gratuit
         * par défaut plutôt que de faire planter l'appelant.
         */
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
