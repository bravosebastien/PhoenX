# PHOEN-X — Référentiel des Zones Stables

Ce fichier répertorie les modules et fichiers considérés comme stabilisés, testés et validés.
**RÈGLE D'OR :** Ne jamais modifier ces zones sans instruction explicite et ciblée dans le prompt. Toute modification non demandée est proscrite, même pour du "nettoyage" ou de l'optimisation.

## 1. Personnalités
*   **Fichiers :** `PersonalityDetailScreen.kt`, ViewModels associés, `PersonalityEntity.CATEGORIES`.
*   **Sécurité :** Règles Firestore (`personalities` et `personalities/media`).
*   **État :** Synchronisation multi-appareils, suppression sécurisée (`NonCancellable`/`Dispatchers.IO`), refonte visuelle v9.7.6-9.7.7 validée.

## 2. Rencontres — Performance
*   **Fichiers :** `ImageUtils.kt`, `DecryptedCache.kt`, `SecureAsyncImage.kt`.
*   **Sync :** Blocs de préchargement dans `InitialSyncWorker.kt` (`encounterImagePath` et médias de Rencontres).
*   **Attention :** Fichiers partagés avec Coffre-Fort, Livre et Souvenirs. Risque élevé de régression.

## 3. Icônes d'action
*   **Fichiers :** `PersonalityDetailScreen.kt`, `EncounterDetailScreen.kt` (en-tête TopAppBar).
*   **Style :** Glassmorphism léger validé.
*   **Exception :** `MediaViewerScreen.kt` garde sa croix flottante par choix de design.

## 4. Mappemonde / Sync
*   **Fichier :** `EntryMapper.kt`.
*   **Fonction :** `Map<String, Any?>.toOfflineEntry(...)` (Latitude, longitude, locationId, locationName, pactId, tonalNuance).
*   **État :** Corrigé et testé pour la persistance des lieux.

## 5. InitialSyncWorker.kt
*   **Fichier :** `InitialSyncWorker.kt`.
*   **Règle :** Modification strictement limitée au bloc concerné par la demande. Aucun remaniement global autorisé.

## 6. Sécurité Firestore
*   **Fichier :** `firestore.rules`.
*   **Règle :** Ne pas réécrire les blocs existants (users, entries, persons, personalities, book, entry_keys, etc.). Toujours effectuer un comptage automatisé des blocs `match` avant/après déploiement.
