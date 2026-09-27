#!/usr/bin/env python3
"""
Script de calcul automatique des commissions Google Play et d'alerte de seuil 1M$ USD.
Exécuté de manière autonome hors de l'application Android.
"""

import json
import sys

THRESHOLD_1M_USD = 1000000.0  # Seuil de 1 Million USD
WARN_THRESHOLD_80 = 800000.0  # Alerte à 80% (800k$)
WARN_THRESHOLD_95 = 950000.0  # Alerte à 95% (950k$)

def analyze_revenue(transactions):
    total_gross = 0.0
    total_google_commission = 0.0
    total_net = 0.0

    for tx in transactions:
        gross = float(tx.get("gross_amount", 0.0))
        comm = float(tx.get("google_commission_amount", 0.0))
        net = float(tx.get("net_amount", 0.0))

        total_gross += gross
        total_google_commission += comm
        total_net += net

    # Évaluation du niveau d'alerte seuil 1M$
    alert_level = "GREEN"
    alert_message = "Chiffre d'affaires sous le seuil des 1M$. Commission appliquée : 15%."

    if total_gross >= THRESHOLD_1M_USD:
        alert_level = "RED"
        alert_message = f"🚨 SEUIL DE 1M$ DEPASSÉ ({total_gross:,.2f} $). Les prochaines ventes hors abonnements récurrents seront prélevées à 30% par Google."
    elif total_gross >= WARN_THRESHOLD_95:
        alert_level = "ORANGE"
        alert_message = f"⚠️ APPROCHE IMMINENTE DU SEUIL DE 1M$ ({total_gross:,.2f} $ / 1M$). Anticiper le passage à 30% de commission Google."
    elif total_gross >= WARN_THRESHOLD_80:
        alert_level = "YELLOW"
        alert_message = f"⚡ SEUIL DES 800k$ ATTEINT ({total_gross:,.2f} $ / 1M$). Préparer l'évaluation d'une bascule Stripe."

    return {
        "total_gross": round(total_gross, 2),
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
            print(f"Revenu Brut Cumulé  : {result['total_gross']:>12.2f} €/$")
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
