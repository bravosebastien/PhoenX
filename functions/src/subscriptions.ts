import { onRequest } from "firebase-functions/v2/https";
import { Timestamp, FieldValue } from "firebase-admin/firestore";
import * as admin from "firebase-admin";
import { db } from "./admin";
import { loadBillingConfig, activePromotions, refreshEntitlementSummary } from "./billing";
import * as crypto from "crypto";

// Événements après lesquels l'abonnement n'est plus actif → retour au palier gratuit.
const DEACTIVATING_EVENTS = ["EXPIRATION", "REVOCATION"];

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

        // 1. Authentification timing-safe de l'appel
        const expectedSecret = process.env.REVENUECAT_WEBHOOK_SECRET || "";
        const receivedHeader = req.get("Authorization") ?? "";
        let receivedSecret = receivedHeader;
        if (receivedHeader.startsWith("Bearer ")) {
            receivedSecret = receivedHeader.slice(7);
        }

        const expectedBuf = Buffer.from(expectedSecret);
        const receivedBuf = Buffer.from(receivedSecret);

        if (!expectedSecret || expectedBuf.length !== receivedBuf.length || !crypto.timingSafeEqual(expectedBuf, receivedBuf)) {
            console.warn("[revenueCatWebhook] Appel refusé : secret invalide.");
            res.status(401).send("Unauthorized");
            return;
        }

        const event = req.body?.event;
        if (!event) {
            res.status(400).send("Missing event");
            return;
        }

        const eventId: string | undefined = event.id || event.event_id;
        const uid: string | undefined = event.app_user_id;
        const type: string = event.type ?? "UNKNOWN";
        const environment: string = event.environment ?? "PRODUCTION";

        if (!uid || uid.startsWith("$RCAnonymousID")) {
            console.log(`[revenueCatWebhook] Événement ${type} ignoré (utilisateur anonyme ou absent).`);
            res.status(200).send("Ignored");
            return;
        }

        if (type === "TEST") {
            console.log(`[revenueCatWebhook] Événement TEST reçu pour ${uid}.`);
            res.status(200).send("Test OK");
            return;
        }

        // Chargement de la configuration dynamique
        const cfg = await loadBillingConfig();

        // Contrôle de l'environnement Sandbox
        if (environment === "SANDBOX" && cfg.billing.acceptSandboxPurchases === false) {
            console.log(`[revenueCatWebhook] Événement Sandbox ignoré pour ${uid} (acceptSandboxPurchases=false).`);
            res.status(200).send("Sandbox purchases disabled");
            return;
        }

        // 2. Anti-doublon d'événement dans une transaction
        let processedRef: admin.firestore.DocumentReference | null = null;
        if (eventId) {
            processedRef = db.collection("processedPaymentEvents").doc(eventId);
            const isAlreadyProcessed = await db.runTransaction(async (tx) => {
                const snap = await tx.get(processedRef!);
                if (snap.exists) return true;
                tx.set(processedRef!, {
                    processedAt: FieldValue.serverTimestamp(),
                    type,
                    uid
                });
                return false;
            });

            if (isAlreadyProcessed) {
                console.log(`[revenueCatWebhook] Événement ${eventId} déjà traité — ignoré.`);
                res.status(200).send("Already processed");
                return;
            }
        }

        try {
            const userRef = db.collection("users").doc(uid);
            const userSnap = await userRef.get();
            if (!userSnap.exists) {
                console.warn(`[revenueCatWebhook] Compte ${uid} introuvable — événement ${type} non appliqué.`);
                if (processedRef) await processedRef.delete().catch(() => {});
                res.status(200).send("User not found");
                return;
            }

            const userData = userSnap.data() || {};
            const eventTimeMs = event.event_timestamp_ms || Date.now();

            // Gestion des achats de recharges de crédits de pages (NON_RENEWING_PURCHASE)
            if (type === "NON_RENEWING_PURCHASE") {
                const productId = event.product_id;
                const packs = cfg.creditPacks?.packs || [];
                const pack = packs.find((p: any) => p.storeProductId === productId);

                if (pack) {
                    const basePages = pack.pages || 0;
                    const currentSummary = await refreshEntitlementSummary(uid);
                    const currentTier = currentSummary.tier;
                    const promos = activePromotions(uid, currentTier, userData.createdAt, Date.now(), cfg);

                    let bonusPercent = 0;
                    const promoBonus = promos.find(p => p.effect && p.effect.type === "pack_bonus_percent");
                    if (promoBonus && typeof promoBonus.effect.value === "number") {
                        bonusPercent = promoBonus.effect.value;
                    }

                    const bonusPages = Math.floor(basePages * (bonusPercent / 100));
                    const totalPages = basePages + bonusPages;

                    await db.runTransaction(async (tx) => {
                        const walletRef = db.collection("users").doc(uid).collection("wallet").doc("main");
                        const walletDoc = await tx.get(walletRef);
                        const wallet = walletDoc.data() || { monthlyBalance: 0, purchasedBalance: 0, reserved: 0 };

                        wallet.purchasedBalance = (wallet.purchasedBalance || 0) + totalPages;
                        tx.set(walletRef, wallet, { merge: true });

                        const ledgerRef = db.collection("users").doc(uid).collection("creditLedger").doc();
                        tx.set(ledgerRef, {
                            type: "purchase",
                            amount: basePages,
                            balanceAfter: (wallet.monthlyBalance || 0) + wallet.purchasedBalance,
                            ref: `product_${productId}`,
                            createdAt: FieldValue.serverTimestamp()
                        });

                        if (bonusPages > 0) {
                            const bonusLedgerRef = db.collection("users").doc(uid).collection("creditLedger").doc();
                            tx.set(bonusLedgerRef, {
                                type: "promo_bonus",
                                amount: bonusPages,
                                balanceAfter: (wallet.monthlyBalance || 0) + wallet.purchasedBalance,
                                ref: `promo_bonus_${promoBonus?.id}`,
                                createdAt: FieldValue.serverTimestamp()
                            });
                        }
                    });

                    console.log(`[revenueCatWebhook] Recharge ${productId} (${totalPages} pages) créditée pour ${uid}.`);
                } else {
                    console.log(`[revenueCatWebhook] Produit ${productId} non reconnu dans creditPacks.`);
                }
            } else {
                // Événement d'abonnement : contrôle d'ancienneté d'événement
                const rcSource = userData.entitlementSources?.revenuecat;
                const lastEventAtMs = rcSource?.lastEventAtMs || 0;

                if (eventTimeMs < lastEventAtMs) {
                    console.log(`[revenueCatWebhook] Événement d'abonnement ${type} plus ancien (${eventTimeMs} < ${lastEventAtMs}) — ignoré.`);
                    res.status(200).send("Stale event ignored");
                    return;
                }

                // Détermination du nouveau palier d'abonnement RevenueCat
                let newTier = cfg.billing.defaultTier;

                if (!DEACTIVATING_EVENTS.includes(type)) {
                    const entitlementIds: string[] = Array.isArray(event.entitlement_ids)
                        ? event.entitlement_ids
                        : (event.entitlement_id ? [event.entitlement_id] : []);

                    const tiersMap = cfg.tiers || {};
                    const matchedTiers: Array<{ key: string; rank: number }> = [];

                    for (const [tKey, tObj] of Object.entries(tiersMap) as Array<[string, any]>) {
                        if (tObj.revenueCatEntitlement && entitlementIds.includes(tObj.revenueCatEntitlement)) {
                            matchedTiers.push({ key: tKey, rank: tObj.rank || 0 });
                        }
                    }

                    if (matchedTiers.length > 0) {
                        matchedTiers.sort((a, b) => b.rank - a.rank);
                        newTier = matchedTiers[0].key;
                    } else if (entitlementIds.length > 0) {
                        console.warn(`[revenueCatWebhook] Aucun palier trouvé pour les entitlements ${JSON.stringify(entitlementIds)}.`);
                        res.status(200).send("Unmapped entitlement");
                        return;
                    }
                }

                const expirationMs = event.expiration_at_ms;
                const expiresAt = (typeof expirationMs === "number" && newTier !== cfg.billing.defaultTier)
                    ? Timestamp.fromMillis(expirationMs)
                    : null;

                const rcData = {
                    tier: newTier,
                    expiresAt,
                    productId: event.product_id || null,
                    lastEvent: type,
                    lastEventAtMs: eventTimeMs
                };

                // Mise à jour EXCLUSIVE de entitlementSources.revenuecat
                await userRef.set({
                    entitlementSources: {
                        revenuecat: rcData
                    }
                }, { merge: true });

                await refreshEntitlementSummary(uid);

                console.log(`[revenueCatWebhook] ${uid} → source revenuecat mise à jour (palier ${newTier}, événement ${type}).`);
            }

            res.status(200).send("OK");
        } catch (e) {
            console.error("[revenueCatWebhook] Erreur de traitement :", e);
            if (processedRef) {
                await processedRef.delete().catch(() => {});
            }
            res.status(500).send("Internal error");
        }
    }
);
