package com.example.phoenx.data.config

import com.google.firebase.firestore.DocumentSnapshot

data class LibraryCompartmentsConfig(
    val videothequeUrl: String? = null,
    val photothequeUrl: String? = null,
    val discothequeUrl: String? = null,
    val coffreFortUrl: String? = null,
    val portraitProcheUrl: String? = null,
    val mappemondeUrl: String? = null,
    val centQuestionsUrl: String? = null,
    val arbreGenealogiqueUrl: String? = null,
    val filPenseeUrl: String? = null,
    val livreMaVieUrl: String? = null,
    val litteraireUrl: String? = null,
    val personalitiesUrl: String? = null,
    val capsuleTemporelleUrl: String? = null,
    val reconciliationUrl: String? = null,
    val miroirDeuxUrl: String? = null,
    val mesClassementsUrl: String? = null,
    val lettreAMoiUrl: String? = null
) {
    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot?): LibraryCompartmentsConfig {
            if (snapshot == null || !snapshot.exists()) return LibraryCompartmentsConfig()
            return LibraryCompartmentsConfig(
                videothequeUrl = snapshot.getString("videothequeUrl")?.takeIf { it.isNotBlank() },
                photothequeUrl = snapshot.getString("photothequeUrl")?.takeIf { it.isNotBlank() },
                discothequeUrl = snapshot.getString("discothequeUrl")?.takeIf { it.isNotBlank() },
                coffreFortUrl = snapshot.getString("coffreFortUrl")?.takeIf { it.isNotBlank() },
                portraitProcheUrl = snapshot.getString("portraitProcheUrl")?.takeIf { it.isNotBlank() },
                mappemondeUrl = snapshot.getString("mappemondeUrl")?.takeIf { it.isNotBlank() },
                centQuestionsUrl = snapshot.getString("centQuestionsUrl")?.takeIf { it.isNotBlank() },
                arbreGenealogiqueUrl = snapshot.getString("arbreGenealogiqueUrl")?.takeIf { it.isNotBlank() },
                filPenseeUrl = snapshot.getString("filPenseeUrl")?.takeIf { it.isNotBlank() },
                livreMaVieUrl = snapshot.getString("livreMaVieUrl")?.takeIf { it.isNotBlank() },
                litteraireUrl = snapshot.getString("litteraireUrl")?.takeIf { it.isNotBlank() },
                personalitiesUrl = snapshot.getString("personalitiesUrl")?.takeIf { it.isNotBlank() },
                capsuleTemporelleUrl = snapshot.getString("capsuleTemporelleUrl")?.takeIf { it.isNotBlank() },
                reconciliationUrl = snapshot.getString("reconciliationUrl")?.takeIf { it.isNotBlank() },
                miroirDeuxUrl = snapshot.getString("miroirDeuxUrl")?.takeIf { it.isNotBlank() },
                mesClassementsUrl = snapshot.getString("mesClassementsUrl")?.takeIf { it.isNotBlank() },
                lettreAMoiUrl = snapshot.getString("lettreAMoiUrl")?.takeIf { it.isNotBlank() }
            )
        }
    }
}
