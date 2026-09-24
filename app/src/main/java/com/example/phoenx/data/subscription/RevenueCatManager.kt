package com.example.phoenx.data.subscription

import android.content.Context
import com.example.phoenx.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback

/**
 * Initialise le SDK RevenueCat (moteur de paiement — voir Partie 5 de
 * decisions-23sept2026-architecture-abonnements-malleabilite.md) et garde son
 * "app_user_id" synchronisé avec l'UID Firebase du compte connecté, pour que
 * la Cloud Function revenueCatWebhook (functions/src/subscriptions.ts) sache
 * quel compte Firestore mettre à jour à chaque achat/renouvellement/résiliation.
 *
 * ⚠️ Ce fichier ne détermine JAMAIS lui-même le palier actif du compte : c'est
 * uniquement la tuyauterie d'achat (Google Play Billing, via RevenueCat). La
 * vérité sur le palier vient uniquement de users/{uid}.subscriptionTier, écrit
 * côté serveur — voir EntitlementsManager.
 *
 * Clé API à renseigner dans local.properties (jamais commitée) :
 *   REVENUECAT_API_KEY=goog_xxxxxxxxxxxxxxxxxxxxxxxxxx
 * Tant qu'elle est absente, l'initialisation est ignorée sans planter l'app.
 */
object RevenueCatManager {

    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        val apiKey = BuildConfig.REVENUECAT_API_KEY
        if (apiKey.isBlank() || apiKey == "REPLACE_ME") {
            android.util.Log.w(
                "RevenueCatManager",
                "Clé API RevenueCat absente (REVENUECAT_API_KEY dans local.properties) — initialisation ignorée."
            )
            return
        }

        Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
        Purchases.configure(PurchasesConfiguration.Builder(context, apiKey).build())
        initialized = true

        // Synchronise l'identité RevenueCat avec la session Firebase active,
        // pour toute connexion/déconnexion, où qu'elle ait lieu dans l'app.
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid != null) {
                Purchases.sharedInstance.logIn(uid, object : LogInCallback {
                    override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {}
                    override fun onError(error: PurchasesError) {
                        android.util.Log.w("RevenueCatManager", "logIn échoué : ${error.message}")
                    }
                })
            } else if (!Purchases.sharedInstance.isAnonymous) {
                Purchases.sharedInstance.logOut(object : ReceiveCustomerInfoCallback {
                    override fun onReceived(customerInfo: CustomerInfo) {}
                    override fun onError(error: PurchasesError) {
                        android.util.Log.w("RevenueCatManager", "logOut échoué : ${error.message}")
                    }
                })
            }
        }
    }
}
