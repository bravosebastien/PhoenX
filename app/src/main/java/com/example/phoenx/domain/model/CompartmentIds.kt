package com.example.phoenx.domain.model

object CompartmentIds {
    const val LIBRARY_BOOKS = "LIBRARY_BOOKS"
    const val LIBRARY_MUSIC = "LIBRARY_MUSIC"
    const val LIBRARY_VIDEO = "LIBRARY_VIDEO"
    const val FIL_PENSEE = "FIL_PENSEE"
    const val LETTRES = "LETTRES"
    const val MES_MEILLEURS = "MES_MEILLEURS"
    const val PHOTOS = "PHOTOS"
    const val MAPPEMONDE = "MAPPEMONDE"
    const val CENT_QUESTIONS = "CENT_QUESTIONS"
    const val COFFRE_FORT = "COFFRE_FORT"
    const val TIROIR_SECRET = "TIROIR_SECRET"
    const val LE_PACTE = "LE_PACTE"
    const val PORTRAIT_PROCHE = "PORTRAIT_PROCHE"
    const val RECONCILIATION = "RECONCILIATION"

    val ALL = listOf(
        LIBRARY_BOOKS, LIBRARY_MUSIC, LIBRARY_VIDEO, FIL_PENSEE,
        LETTRES, MES_MEILLEURS, PHOTOS, MAPPEMONDE, CENT_QUESTIONS,
        COFFRE_FORT, TIROIR_SECRET, LE_PACTE, PORTRAIT_PROCHE, RECONCILIATION
    )

    fun getLabel(id: String): String = when (id) {
        LIBRARY_BOOKS -> "Livres de vie"
        LIBRARY_MUSIC -> "Discothèque"
        LIBRARY_VIDEO -> "Vidéothèque"
        FIL_PENSEE -> "Fil d'Or"
        LETTRES -> "Lettres"
        MES_MEILLEURS -> "Mes Meilleurs"
        PHOTOS -> "Photos"
        MAPPEMONDE -> "Mappemonde"
        CENT_QUESTIONS -> "100 Questions"
        COFFRE_FORT -> "Coffre Fort"
        TIROIR_SECRET -> "Tiroir Secret"
        LE_PACTE -> "Le Pacte"
        PORTRAIT_PROCHE -> "Portrait Proche"
        RECONCILIATION -> "Réconciliation"
        else -> id
    }
}
