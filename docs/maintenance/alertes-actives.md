# Alertes actives — maintenance PHOEN-X

Liste des alertes de conformité ou de maintenance technique en cours.
Une alerte est retirée de cette liste uniquement une fois le correctif
confirmé par un vrai build ET, si applicable, un vrai test sur appareil —
jamais sur la seule base d'une description.

---

## 🔴 targetSdk obsolète — code corrigé le 27 septembre 2026, build en attente de re-confirmation

- **Constat :** `app/build.gradle.kts` ciblait l'API 35 alors que Google
  Play exige l'API 36 pour toute nouvelle soumission depuis le 31 août
  2026 (voir `echeances-play-store.md`, point 1).
- **Action :** `compileSdk` et `targetSdk` portés à 36 dans la Pull
  Request `maintenance/target-sdk-36-and-ci`.
- **Premier build (PR #1, run initial) : ❌ ÉCHEC — mais pas à cause du
  code.** Le journal réel du run a montré la vraie cause : l'étape
  d'installation du SDK Android (`android-actions/setup-android@v3`)
  tentait de retélécharger un jeu d'outils différent de celui déjà
  présent sur la machine GitHub, et restait bloquée sur une acceptation
  de licences Google interactive jamais validée ("6 of 7 SDK package
  licenses not accepted"). **Corrigé** en utilisant directement le SDK
  déjà installé sur la machine et en acceptant ses licences de façon non
  interactive, sans passer par cette action tierce.
- **État de vérification actuel :** ⏳ nouveau build déclenché après ce
  correctif, résultat à reconfirmer avant de considérer targetSdk 36
  comme réellement validé. **Ne pas soumettre de nouvelle version au
  Play Store avant un ✅ confirmé ET un test réel sur appareil (téléphone
  ET tablette).**
- Si un futur échec concerne cette fois une vraie incompatibilité entre
  le plugin Android Gradle (AGP, actuellement 8.6.0) et l'API 36 : ne pas
  deviner une version de remplacement à l'aveugle — lire le message
  d'erreur réel et traiter ce point comme un chantier séparé.

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
