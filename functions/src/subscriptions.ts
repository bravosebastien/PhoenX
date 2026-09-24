import { onRequest } from "firebase-functions/v2/https";
import { Timestamp, FieldValue } from "firebase-admin/firestore";
import { db } from "./admin";

/**
 * ABONNEMENTS — Webhook RevenueCat (moteur de paiement)
 * Voir claude/decisions-23sept2026-architecture-abonnements-malleabilite.md.
 *
 * RevenueCat appelle cette URL à chaque événement d'abonnement (achat,
 * renouvellement, résiliation, expiration, changement de palier...). Cette
 * fonction est la SEULE à écrire le palier actif d'un compte dans Firestore
 * (users/{uid}.subscriptionTier / subscriptionExpiresAt / subscriptionSource) :
 * ces champs sont verrouillés en écriture côté client par firestore.rules.
 *
 * L'app_user_id RevenueCat = l'UID Firebase du compte (voir
 * RevenueCatManager.kt côté Android, qui fait Purchases.logIn(uid)).
 *
 * La correspondance "entitlement RevenueCat → palier PHOEN-X" n'est PAS codée
 * en dur : elle est lue dans appConfig/subscriptionTiers.entitlementMapping,
 * modifiable en Console Firebase sans redéployer (principe de malléabilité).
 *
 * Règle de descente de palier (Partie 3 du document de décisions) : cette
 * fonction se contente de changer le palier. Elle ne supprime JAMAIS aucun
 * contenu. Le gel réversible du contenu au-delà des nouvelles limites sera
 * géré par le moteur de droits, fonctionnalité par fonctionnalité.
 */

// Secret partagé avec RevenueCat (champ "Authorization header" du webhook,
// dans le tableau de bord RevenueCat). Même mécanisme que GEMINI_API_KEY.
// À créer une fois avec :
//   firebase functions:secrets:set REVENUECAT_WEBHOOK_SECRET

const DEFAULT_TIER = "DECOUVERTE";
const KNOWN_TIERS = ["DECOUVERTE", "SOLIDAIRE", "ESSENCE", "LIGNEE", "PRESTIGE"];

// Événements après lesquels l'abonnement n'est plus actif → retour au palier gratuit.
const DEACTIVATING_EVENTS = ["EXPIRATION"];

export const revenueCatWebhook = onRequest(
    {
        region: "us-central1",
        invoker: "public",
        secrets: ["REVENUECAT_WEBHOOK_SECRET"],
    },
    async (req, res) => {
        if (req.method !== "POST") {
            res.status(405).send("Method Not Allowed");
            return;
        }

        // 1. Authentification de l'appel (seul RevenueCat connaît ce secret)
        const expected = process.env.REVENUECAT_WEBHOOK_SECRET || "";
        const received = req.get("Authorization") ?? "";
        if (!expected || (received !== expected && received !== `Bearer ${expected}`)) {
            console.warn("[revenueCatWebhook] Appel refusé : secret invalide.");
            res.status(401).send("Unauthorized");
            return;
        }

        const event = req.body?.event;
        if (!event) {
            res.status(400).send("Missing event");
            return;
        }

        const uid: string | undefined = event.app_user_id;
        const type: string = event.type ?? "UNKNOWN";

        // Identifiants anonymes RevenueCat ("$RCAnonymousID:...") = achat fait
        // avant connexion : aucun compte Firestore à mettre à jour.
        if (!uid || uid.startsWith("$RCAnonymousID")) {
            console.log(`[revenueCatWebhook] Événement ${type} ignoré (utilisateur anonyme ou absent).`);
            res.status(200).send("Ignored");
            return;
        }

        // Événement de test envoyé depuis le tableau de bord RevenueCat.
        if (type === "TEST") {
            console.log(`[revenueCatWebhook] Événement TEST reçu pour ${uid}.`);
            res.status(200).send("Test OK");
            return;
        }

        try {
            // 2. Lecture de la correspondance entitlement → palier (configuration à distance)
            const configSnap = await db.collection("appConfig").doc("subscriptionTiers").get();
            const mapping: Record<string, string> = (configSnap.get("entitlementMapping") as Record<string, string>) ?? {};

            // 3. Détermination du nouveau palier
            let newTier = DEFAULT_TIER;
            if (!DEACTIVATING_EVENTS.includes(type)) {
                const entitlementIds: string[] = Array.isArray(event.entitlement_ids) ? event.entitlement_ids : [];
                const mappedTiers = entitlementIds
                    .map((id) => mapping[id])
                    .filter((t): t is string => typeof t === "string" && KNOWN_TIERS.includes(t));

                if (mappedTiers.length > 0) {
                    // En cas de plusieurs entitlements actifs, on retient le palier le plus élevé.
                    newTier = mappedTiers.sort((a, b) => KNOWN_TIERS.indexOf(b) - KNOWN_TIERS.indexOf(a))[0];
                } else if (entitlementIds.length > 0) {
                    console.warn(`[revenueCatWebhook] Aucun palier trouvé pour les entitlements ${JSON.stringify(entitlementIds)} — vérifier appConfig/subscriptionTiers.entitlementMapping.`);
                    // Correspondance manquante : on ne touche pas au palier actuel plutôt que de rétrograder par erreur.
                    res.status(200).send("Unmapped entitlement");
                    return;
                }
            }

            // 4. Écriture côté serveur (seul chemin autorisé pour ces champs)
            const update: Record<string, unknown> = {
                subscriptionTier: newTier,
                subscriptionSource: "revenuecat",
                subscriptionLastEvent: type,
                subscriptionUpdatedAt: FieldValue.serverTimestamp(),
            };
            const expirationMs = event.expiration_at_ms;
            update.subscriptionExpiresAt = (typeof expirationMs === "number" && newTier !== DEFAULT_TIER)
                ? Timestamp.fromMillis(expirationMs)
                : null;

            const userRef = db.collection("users").doc(uid);
            const userSnap = await userRef.get();
            if (!userSnap.exists) {
                console.warn(`[revenueCatWebhook] Compte ${uid} introuvable — événement ${type} non appliqué.`);
                res.status(200).send("User not found");
                return;
            }

            await userRef.set(update, { merge: true });
            console.log(`[revenueCatWebhook] ${uid} → palier ${newTier} (événement ${type}).`);
            res.status(200).send("OK");
        } catch (e) {
            console.error("[revenueCatWebhook] Erreur de traitement :", e);
            // 500 → RevenueCat réessaiera automatiquement plus tard.
            res.status(500).send("Internal error");
        }
    }
);
