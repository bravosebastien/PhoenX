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
    private val ACCENT_COLOR_KEY = androidx.datastore.preferences.core.intPreferencesKey("accent_color")
    private val BACKGROUND_COLOR_KEY = androidx.datastore.preferences.core.intPreferencesKey("background_color")
    private val BACKGROUND_STYLE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("background_style")
    private val SYNC_MIGRATION_V1_DONE_KEY = booleanPreferencesKey("sync_migration_v1_done")
    
    // v8.9.0 : Thème Global (Plume & Papier)
    private val GLOBAL_BACKGROUND_ID_KEY = androidx.datastore.preferences.core.stringPreferencesKey("global_background_id")
    private val GLOBAL_FONT_ID_KEY = androidx.datastore.preferences.core.stringPreferencesKey("global_font_id")

    fun isSyncMigrationV1Done(): Flow<Boolean> = context.dataStore.data
        .map { it[SYNC_MIGRATION_V1_DONE_KEY] ?: false }

    suspend fun setSyncMigrationV1Done(done: Boolean) {
        context.dataStore.edit { it[SYNC_MIGRATION_V1_DONE_KEY] = done }
    }

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

    val accentColor: Flow<Int?> = context.dataStore.data
        .map { preferences ->
            preferences[ACCENT_COLOR_KEY]
        }

    val backgroundColor: Flow<Int?> = context.dataStore.data
        .map { preferences ->
            preferences[BACKGROUND_COLOR_KEY]
        }

    val backgroundStyle: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[BACKGROUND_STYLE_KEY] ?: "RADIAL"
        }

    val globalBackgroundId: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[GLOBAL_BACKGROUND_ID_KEY] ?: "classic_ivory"
        }

    val globalFontId: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[GLOBAL_FONT_ID_KEY] ?: "eb_garamond"
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

    suspend fun setAccentColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[ACCENT_COLOR_KEY] = color
        }
    }

    suspend fun setBackgroundColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[BACKGROUND_COLOR_KEY] = color
        }
    }

    suspend fun setBackgroundStyle(style: String) {
        context.dataStore.edit { preferences ->
            preferences[BACKGROUND_STYLE_KEY] = style
        }
    }

    suspend fun setGlobalTheme(backgroundId: String, fontId: String) {
        context.dataStore.edit { preferences ->
            preferences[GLOBAL_BACKGROUND_ID_KEY] = backgroundId
            preferences[GLOBAL_FONT_ID_KEY] = fontId
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
