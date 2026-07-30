# PHOEN-X — Inventaire Fonctionnel & Technique (v9.3.3)

Ce document récapitule l'architecture et les fonctionnalités du projet PHOEN-X après l'implémentation du système de dépôts directs (StandaloneMedia), de la refonte du Livre de Vie (Plan IA, Personnages) et de la propagation de liaison des UIDs.

---

## 1. ARCHITECTURE DES DONNÉES (Room & Local)

### A. Entités Room (Base de données locale)
*   `OfflineEntry` : Cœur du système. Stocke les souvenirs (Texte/Audio/Photo) chiffrés avec leurs métadonnées IA (aiSummary, aiTags).
*   `StandaloneMediaEntity` : (NOUVEAU v9.3) Dépôts directs (Spotify, YouTube, Photo, Littéraire) décorrélés des récits de capture. Supporte le CRUD complet et le chiffrement Tink.
*   `AmendmentEntity` : Extensions ou corrections apportées à un souvenir existant.
*   `PersonEntity` : (ENRICHI v9.3) Répertoire détaillé des personnages (Humains/Animaux) avec attributs physiques et portrait Cameo. Transmis à l'IA Biographe.
*   `CreatorProfileEntity` : (v9.1) Portrait de vie enrichi du propriétaire (Bio, profession, famille, physique).
*   `PortraitEntity` : Entité de stockage technique (en réserve). Les vrais portraits de proches utilisent désormais le système flexible `OfflineEntry`.
*   `RecipientEntity` : Liste des proches (Destinataires) avec liaison UID Firebase (`linkedUid`).
*   `DepositaryEntity` : (REFONDU v9.2) Dépositaires de confiance gérés en sous-collection Firestore et Room.
*   `WitnessEntity` : Témoins pour le "Pacte" ou les témoignages post-mortem.
*   `NotificationContactEntity` : Contacts d'urgence pour le protocole de silence.
*   `PactEntity` : Contrats de confiance bilatéraux.
*   `FavoriteEntity` : Marquage des souvenirs préférés.
*   `LegacyEntity` : Ancienne structure de legs (en cours de fusion avec le Livre).

---

## 2. DOMAINES FONCTIONNELS

### B. Capture & IA Narrative (Le Créateur)
*   **Fichiers** : `CaptureScreen.kt`, `CaptureViewModel.kt`, `AdvancedOptionsContent.kt`, `AIManager.kt`.
*   **Rôles** : Pipeline de création multimédia (Texte, Audio, Photo, Nuit). Gère le chiffrement Tink local et l'analyse IA (Gemini Nano local + Vertex AI Cloud).
*   **Nouveauté v9.3** : Prise en compte du "Ton de l'Âme" (soulTone) et distinction de l'origine (MEMORY/QUESTION/PORTRAIT).

### C. Le Livre de Vie (Le Manuscrit Post-Mortem)
*   **Fichiers** : `BookEditorScreen.kt`, `BookEditorViewModel.kt`, `BookGeneratorService.kt`, `BookStateViews.kt`.
*   **Rôles** : Orchestration du biographe IA.
*   **Flux v9.3** :
    1.  Calcul du ton dominant et insights d'évolution (detectThoughtEvolution).
    2.  Génération d'un **Plan de Livre** validable par le Créateur avant rédaction.
    3.  Rédaction finale structurée avec insertion de balises médias [PHOTO:id].
    4.  Garde-fou : Minimum 10 souvenirs requis pour la 1ère génération.

### D. Dépôts Directs & Vitrines (StandaloneMedia)
*   **Fichiers** : `LiteraryLibraryScreen.kt`, `RecipientDiscothequeScreen.kt`, `RecipientVideothequeScreen.kt`, `RecipientPhotosScreen.kt`, `StandaloneMediaRepository.kt`.
*   **Rôles** : Permet de léguer des pépites isolées sans passer par le flux de Capture.
    *   **Bibliothèque Littéraire** : Extraits de textes chiffrés (Tink).
    *   **Discothèque** : Liens Spotify (Publics mais accès scellé).
    *   **Vidéothèque** : Liens YouTube (Publics mais accès scellé).
    *   **Photothèque** : Photos chiffrées (Tink) avec upload vers Storage (`/standalone_photos/`).
*   **CRUD** : Support de la suppression, modification (Littéraire) et gestion de la visibilité par UID.

### E. Le Cercle de Confiance (Transmission)
*   **Fichiers** : `TrustCircleScreen.kt`, `CharactersScreen.kt`, `RecipientSelector.kt`.
*   **Rôles** : Gestion des proches et des dépositaires.
*   **Propagation UID (NOUVEAU v9.3.2)** : Mécanisme automatique (Cloud Function) qui remplace les IDs de documents locaux par les vrais UIDs Firebase partout dans la base lors de l'acceptation d'une invitation.
*   **Backfill** : Outil admin de rattrapage des UIDs pour les anciens comptes.

### F. Home & Immersion
*   **Fichiers** : `HomeScreen.kt`, `AnimatedEarthCard.kt`, `HomeVideoGallery.kt`.
*   **Rôles** : Hub central.
*   **Visuel** : Terre animée (texture 300dp) avec rotation 60fps sans lag (manual bitmap loading).
*   **Galerie Guide** : Grille 3x2 de vidéos de présentation gérée via Firestore `presentationVideos`.

---

## 3. CLOUD FUNCTIONS (Backend Node.js)

### IA & Contenu
*   `analyzeEntry` : Analyse initiale des souvenirs (ton, tags, résumé).
*   `generateBiographerQuestion` : Pose des questions personnalisées basées sur le Fil de Pensée.
*   `detectThoughtEvolution` : Analyse les transitions thématiques au fil de la vie.
*   `generateBookPlan` : (NOUVEAU v9.3) Propose un sommaire structuré à partir des scènes.
*   `generateBookChapters` : Rédaction finale du livre.
*   `modifyBookChapter` : Permet à l'utilisateur de retoucher un chapitre via prompt.
*   `generateGlobalIntro` : Rédige l'introduction poétique du Livre.

### Protocole & Sécurité
*   `checkCreatorSilence` : Tâche planifiée vérifiant l'inactivité du Créateur.
*   `activateProtocol` : Déclenche l'ouverture du Livre et du Coffre-fort par le Dépositaire.
*   `acceptUniversalInvitation` : (ENRICHI v9.3.2) Gère la liaison UID et lance la **propagation de liaison** automatique.
*   `backfillRecipientUids` : (NOUVEAU v9.3.2) Script de secours admin pour réparer les IDs de l'existant.

### Social & Témoignages
*   `generateUniversalInvitation` : Crée un token d'invitation pour un rôle (Recipient/Depositary/Witness).
*   `submitWitnessTestimony` : Enregistre le témoignage d'un proche pour le legs.
*   `sealPendingQuestion` : Scelle une réponse du Créateur destinée à un proche.

---

## 4. PIPELINE DE SÉCURITÉ MÉDIA
1.  **Capture/Dépôt** : Le fichier est lu sur le device.
2.  **Chiffrement** : AES-256-GCM (Tink) appliqué sur les octets bruts (Photo/Audio/Texte).
3.  **Transit** : Envoi de l'URL chiffrée (Blob) dans Firestore et du fichier crypté (.enc) dans Storage.
4.  **Lecture** : Impossible sans la clé miroir (verrouillée jusqu'au statut `"activated"` uniformément pour l'ensemble des souvenirs et dépôts).
