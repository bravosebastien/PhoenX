# Tableau de Bord Financier & Synthèse de Direction — PHOEN-X

Dernière mise à jour : Février 2025  
Période d'analyse : **Actuelle / Bêta & Démarrage**  
Destinataires : **Sébastien & Marco**

---

## 1. Résumé Exécutif (Langage Simple)

| Indicateur | Valeur Actuelle | Tendance | Commentaires |
| :--- | :--- | :--- | :--- |
| **Revenus Bruts (TTC)** | `En phase d'amorce` | ↗️ Progression | Premier volume de ventes sur Google Play |
| **Commission Google Prélevée** | `15 %` | ➡️ Stable | Éligible au taux réduit sous le seuil des 1M$ |
| **Revenus Nets Encaissés** | `85 % du brut HT` | ↗️ Alignée | Montants effectivement perçus sur le compte d'entreprise |
| **Proximité du Seuil 1M$ USD** | `0.5 % du seuil` | 🟢 Zone Verte | Marge de sécurité confortable (> 990 000 $ disponibles) |

---

## 2. Analyse de la Situation Financière

1. **Taux de Commission Actuel** : PHOEN-X bénéficie du barème réduit Google Play à **15 %**. Chaque tranche de 100 € de vente nette génère environ **85 € encaissés** pour l'entreprise (hors TVA gérée directement par Google).
2. **Gestion Simplifiée de la TVA** : Grâce au statut de *Merchant of Record* de Google Play, aucune démarche de déclaration de TVA internationale n'est nécessaire à ce stade sur les ventes applicatives.
3. **Risque de Changement de Tarif** : Le seuil de 1 million de dollars annuels est encore éloigné. Les marges actuelles sont totalement sécurisées.

---

## 3. Recommandations Concrètes pour Sébastien & Marco

1. **Recommandation n°1 — Conserver la simplicité actuelle** : Rester à 100 % sur Google Play Billing via RevenueCat. Ne pas ouvrir de système Stripe web prématuré tant que le volume ne le justifie pas (gain marginal compensé par la lourdeur de la TVA).
2. **Recommandation n°2 — Surveillance trimestrielle** : Exécuter le script `scripts/finance/calculate_commissions_and_alerts.py` à chaque fin de trimestre pour suivre le cumul annuel de chiffre d'affaires.
3. **Recommandation n°3 — Anticipation des marges** : Si le chiffre d'affaires annuel franchit 800 000 $, planifier une réunion d'arbitrage pour décider soit d'absorber le passage à 30 %, soit de lancer un canal Web complémentaire (Paddle/Stripe).

---

## ⚠️ Avertissement Légal & Comptable

> **AVERTISSEMENT STRICT** :  
> Ce tableau de bord est un **outil d'analyse interne informatif** et de pilotage stratégique.  
> **Il ne remplace EN AUCUN CAS une vérification ou un suivi par un expert-comptable professionnel qualifié** avant toute décision fiscale, juridique ou déclaration officielle d'entreprise.
