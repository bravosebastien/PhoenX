# PHOEN-X — Document Maître Absolu
## Version 4.1 — Document unique et définitif
### Intègre les optimisations de Haute Valeur Ajoutée (Juin 2026)

---

> Ce document contient absolument tout : la vision fondatrice, les 24 fonctionnalités optimisées,
> l'architecture technique E2EE+, la base de données, le design system "Matière", la roadmap,
> et les prompts exacts. Il remplace toutes les versions précédentes.

---

# ═══════════════════════════════════════
# CHAPITRE 1 — L'HISTOIRE QUI FONDE TOUT
# ═══════════════════════════════════════

## Pourquoi PHOEN-X existe
Le fondateur a survécu à trois arrêts cardiaques. Chaque retour a imposé une urgence : celle de transmettre ce qui n'est ni administratif ni financier, mais purement émotionnel et intellectuel. PHOEN-X est la réponse viscérale à la question : *"Si je n'étais pas revenu, qu'est-ce que j'aurais laissé ?"*

## La phrase fondatrice
> *"J'ai survécu trois fois. Chaque retour m'a appris que l'urgence n'était pas médicale — c'était ce que je n'avais jamais encore transmis."*

---

# ════════════════════════════════════════════════════════════
# CHAPITRE 4 — LES 24 FONCTIONNALITÉS COMPLÈTES (OPTIMISÉES)
# ════════════════════════════════════════════════════════════

## 4.1 — LA COMMODE : L'interface "Matière"
L'application ne doit pas ressembler à un outil numérique froid (type Notion ou banque). Elle matérialise une **commode intime**. L'interface utilise des textures de bois, de papier et de cuir, des ombres portées et un grain organique pour renforcer le sentiment de toucher un objet physique, un meuble de famille.

## 4.2 — LE FIL DE PENSÉE & DIALOGUE TEMPOREL ⭐ Signature
- **Concept** : Chronologie liée à l'âge exact (Ans/Mois/Jours).
- **Optimisation "Dialogue Temporel"** : Les Amendements ne sont pas de simples notes. L'interface scénarise la confrontation (ex: vue côte à côte d'une pensée à 30 ans et son amendement à 50 ans). 
- **Analyse IA** : L'IA souligne l'évolution stylistique : "Tu utilisais des mots plus durs à 30 ans, voici ton apaisement à 50 ans."

## 4.10 — LE LIVRE DE VIE (AUDIO & FILM)
- **Règle d'Or** : Priorité absolue aux **fichiers audio bruts** (la voix qui tremble, un soupir, un rire). 
- **Voix Synthétique** : Utilisée uniquement comme "liant" narratif pour lire les transitions textuelles, jamais pour remplacer la vérité de l'enregistrement original.

## 4.13 — LA PREUVE DE VIE PASSIVE
- **Concept** : Inversion du protocole. Le silence est le signal.
- **Optimisation "Zéro Anxiété"** : La preuve de vie est **passive**. Si l'utilisateur ouvre l'app, enregistre un mémo, ou si l'API Health Connect détecte une activité (pas de marche), la vie est confirmée automatiquement.
- **Action Manuelle** : demandée uniquement après 15 jours d'inactivité numérique totale de l'appareil.

## 4.16 — LA SALLE DES QUESTIONS : Recherche d'Intentions
- **Optimisation sémantique** : L'IA ne cherche pas des mots, mais des **intentions amoureuses**. 
- **Exemple** : À la question "Était-il fier de moi ?", l'IA remonte des fragments sur la joie de voir ses enfants réussir, même si le mot "études" n'est pas écrit. Elle transforme l'archive en une présence intelligente.

## 4.18 — LE MODE 3H DU MATIN : Intégration Hardware ⭐
- **Friction Zéro** : L'utilisateur doit pouvoir enregistrer les yeux fermés, dans le noir total.
- **Implémentation** : Utilisation des **Quick Settings Tiles** (volet de notification) et raccourci par double-clic sur le bouton d'alimentation. Activation immédiate du micro sans regarder l'écran.

## 4.21 — LE PACTE : Capsules de Crise
- **Optimisation "Thérapeutique"** : Création de **Capsules de Conflit/Résolution**. Permet de sceller deux versions d'une crise en temps réel, avec interdiction stricte de lecture avant 5 ou 10 ans (verrouillage temporel).

---

# ════════════════════════════════════════════
# CHAPITRE 5 — ARCHITECTURE TECHNIQUE E2EE+
# ════════════════════════════════════════════

## Le Paradoxe Chiffrement vs IA (Résolu)
Pour garantir une confidentialité totale (E2EE) sans sacrifier l'intelligence :
1. **On-Device AI (Gemini Nano)** : L'analyse des textes, la génération des résumés (`aiSummary`) et des tags (`aiTags`) se font **localement** sur le smartphone via Google AI Edge SDK.
2. **Chiffrement Post-Traitement** : Une fois générés, les résumés et tags sont chiffrés avec Tink avant d'être envoyés sur Firestore.
3. **Zéro fuite** : Aucune donnée sensible ou métadonnée ne transite jamais en clair sur le réseau.

---

# ═══════════════════════════════════════════════
# CHAPITRE 11 — LES RÈGLES ABSOLUES (MAJ V4.1)
# ═══════════════════════════════════════════════

1. **Priorité au Brut** : Ne jamais remplacer un enregistrement vocal réel par une voix synthétique.
2. **Confidentialité Totale** : Analyse IA impérativement locale (On-device) pour les contenus sensibles.
3. **Matière visuelle** : Interdiction du design "Flat". Toujours de la texture, du grain et de la profondeur.
4. **Sceau de l'Âge** : L'âge exact est le tampon de certification de chaque souvenir.
5. **Délai de Paix** : Aucun message de réconciliation ou de regret ne peut être ouvert avant 30 jours après l'activation.

---

# ══════════════════════════════════════════════════════════════
# CHAPITRE 10 — LES PROMPTS MIS À JOUR (EXTRAITS CLÉS)
# ══════════════════════════════════════════════════════════════

### PROMPT IA & SÉCURITÉ (Refonte)
"Configure le module IA pour utiliser **Gemini Nano** via le SDK Edge. Implémente la génération de `aiSummary` localement. Assure-toi que le résultat est passé au `EncryptionManager` (Tink) avant tout appel `db.collection('entries').add()`."

### PROMPT MODE 3H DU MATIN (Hardware)
"Implémente un `QuickSettingsTileService` pour déclencher l'enregistrement audio immédiatement. Ajoute un `AccessibilityService` optionnel pour mapper le double-clic du bouton Power au démarrage du micro en mode furtif (écran éteint)."
