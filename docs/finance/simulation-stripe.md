# Simulation Comparative : Google Play Billing vs Stripe / Merchant of Record — PHOEN-X

Ce document propose une simulation financière comparative pour anticiper une éventuelle bascule ou ouverture d'un canal de vente Web avec **Stripe** ou un **Merchant of Record alternatif (Paddle/Lemon Squeezy)**.

⚠️ **DECISION HUMAINE UNIQUEMENT** : Ce document est une simulation purement informative. Aucune bascule automatique n'est effectuée. Toute décision commerciale ou technique reste 100 % manuelle.

---

## 1. Comparatif des Modèles Financiers & Opérationnels

| Critère | Google Play Billing (≤ 1M$) | Google Play Billing (> 1M$) | Stripe Direct (Web) | Alternative MoR (Paddle/Lemon) |
| :--- | :--- | :--- | :--- | :--- |
| **Frais / Commission** | **15 %** TTC/HT | **30 %** TTC/HT | **1.5% + 0.25€** (EU) + 0.5% Billing | **5% + 0.50$** par transaction |
| **Coût Total Estimé** | **15.0 %** | **30.0 %** | **~ 2.4 % - 3.4 %** | **~ 5.5 % - 6.0 %** |
| **Gestion de la TVA** | ✅ **Inclus** (Google est MoR) | ✅ **Inclus** (Google est MoR) | ❌ **À votre charge** (Stripe Tax aide, mais vous déclarez) | ✅ **Inclus** (Paddle/Lemon est MoR) |
| **Responsabilité Légale** | Google Play | Google Play | **Sébastien & Marco (PHOEN-X)** | Paddle / Lemon Squeezy |
| **Usage sur Android** | Obligatoire In-App | Obligatoire In-App | Web / Canal Externe uniquement | Web / Canal Externe |
| **Friction Utilisateur** | Très faible (Google Pay) | Très faible (Google Pay) | Moyenne (Saisie CB) | Faible à Moyenne |

---

## 2. Simulation Chiffrée sur un Panier Type de 100 € HT

### Scénario A : Vente de 100 € HT sous le seuil des 1M$ sur Google Play
* Prix Brut HT : `100.00 €`
* Commission Google (15%) : `-15.00 €`
* **Net encaissé PHOEN-X : `85.00 €`**

### Scénario B : Vente de 100 € HT au-dessus du seuil des 1M$ sur Google Play (Achats ponctuels)
* Prix Brut HT : `100.00 €`
* Commission Google (30%) : `-30.00 €`
* **Net encaissé PHOEN-X : `70.00 €`** (Marge en baisse de 15 € par tranche de 100 €)

### Scénario C : Vente de 100 € HT via Stripe Direct (Canal Web)
* Prix Brut HT : `100.00 €`
* Frais Stripe (1.5% + 0.25€ + 0.5% Billing) : `-2.25 €`
* Frais de comptabilité & déclaration TVA OSS (estimé) : `-3.00 €`
* **Net encaissé PHOEN-X : `~ 94.75 €`**
* ⚠️ *Inconvénient majeur* : PHOEN-X doit gérer l'immatriculation TVA dans chaque pays UE et faire les déclarations Guichet Unique (OSS).

### Scénario D : Vente de 100 € HT via Paddle / Lemon Squeezy (MoR Web)
* Prix Brut HT : `100.00 €`
* Frais Paddle (5% + 0.50€) : `-5.50 €`
* **Net encaissé PHOEN-X : `94.50 €`**
* ✅ *Avantage* : Le prestataire gère la TVA comme Google, sans contrainte déclarative directe.

---

## 3. Recommandations de Stratégie

1. **Phase Amorce (Actuelle)** : Conserver **Google Play Billing** à 100 %. La commission de 15 % inclut la gestion complète de la TVA mondiale, ce qui évite des frais juridiques et comptables élevés.
2. **Phase Seuil 1M$ USD** : Si le chiffre d'affaires annuel franchit 1M$, évaluer la création d'un **Canal Web complémentaire** (avec Paddle ou Stripe) pour les achats de coffret longue durée ou abonnements annuels.
