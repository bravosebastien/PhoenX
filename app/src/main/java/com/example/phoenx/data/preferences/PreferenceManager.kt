package com.example.phoenx.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "phoenx_prefs")

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val VOICE_MODE_KEY = booleanPreferencesKey("voice_mode_active")
    private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    private val SHOW_WELCOME_GUIDE_KEY = booleanPreferencesKey("show_welcome_guide")

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
}
