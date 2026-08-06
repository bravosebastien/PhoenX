package com.example.phoenx.ui.screens.library.components

object LibraryOnboardingData {
    fun getContent(type: String): List<String> {
        return when(type) {
            "PHOTO" -> listOf(
                "Toutes les photos de tes souvenirs se rangent ici automatiquement, sans rien faire de plus.",
                "Donne un titre à chaque photo pour la retrouver facilement.",
                "Un commentaire personnel (optionnel) t'aide à te souvenir du contexte.",
                "Clique sur une vignette pour l'ouvrir en grand. Utilise le crayon pour la modifier.",
                "Choisis qui pourra voir cette photo parmi tes destinataires, ou laisse-la visible par tous.",
                "**Restreindre les destinataires d'une photo ne concerne QUE cette photo précise. Cela n'a aucun effet sur le souvenir auquel elle est rattachée dans L'Étincelle & son Récit — les deux visibilités sont indépendantes.**"
            )
            "DISCO" -> listOf(
                "Dépose ici tes notes vocales, ou des liens vers tes musiques préférées sur Spotify ou Deezer.",
                "Une note vocale porte déjà son propre message — donne-lui simplement un titre pour la retrouver.",
                "Pour un lien musical, ajoute un commentaire pour expliquer pourquoi ce morceau compte pour toi.",
                "Clique sur un élément pour l'écouter. Utilise le crayon pour le modifier.",
                "Choisis qui pourra écouter cet élément parmi tes destinataires, ou laisse-le accessible à tous.",
                "**Comment récupérer un lien ?** 1. Ouvre Spotify ou Deezer. 2. Trouve ta chanson. 3. Appuie sur 'Partager' puis 'Copier le lien'. 4. Reviens ici et colle le lien.",
                "**Restreindre les destinataires d'une note vocale ou d'un lien musical ne concerne QUE cet élément précis. Cela n'a aucun effet sur le souvenir auquel il est rattaché dans L'Étincelle & son Récit — les deux visibilités sont indépendantes.**"
            )
            "VIDEO" -> listOf(
                "Toutes tes vidéos personnelles et tes liens YouTube se retrouvent ici.",
                "Une miniature de la vidéo s'affiche automatiquement dès son ajout.",
                "Donne un titre et, si tu le souhaites, un commentaire pour expliquer ce moment.",
                "Clique sur une vignette pour lancer la lecture. Utilise le crayon pour le modifier.",
                "Choisis qui pourra voir cette vidéo parmi tes destinataires, ou laisse-la visible par tous.",
                "**Comment récupérer un lien YouTube ?** 1. Ouvre l'app YouTube. 2. Trouve ta vidéo. 3. Appuie sur 'Partager' puis 'Copier le lien'. 4. Reviens ici et colle le lien.",
                "**Restreindre les destinataires d'une vidéo ne concerne QUE cette vidéo précise. Cela n'a aucun effet sur le souvenir auquel elle est rattachée dans L'Étincelle & son Récit — les deux visibilités sont indépendantes.**"
            )
            "LITERARY" -> listOf(
                "Dépose ici tes textes, citations ou passages qui comptent pour toi — un souvenir d'enfance, une lettre, un poème que tu as écrit.",
                "Donne un titre à chaque extrait pour le retrouver facilement dans la liste.",
                "Ajoute une photo de couverture personnelle : elle remplacera le fond parchemin par défaut sur la vignette.",
                "Un commentaire personnel (optionnel) t'aide à te souvenir du contexte — pourquoi ce texte compte pour toi.",
                "Clique sur une vignette pour lire l'extrait en plein écran. Utilise le crayon pour le modifier.",
                "Choisis qui pourra lire cet extrait parmi tes destinataires, ou laisse-le visible par tous.",
                "**Restreindre les destinataires d'un extrait ne concerne QUE cet extrait précis. Cela n'a aucun effet sur le souvenir auquel il est rattaché dans L'Étincelle & son Récit — les deux visibilités sont indépendantes.**"
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
