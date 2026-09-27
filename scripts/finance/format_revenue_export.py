#!/usr/bin/env python3
"""
Script de structuration des exports financiers RevenueCat / Google Play pour PHOEN-X.
Ce script est un outil autonome (exécuté hors de l'application Android).
"""

import json
import csv
import sys
from datetime import datetime

DEFAULT_GOOGLE_COMMISSION_RATE = 0.15  # 15% par défaut (Programme 15% Google)

def format_transaction(raw_item):
    """
    Formatte une ligne de transaction brute vers la structure canonique PHOEN-X.
    """
    gross = float(raw_item.get("gross_amount", 0.0))
    commission_rate = float(raw_item.get("google_commission_rate", DEFAULT_GOOGLE_COMMISSION_RATE))
    vat = float(raw_item.get("estimated_vat_amount", gross * 0.20 / 1.20)) # Estimation TVA 20% si non fournie

    # Base hors taxe estimée
    ht_amount = gross - vat
    commission_amount = round(ht_amount * commission_rate, 2)
    net_amount = round(ht_amount - commission_amount, 2)

    return {
        "transaction_id": str(raw_item.get("transaction_id", "")),
        "app_user_id": str(raw_item.get("app_user_id", "anonymized")),
        "product_id": str(raw_item.get("product_id", "unknown")),
        "purchase_type": str(raw_item.get("purchase_type", "SUBSCRIPTION")),
        "purchase_date": raw_item.get("purchase_date", datetime.utcnow().isoformat() + "Z"),
        "currency": str(raw_item.get("currency", "EUR")),
        "gross_amount": gross,
        "estimated_vat_amount": round(vat, 2),
        "google_commission_rate": commission_rate,
        "google_commission_amount": commission_amount,
        "net_amount": net_amount,
        "country_code": str(raw_item.get("country_code", "FR")),
        "store": str(raw_item.get("store", "play_store"))
    }

def main():
    print("PHOEN-X Finance - Outillage de structuration des données de vente")
    if len(sys.argv) < 2:
        print("Usage: python format_revenue_export.py <input_data.json>")
        return

    input_file = sys.argv[1]
    try:
        with open(input_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
            formatted = [format_transaction(item) for item in data]
            output_file = input_file.replace('.json', '_formatted.json')
            with open(output_file, 'w', encoding='utf-8') as out_f:
                json.dump(formatted, out_f, indent=2, ensure_ascii=False)
            print(f"✅ Export financier généré avec succès : {output_file}")
    except Exception as e:
        print(f"❌ Erreur lors du traitement de {input_file} : {e}")

if __name__ == "__main__":
    main()
