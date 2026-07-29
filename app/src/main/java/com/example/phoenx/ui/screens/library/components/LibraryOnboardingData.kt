package com.example.phoenx.ui.screens.library.components

object LibraryOnboardingData {
    fun getContent(type: String): List<String> {
        val specific = when(type) {
            "LITERARY" -> "un extrait littéraire ou un passage de livre"
            "DISCO" -> "un morceau Spotify ou une musique fétiche"
            "VIDEO" -> "un lien YouTube ou une vidéo importante"
            "PHOTO" -> "une photo isolée"
            else -> "un contenu"
        }
        
        return listOf(
            "Tu peux déposer directement $specific ici, sans le rattacher à un souvenir précis.",
            "Par défaut, ce dépôt est visible par TOUS tes Destinataires.",
            "Tu peux choisir de le réserver à une ou plusieurs personnes via le sélecteur, exactement comme pour un souvenir.",
            "Ce contenu reste scellé et illisible pour tes proches jusqu'à l'activation du protocole.",
            "Pour lier ce contenu à un récit de vie plutôt que de le déposer seul, passe par l'écran de Capture."
        )
    }

    fun getTitle(type: String): String = when(type) {
        "LITERARY" -> "La Bibliothèque Littéraire"
        "DISCO" -> "La Grande Discothèque"
        "VIDEO" -> "La Grande Vidéothèque"
        "PHOTO" -> "La Grande Photothèque"
        else -> "Aide"
    }
}
