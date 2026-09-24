package com.example.phoenx.data.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.example.phoenx.data.preferences.PreferenceManager
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AnalyticsTracker (CNIL Compliant)
 * RÈGLE ABSOLUE : Aucun contenu, nom, email, UID ou identifiant de souvenir/personne n'est jamais envoyé.
 * Ne pas appeler setUserId.
 */
@Singleton
class AnalyticsTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager
) {
    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface AnalyticsEntryPoint {
        fun analyticsTracker(): AnalyticsTracker
    }

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    private var isConsentGiven: Boolean = false
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            try {
                isConsentGiven = preferenceManager.analyticsConsent.first()
                firebaseAnalytics.setAnalyticsCollectionEnabled(isConsentGiven)
            } catch (e: Exception) {
                Log.e("AnalyticsTracker", "Erreur initialisation consentement analytics: ${e.message}")
            }
        }
    }

    fun setConsent(enabled: Boolean) {
        isConsentGiven = enabled
        try {
            firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
        } catch (e: Exception) {
            Log.e("AnalyticsTracker", "Erreur setAnalyticsCollectionEnabled: ${e.message}")
        }
        scope.launch {
            try {
                preferenceManager.setAnalyticsConsent(enabled)
            } catch (e: Exception) {
                Log.e("AnalyticsTracker", "Erreur sauvegarde consentement: ${e.message}")
            }
        }
    }

    fun isConsentGiven(): Boolean = isConsentGiven

    fun logSignupCompleted() {
        if (!isConsentGiven) return
        try {
            firebaseAnalytics.logEvent("signup_completed", null)
        } catch (e: Exception) {
            Log.e("AnalyticsTracker", "Erreur logSignupCompleted: ${e.message}")
        }
    }

    fun logMemoryCreated(source: String) {
        if (!isConsentGiven) return
        try {
            val bundle = Bundle().apply {
                putString("source", source)
            }
            firebaseAnalytics.logEvent("memory_created", bundle)
        } catch (e: Exception) {
            Log.e("AnalyticsTracker", "Erreur logMemoryCreated: ${e.message}")
        }
    }

    fun logMemoryMilestone(count: Int) {
        if (!isConsentGiven) return
        try {
            val bundle = Bundle().apply {
                putInt("count", count)
            }
            firebaseAnalytics.logEvent("memory_milestone", bundle)
        } catch (e: Exception) {
            Log.e("AnalyticsTracker", "Erreur logMemoryMilestone: ${e.message}")
        }
    }

    fun logRecipientAdded(isFirstRecipient: Boolean) {
        if (!isConsentGiven) return
        try {
            firebaseAnalytics.logEvent("recipient_added", null)
            if (isFirstRecipient) {
                firebaseAnalytics.logEvent("first_recipient_added", null)
            }
        } catch (e: Exception) {
            Log.e("AnalyticsTracker", "Erreur logRecipientAdded: ${e.message}")
        }
    }

    fun logInvitationAccepted(role: String) {
        if (!isConsentGiven) return
        try {
            val bundle = Bundle().apply {
                putString("role", role)
            }
            firebaseAnalytics.logEvent("invitation_accepted", bundle)
        } catch (e: Exception) {
            Log.e("AnalyticsTracker", "Erreur logInvitationAccepted: ${e.message}")
        }
    }

    fun logBecomeCreatorPromptShown(role: String) {
        if (!isConsentGiven) return
        try {
            val bundle = Bundle().apply {
                putString("role", role)
            }
            firebaseAnalytics.logEvent("become_creator_prompt_shown", bundle)
        } catch (e: Exception) {
            Log.e("AnalyticsTracker", "Erreur logBecomeCreatorPromptShown: ${e.message}")
        }
    }

    fun logBecomeCreatorAccepted(role: String) {
        if (!isConsentGiven) return
        try {
            val bundle = Bundle().apply {
                putString("role", role)
            }
            firebaseAnalytics.logEvent("become_creator_accepted", bundle)
        } catch (e: Exception) {
            Log.e("AnalyticsTracker", "Erreur logBecomeCreatorAccepted: ${e.message}")
        }
    }

    fun logBecomeCreatorLater(role: String) {
        if (!isConsentGiven) return
        try {
            val bundle = Bundle().apply {
                putString("role", role)
            }
            firebaseAnalytics.logEvent("become_creator_later", bundle)
        } catch (e: Exception) {
            Log.e("AnalyticsTracker", "Erreur logBecomeCreatorLater: ${e.message}")
        }
    }
}
