# PHOEN-X — Document Maître Définitif
Version 3.0 · Source de vérité unique

## CHAPITRE 1 — VISION ET HISTOIRE
PHOEN-X est une plateforme de mémoire vivante augmentée par IA, née de l'expérience du fondateur (3 arrêts cardiaques).
- **Phrase fondatrice** : "J'ai survécu trois fois. Chaque retour m'a appris que l'urgence n'était pas médicale — c'était ce que je n'avais jamais encore transmis."
- **Mission** : Capturer, organiser et transmettre l'héritage émotionnel et la trajectoire de pensée, de son vivant et au-delà.

## CHAPITRE 2 — ACTEURS
1. **Créateur** : Propriétaire du compte, construit l'héritage.
2. **Dépositaire** : Personne de confiance, active le protocole de transmission.
3. **Destinataire** : Proche qui reçoit les contenus.
4. **Famille** : Accès collectif possible.

## CHAPITRE 3 — FONCTIONNALITÉS CLÉS
- **Le Fil de Pensée par Âge** (Signature) : Stockage et navigation par âge exact (années/mois/jours).
- **La Commode** : Métaphore de l'interface (tiroirs sécurisés).
- **Items Émotionnels** : Catégories avec distinction "Assouvi / Non assouvi".
- **Tiroir à Clé Unique** : Contenu ultra-privé avec clé physique/numérique unique.
- **Les 100 Questions** : Banque de questions guidées par IA.
- **Dates d'Ouverture Programmées** : Capsules temporelles.
- **Preuve de Vie** : Inversion du protocole (le Créateur confirme sa présence hebdo).
- **Mode Détective** : Énigmes pour déverrouiller des souvenirs.
- **Livre/Audio/Film de Vie** : Générations IA basées sur les dépôts.
- **Mode 3h du Matin** : Capture ultra-rapide nocturne.
- **Le Pacte** : Récits croisés entre deux utilisateurs.

## CHAPITRE 4 — ARCHITECTURE TECHNIQUE
- **Stack** : Kotlin, Jetpack Compose, Firebase (Auth, Firestore, Storage, Functions), Vertex AI.
- **Sécurité (Critique)** : Chiffrement E2EE via Google Tink avant tout upload. Clé dérivée via Argon2.
- **Multi-plateforme** : App Android (Créateur) + Web (Destinataire).

## CHAPITRE 5 — BASE DE DONNÉES (Extraits)
- `users/{userId}` : Inclut `dateOfBirth` (OBLIGATOIRE).
- `entries/{entryId}` : Inclut `ageAtCreation` {years, months, days}.
- Chiffrement des champs sensibles (Content, Metadata, Title).

## CHAPITRE 6 — DESIGN SYSTEM
- **BackgroundPrimary** : #1A1A1F (Anthracite profond)
- **AccentPrimary** : #C97B3A (Braise/Doré)
- **TextPrimary** : #F2EDE8 (Blanc chaud)
- **Typo** : Playfair Display (Contenu) / Inter (Interface)
- **Animations** : Lentes et organiques (600ms).

## CHAPITRE 7 — ROADMAP
1. **MVP Technique** (S1-S4) : Structure, Chiffrement, Fil de Pensée, Capture.
2. **V1 Produit** (M2-M4) : Audio/Photo, Dépositaire, Preuve de Vie, Design.
3. **V2 & V3** : IA avancée, Livre de vie, Mode Détective, Cube Destinataire.

---
*Ce document sert de référence permanente pour Gemini lors de la génération de code.*
