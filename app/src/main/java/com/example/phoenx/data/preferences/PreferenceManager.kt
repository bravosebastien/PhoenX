package com.example.phoenx.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.phoenx.data.encryption.EncryptionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "phoenx_prefs")

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager
) {
    private val VOICE_MODE_KEY = booleanPreferencesKey("voice_mode_active")
    private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    private val SHOW_WELCOME_GUIDE_KEY = booleanPreferencesKey("show_welcome_guide")
    private val VIDEO_BANNER_DISMISSED_KEY = booleanPreferencesKey("video_banner_dismissed")
    private val LAST_RECOVERY_REMINDER_KEY = longPreferencesKey("last_recovery_reminder")
    private val RECOVERY_PHRASE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("recovery_phrase")
    private val SILENCE_ONBOARDING_DONE_KEY = booleanPreferencesKey("silence_onboarding_done")

    fun isDepositaryOnboardingSeen(userId: String): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[booleanPreferencesKey("depositary_onboarding_seen_$userId")] ?: false
        }

    val isVoiceModeActive: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[VOICE_MODE_KEY] ?: false
        }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] ?: false
        }

    val shouldShowWelcomeGuide: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_WELCOME_GUIDE_KEY] ?: true
        }

    val isVideoBannerDismissed: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[VIDEO_BANNER_DISMISSED_KEY] ?: false
        }

    val isSilenceOnboardingDone: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SILENCE_ONBOARDING_DONE_KEY] ?: false
        }

    // ═══ SYSTÈME AVANCÉ EN VEILLE ═══
    // Chiffrement E2EE avec Argon2id + BIP-39
    // Conservé pour activation future (V2 Pro)
    // ══════════════════════════════════════
    /*
    val lastRecoveryReminder: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_RECOVERY_REMINDER_KEY] ?: 0L
        }

    val recoveryPhrase: Flow<String?> = context.dataStore.data
        .map { preferences ->
            val encrypted = preferences[RECOVERY_PHRASE_KEY]
            if (encrypted != null) {
                try {
                    encryptionManager.decrypt(encrypted)
                } catch (e: Exception) {
                    null // Clé de session pas encore initialisée
                }
            } else null
        }
    */

    suspend fun setVoiceModeActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VOICE_MODE_KEY] = active
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }

    suspend fun setShouldShowWelcomeGuide(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_WELCOME_GUIDE_KEY] = show
        }
    }

    suspend fun setVideoBannerDismissed(dismissed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIDEO_BANNER_DISMISSED_KEY] = dismissed
        }
    }

    suspend fun setDepositaryOnboardingSeen(userId: String, seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("depositary_onboarding_seen_$userId")] = seen
        }
    }

    suspend fun setSilenceOnboardingDone(done: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SILENCE_ONBOARDING_DONE_KEY] = done
        }
    }

    // ═══ SYSTÈME AVANCÉ EN VEILLE ═══
    /*
    suspend fun updateLastRecoveryReminder(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_RECOVERY_REMINDER_KEY] = timestamp
        }
    }

    suspend fun setRecoveryPhrase(phrase: String) {
        val encrypted = encryptionManager.encrypt(phrase)
        context.dataStore.edit { preferences ->
            preferences[RECOVERY_PHRASE_KEY] = encrypted
        }
    }
    */
}
