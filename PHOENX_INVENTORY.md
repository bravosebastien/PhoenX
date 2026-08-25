# 📖 Inventaire Fonctionnel PHOEN-X (v9.2)

## 1. Vue d'ensemble Architecture & Sécurité
*   **Modèle Local-First (Room)** : Persistance immédiate dans SQLite pour une réactivité maximale sans réseau.
*   **Synchronisation Cloud (Firestore / Storage)** : Merge différentiel descendant (InitialSyncWorker) et upload ascendant (SyncWorker) pour la cohérence multi-appareils.
*   **Chiffrement de Bout-en-Bout (E2EE)** : Utilisation de **Google Tink (AES-256-GCM)** pour les contenus des souvenirs. Clés maîtres gérées via Firebase et protégées par authentification.
*   **Navigation en 3 Graphes (Compose Navigation)** :
    1.  **AuthGraph** : Inscription, Connexion, Récupération de clé.
    2.  **CreatorGraph** : Espace du propriétaire (Capture, Fil, Livre, Cercle).
    3.  **RecipientGraph** : Espace des héritiers (Cube de mémoire, Consultation post-protocole).

---

## 2. Domaines Fonctionnels

### A. Authentification & Accès
*   **Fichiers** : `AuthScreen.kt`, `AuthViewModel.kt`, `RecoveryScreen.kt`, `EncryptionManager.kt`.
*   **Rôles** : Gère la création de compte, le chiffrement de la clé de session, l'onboarding et l'acceptation des CGU.
*   **État** : Fonctionnel. *Dette : Les liens CGU/Confidentialité pointent vers des placeholders.*

### B. Capture de Souvenirs (L'Âme du Souvenir)
*   **Fichiers** : `CaptureScreen.kt`, `CaptureViewModel.kt`, `AudioCaptureContent.kt`, `PhotoCaptureContent.kt`, `TextCaptureContent.kt`, `AdvancedOptionsContent.kt`.
*   **Rôles** : Flux de création en deux étapes (1. Contenu brut, 2. Habillage/Rangement). Supporte le texte, la voix (STT), la photo et la galerie.
*   **État** : Stable. Câblage Étape 1/2 et Ton de l'Âme validés.

### C. Fil de Pensée (Timeline)
*   **Fichiers** : `FilScreen.kt`, `FilViewModel.kt`, `MemoryDetailScreen.kt`.
*   **Rôles** : Chronologie interactive des souvenirs. Permet le filtrage par catégorie, par destinataire ou par personnage cité.
*   **État** : Fonctionnel. Correction du merge différentiel effectuée pour la restauration cloud.

### D. Livre de Vie & IA Biographe
*   **Fichiers** : `BookGeneratorService.kt`, `BookReaderFlowScreen.kt`, `BookEditorScreen.kt`.
*   **Rôles** : Orchestre la génération de chapitres via Gemini. Lecture immersive en flux continu avec illustration dynamique des proches.
*   **État** : Stable (v9.4.29). Détection automatique des personnages cités et insertion de photos inline validée.

### E. Le Cercle de Confiance (Héritage)
*   **Fichiers** : `CercleConfianceScreen.kt`, `RecipientsScreen.kt`, `WitnessInviteScreen.kt`, `DepositaryViewModel.kt`.
*   **Rôles** : Gestion des Destinataires (héritiers), des Témoins (récits externes) et des Dépositaires (gardiens du protocole).
*   **État** : Stable.

### F. Les Personnages & Portraits de Proches (v9.2)
*   **Fichiers** : `CharactersScreen.kt`, `CharactersViewModel.kt`, `CharacterEditScreen.kt`, `PortraitScreen.kt`, `PortraitViewModel.kt`.
*   **Rôles** : Répertoire des proches. Nouveau flux "Portrait" (v9.2) : 20 questions introspectives pour léguer une vision profonde de chaque proche (sauvegarde atomique dans le Fil).
*   **État** : Fonctionnel. Suppression avec cascade manuelle (nettoyage des tags dans les souvenirs) validée.

### G. Mon Portrait de Vie (v9.1)
*   **Fichiers** : `CreatorRichProfileScreen.kt`, `CreatorRichProfileViewModel.kt`, `CreatorProfileEntity.kt`.
*   **Rôles** : Profil biographique détaillé du Créateur lui-même (physique, famille, parcours). Transmis à l'IA pour personnaliser le ton du récit.
*   **État** : Fonctionnel (Sync Firestore immédiate).

### H. Coffre-Fort & Mode Détective
*   **Fichiers** : `DetectiveHomeScreen.kt`, `DetectiveCreateScreen.kt`, `EnigmaUtils.kt`.
*   **Rôles** : Scellement de souvenirs derrière des énigmes. Supporte le "Secret Ultime" sans déblocage automatique.
*   **État** : Stable.

### I. Mappemonde (Géolocalisation)
*   **Fichiers** : `MappamondeScreen.kt`, `LocationDetailScreen.kt`.
*   **Rôles** : Visualisation spatiale des souvenirs. Regroupement par lieux et gestion des métadonnées GPS.
*   **État** : Fonctionnel.

### J. Vérification de Présence (Silence)
*   **Fichiers** : `SilenceOnboardingScreen.kt`, `SilenceCheckInScreen.kt`, `SilenceManager.kt`.
*   **Rôles** : Gère le rythme de vie (Check-in). Déclenche les alertes dépositaires en cas de silence prolongé.
*   **État** : Fonctionnel. Fix de la synchronisation du flag d'onboarding (v8.9.9b).

### K. Mon Quiz (Transmission Ludique)
*   **Fichiers** : `QuizCreateScreen.kt`, `QuizPlayScreen.kt`, `QuizViewModel.kt`.
*   **Rôles** : Permet au Créateur de créer des questions sur sa propre vie. L'IA aide à générer des distracteurs crédibles.
*   **État** : Fonctionnel (Cloud-only : Firestore).

### L. Le Miroir à Deux (Lien entre Créateurs)
*   **Fichiers** : `PactScreen.kt`, `PactDetailScreen.kt`, `PactViewModel.kt`.
*   **Rôles** : Système de souvenirs liés entre deux utilisateurs distincts (souvenirs communs).
*   **État** : Stable.

### M. Réconciliation (Derniers mots)
*   **Fichiers** : `ReconciliationScreen.kt`, `ReconciliationViewModel.kt`.
*   **Rôles** : Espace dédié aux messages de paix ou de clôture, destinés à être lus après le départ.
*   **État** : Stable.

### N. Capsules Temporelles & Mailbox
*   **Fichiers** : `MailboxScreen.kt`, `MailboxViewModel.kt`.
*   **Rôles** : Gestion des messages programmés dans le futur (ouvertures différées selon des dates précises).
*   **État** : Fonctionnel.

### O. Médiathèque (Héritage Multimédia)
*   **Fichiers** : `RecipientDiscothequeScreen.kt`, `RecipientVideothequeScreen.kt`, `MediaViewerScreen.kt`.
*   **Rôles** : Espaces dédiés à la consultation des médias triés par type pour les héritiers.
*   **État** : Fonctionnel.

### P. Bibliothèque — Rendu Visuel (L'Art de l'Archive)
*   **Fichiers** : `CompartmentPainter.kt`, `CulturePainter.kt`, `TransmissionPainter.kt`, `ExplorationPainter.kt`.
*   **Rôles** : Moteurs de rendu graphique pour les différents compartiments de la bibliothèque, créant une esthétique "papier et encre".
*   **État** : Stable.

### Q. Espace Héritier (Post-activation)
*   **Fichiers** : `HeirHeritageScreen.kt`, `HeirAllocationScreen.kt`, `RecipientCubeScreen.kt`, `RecipientBooksScreen.kt`, `RecipientArchiveScreen.kt`, `RecipientPermissionsScreen.kt`.
*   **Rôles** : Interface de consultation pour les héritiers une fois le protocole validé. Permet d'accéder au Cube 3D, au Livre de Vie et aux archives chiffrées.
*   **État** : Fonctionnel.

### R. Invitation & Accès Invité
*   **Fichiers** : `UniversalJoinScreen.kt`, `UniversalJoinViewModel.kt`, `GuestDashboardScreen.kt`, `UniversalFeedScreen.kt`, `UniversalMessageScreen.kt`.
*   **Rôles** : Gère l'onboarding des nouveaux membres invités (Témoins, Dépositaires, Destinataires) et leur tableau de bord simplifié.
*   **État** : Stable.

### S. Réglages & Accessibilité
*   **Fichiers** : `SettingsScreen.kt`, `AccessibilitySettingsScreen.kt`, `NotificationContactsScreen.kt`.
*   **Rôles** : Paramètres généraux de l'app, options d'accessibilité (contraste, voix) et gestion des contacts d'urgence.
*   **État** : Fonctionnel.

### T. Apparence & Identité Visuelle (v9.2)
*   **Fichiers** : `ThemeViewModel.kt`, `LocalAppTheme.kt`, `PhoenXMatiere.kt`.
*   **Rôles** : Centralisation de la charte graphique dynamique (couleurs d'accentuation, textures "matière").
*   **État** : En cours de déploiement (v9.2).

---

## 3. Entités de Données (Room - SQLite)

| Entité | Rôle |
| :--- | :--- |
| `OfflineEntry` | Souvenir chiffré (Texte/Audio/Photo) avec métadonnées IA. Sert aussi de conteneur pour les **Portraits de proches** (type `PORTRAIT`). |
| `AmendmentEntity` | Addendum textuel lié à un souvenir existant. |
| `PortraitEntity` | Résumés biographiques intermédiaires (Portrait d'Essence) générés par l'IA. |
| `FavoriteEntity` | Marqueur de préférence pour la sélection des chapitres du Livre. |
| `RecipientEntity` | Identité et droits d'accès d'un héritier du Livre. |
| `DepositaryEntity` | Gardien local (un seul actif) chargé de la transmission. |
| `LegacyEntity` | Structure des héritages et transmissions globales. |
| `PactEntity` | Liaison de souvenirs entre deux utilisateurs distincts (Le Miroir à Deux). |
| `WitnessEntity` | Statut et contenu des témoignages externes sollicités. |
| `NotificationContactEntity` | Email/Nom des personnes à prévenir au déclenchement du protocole. |
| `PersonEntity` | Fiche détaillée d'un proche cité (Portrait Cameo + bio v9.0). |
| `CreatorProfileEntity` | Portrait de vie enrichi du propriétaire du livre (v9.1). |

---

## 4. Cloud Functions (Logique Serveur & IA)

| Fonction | Rôle |
| :--- | :--- |
| `analyzeEntry` | Extraction de thèmes/émotions via Gemini pour les métadonnées. |
| `generateBiographerQuestion` | Génère une question personnalisée pour stimuler la capture. |
| `generateEssencePortrait` | Synthèse des traits de caractère à partir du Fil de Pensée. |
| `detectThoughtEvolution` | Analyse les transitions thématiques au fil des âges. |
| `generateYoungSelfSuggestions` | Aide à la rédaction de lettres au "Jeune Moi". |
| `generateBookChapters` | Rédaction stylisée du manuscrit à partir des scènes de vie. |
| `generateDistractors` | Génère des fausses réponses crédibles pour les Quiz. |
| `checkCreatorSilence` | Tâche planifiée vérifiant l'absence de signes de vie. |
| `activateProtocol` | Passage du compte en mode "Héritage" (déblocage des clés). |
| `scheduledNotifications` | Envoi différé des notifications de protocole. |
| `resolveCreatorSilence` | Rétablissement du statut OK après un retour utilisateur. |
| `notifyQuestionRightGranted` | Notifie un héritier qu'il peut poser des questions. |
| `notifyNewPendingQuestion` | Notifie le Créateur qu'une question l'attend. |
| `notifyNewTestimony` | Notifie le Créateur d'un nouveau témoignage reçu. |
| `sealPendingQuestion` | Verrouillage définitif d'une réponse apportée à l'IA Biographe. |
| `generateDepositaryInviteToken` | Crée un token sécurisé pour inviter un Dépositaire. |
| `generateDepositaryShortCode` | Génère un code court (4h) pour liaison rapide Dépositaire. |
| `redeemDepositaryShortCode` | Valide un code court et renvoie les infos de liaison. |
| `joinAsDepositary` | Effectue la liaison atomique entre Créateur et Dépositaire. |
| `sendWitnessInvitation` | Envoie une invitation par email à un futur Témoin. |
| `verifyWitnessToken` | Valide l'accès d'un Témoin à son espace de rédaction. |
| `submitWitnessTestimony` | Enregistre le témoignage scellé du Témoin. |
| `generateUniversalInvitation` | Système de liaison v7.2 pour tous les rôles. |
| `getInvitationDetails` | Récupère les détails d'une invitation universelle. |
| `getCreatorBookStatus` | Vérifie pour un héritier si le livre est accessible. |
| `getCreatorProtocolStatus` | Statut du protocole de transmission pour l'héritier. |
| `acceptUniversalInvitation` | Transaction finale d'acceptation de rôle. |
| `migrateLegacyRoles` | Script de compatibilité pour les structures de données anciennes. |
| `onWitnessDeleted` | Nettoie les liens lors de la suppression d'un Témoin. |
| `onRecipientDeleted` | Nettoie les liens lors de la suppression d'un Destinataire. |
| `onDepositaryDeleted` | Nettoie les liens lors de la suppression d'un Dépositaire. |
| `onUserDeletedCleanup` | Nettoyage intégral de toutes les données lors de la suppression du compte. |
| `becomeCreator` | Migration d'un compte invité vers le statut de propriétaire de livre. |
| `modifyBookChapter` | Permet à l'auteur de faire retravailler un chapitre par l'IA. |
| `generateGlobalIntro` | Rédaction de l'introduction personnalisée du Livre de Vie. |

---
*Document mis à jour le 23 octobre 2024. Référence de contexte Phoen-X v9.2.*
