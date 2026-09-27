# Alertes actives — maintenance PHOEN-X

Liste des alertes de conformité ou de maintenance technique en cours.
Une alerte est retirée de cette liste uniquement une fois le correctif
confirmé par un vrai build ET, si applicable, un vrai test sur appareil —
jamais sur la seule base d'une description.

---

## 🟡 targetSdk 36 — build compilation confirmé ✅, test appareil encore requis

- **Constat initial :** `app/build.gradle.kts` ciblait l'API 35 alors que
  Google Play exige l'API 36 pour toute nouvelle soumission depuis le
  31 août 2026 (voir `echeances-play-store.md`, point 1).
- **Action :** `compileSdk` et `targetSdk` portés à 36 dans la Pull
  Request `maintenance/target-sdk-36-and-ci`.
- **Parcours réel de vérification (3 échecs de CI, aucun lié au code Kotlin) :**
  1. Échec sur les licences du SDK Android (action tierce
     `android-actions/setup-android@v3` mal adaptée à cette machine) —
     corrigé en utilisant le SDK déjà préinstallé.
  2 et 3. Échec sur l'absence de `local.properties` (clé Google Maps) —
     corrigé en générant ce fichier avec une valeur factice, uniquement
     sur la machine GitHub, jamais commité.
- **✅ Build de compilation confirmé vert le 27 septembre 2026**
  (assembleDebug réussi avec targetSdk/compileSdk 36).
- **🔴 Reste bloquant avant toute publication :** aucun test réel sur
  appareil encore fait avec ce changement. Android 16 (API 36) peut
  modifier des comportements runtime (permissions, restrictions
  d'arrière-plan) qu'une compilation réussie ne détecte jamais. **Tester
  sur téléphone ET tablette avant de soumettre une nouvelle version au
  Play Store.**

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
