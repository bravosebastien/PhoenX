# Modèle de Données des Revenus — PHOEN-X

Ce document définit la structure et le format des données financières générées par les achats in-app et abonnements PHOEN-X via **RevenueCat** et **Google Play Billing**.

---

## 1. Origine des Données Financiéres

PHOEN-X utilise **RevenueCat** comme couche d'abstraction au-dessus de **Google Play Billing**. Les données de vente sont récupérables via deux canaux principaux :

1. **Webhooks RevenueCat** : notifications temps réel envoyées sur chaque événement (achat, renouvellement, annulation, remboursement).
2. **Rapports d'Exportation Périodiques (CSV/JSON)** : exports mensuels ou quotidiens téléchargeables depuis le tableau de bord RevenueCat ou la Google Play Console (Financial Reports).

---

## 2. Structure Canonique des Données de Vente

Chaque transaction est modélisée selon les champs obligatoires suivants :

| Champ | Type | Description | Exemple |
| :--- | :--- | :--- | :--- |
| `transaction_id` | String | Identifiant unique de la transaction (Google Play / RevenueCat) | `GPA.3312-4589-1234-56789` |
| `app_user_id` | String | Identifiant anonymisé du compte PHOEN-X | `anon_usr_9f8a7b6c` |
| `product_id` | String | Référence du produit in-app ou de l'abonnement | `phoenx_premium_monthly` / `phoenx_lifetime_chest` |
| `purchase_type` | Enum | Type d'achat (`NON_CONSUMABLE`, `SUBSCRIPTION`, `RENEWAL`, `REFUND`) | `SUBSCRIPTION` |
| `purchase_date` | String (ISO-8601) | Date et heure UTC de la transaction | `2025-02-28T14:30:00Z` |
| `currency` | String (ISO-4217) | Devise de facturation du client | `EUR` |
| `gross_amount` | Decimal | Montant brut payé par l'utilisateur (TTC) | `9.99` |
| `estimated_vat_amount` | Decimal | Estimation de la TVA collectée par Google Play | `1.66` (EUR @ 20%) |
| `google_commission_rate` | Decimal | Taux de commission Google Play appliqué (0.15 = 15%) | `0.15` |
| `google_commission_amount` | Decimal | Montant de la commission prélevée par Google Play | `1.25` |
| `net_amount` | Decimal | Montant net encaissé par PHOEN-X (avant impôts de la société) | `7.08` |
| `country_code` | String (ISO-3166) | Pays de facturation de l'acheteur | `FR` |
| `store` | Enum | Magasin d'origine (`play_store`, `app_store`, `stripe`) | `play_store` |

---

## 3. Webhooks RevenueCat (Événements Clés)

Les événements souscrits auprès de RevenueCat pour alimenter notre suivi financier sont :

* `INITIAL_PURCHASE` : Premier achat d'un abonnement ou produit unique.
* `RENEWAL` : Renouvellement automatique d'un abonnement.
* `CANCELLATION` : Résiliation d'un abonnement.
* `NON_RENEWING_PURCHASE` : Achat ponctuel (ex: extension Coffre-Fort).
* `EXPIRATION` : Fin d'un abonnement non renouvelé.
* `PRODUCT_CHANGE` : Changement de niveau d'abonnement.

---

## 4. Stratégie d'Archivage & Exportation

Afin d'éviter tout surcoût et toute complexité dans la base de données applicative Firestore, **les données financières ne sont JAMAIS stockées dans Firestore**.

Elles sont exportées de manière autonome :
1. Fichiers JSON/CSV périodiques générés par le script autonome (`scripts/finance/format_revenue_export.py`).
2. Archivage sécurisé hors application pour la comptabilité de Sébastien et Marco.
