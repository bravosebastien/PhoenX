package com.example.phoenx.ui.screens.library.archive

// Mode de consultation de la bibliothèque
enum class ViewerMode {
    CREATOR_PREVIEW,   // Le Créateur prévisualise ce que verront ses proches
    RECIPIENT_FULL,    // Destinataire avec accès complet
    RECIPIENT_PARTIAL  // Destinataire avec accès limité
}

// État d'accès d'un compartiment pour un Destinataire donné
enum class CompartmentAccess {
    OPEN,           // Accessible — pleine couleur
    LOCKED_OTHER,   // Destiné à quelqu'un d'autre — serrure dorée
    LOCKED_DATE,    // Pas encore le bon moment — sablier doré
    LOCKED_KEY,     // Nécessite une clé physique — serrure ronde
    LOCKED_ENIGMA,  // Mode Détective — roue de combinaison
    HIDDEN          // N'apparaît pas du tout
}

// Identifiant unique de chaque compartiment
enum class CompartmentId {
    BIBLIOTHEQUE,      // Textes et écrits
    DISCOTHEQUE,       // Contenus audio / vinyles
    VIDEOTHEQUE,       // Contenus vidéo / cassettes
    FIL_PENSEE,        // Fil de Pensée par âge / parchemins
    LETTRES,           // Lettres et messages programmés
    MES_MEILLEURS,     // Livres films musiques préférés
    PHOTOS,            // Photos et souvenirs visuels
    MAPPEMONDE,        // Globe interactif des voyages
    CENT_QUESTIONS,    // Les 100 Questions / urne
    COFFRE_FORT,       // Mode Détective / coffre
    TIROIR_SECRET,     // Tiroir à Clé Unique
    LE_PACTE,          // Deux livres reliés / le Pacte
    PORTRAIT_PROCHE,   // Portrait d'un Proche
    RECONCILIATION     // Protocole de Réconciliation
}

// Un compartiment de la bibliothèque
data class LibraryCompartment(
    val id: CompartmentId,
    val title: String,
    val subtitle: String,
    val access: CompartmentAccess = CompartmentAccess.OPEN,
    val unlocksAt: String? = null,     // date d'ouverture si LOCKED_DATE
    val itemCount: Int = 0,            // nombre d'éléments dedans
    val hasNewContent: Boolean = false, // badge "nouveau"
    val route: String                  // route de navigation
)
