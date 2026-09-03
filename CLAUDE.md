# PHOEN-X — Contexte projet pour Claude Code

Ce fichier donne à Claude Code une connaissance complète de l'écosystème PHOEN-X avant toute intervention sur le code. **Lis-le entièrement avant de commencer une tâche.** Il est basé sur le Document Technique Maître v12 (2 septembre 2026), la Présentation de l'Écosystème v5, le fichier des zones stables, et les derniers compléments de session. Il devra être mis à jour au fil des chantiers (voir section finale).

---

## 1. Ce qu'est PHOEN-X, en une phrase

Application Android de transmission mémorielle et d'héritage numérique. Le **Créateur** dépose des souvenirs de son vivant ; à sa disparition, confirmée via un protocole impliquant un **Dépositaire**, ces souvenirs sont transmis à des **Destinataires**. Des **Témoins** offrent en parallèle un témoignage confidentiel sur le Créateur. Des **Contacts de Notification** reçoivent un simple email, sans accès à l'application. Une **Famille** (accès collectif) reste non construite. Phrase signature : « Ce n'est pas une archive. C'est une présence. »

**Vocabulaire imposé :** ne jamais utiliser le mot « Héritier » dans une chaîne visible par l'utilisateur (raison légale : sens juridique précis en droit français) — toujours « Destinataire ». Les noms de fichiers/variables internes historiques (`HeirAllocationScreen.kt`, `isHeirMode`, `heirKey`) restent en l'état, volontairement.

**Positionnement de sécurité obligatoire dans toute communication :** PHOEN-X n'est **PAS** Zero-Knowledge absolu (les clés transitent par des Cloud Functions pour permettre la transmission post-mortem). C'est une architecture "Zero-Trust Access Control". Formulation correcte : « Les contenus médias et récits sont chiffrés sur l'appareil du Créateur (AES-256-GCM / RSA-2048) avant tout transfert Cloud. L'infrastructure PHOEN-X agit comme un tiers de confiance : les données restent chiffrées au repos et ne sont jamais analysées, lues ni exploitées à des fins commerciales. Le déchiffrement côté Destinataire est conditionné par un protocole de sécurité strict. » Ne jamais écrire « Zero-Knowledge absolu ».

---

## 2. Stack technique

Android Studio + Kotlin 2.1.10 · Jetpack Compose BOM 2024.10.01 · MVVM + Hilt + KSP · Firebase Auth BOM 33.1.2 · Firestore BOM 33.1.2 · Room (migrations sans trou, dernière connue v57+, vérifier au démarrage) · Firebase Storage (chemins relatifs uniquement, jamais d'URL complète) · Cloud Functions TypeScript us-central1 (~50 fonctions, `nodejs22` — migration confirmée faite le 27 août 2026, non urgente) · FCM · AES-256-GCM (contenu) + RSA-2048/OAEP-SHA256/MGF1-SHA1 (Android Keystore) · IA cloud `@google/genai`, modèle **`gemini-3.5-flash`** exclusivement (`ai.ts` ligne 11, `AI_MODEL`) · WorkManager 2.9.1 · SpeechRecognizer natif · Google Maps SDK + Maps Compose 19.0.0 · ExoPlayer Media3 1.3.1 · Coil 3.0.0 + `SecureAsyncImage` (composant maison) · AudioRecord → WAV maison (pas MediaRecorder/MP4) · `LocalAppTheme` (CompositionLocal maison) · package `app.phoenx.mobile` (namespace interne resté `com.example.phoenx.*`, sans impact) · domaine `phoenx.app` connecté et SSL validé.

### Protocole de vérification — non négociable

- **`./gradlew assembleDebug` ou `compileDebugKotlin` est la SEULE source de vérité.** Jamais une relecture de code, jamais une description d'un correctif ne suffit.
- Une sortie « X up-to-date » sur TOUTES les tâches juste après une modification = signal d'alarme (rien n'a réellement été recompilé). Exiger `./gradlew clean` puis rebuild.
- **BUILD SUCCESSFUL ne garantit JAMAIS qu'un comportement fonctionne réellement sur un appareil.** Toujours exiger un test réel sur appareil ET, quand possible, une vérification directe en Console Firebase.
- Un correctif de CODE Kotlin commité ne change RIEN sur un appareil déjà installé tant qu'un nouveau `.aab` n'a pas été régénéré ET republié — contrairement aux règles de sécurité Firestore et aux Cloud Functions, effectives immédiatement dès déploiement (`firebase deploy --only functions` / `--only firestore:rules`). **Un fichier modifié mais jamais déployé ne sert à rien** — piège vécu plusieurs fois (Cloud Functions comme APK).
- Sous PowerShell Windows, la redirection `>` d'une commande produisant des données BINAIRES (ex. `adb shell cat fichier.wav`) corrompt le fichier (ré-encodage type UTF-16, taille doublée). Utiliser `adb exec-out` ou `cmd /c "... > fichier"`. Toujours comparer la taille en octets extraite vs. sur l'appareil avant de conclure.

---

## 3. Les 30 règles absolues / pièges connus (à ne JAMAIS reproduire)

Cette liste est cumulative depuis le début du projet — chaque numéro correspond à un incident réel vécu.

1. Les écritures client sur `isCreator` et `myRoles` sont systématiquement rejetées par construction (règles Firestore).
2. Toute migration Room exige 3 preuves : champ déclaré sur l'entité, bloc `MIGRATION_X_Y` réel, enregistrement dans `addMigrations()`.
3. Les index Firestore composites doivent être anticipés dès la conception d'une requête.
4. Les App Links ne sont jamais suffisants avec le seul attribut `autoVerify`.
5. Media3/UnstableApi : géré par flag de compilation global (`freeCompilerArgs`), pas par annotation locale.
6. Ne jamais imbriquer une `LazyColumn` dans une autre.
7. Ne jamais détourner un champ existant de sa vocation d'origine pour y stocker une donnée différente — toujours un nouveau champ + migration dédiée.
8. Tout refactor de grande ampleur se fait par lots successifs, avec un vrai build de vérification entre chaque lot.
9. **« Description ≠ preuve »** — principe fondamental, s'applique à absolument tout correctif annoncé (par un assistant de code notamment).
10. Un `where()` sur un champ/collection inexistant renvoie zéro document, en silence total, sans erreur.
11. Dans une transaction Firestore : toutes les lectures avant toute écriture, strictement.
12. Ne jamais confondre un identifiant de document local (Room) et un vrai identifiant utilisateur Firebase (UID).
13. Comparer systématiquement la liste complète des blocs `match /` avant/après tout déploiement de règles (comptage automatisé : `Select-String "match /"` sur `firestore.rules`).
14. Ne jamais introduire de porte dérobée, même temporaire, même pour tester.
15. `setTimeout` ne garantit rien dans un environnement serverless.
16. Un `LaunchedEffect` doit être déclenché par un contenu précis et intentionnel, jamais par un effet de bord accidentel.
17. `git reset --hard` : jamais poussé vers le dépôt distant, local uniquement.
18. Aucun test automatisé n'existe dans le projet à ce jour — toute vérification repose sur relecture de code, test manuel, logcat réel, Console Firebase.
19. Un build « X up-to-date » sur toutes les tâches après une modification = signal d'alarme (voir section 2).
20. Un nettoyage `.distinct()`/`.trim()` ajouté uniquement en LECTURE ne corrige que l'affichage — les données déjà corrompues en base restent corrompues jusqu'à leur prochaine ÉCRITURE réelle.
21. **Un `PERMISSION_DENIED` sur un "listen" Firestore peut persister même après correction ET déploiement confirmés**, à cause du cache de persistance locale sur disque — parfois un vrai « Vider les données » de l'application est nécessaire (pas juste un redémarrage).
22. Une écriture en arrière-plan qui réussit (log "Upload RÉUSSI") ne prouve rien sur la capacité de l'écran à LIRE cette donnée juste après — écriture différée et listener actif peuvent avoir des causes d'échec totalement indépendantes.
23. Vérifier qu'un bouton de sauvegarde unifié couvre bien TOUS les champs modifiables d'un écran, y compris ceux ajoutés après coup.
24. Sur toute demande de comportement en cascade (suppression, nettoyage de références orphelines) : toujours vérifier le comportement RÉEL sur un cas concret construit à la main, jamais se fier à la seule description.
25. Un rendu validé sur un format d'écran (téléphone) n'est PAS automatiquement valide sur un autre (tablette) — toujours tester sur chaque format visé.
26. Pour toute clé API restreinte par empreinte de certificat (Google Maps etc.) : distinguer les 3 empreintes SHA-1 possibles (production Play, debug.keystore local — qui peut différer par poste —, empreinte historique d'un ancien package). Un renommage de package exige de dupliquer CHAQUE empreinte, pas seulement celle de production.
27. **Le piège le plus insidieux du projet : du code placé après un `.collect{}` dans le même `viewModelScope.launch` est du code MORT à l'exécution, sans aucun avertissement de compilation.** Chaque écouteur réactif indépendant doit vivre dans son propre `launch`.
28. Le paramètre `error` d'un `addSnapshotListener` ne doit jamais être ignoré — un listener peut échouer silencieusement sans autre signal.
29. Une simulation mentale par un assistant de code, même très détaillée et convaincante, **n'est jamais une preuve** — seul un vrai test device (ou un vrai log runtime) fait foi. Un correctif "décrit" avec un résumé technique complet n'a pas toujours été réellement écrit dans les fichiers — exiger de voir le contenu réel du fichier modifié.
30. Ne jamais reconstruire une fonctionnalité supposée manquante sans vérifier au préalable qu'elle n'existe pas déjà.

**Pièges complémentaires, plus récents :**
- Un abandon silencieux type `val user = auth.currentUser ?: return` sur un écran atteignable par lien externe (avant restauration de session Firebase Auth) peut bloquer indéfiniment sans erreur — 6 routes à accès direct identifiées et auditées (voir section 8).
- **Ne jamais écrire à l'intérieur du `addSnapshotListener` qui surveille le document même sur lequel on écrit** — boucle réactive infinie garantie (incident critique du 25-26 août, voir section 6).
- **Ne jamais réparer silencieusement une clé cryptographique en cas de divergence détectée** — consigner et alerter seulement ; toute réparation reste un geste manuel délibéré.
- Un test `endsWith(".enc")` sur une URL de fichier distant est fragile (une URL Firebase se termine par un jeton `?alt=media&token=...`, jamais par l'extension) → utiliser `contains(".enc")`. Ce test fragile peut exister ailleurs, non encore audité partout.
- Une régénération qui invalide la clé de chiffrement du contenu qu'elle remplace rend caduque toute sauvegarde qui n'aurait sauvegardé que le contenu, pas la clé.
- Un fait exact produit par une IA générative ne prouve pas que le mécanisme a puisé dans la bonne source autorisée — à vérifier avec de vraies données, jamais par supposition.
- Un champ média ajouté pour un usage nouveau, sans reproduire les mécanismes de performance déjà éprouvés ailleurs (compression, préchargement, cache local), réintroduit une régression de performance déjà résolue ailleurs.
- Une fonction de conversion (mapper) qui lit des champs uniquement pour un log de diagnostic, sans les transmettre au constructeur de l'objet final, provoque une perte de données silencieuse — visible dans les logs, absente de l'objet réellement sauvegardé.
- Quand une première correction basée uniquement sur l'analyse de logs échoue, demander directement le fichier source réel plutôt que de retenter à l'aveugle sur les seuls logs.
- Quand une découpe de texte se fait autour d'une balise/séparateur, couper à l'indice+1 (pas à l'indice du séparateur), sous peine de faire basculer la ponctuation qui suit du mauvais côté.

---

## 4. Le chiffrement — principe et mécanisme des clés miroir

- Clé AES-256 générée une seule fois à l'inscription (32 octets, SecureRandom), stockée en Base64 sur `users/{uid}.encryptionKey`.
- **Clés miroir :** `users/{uid}/book_keys/main` (clé dédiée au Livre) et `users/{uid}/entry_keys/main` (miroir de `encryptionKey`). Lecture autorisée si propriétaire OU si `protocolStatus == "activated"` ET rôle `{creatorId}_recipient` présent dans `myRoles` du lecteur. Écriture réservée au propriétaire uniquement.
- `EncryptionManager.decryptText` : `explicitKey = null` pour le Créateur (clé implicite), = clé miroir pour un Destinataire après activation. Discriminant : `isHeirMode = (targetId != currentUid)`.
- **Faille résiduelle connue, non corrigée, non urgente :** `entry_keys` n'est PAS filtrée par `recipientIds` d'un souvenir donné — un Destinataire activé peut techniquement déchiffrer TOUS les souvenirs, pas seulement les siens. L'affichage, lui, filtre correctement. S'étend à tous les nouveaux types de contenu (WAV, couvertures, compléments plein écran).
- `aiSummary`/`aiTags` : chiffrés depuis v9.4.13 (Blob, plus String), rétrocompatibilité via `when (obj) { is Blob -> decrypt ; is String -> obj }`, sans try/catch autour du déchiffrement.
- Fichiers : Firestore ne stocke QUE le chemin Storage relatif, jamais d'URL complète avec jeton. Créateur résout via `getSafeUrl()` local (`storage.getReference(path).downloadUrl`). Destinataire : exclusivement via Cloud Function `getInheritedFileUrl(creatorId, docType, docId)`, URL signée valable **15 minutes**. Storage n'a PAS de `firestore.get()` dans ses règles natives — l'accès Destinataire ne peut jamais passer directement par Storage.
- Vidéos distantes côté Destinataire : AES-GCM (`CipherInputStream`) exige une lecture strictement séquentielle, incompatible avec les accès non-séquentiels d'ExoPlayer/Mp4Extractor sur un conteneur MP4 → la vidéo est **entièrement téléchargée et déchiffrée** vers un fichier temporaire local avant lecture. **Limite : vidéos longues = attente longue.** D'où la limite de durée appliquée : **90s standard, 30s Miroir à Deux**. Solution durable non construite : AES-CTR + MAC séparé.
- **Incident critique du 25-26 août (boucle d'écriture) :** vérification RSA logée à l'intérieur du listener surveillant `users/{uid}` → boucle infinie (~15 écritures/s). Corrigé : vérification déplacée hors du listener, `Mutex` + `@Synchronized` sur `ensureRsaKeyPairExists()`, écritures de clé publique vers Firestore DÉSACTIVÉES (divergence = log + alerte seulement, jamais réparation auto). **Empreinte SHA-256 de référence, jamais perdue : `d9a48a9778012a45`** (compte bravosebastien@gmail.com, uid `bLRNen7rArXinv5iQILx5OS3sxh2`).

---

## 5. Firestore — collections clés (non exhaustif, voir doc maître pour le détail complet)

- `users/{uid}` : champs verrouillés `isCreator`, `myRoles` (jamais écrits côté client) ; champs non verrouillés type `hasSeenBecomeCreatorPrompt`, `assistantNickname`.
- `users/{uid}/persons/{personId}` : partagée entre "Personnages du Livre" (distinctionType/lien) et "Personnes de l'Arbre" (parentIds CSV `,id1,id2,`/isDeceased/biography/isReparented/reparentedRelationLabel). **Bug de fond connu, non résolu : mélange visuel des deux types dans l'écran Arbre.**
- `users/{uid}/persons/{personId}/media/{mediaId}` : médias propres à une Personne de l'Arbre, toujours un nouvel upload (jamais réutilisation d'un média de souvenir).
- `users/{uid}/entries/{entryId}` (OfflineEntry) : `mediaUrl` (chemin relatif), `recipientIds` (CSV nettoyé), `includeInBook` (souvenir entier), `includedInBook` (avec un d — photo individuelle, v9.6.7), `isUltimateSecret`, `compartmentIds`, `userComment`, `coverUrl`/`localCoverPath`, `mediaProvider` (PHOENX/SPOTIFY/DEEZER/YOUTUBE), `tonalNuance`.
- `users/{uid}/book/current_draft` : manuscrit **unique et non segmenté** (jamais d'attribution de destinataires par chapitre — `recipientIds` s'applique au Livre entier). `generatedAt`/`lastUpdatedAt` annotés `@get:Exclude` (piège Timestamp vs Long — voir section 6).
- `users/{uid}/recipients/{recipientId}` : `transmissionBackgroundId`/`transmissionFontId` (ambiance PAR Destinataire, migration v47).
- Collections racine : `invitations`, `tasks`, `mail` (fermée en écriture directe, passe par `sendMail`), `activationProtocols`, `appConfig` (lecture publique, écriture fermée — `appConfig/stats`, `appConfig/homeCards.genealogyCardImageUrl`), `assistantKnowledgeBase` (~35 docs, modifiables uniquement à la main en Console), `pendingQuestions`, collection Lien Vivant (isolée, jamais liée à `protocolStatus`).

### Règles de sécurité — structure de référence

```
match /users/{userId} {
  allow read: if request.auth != null && (request.auth.uid == userId ||
    (resource != null && request.auth.uid in resource.data.get('depositaryUids', [])));
  allow create: if request.auth != null && request.auth.uid == userId &&
    !request.resource.data.keys().hasAny(['myRoles', 'isCreator']);
  allow update: if request.auth != null && request.auth.uid == userId &&
    !request.resource.data.diff(resource.data).affectedKeys().hasAny(['myRoles', 'isCreator']) &&
    !(resource.data.get('isIdentityLocked', false) == true &&
      request.resource.data.diff(resource.data).affectedKeys().hasAny(['lastName','firstName','dateOfBirth']));
}
```
(`resource != null &&` obligatoire — sinon plantage sur un document inexistant lors de l'inscription.)

```
match /persons/{personId} {
  allow write: if request.auth != null && request.auth.uid == userId;
  allow read: if request.auth != null && (request.auth.uid == userId || (
    get(/databases/$(database)/documents/users/$(userId)).data.protocolStatus == "activated" &&
    (userId + '_recipient') in get(/databases/$(database)/documents/users/$(request.auth.uid)).data.myRoles
  ));
  match /media/{mediaId} { /* même clause, dupliquée au niveau media ET au niveau racine */ }
}
```

**Comptage de non-régression obligatoire avant/après tout déploiement de règles :** `Select-String "match /" firestore.rules` — total de référence au 2 septembre : **30 blocs `match /`** (dont racine service + bloc de secours final à soustraire pour obtenir le nombre de blocs fonctionnels).

---

## 6. Cloud Functions — points de vigilance

- 10 modules (admin, ai, book, lifecycle, identity, media, questions, witnesses, invitations, protocol), exports explicites obligatoires dans `index.ts` (jamais `export *`). **Ajouter une fonction dans un module ne suffit pas à la déployer — il faut l'ajouter à l'export d'`index.ts`, sans quoi aucune erreur ne le signale.**
- Toujours déclarer `invoker: "public"` et une région explicite pour toute nouvelle fonction v2.
- Fonctions clés : `becomeCreator`, `confirmCreatorProofOfLife` (bouton "Je suis en vie"), `markEntryAutoUnlocked` (déverrouillage auto d'énigme), `sendMail`, `getInheritedFileUrl`, `getLivingLinkFileUrl`, `askAssistant` (IA), `generateBookChapters` (régénération complète du Livre), `modifyBookChapter` (retouche d'un seul chapitre, sans consulter souvenirs/autres chapitres — deux mécanismes bien distincts, ne jamais les confondre).
- Dette connue : `notifyNewTestimony` mal déployée (europe-west1 au lieu de us-central1, https trigger au lieu de trigger Firestore réactif).
- **Saga couverture du Livre (25 août) :** cause racine = `HomeViewModel.loadExtraStats()` empilait tous les chargements dans un seul `viewModelScope.launch{try{...}catch{}}` ; un `.collect{}` suspendait la coroutine, tout code après (dont le listener de couverture) n'était jamais atteint, sans erreur, masqué par un catch vide. Corrigé : chaque écouteur indépendant, dans son propre `launch`, `error` de `addSnapshotListener` traité et loggé.

---

## 7. Les six rôles — résumé fonctionnel

1. **Créateur** : construit son héritage (Commode/Bibliothèque à tiroirs, Fil de Pensée, Livre de Ma Vie, Arbre Généalogique, Coffre-Fort, Mon Quiz, Mappemonde, Personnalités, Rencontres, Lien Vivant, Le Miroir à Deux, Réconciliation, Capsules Temporelles).
2. **Dépositaire (Gardien)** : confirme le décès. Modèle "Confiance Simple" actuel = UN SEUL Dépositaire peut déclencher le protocole. Le second Dépositaire éventuel = **secours de continuité, jamais co-validateur obligatoire**. Le futur protocole à 3 gardes-fous (déclenchement + code administratif après examen humain + double vérification séquentielle d'identité du Dépositaire) reste **en attente d'avis d'avocat, rien à coder**.
3. **Destinataire** : reçoit. Refonte majeure (liste unique, parité Livre, sync locale complète) toujours décidée mais non construite. Visualiseur plein écran RÉSOLU (19 août). Bibliothèque/vitrines côté Destinataire toujours absentes (chantier cadré, non lancé).
4. **Famille** : accès collectif, non construit.
5. **Témoin** : témoignage confidentiel, parcours d'inscription redirigé directement vers le formulaire depuis le 1er août.
6. **Contact de Notification** : email seul, jamais d'accès applicatif.

Chaque rôle non-Créateur peut devenir Créateur via un écran plein affiché une seule fois dans la vie du compte, juste après un geste chargé d'émotion (`hasSeenBecomeCreatorPrompt`).

---

## 8. État des grandes fonctionnalités (au 3 septembre 2026)

### Arbre Généalogique — fonctionnel, quelques bugs résiduels
Construction personne par personne via `parentIds` (CSV, pas de `childrenIds` symétrique — déduction par requête inverse). **Public à TOUS les Destinataires sans restriction personne par personne** (choix assumé). Suppression d'une personne → remontée automatique des enfants vers les grands-parents (`isReparented`/`reparentedRelationLabel`). Co-parents : groupés visuellement à la même génération (Union-Find), plus de doublon de l'enfant commun. Zoom/déplacement (pincement + glissement) fonctionnels. **Non résolu :** affichage cassé sur tablette, mélange Personnages du Livre/Personnes de l'Arbre, bouton "Modifier identité/liens" au texte empilé, photo de profil dédiée absente, dédoublonnage (lier une personne existante) jamais reconfirmé.

### Livre de Ma Vie — chantier le plus actif début septembre
- IA = `gemini-3.5-flash` exclusivement. Sources : réponses 100 Questions, Amendements, `userComment`, Arbre. Exclues : Coffre-Fort, Miroir à Deux, Mon Quiz. **Ne consulte JAMAIS les données de lieu/Mappemonde.** Manuscrit unique non segmenté par chapitre.
- Deux mécanismes distincts : `generateBookChapters` (régénération complète, réécrit TOUT) vs `modifyBookChapter` (retouche d'UN chapitre, sans consulter souvenirs/autres chapitres).
- **Mode "Pages" (2 septembre, refermé) :** lecture page par page en plus du défilement historique, coexistants. Plusieurs bugs de pagination corrigés (vocaux affichés comme photos, proportions photo, marges système dynamiques via `WindowInsets.systemBars`).
- **Contrôle éditorial photo par photo (2 septembre, refermé, v9.8.14) :** champ `includedInBook` (avec d) sur la photo elle-même, exclusion persistante + anti-empilement + quota `MAX_PHOTOS_PER_CHAPTER = 3`.
- **Sauvegarde de secours avant régénération (2 septembre, refermé) :** `book/backup_draft` ↔ `book/current_draft` ET `book_keys/backup` ↔ `book_keys/main` sauvegardés/restaurés SYMÉTRIQUEMENT (texte + clé ensemble — sinon "Contenu chiffré" après restauration).
- **🕓 Bug ouvert : ponctuation autour de `[PHOTO:uuid]`/`[AUDIO:uuid]`** — coupure à l'indice du séparateur au lieu de l'indice+1. Correctif identifié, pas encore appliqué.
- **🕓 Question ouverte : fiabilité du sourçage IA** (cas Esteban — fait exact, mais mécanisme de sourçage non vérifié avec de vraies données).
- Coût mesuré : ~0,13-0,14€/régénération complète sur petit Livre (3 chapitres). Gros Livre (60-80p) non mesuré. Piste : 1 régénération complète/mois par palier payant, retouche par chapitre quasi illimitée.
- **Lot 0 (3 septembre, confirmé sur appareil) :** `authorProfile` (Portrait de Vie) désormais bien transmis au prompt de génération — piège rencontré : correctif codé mais jamais déployé (`firebase deploy --only functions` oublié).

### Mappemonde — résolue et stable
Bug d'affichage résolu début août (empreinte SHA-1 debug manquante pour le nouveau package). Intégrée au reste de l'application (20-21 août : ouverture d'un souvenir depuis la carte, suppression d'un lieu sans perdre les souvenirs). **Perte de localisation résolue le 1er septembre** (`EntryMapper.kt` : `toOfflineEntry()` ne transmettait pas `latitude`/`longitude`/`locationId`/`locationName`/`pactId`/`tonalNuance` au constructeur — corrigé).

### Personnalités — nouveau tiroir, chantier clos (31 août-1er septembre)
Distinct d'Arbre/Portrait Proche/Rencontres. Sync multi-appareils, suppression, refonte visuelle (v9.7.6-9.7.7) tous validés.

### Rencontres — fonctionnel, perf résolue
Régression de performance (>1min d'attente) corrigée le 1er septembre en répliquant les "tuyaux" de perf déjà éprouvés pour les Souvenirs (`ImageUtils.kt`, préchargement `InitialSyncWorker`, `DecryptedCache.kt` LRU 20 Mo).

### Réconciliation / Capsules Temporelles — chantier de réflexion tout juste ouvert
Quasi aucune documentation antérieure. **Question de conception non tranchée :** pérennité de la clé de chiffrement d'une Capsule si le compte Créateur venait à disparaître (clé actuellement liée au compte).

### Assistant IA flottant
`askAssistant` (Cloud Function, isolée — aucun accès aux autres collections). Base de connaissance ~35 fiches, éditée manuellement en Console Firebase (aucun déploiement auto). **Non résolu :** présence limitée à quelques écrans (pas toutes les pages), bug clavier recouvrant le champ de saisie (`imePadding` manquant).

### Sécurité — audit des écrans à accès direct
6 routes atteignables sans passer par l'accueil (invitations, Dépositaire, alerte de silence, Témoin, question, lecture du Livre). Un seul risque critique trouvé et corrigé (`DepositaryViewModel.loadCreatorStatus` — blocage infini possible au moment le plus grave). Les autres : `WitnessViewModel` moyen non traité, 13 autres jugés faible risque non modifiés.

---

## 8bis. Régénération partielle du Livre — chantier actif, le plus avancé en ce moment (3 septembre)

**C'est aujourd'hui le chantier le plus important en cours sur le projet — vérifie toujours son état réel avant de proposer autre chose.**

**Objectif :** aujourd'hui, régénérer le Livre réécrit TOUT, même si un seul chapitre est concerné par un changement (voir section 8). Objectif : ne réécrire que les chapitres réellement périmés, en s'appuyant sur un mécanisme déjà construit côté serveur (`generateBookPlan`) mais dont le résultat est aujourd'hui calculé puis jeté après usage.

**Principe retenu (rapport du 3 septembre, lecture directe du code) :** conserver, pour chaque chapitre, `sceneIds` (les souvenirs qui le composent — format déjà produit par `generateBookPlan`) et `sourceFingerprint` (empreinte de TOUT ce qui a nourri le chapitre : résumé du souvenir, commentaire, fiches Arbre des personnes citées — y compris leur biographie et leurs liens de parenté —, amendements, compléments médias, Ton de l'Âme global, tranche d'âge globale). À l'ouverture de « Régénérer », recalcul local gratuit de cette empreinte pour détecter les chapitres périmés, pré-cochés avec une raison en clair, avant tout appel IA.

**Plan en 5 lots, chacun testé sur appareil avant le suivant (règle absolue du projet — jamais deux lots ouverts en parallèle) :**
- **Lot 0 — ✅ CLOS, confirmé sur appareil le 3 septembre.** Retrait de l'appel cassé à `detectThoughtEvolution` (payé à chaque génération, résultat toujours jeté — mauvais nom de paramètre + mauvaise lecture du retour) ; branchement réel de `authorProfile` (Portrait de Vie) dans le prompt serveur, qui n'était jusque-là jamais lu malgré son envoi.
- **Lot 1 — ✅ CLOS, confirmé sur appareil ET en lecture Firestore directe le 3 septembre, commité.** `BookModels.kt`/`BookGeneratorService.kt` : ajout de `sceneIds`/`sourceFingerprint` par chapitre, `computeChapterFingerprint()` en SHA-256, correctif annexe `plan ?: generateBookPlan()` → `effectivePlan`. Vérifié sur les 4 chapitres d'une vraie régénération : `sceneIds` non vides et empreinte SHA-256 valide (64 caractères hex).
- **Diagnostic complémentaire (3 septembre, ✅ CLOS, commité et déployé) — hors plan initial, trois défauts trouvés en comparant deux régénérations successives du même Livre :**
  1. *Ordre des chapitres instable d'une régénération à l'autre, non chronologique.* Cause confirmée par lecture directe du code : `extractScenes()` trie bien les scènes par âge, mais ni `generateBookPlan` ni `generateBookChapters` (prompts serveur, `ai.ts`) n'imposent un ordre chronologique aux chapitres — et le bouton "Régénérer" appelle `generateBook()` sans plan, donc redéclenche un `generateBookPlan()` non déterministe à chaque fois. **Corrigé côté code (pas seulement par consigne à l'IA) :** `BookGeneratorService.generateBook()` trie désormais les chapitres reçus par âge minimal de leurs scènes avant de leur attribuer `orderIndex`, indépendamment de l'ordre renvoyé par l'IA.
  2. *Invention de détails précis (lieux, sensations) quand la matière source est pauvre* — cas vérifié en déchiffrant directement le souvenir de test concerné ("MA RÉUNION", résumé de deux mots, sans commentaire, compléments tous génériques) : une régénération a produit un séjour inventé sur l'Île de la Réunion, l'autre un récit correct de réunion de famille. **Corrigé :** ajout d'une RÈGLE DE PRUDENCE FACTUELLE explicite dans le prompt de `generateBookChapters` (`ai.ts`) — si la matière est pauvre, l'IA doit rester sobre et ne pas inventer de lieux/dates/détails non vérifiables.
  3. *Rattachement chapitre ↔ `sceneIds` fragile* (trouvé en marge du point 1) : le Lot 1 associait `sceneIds` au chapitre par simple position dans le tableau (`effectivePlan.getOrNull(index)`), sans garantie que l'IA renvoie ses chapitres dans le même ordre que le plan. **Corrigé :** `generateBookChapters` renvoie désormais explicitement le champ `sceneIds` pour chaque chapitre qu'il rédige ; le client ne se fie plus à la position dans le tableau (repli par titre uniquement si l'IA ne renvoie pas ce champ). **Vérifié en Firestore sur une vraie régénération : 14/14 scènes attribuées, aucun chevauchement, aucun chapitre orphelin.**
  
  **⚠️ Limitation connue, assumée (pas un bug) :** le tri chronologique des chapitres se base sur `age.years` en **années entières** (granularité déjà présente avant ce correctif, jamais aux mois/jours). Des souvenirs très rapprochés dans le temps (même année) ne sont donc pas garantis parfaitement ordonnés entre eux — seulement vérifié sur un jeu de test où toutes les scènes avaient 52 ans (limite de test, pas de l'usage réel). Suffisant pour un vrai Livre qui s'étale sur plusieurs années. À revisiter seulement si un vrai Livre montre un problème concret sur ce point précis.
- **Lot 2 — non commencé.** Tableau de bord de régénération : liste des chapitres avec état (intact / à retravailler + raison en clair), périmés pré-cochés, souvenirs pas encore racontés listés. Le bouton lance encore une régénération complète à ce stade — uniquement de l'affichage gratuit.
- **Lot 3 — non commencé.** Le bouton n'envoie plus que les chapitres cochés + plan réduit (régénération réellement PARTIELLE). Les deux pièges critiques ci-dessous restent entièrement à respecter pour ce lot ; le rattachement chapitre/`sceneIds` (piège historique de ce lot) est déjà réglé par le diagnostic complémentaire ci-dessus.
- **Lot 4 — non commencé, finitions.** Titres des chapitres voisins transmis pour soigner les transitions, avertissement si un chapitre a été retouché à la main via « Demander à l'IA », introduction régénérée seulement si les titres ont changé.

**🔴 Piège critique impératif pour le Lot 3 :** une régénération PARTIELLE ne doit **JAMAIS** fabriquer une nouvelle clé de chiffrement — elle doit réutiliser la clé existante lue dans `book_keys/main`. Seule une régénération TOTALE depuis zéro a le droit d'en créer une nouvelle. Sinon, les chapitres conservés (chiffrés avec l'ancienne clé) deviennent illisibles — et le défaut est particulièrement sournois car le déchiffrement, en cas d'échec, renvoie le texte chiffré brut « pour diagnostic » plutôt qu'une erreur claire (donc un chapitre rempli de charabia, pas un message d'erreur).

**🔴 Autre piège impératif pour le Lot 3 :** conserver l'identifiant, le titre et le numéro d'ordre de chaque chapitre réécrit — ne remplacer que son texte. Un nouvel identifiant tiré au hasard casserait les marque-pages de lecture (qui s'appuient dessus) et la correspondance avec le plan.

**🕓 Point le plus urgent à vérifier avant tout, indépendant du reste de ce chantier :** le sélecteur « qui est mentionné » permet de taguer un Destinataire, un Témoin ou un Dépositaire dans un souvenir (unifié début août, 4 tables différentes) — mais `extractScenes()` ne résout les personnes que dans la table `persons`, et écarte silencieusement tout identifiant absent de cette table. **Soupçon fort et non confirmé : taguer un Destinataire/Témoin/Dépositaire dans un souvenir n'apporte peut-être rien à l'IA au moment d'écrire le Livre.** Si confirmé, défaut à corriger indépendamment du reste de ce chantier.

**Autres points non vérifiés, signalés dans le rapport source :** où sont stockées les fiches de Rencontres (pour confirmer qu'elles ne nourrissent pas le Livre — probable, aucune entité Room dédiée identifiée) ; le gain réel en euros d'une régénération partielle, jamais mesuré ; la qualité littéraire d'un chapitre réécrit seul (transitions avec les voisins) — ne se juge qu'à la lecture réelle.

**Ce que ce chantier NE remet PAS en cause** (vérifié explicitement dans le rapport source) : `extractScenes()`, `generateBookChapters` serveur, `generateBookPlan` serveur, `modifyBookChapter`, le lecteur/pagination/balises `[PHOTO:…]`/exclusion manuelle/plafond 3 photos, le chiffrement et les clés miroir (sous réserve du piège ci-dessus), la sauvegarde de secours, les règles Firestore, aucune migration Room.

---

## 9. Zones stables — GARDE-FOU, ne pas toucher sans lien direct avec la demande

**Règle générale :** ne jamais modifier un fichier, une fonction ou une règle de sécurité non directement concerné par la demande explicite en cours — même en cas d'amélioration repérée "au passage". Signaler sans toucher, attendre un prompt dédié.

1. **Personnalités** — `PersonalityDetailScreen.kt`, ViewModel, `PersonalityEntity.CATEGORIES`, règles `personalities`/`personalities/media`. Validé, stable.
2. **Rencontres — performance** — `ImageUtils.kt`, blocs `InitialSyncWorker.kt` pour `encounterImagePath`, `DecryptedCache.kt`, `SecureAsyncImage.kt`. Composants PARTAGÉS avec Coffre-Fort/Livre/Souvenirs — fragile à toute retouche indirecte.
3. **Icônes d'action** — `PersonalityDetailScreen.kt`/`EncounterDetailScreen.kt` (TopAppBar, glassmorphism léger). `MediaViewerScreen.kt` garde sa croix flottante en EXCEPTION ASSUMÉE — ne jamais "corriger" ça.
4. **Mappemonde / `EntryMapper.kt`** — `toOfflineEntry(...)` vient d'être corrigée. Ne pas y retoucher sans lien direct avec un nouveau bug identifié.
5. **`InitialSyncWorker.kt`** en général — fichier central déjà modifié plusieurs fois. Toute modification doit rester strictement localisée au bloc concerné.
6. **`firestore.rules`** — blocs existants déjà vérifiés. Toujours refaire le comptage automatisé avant/après tout déploiement.

**Chantiers légitimement encore en mouvement (pas protégés) :** Livre (pages, sauvegarde de secours, ponctuation, fiabilité IA), Réconciliation/Capsules Temporelles (diagnostic en cours).

---

## 10. Lacunes/dettes connues, non urgentes mais à ne pas "redécouvrir" comme neuves

- Décalage email connecté/invité sur un lien d'invitation (non corrigé).
- Accès en lecture du Dépositaire au document `users` complet du Créateur (trop large, non urgent).
- Icône ouvrant encore "Mode Détective" au lieu de "Le Coffre-Fort" quelque part dans l'édition d'un souvenir.
- Bug de contraste sur l'écran de choix de police.
- Rattrapage rétroactif du rangement automatique par tiroirs jamais fait (nouveaux ajouts seulement).
- Liens externes (YouTube/Spotify/Deezer) toujours impossibles à déposer À L'INTÉRIEUR d'un souvenir (autonomes seulement).
- Extraits Littéraires en complément d'un souvenir, Bibliothèque côté Destinataire : cadrés, non lancés.
- Contradiction non réconciliée sur Le Miroir à Deux : liaison réelle de compte construite (v9, début août) vs. "juste un nom et un email" (audit du 28 août) — à vérifier explicitement avant toute communication publique.
- Tests automatisés Firestore (émulateur) : chantier pré-lancement identifié, non commencé — valeur reconnue précisément sur les règles de sécurité (zone stable mais historiquement la plus sensible).
- Système de paiement, moteur de quotas, plafond de pages, export EPUB : tout reste à construire (aucun moteur de quotas n'existe aujourd'hui — usage illimité pour tout le monde).
- Voir section 8bis pour le point le plus urgent actuellement en suspens : un Destinataire/Témoin/Dépositaire tagué dans un souvenir est peut-être silencieusement ignoré par l'IA du Livre (`extractScenes()` ne résout que la table `persons`).
- Tri chronologique des chapitres du Livre basé sur `age.years` en années entières (voir section 8bis, diagnostic complémentaire du 3 septembre) : des souvenirs très rapprochés dans le temps (même année) ne sont pas garantis parfaitement ordonnés entre eux. Assumé, suffisant pour un vrai Livre étalé sur plusieurs années — à revisiter seulement si un vrai Livre (pas un jeu de test) montre un problème concret.

---

## 11. Modèle économique (repère, pas un sujet de code)

5 paliers : Découverte (0€), Solidaire (1,99€/mois), Essence (4,99€/mois), Lignée (9,99€/mois), Prestige (14,99-19,99€/mois). **2 Dépositaires minimum sur TOUS les paliers y compris gratuit** (exigence de sécurité, pas commerciale). Aucune hiérarchie de support selon le palier. Export Livre format liseuse : 9,90€ à l'unité.

---

## 12. Méthode de travail imposée sur ce projet

1. Diagnostic avec code réel avant tout correctif — jamais de supposition.
2. Build de vérification après chaque étape.
3. Précaution chirurgicale sur tout chantier isolé (zones stables, section 9).
4. Toute suppression de code précédée d'un inventaire validé.
5. Les migrations Room ne se modifient JAMAIS une fois exécutées — toujours une nouvelle version N+1.
6. Méfiance systématique envers les try-catch globaux susceptibles d'avaler une vraie erreur en silence.
7. Vigilance permanente sur la confusion UID Firebase / DocID local Room.
8. Un correctif annoncé par un assistant de code (Gemini, Claude...) doit toujours être vérifié sur le contenu RÉEL du fichier modifié, jamais sur sa seule description — et testé sur un vrai appareil avant d'être considéré comme acquis.

---

## 13. Maintenir ce fichier à jour

Ce fichier est un instantané au 3 septembre 2026. À chaque session de travail significative (nouveau chantier refermé, nouveau bug critique trouvé, nouvelle zone stable créée), mettre à jour les sections concernées — en particulier section 8 (état des fonctionnalités), section 9 (zones stables) et section 10 (lacunes connues). Ne jamais perdre l'historique des pièges (section 3) : c'est la mémoire la plus précieuse du projet.

Comment me parler et te comporter avec moi

Je ne suis pas développeur et je ne connais pas les termes techniques. Merci de :

M'expliquer chaque action en français simple, sans jargon (pas de noms de fichiers, de fonctions ou de messages d'erreur bruts, sauf si je le demande explicitement).
Terminer chaque tâche par un résumé court (3 à 5 phrases) : ce qui a été fait, ce qui a été réellement testé/vérifié, et ce qui reste ouvert — plutôt que d'afficher tout le détail technique.
Ne jamais considérer un correctif comme terminé sur la seule base d'une compilation réussie ou d'une description : toujours confirmer par un vrai test sur appareil, et pour les Cloud Functions, confirmer explicitement que le déploiement a bien été exécuté.
Me demander confirmation avant de toucher aux zones stables déjà listées dans ce document, et avant toute décision qui touche à la sécurité, à la confidentialité des données, ou à un choix de conception qui n'est pas déjà tranché.
Si un point est ambigu, me poser la question plutôt que de supposer une réponse à ma place.