# PHOEN-X — Inventaire Fonctionnel & Technique (v9.4.5)

Ce document récapitule l'architecture et les fonctionnalités du projet PHOEN-X, incluant le renforcement critique de la sécurité (lots v9.4.0 à v9.4.5).

---

## 1. SÉCURITÉ — DERNIERS CORRECTIFS (v9.4.0 à v9.4.5)

*   **Verrouillage Post-Mortem Unifié** : Les règles Firestore (`entries`, `standaloneMedia`, `book`, `quizzes`, `legacies`) exigent désormais `protocolStatus == "activated"` pour tout accès tiers. Les données restent scellées tant que le décès n'est pas confirmé et incontesté.
*   **Dénormalisation des Dépositaires** : Ajout du champ `depositaryUids` (tableau d'UIDs) sur le document `users/{userId}`. Permet un contrôle d'accès ultra-rapide aux Security Rules pour le Dashboard Dépositaire sans multiplier les lectures `exists()`.
*   **Révocation & Nettoyage Robuste** :
    *   `revokeUidAccess` : Lors du retrait d'un proche, les accès sont révoqués atomiquement (retrait des tableaux `recipientIds` ou passage à `null` pour les champs simples avec archivage `previousRecipientId`).
    *   `cleanupMemberRoles` : Sécurisation par `try/catch` pour garantir que le nettoyage du Créateur s'exécute même si le profil du membre supprimé n'existe plus.
*   **Infrastructure Médias (Héritage)** :
    *   **Storage Rules** : Accès direct verrouillé au propriétaire seul (`owner-only`).
    *   **URLs Signées** : Création de la Cloud Function `getInheritedFileUrl`. Elle vérifie le décès, le rôle et les `recipientIds` avant de générer une URL de téléchargement temporaire (15 min).
*   **Anti-Brute-Force (Short Codes)** : Pattern transactionnel "no-throw" dans `redeemDepositaryShortCode`. Le compteur d'échecs (`rateLimits`) est persisté dans tous les cas, même si le code est invalide ou expiré, bloquant toute tentative de probing.
*   **Garde de Signe de Vie** : `notifyDeathContactsInternal` refuse désormais l'activation si `escalationLevel < 3` ou si un "check-in" a eu lieu après la demande de protocole.
*   **Relais Mail Fermé** : La collection `/mail` est désormais interdite en écriture directe au client. Tout envoi passe par des Cloud Functions dédiées.

---

## 2. ARCHITECTURE DES DONNÉES

### A. Entités Room (Local)
*   `OfflineEntry` : Souvenirs chiffrés (Texte/Audio/Photo) + Métadonnées IA.
*   `StandaloneMediaEntity` : Dépôts directs (Spotify, YouTube, Photo, Littéraire) avec CRUD complet.
*   `AmendmentEntity` : Extensions/corrections atomiques.
*   `PersonEntity` : Répertoire des personnages (Cameos Storage).
*   `CreatorProfileEntity` : Portrait de vie enrichi (Bio, famille).
*   `RecipientEntity` & `DepositaryEntity` : Gestion du cercle et des gardiens.
*   `WitnessEntity`, `NotificationContactEntity`, `PactEntity`, `LegacyEntity`.

---

## 3. DOMAINES FONCTIONNELS

### B. Le Livre de Vie (IA Narrative)
*   **Flux** : Souvenirs (min 10) -> IA (Ton de l'âme) -> Plan de Livre (Chapitres) -> Validation Créateur -> Rédaction 1ère personne avec balises médias [PHOTO:uuid].
*   **Sécurité** : Chiffrement Tink (AES-256-GCM) sur le manuscrit.

### C. Médiathèques & Dépôts Directs
*   **4 Vitrines** : Littérature, Discothèque (Spotify), Vidéothèque (YouTube), Archives Photos.
*   **Liaison** : Attribution spécifique à des héritiers. Visibilité restreinte par défaut.

### D. Home & Administration
*   **Terre Animée** : Visualisation immersive pilotée par Remote Config.
*   **Boutons Admin (UID bLRN...)** : 
    *   `RATTRAPAGE UIDs` : Propage les UIDs Firebase dans les anciennes archives.
    *   `RATTRAPAGE DÉPOSITAIRES` : Reconstruit le tableau `depositaryUids` pour la sécurité.

---

## 4. CLOUD FUNCTIONS (Gen2 - Cloud Run)

*   **Accès** : `getInheritedFileUrl` (Signature de blobs Storage).
*   **Protocole** : `checkCreatorSilence`, `activateProtocol`, `resolveCreatorSilence`.
*   **Social** : `acceptUniversalInvitation`, `joinAsDepositary`, `sealPendingQuestion`.
*   **IA** : `analyzeEntry`, `generateBookPlan`, `generateBookChapters`, `modifyBookChapter`.
*   **Rattrapage** : `backfillRecipientUids`, `backfillDepositaryUids`.

---

## 5. CONFIGURATION INFRASTRUCTURE REQUISE
1.  **IAM** : Le compte de service COMPUTE par défaut (`962448590786-compute@...`) doit posséder le rôle **"Créateur de jetons de compte de service"** pour signer les URLs.
2.  **Secrets** : `GEMINI_API_KEY` et `SMSPARTNER_API_KEY` configurés dans Secret Manager.
3.  **Rules** : Firestore & Storage déployées en version 9.4.5.
