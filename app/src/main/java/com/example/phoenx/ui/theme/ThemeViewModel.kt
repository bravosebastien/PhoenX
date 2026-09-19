package com.example.phoenx.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.preferences.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    val preferenceManager: PreferenceManager,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    val accentColor: StateFlow<Color> = preferenceManager.accentColor
        .map { it?.let { Color(it) } ?: AccentPrimary }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccentPrimary)

    // v8.9.0 : Thème Global (Plume & Papier) - Source de vérité unique
    val globalBackgroundId: StateFlow<String> = preferenceManager.globalBackgroundId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "classic_ivory")

    val globalFontId: StateFlow<String> = preferenceManager.globalFontId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "eb_garamond")

    val globalBackgroundColor: StateFlow<Color> = preferenceManager.globalBackgroundColor
        .map { it?.let { Color(it) } ?: Color(0xFFFFFDF5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Color(0xFFFFFDF5))

    fun setAccent(color: Color) {
        viewModelScope.launch {
            preferenceManager.setAccentColor(color.toArgb())
            syncThemeToFirestore()
        }
    }

    fun setGlobalTheme(backgroundId: String, fontId: String) {
        viewModelScope.launch {
            preferenceManager.setGlobalTheme(backgroundId, fontId)
            syncThemeToFirestore()
        }
    }

    fun setGlobalBackgroundColor(color: Color) {
        viewModelScope.launch {
            preferenceManager.setGlobalBackgroundColor(color.toArgb())
            syncThemeToFirestore()
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            preferenceManager.setGlobalTheme("classic_ivory", "eb_garamond")
            preferenceManager.setGlobalBackgroundColor(Color(0xFFFFFDF5).toArgb())
            preferenceManager.setAccentColor(AccentPrimary.toArgb())
            syncThemeToFirestore()
        }
    }

    private suspend fun syncThemeToFirestore() {
        val userId = auth.currentUser?.uid ?: return
        try {
            val bgId = preferenceManager.globalBackgroundId.first()
            val fId = preferenceManager.globalFontId.first()
            val acc = preferenceManager.accentColor.first()
            val bgCol = preferenceManager.globalBackgroundColor.first() ?: Color(0xFFFFFDF5).toArgb()

            // v12.7 : "appTheme" est le look de l'application elle-même (Profil/Réglages) — on ne
            // touche plus ici à "transmissionBackgroundId/FontId", qui appartient désormais
            // uniquement à l'habillage du Livre (voir BookEditorViewModel.updateGlobalAmbiance),
            // pour que changer l'un ne modifie plus l'autre par accident.
            android.util.Log.e("PHOENX_LOOP", "ThemeViewModel: syncThemeToFirestore (appTheme)")
            db.collection("users").document(userId).update(
                mapOf(
                    "appTheme" to mapOf(
                        "backgroundId" to bgId,
                        "fontId" to fId,
                        "accentColor" to acc,
                        "backgroundColor" to bgCol
                    )
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("ThemeVM", "Erreur sync Firestore: ${e.message}")
        }
    }
}
