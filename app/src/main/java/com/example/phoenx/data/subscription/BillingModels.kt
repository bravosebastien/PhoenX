package com.example.phoenx.data.subscription

/**
 * Modèles côté application, reflet direct de la réponse de la Cloud Function
 * getMyBillingState (functions/src/billing.ts). Toujours lus depuis le
 * serveur, jamais codés en dur.
 */
data class BillingTierInfo(
    val key: String,
    val rank: Int,
    val nameFr: String,
    val tagline: String,
    val highlight: Boolean,
    val monthlyProductId: String?,
    val annualProductId: String?,
    val features: Map<String, Boolean>,
    val limits: Map<String, Long>
)

data class ActivePromotion(
    val id: String,
    val bannerText: String?
)

data class BillingState(
    val enforced: Boolean = false,
    val currentTierKey: String = "DECOUVERTE",
    val entitlementSource: String = "default",
    val isTrial: Boolean = false,
    val expiresAtMillis: Long? = null,
    val monthlyBalance: Long = 0,
    val purchasedBalance: Long = 0,
    val activePromotions: List<ActivePromotion> = emptyList(),
    val tiers: List<BillingTierInfo> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: String? = null
) {
    fun tierInfo(key: String): BillingTierInfo? = tiers.find { it.key == key }
    val currentTier: BillingTierInfo? get() = tierInfo(currentTierKey)
}

@Suppress("UNCHECKED_CAST")
fun parseBillingState(raw: Map<String, Any?>): BillingState {
    val entitlement = raw["entitlement"] as? Map<String, Any?> ?: emptyMap()
    val wallet = raw["wallet"] as? Map<String, Any?> ?: emptyMap()
    val rawTiers = raw["tiers"] as? Map<String, Any?> ?: emptyMap()
    val rawPromos = raw["activePromotions"] as? List<Map<String, Any?>> ?: emptyList()

    val tiers = rawTiers.mapNotNull { (key, value) ->
        val t = value as? Map<String, Any?> ?: return@mapNotNull null
        val name = (t["name"] as? Map<String, Any?>)?.get("fr") as? String ?: key
        val tagline = (t["tagline"] as? Map<String, Any?>)?.get("fr") as? String ?: ""
        val storeProductIds = t["storeProductIds"] as? Map<String, Any?> ?: emptyMap()
        val features = (t["features"] as? Map<String, Any?>)?.mapValues { it.value as? Boolean ?: false } ?: emptyMap()
        val limits = (t["limits"] as? Map<String, Any?>)?.mapValues { (it.value as? Number)?.toLong() ?: 0L } ?: emptyMap()
        BillingTierInfo(
            key = key,
            rank = (t["rank"] as? Number)?.toInt() ?: 0,
            nameFr = name,
            tagline = tagline,
            highlight = t["highlight"] as? Boolean ?: false,
            monthlyProductId = storeProductIds["monthly"] as? String,
            annualProductId = storeProductIds["annual"] as? String,
            features = features,
            limits = limits
        )
    }.sortedBy { it.rank }

    return BillingState(
        enforced = raw["enforced"] as? Boolean ?: false,
        currentTierKey = entitlement["tier"] as? String ?: "DECOUVERTE",
        entitlementSource = entitlement["source"] as? String ?: "default",
        isTrial = entitlement["isTrial"] as? Boolean ?: false,
        expiresAtMillis = (entitlement["expiresAt"] as? com.google.firebase.Timestamp)?.toDate()?.time,
        monthlyBalance = (wallet["monthlyBalance"] as? Number)?.toLong() ?: 0L,
        purchasedBalance = (wallet["purchasedBalance"] as? Number)?.toLong() ?: 0L,
        activePromotions = rawPromos.map { ActivePromotion(it["id"] as? String ?: "", it["bannerText"] as? String) },
        tiers = tiers,
        isLoading = false,
        loadError = null
    )
}
