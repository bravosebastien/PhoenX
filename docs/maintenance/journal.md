# Journal de maintenance — PHOEN-X

Historique lisible des actions de veille et de maintenance technique, pour
que Sébastien puisse suivre ce qui a été détecté/proposé/mergé sans avoir à
fouiller les Pull Requests une par une.

---

## 27 septembre 2026 — Mise en place de la veille technique + correction targetSdk

**Constaté :** en vérifiant directement le code (pas sur la seule
description d'un correctif annoncé par Gemini), le fichier
`app/build.gradle.kts` ciblait encore l'API 35 alors que Google Play exige
l'API 36 pour toute nouvelle soumission depuis le 31 août 2026 — risque de
rejet de la prochaine mise à jour.

Par ailleurs, une première tentative de mise en place de fichiers de
maintenance (Dependabot, workflow de veille, documentation) avait été
**annoncée par Gemini sans jamais être réellement poussée sur le dépôt**
(fichiers inexistants confirmés par une vérification directe du dépôt
GitHub, le 27 septembre 2026 également) — cas typique de "description ≠
preuve".

**Actions faites dans cette session :**
1. Correction de `targetSdk`/`compileSdk` à 36 dans `app/build.gradle.kts`.
2. Ajout d'un vrai workflow de compilation (`build-check.yml`) qui compile
   réellement le projet sur chaque Pull Request (SDK Android absent de
   l'environnement de travail utilisé pour cette session — la compilation
   réelle se fait donc sur les machines GitHub, pas localement).
3. Ajout de Dependabot (`.github/dependabot.yml`) pour la veille des
   dépendances Gradle, npm (Cloud Functions) et GitHub Actions.
4. Ajout d'un workflow de suivi de conformité Play Store
   (`play-store-watch.yml`) — vérifie que le code suit la valeur
   documentée, ne remplace pas une vraie veille humaine périodique.
5. Création de `echeances-play-store.md` et `alertes-actives.md` avec les
   informations réellement vérifiées ce jour (targetSdk API 36, Validation
   des développeurs Android confirmée conforme).

**État de vérification à cette date :** build réel en attente de
confirmation sur la Pull Request. Aucun test sur appareil physique encore
fait — obligatoire avant toute publication.

**Suite (même jour) — deux échecs réels de build-check.yml, corrigés un
par un, en lisant le journal réel à chaque fois plutôt qu'en supposant :**

1. **1er échec :** `android-actions/setup-android@v3` retéléchargeait un
   cmdline-tools différent de celui déjà présent sur la machine GitHub et
   restait bloqué sur une acceptation de licences interactive jamais
   validée. **Corrigé** en utilisant directement le SDK préinstallé et en
   acceptant ses licences de façon non interactive.
2. **2e échec :** `FileNotFoundException` sur `local.properties` —
   `com.google.android.libraries.mapsplatform.secrets-gradle-plugin`
   (clé Google Maps) exige ce fichier ou un `local.defaults.properties`
   de repli, qui n'existait pas dans le dépôt. **Corrigé** en ajoutant
   `local.defaults.properties` (valeur factice `MAPS_API_KEY`, aucun
   vrai secret) — mécanisme prévu par le plugin lui-même pour CI et les
   nouveaux postes de développement sans clé réelle.

Aucun des deux échecs n'était lié au changement `targetSdk`/`compileSdk`
36 lui-même — important à distinguer avant de conclure quoi que ce soit
sur une éventuelle incompatibilité de code avec l'API 36.

**Reste ouvert :**
- Confirmer que `build-check.yml` passe réellement au vert après ce 2e
  correctif (résultat non encore connu au moment d'écrire cette ligne).
- Si un futur échec concerne cette fois une vraie incompatibilité entre
  le plugin Android Gradle (AGP 8.6.0) et l'API 36, traiter séparément —
  ne pas deviner de nouvelle version à l'aveugle.
- Test réel sur téléphone ET tablette après un build `.aab` republié.
- Note pour Sébastien : une app compilée avec le `local.defaults.properties`
  de secours n'affichera pas de vraie carte (Mappemonde) — normal en CI,
  sans impact sur ton build local qui utilise ton vrai `local.properties`.
