package com.example.phoenx.ui.screens.library.archive

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.phoenx.ui.screens.library.*
import com.example.phoenx.ui.theme.AppThemeState

/**
 * CompartmentPainter — Chef d'orchestre du rendu visuel de la Bibliothèque.
 * v8.9.8 : Découpage modulaire et thémage dynamique.
 */

fun DrawScope.drawCompartmentContent(
    compartment: LibraryCompartment,
    glowIntensity: Float,
    theme: AppThemeState
) {
    when (compartment.id) {
        // Culture (Module CulturePainter)
        CompartmentId.BIBLIOTHEQUE -> drawBooks(theme)
        CompartmentId.DISCOTHEQUE -> drawVinyls(theme)
        CompartmentId.VIDEOTHEQUE -> drawCassettes(theme)
        
        // Transmission (Module TransmissionPainter)
        CompartmentId.CENT_QUESTIONS -> drawUrn(glowIntensity, theme)
        CompartmentId.COFFRE_FORT -> drawSafe(glowIntensity, theme)
        CompartmentId.LE_PACTE -> drawPactBooks(glowIntensity, theme)
        CompartmentId.RECONCILIATION -> drawSealedLetter(glowIntensity, theme)

        // Exploration (Module ExplorationPainter)
        CompartmentId.FIL_PENSEE -> drawScrolls(glowIntensity, theme)
        CompartmentId.LETTRES -> drawMailbox(glowIntensity, theme)
        CompartmentId.PHOTOS -> drawPhotoFrames(glowIntensity, theme)
        CompartmentId.MAPPEMONDE -> drawGlobe(glowIntensity, theme)
        CompartmentId.PORTRAIT_PROCHE -> drawPortraitFrame(glowIntensity, theme)
        
        // Compartiments orphelins (v8.9.4) - Absorbés ou fusionnés
        CompartmentId.MES_MEILLEURS, CompartmentId.TIROIR_SECRET -> { /* No-op */ }
    }
}
