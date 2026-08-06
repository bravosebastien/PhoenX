package com.example.phoenx.ui.screens.library.components

object LibraryOnboardingData {
    fun getContent(type: String): List<String> {
        return when(type) {
            "PHOTO" -> listOf(
                "Toutes les photos de tes souvenirs se rangent ici automatiquement, sans rien faire de plus.",
                "Donne un titre à chaque photo pour la retrouver facilement.",
                "Un commentaire personnel (optionnel) t'aide à te souvenir du contexte.",
                "Clique sur une vignette pour l'ouvrir en grand. Utilise le crayon pour la modifier.",
                "Choisis qui pourra voir cette photo parmi tes destinataires, ou laisse-la visible par tous."
            )
            "DISCO" -> listOf(
                "Dépose ici tes notes vocales, ou des liens vers tes musiques préférées sur Spotify ou Deezer.",
                "Une note vocale porte déjà son propre message — donne-lui simplement un titre pour la retrouver.",
                "Pour un lien musical, ajoute un commentaire pour expliquer pourquoi ce morceau compte pour toi.",
                "Clique sur un élément pour l'écouter. Utilise le crayon pour le modifier.",
                "Choisis qui pourra écouter cet élément parmi tes destinataires, ou laisse-le accessible à tous."
            )
            "VIDEO" -> listOf(
                "Toutes tes vidéos personnelles et tes liens YouTube se retrouvent ici.",
                "Une miniature de la vidéo s'affiche automatiquement dès son ajout.",
                "Donne un titre et, si tu le souhaites, un commentaire pour expliquer ce moment.",
                "Clique sur une vignette pour lancer la lecture. Utilise le crayon pour la modifier.",
                "Choisis qui pourra voir cette vidéo parmi tes destinataires, ou laisse-la visible par tous."
            )
            "LITERARY" -> listOf(
                "Dépose ici tes textes, citations ou passages qui comptent pour toi — un souvenir d'enfance, une lettre, un poème que tu as écrit.",
                "Donne un titre à chaque extrait pour le retrouver facilement dans la liste.",
                "Ajoute une photo de couverture personnelle : elle remplacera le fond parchemin par défaut sur la vignette.",
                "Un commentaire personnel (optionnel) t'aide à te souvenir du contexte — pourquoi ce texte compte pour toi.",
                "Clique sur une vignette pour lire l'extrait en plein écran. Utilise le crayon pour le modifier.",
                "Choisis qui pourra lire cet extrait parmi tes destinataires, ou laisse-le visible par tous."
            )
            else -> emptyList()
        }
    }

    fun getTitle(type: String): String = when(type) {
        "PHOTO" -> "La Grande Photothèque"
        "DISCO" -> "La Discothèque"
        "VIDEO" -> "La Vidéothèque"
        "LITERARY" -> "La Bibliothèque Littéraire"
        else -> "Information"
    }
}
