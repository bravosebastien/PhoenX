#!/usr/bin/env python3
"""
Script de calcul automatique des commissions Google Play et d'alerte de seuil 1M$ USD.
Exécuté de manière autonome hors de l'application Android.
"""

import json
import sys

THRESHOLD_1M_USD = 1000000.0  # Seuil de 1 Million USD (le seuil Google est TOUJOURS en USD, quelle que soit la devise de vente)
WARN_THRESHOLD_80 = 800000.0  # Alerte à 80% (800k$)
WARN_THRESHOLD_95 = 950000.0  # Alerte à 95% (950k$)

# Taux de conversion indicatifs vers l'USD, à mettre à jour manuellement de temps en temps
# (ce script est un outil hors-ligne, pas un calcul en temps réel de trésorerie).
# Ils servent UNIQUEMENT à comparer un chiffre d'affaires multi-devises au seuil des 1M$ USD de Google.
EXCHANGE_RATES_TO_USD = {
    "USD": 1.0,
    "EUR": 1.08,
    "GBP": 1.27,
    "CHF": 1.13,
    "CAD": 0.74,
}
DEFAULT_RATE_TO_USD = 1.0  # repli si une devise inconnue apparaît (log d'avertissement affiché)


def convert_to_usd(amount, currency):
    rate = EXCHANGE_RATES_TO_USD.get((currency or "USD").upper())
    if rate is None:
        print(f"⚠️ Devise inconnue '{currency}' : conversion approximative au taux 1:1 vers USD. "
              f"Ajoutez son taux dans EXCHANGE_RATES_TO_USD pour un calcul correct.")
        rate = DEFAULT_RATE_TO_USD
    return amount * rate


def analyze_revenue(transactions):
    total_gross = 0.0            # Somme brute affichée telle quelle (mélange de devises, indicatif)
    total_gross_usd = 0.0        # Somme brute convertie en USD — c'est CETTE valeur qui pilote les seuils Google
    total_google_commission = 0.0
    total_net = 0.0

    for tx in transactions:
        gross = float(tx.get("gross_amount", 0.0))
        currency = tx.get("currency", "USD")
        comm = float(tx.get("google_commission_amount", 0.0))
        net = float(tx.get("net_amount", 0.0))

        total_gross += gross
        total_gross_usd += convert_to_usd(gross, currency)
        total_google_commission += comm
        total_net += net

    # Évaluation du niveau d'alerte seuil 1M$ — TOUJOURS sur le total converti en USD, jamais sur le mélange de devises brut
    alert_level = "GREEN"
    alert_message = "Chiffre d'affaires sous le seuil des 1M$. Commission appliquée : 15%."

    if total_gross_usd >= THRESHOLD_1M_USD:
        alert_level = "RED"
        alert_message = f"🚨 SEUIL DE 1M$ DEPASSÉ ({total_gross_usd:,.2f} $ USD équivalent). Les prochaines ventes hors abonnements récurrents seront prélevées à 30% par Google."
    elif total_gross_usd >= WARN_THRESHOLD_95:
        alert_level = "ORANGE"
        alert_message = f"⚠️ APPROCHE IMMINENTE DU SEUIL DE 1M$ ({total_gross_usd:,.2f} $ USD équivalent / 1M$). Anticiper le passage à 30% de commission Google."
    elif total_gross_usd >= WARN_THRESHOLD_80:
        alert_level = "YELLOW"
        alert_message = f"⚡ SEUIL DES 800k$ ATTEINT ({total_gross_usd:,.2f} $ USD équivalent / 1M$). Préparer l'évaluation d'une bascule Stripe."

    return {
        "total_gross": round(total_gross, 2),
        "total_gross_usd": round(total_gross_usd, 2),
        "total_google_commission": round(total_google_commission, 2),
        "total_net": round(total_net, 2),
        "alert_level": alert_level,
        "alert_message": alert_message
    }

def main():
    if len(sys.argv) < 2:
        print("Usage: python calculate_commissions_and_alerts.py <formatted_revenue.json>")
        return

    input_file = sys.argv[1]
    try:
        with open(input_file, 'r', encoding='utf-8') as f:
            transactions = json.load(f)
            result = analyze_revenue(transactions)
            print("==================================================")
            print("📊 RAPPORT FINANCIER & ANANLYSE DES SEUILS PHOEN-X")
            print("==================================================")
            print(f"Revenu Brut Cumulé (mélange devises, indicatif) : {result['total_gross']:>12.2f}")
            print(f"Revenu Brut Cumulé (converti en USD équivalent) : {result['total_gross_usd']:>12.2f} $")
            print(f"Commission Google   : {result['total_google_commission']:>12.2f} €/$")
            print(f"Revenu Net Encaissé : {result['total_net']:>12.2f} €/$")
            print("--------------------------------------------------")
            print(f"Niveau d'Alerte     : [{result['alert_level']}]")
            print(f"Information         : {result['alert_message']}")
            print("==================================================")
            print("⚠️ Rappel : Ce rapport est une estimation interne.")
            print("   Seul un expert-comptable valide vos déclarations officielles.")
            print("==================================================")
    except Exception as e:
        print(f"❌ Erreur lors du calcul : {e}")

if __name__ == "__main__":
    main()