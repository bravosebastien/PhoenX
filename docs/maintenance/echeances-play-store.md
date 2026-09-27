# Échéances Google Play & Android — à surveiller

Ce fichier recense les échéances de conformité connues pour PHOEN-X sur
Google Play. Il est vérifié manuellement (par Sébastien ou par Claude sur
demande explicite, via une vraie recherche web) — jamais par une simple
supposition. Toute mise à jour de ce fichier doit citer sa source.

**Dernière vérification manuelle complète : 27 septembre 2026.**

---

## 1. Niveau d'API cible (targetSdk) — 🔴 ACTION REQUISE

- **Exigence actuelle (vérifiée le 27 sept. 2026) :** toute nouvelle
  soumission ou mise à jour d'une app existante sur Google Play doit cibler
  l'**API 36 (Android 16)** ou supérieur, depuis le **31 août 2026**.
- **Cas des apps déjà publiées sans nouvelle mise à jour :** le minimum
  pour rester visible auprès des nouveaux utilisateurs était l'API 35, avec
  extension possible jusqu'au 1er novembre 2026 — mais ce minimum ne
  s'applique plus dès qu'une nouvelle mise à jour est soumise : c'est alors
  l'API 36 qui devient obligatoire.
- **État réel du code PHOEN-X (vérifié le 27 sept. 2026) :** `targetSdk`
  et `compileSdk` corrigés à 36 dans `app/build.gradle.kts`. Vérification
  de compilation réelle à confirmer via le workflow
  `.github/workflows/build-check.yml` sur la Pull Request correspondante —
  aucune mise à jour ne doit être soumise au Play Store avant que ce build
  soit confirmé vert ET qu'un test réel sur appareil ait été fait.
- **Source officielle :**
  https://support.google.com/googleplay/android-developer/answer/11926878

## 2. Validation des développeurs Android (identité + packages)

- **Échéance :** 30 septembre 2026. Les applis non enregistrées sont
  supprimées de Google Play dans le monde entier.
- **État vérifié le 27 sept. 2026 (capture Play Console) :** compte
  développeur "Caminoreal" — PHOEN-X (`app.phoenx.mobile`) a son nom de
  package et ses clés de signature à l'état "Enregistrée" (3 clés).
  Onglet Identité (nom légal + adresse) déjà rempli, aucune action en
  attente affichée.
- **Ceci est un sujet distinct du targetSdk (point 1)** — ne pas les
  confondre : l'un concerne l'identité du compte développeur, l'autre la
  version d'Android techniquement ciblée par l'application.

## 3. Ce que ce fichier ne couvre pas (encore)

- Dépréciations d'API Android autres que le niveau cible (ex. permissions
  runtime, restrictions de service en arrière-plan introduites par
  Android 16) — à vérifier lors du premier vrai test sur appareil après le
  passage à l'API 36, pas avant.
- Changements de politique Play Console sur les apps traitant des données
  sensibles (pertinent pour PHOEN-X : décès, héritage numérique) — aucune
  échéance connue à ce jour, à surveiller ponctuellement.

---

## Méthode de mise à jour de ce fichier

1. Ne jamais modifier une échéance sans une source citée (lien officiel).
2. Le workflow `.github/workflows/play-store-watch.yml` ne fait AUCUNE
   recherche web automatique — il compare seulement le code à la valeur
   documentée ici. C'est un humain (ou Claude, sur demande explicite) qui
   doit revérifier périodiquement l'exactitude de ce fichier lui-même.
3. Toute échéance corrigée ici doit aussi mettre à jour la variable
   `REQUIRED_TARGET_SDK` (et `REFERENCE_CHECKED_ON`) dans
   `play-store-watch.yml`, dans le même commit.
