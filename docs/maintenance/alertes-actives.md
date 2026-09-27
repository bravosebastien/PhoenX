# Alertes actives — maintenance PHOEN-X

Liste des alertes de conformité ou de maintenance technique en cours.
Une alerte est retirée de cette liste uniquement une fois le correctif
confirmé par un vrai build ET, si applicable, un vrai test sur appareil —
jamais sur la seule base d'une description.

---

## 🔴 Résolue le 27 septembre 2026 — targetSdk obsolète

- **Constat :** `app/build.gradle.kts` ciblait l'API 35 alors que Google
  Play exige l'API 36 pour toute nouvelle soumission depuis le 31 août
  2026 (voir `echeances-play-store.md`, point 1).
- **Action :** `compileSdk` et `targetSdk` portés à 36 dans la Pull
  Request `maintenance/target-sdk-36-and-ci`.
- **État de vérification :** ⏳ build de compilation en cours de
  vérification via `.github/workflows/build-check.yml` sur cette PR.
  **Ne pas soumettre de nouvelle version au Play Store avant confirmation
  du build ET un test réel sur appareil (téléphone ET tablette).**
- Si le build échoue à cause d'une incompatibilité de version du plugin
  Android Gradle (AGP, actuellement 8.6.0) avec l'API 36 : ne pas deviner
  une version de remplacement à l'aveugle — lire le message d'erreur réel
  du build et traiter ce point comme un chantier séparé.

---

## Alertes classées "pas d'action requise actuellement"

- **Validation des développeurs Android (échéance 30 sept. 2026) :**
  vérifiée conforme le 27 septembre 2026 (voir `echeances-play-store.md`,
  point 2). Rien à faire.

---

## Format d'une nouvelle entrée

```
## 🔴 / 🟡 / ✅ <titre court> — <date de détection>

- **Constat :** ce qui a été détecté et comment (source, capture, code).
- **Action :** ce qui a été fait (branche, fichiers modifiés).
- **État de vérification :** build réel confirmé ? test appareil fait ?
  déploiement Cloud Function confirmé (si applicable) ?
```
