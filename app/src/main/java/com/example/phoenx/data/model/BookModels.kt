package com.example.phoenx.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

enum class ChapterStatus { DRAFT, IN_REVIEW, VALIDATED }
enum class BookStatus { DRAFT, IN_PROGRESS, COMPLETE }

data class BookChapter(
    val id: String = "",
    val title: String = "",
    val content: String = "", // Chiffré Tink
    val status: ChapterStatus = ChapterStatus.DRAFT,
    val lastModified: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0,
    val sceneIds: List<String> = emptyList(),
    val sourceFingerprint: String = "",
    // Lot 2 (tableau de bord de régénération) : empreinte fine par souvenir inclus, clé = sceneId.
    // Vide pour les chapitres générés avant le Lot 2 (repli sur sourceFingerprint).
    val sceneDiagnostics: Map<String, SceneDiagnostic> = emptyMap()
)

/**
 * Empreintes locales d'un souvenir au moment de la génération d'un chapitre (Lot 2).
 * Recalculées à la demande à partir du contenu réel — jamais mises à jour à la main.
 */
data class SceneDiagnostic(
    val contentHash: String = "", // résumé, commentaire, amendments, catégorie/ton
    val characterHashes: Map<String, String> = emptyMap(), // clé = "Prénom Nom" de la personne taguée
    val complementsHash: String = "", // descriptions/commentaires des médias complémentaires
    val photoCount: Int = 0,
    val audioCount: Int = 0,
    val storyCount: Int = 0
)

data class BookTheme(
    val backgroundId: String = "classic_ivory",
    val fontId: String = "eb_garamond"
)

data class BookDraft(
    val id: String = "",
    val userId: String = "",
    val generatedAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val status: BookStatus = BookStatus.DRAFT,
    val chapters: List<BookChapter> = emptyList(),
    val totalEntries: Int = 0,
    val bookTitle: String? = null, // v9.2: Titre personnalisé du Livre
    val recipientIds: List<String> = emptyList(), // v8.5.4 Parity of access
    val sealedMessage: String = "", // v8.6.2 Message personnalisé pour l'héritier
    val globalIntroduction: String = "", // v8.7.0 Intro globale du livre (Chiffrée)
    val theme: BookTheme = BookTheme(), // v8.7.0 Thème visuel choisi par le Créateur
    val coverImageUrl: String? = null, // v9.2.4: Image de couverture personnalisée
    val coverTitleStyle: String = "GOLD", // v9.2.7: Style du titre par défaut
    val visibility: String = "RESTRICTED", // v9.4.27: Sécurisation explicite
    // v9.4.19 / v9.4.29 : Support du cadrage et métadonnées couverture
    val coverScale: Float = 1f,
    val coverOffsetX: Float = 0f,
    val coverOffsetY: Float = 0f,
    val coverUploadedAt: Long? = null,
    // Lot 2 : empreinte du contexte global (âge min/max, Ton de l'Âme, Portrait de Vie) à la dernière
    // génération. Vide pour les livres générés avant le Lot 2.
    val metaFingerprint: String = ""
)

// --- Lot 2 : Tableau de bord de régénération (comparaison locale, gratuite, sans appel IA) ---

enum class ChapterRegenStatus { INTACT, TO_REWORK }

data class ChapterRegenInfo(
    val chapterId: String = "",
    val title: String = "",
    val orderIndex: Int = 0,
    val status: ChapterRegenStatus = ChapterRegenStatus.INTACT,
    val reasons: List<String> = emptyList() // raisons propres à ce chapitre (hors raison globale)
)

data class OrphanSceneInfo(
    val sceneId: String = "",
    val summary: String = "",
    val age: Int = 0
)

data class RegenerationDashboard(
    val chapters: List<ChapterRegenInfo> = emptyList(),
    val orphanScenes: List<OrphanSceneInfo> = emptyList(),
    val globalReason: String? = null // ex: Portrait de Vie / Ton de l'Âme changé, affecte tous les chapitres
)

data class BookMetadata(
    val summaries: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val ages: List<Int> = emptyList(),
    val categories: List<String> = emptyList(),
    val places: List<String> = emptyList(),
    val categoryCounts: Map<String, Int> = emptyMap()
)

data class AiMessage(
    val role: String = "user", // "user" | "model"
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
