import * as admin from "firebase-admin";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import {
    defaultBillingConfig,
    defaultTiersConfig,
    defaultFeaturesConfig,
    defaultCreditPacksConfig,
    defaultPromotionsConfig
} from "./billingDefaults";

const db = admin.firestore();

// ── Cache mémoire de la configuration (60 secondes) ──
let cachedConfig: { data: any; timestamp: number } | null = null;
const CACHE_TTL_MS = 60000;

export async function loadBillingConfig(): Promise<any> {
    const now = Date.now();
    if (cachedConfig && (now - cachedConfig.timestamp < CACHE_TTL_MS)) {
        return cachedConfig.data;
    }

    const billingRef = db.collection("appConfig").doc("billing");
    const tiersRef = db.collection("appConfig").doc("tiers");
    const featuresRef = db.collection("appConfig").doc("features");
    const creditPacksRef = db.collection("appConfig").doc("creditPacks");
    const promotionsRef = db.collection("appConfig").doc("promotions");

    const [billingSnap, tiersSnap, featuresSnap, creditPacksSnap, promotionsSnap] = await Promise.all([
        billingRef.get(),
        tiersRef.get(),
        featuresRef.get(),
        creditPacksRef.get(),
        promotionsRef.get()
    ]);

    const batch = db.batch();
    let hasMissing = false;

    let billingData = billingSnap.exists ? billingSnap.data()! : defaultBillingConfig;
    if (!billingSnap.exists) {
        batch.set(billingRef, defaultBillingConfig);
        hasMissing = true;
    }

    let tiersData = tiersSnap.exists ? tiersSnap.data()! : defaultTiersConfig;
    if (!tiersSnap.exists) {
        batch.set(tiersRef, defaultTiersConfig);
        hasMissing = true;
    }

    let featuresData = featuresSnap.exists ? featuresSnap.data()! : defaultFeaturesConfig;
    if (!featuresSnap.exists) {
        batch.set(featuresRef, defaultFeaturesConfig);
        hasMissing = true;
    }

    let creditPacksData = creditPacksSnap.exists ? creditPacksSnap.data()! : defaultCreditPacksConfig;
    if (!creditPacksSnap.exists) {
        batch.set(creditPacksRef, defaultCreditPacksConfig);
        hasMissing = true;
    }

    let promotionsData = promotionsSnap.exists ? promotionsSnap.data()! : defaultPromotionsConfig;
    if (!promotionsSnap.exists) {
        batch.set(promotionsRef, defaultPromotionsConfig);
        hasMissing = true;
    }

    if (hasMissing) {
        await batch.commit().catch(err => console.error("[loadBillingConfig] Erreur création defaults:", err));
    }

    const config = {
        billing: billingData,
        tiers: tiersData,
        features: featuresData,
        creditPacks: creditPacksData,
        promotions: promotionsData
    };

    cachedConfig = { data: config, timestamp: now };
    return config;
}

export function isEnforced(uid: string, cfg: any): boolean {
    if (!cfg || !cfg.billing) return false;
    if (cfg.billing.enforcementEnabled) return true;
    const testers = cfg.billing.testerUids || [];
    return Array.isArray(testers) && testers.includes(uid);
}

// ── Résolution multi-sources des droits (entitlementSources) ──
export async function resolveEntitlement(uid: string, userDocData: any, cfg: any): Promise<{
    tier: string;
    source: string;
    expiresAt: number | null;
    isTrial: boolean;
}> {
    const defaultTier = cfg.billing.defaultTier || "DECOUVERTE";
    const now = Date.now();

    const sources = userDocData?.entitlementSources;

    if (sources && typeof sources === "object") {
        const candidates: Array<{
            sourceKey: string;
            tier: string;
            rank: number;
            expiresAtMs: number | null;
        }> = [];

        for (const [sKey, sVal] of Object.entries(sources) as Array<[string, any]>) {
            if (!sVal || !sVal.tier) continue;

            let expiresAtMs: number | null = null;
            if (sVal.expiresAt) {
                expiresAtMs = typeof sVal.expiresAt.toMillis === "function"
                    ? sVal.expiresAt.toMillis()
                    : Number(sVal.expiresAt);
            }

            if (expiresAtMs === null || expiresAtMs > now) {
                const tierKey = sVal.tier;
                const tierObj = cfg.tiers[tierKey];
                const rank = tierObj ? (tierObj.rank ?? 0) : -1;
                candidates.push({
                    sourceKey: sKey,
                    tier: tierObj ? tierKey : defaultTier,
                    rank,
                    expiresAtMs
                });
            }
        }

        if (candidates.length > 0) {
            candidates.sort((a, b) => b.rank - a.rank);
            const best = candidates[0];
            return {
                tier: best.tier,
                source: best.sourceKey,
                expiresAt: best.expiresAtMs,
                isTrial: best.sourceKey === "trial"
            };
        }
    }

    // Repli sur l'ancien champ entitlement si entitlementSources est absent
    const legacyEntitlement = userDocData?.entitlement;
    if (legacyEntitlement && legacyEntitlement.tier) {
        let expiresAtMs: number | null = null;
        if (legacyEntitlement.expiresAt) {
            expiresAtMs = typeof legacyEntitlement.expiresAt.toMillis === "function"
                ? legacyEntitlement.expiresAt.toMillis()
                : Number(legacyEntitlement.expiresAt);
        }

        if (expiresAtMs === null || expiresAtMs > now) {
            const validTier = cfg.tiers[legacyEntitlement.tier] ? legacyEntitlement.tier : defaultTier;
            return {
                tier: validTier,
                source: legacyEntitlement.source || "legacy",
                expiresAt: expiresAtMs,
                isTrial: legacyEntitlement.source === "trial"
            };
        }
    }

    // Essai inversé automatique (Reverse Trial)
    if (cfg.billing.reverseTrial && cfg.billing.reverseTrial.enabled) {
        let createdAtMs = now;
        if (userDocData?.createdAt) {
            createdAtMs = typeof userDocData.createdAt.toMillis === "function"
                ? userDocData.createdAt.toMillis()
                : Number(userDocData.createdAt);
        }

        const trialDays = cfg.billing.reverseTrial.days || 30;
        const trialExpiresAtMs = createdAtMs + (trialDays * 86400000);

        if (now < trialExpiresAtMs) {
            const trialTier = cfg.billing.reverseTrial.tier || "ESSENCE";
            if (!sources?.trial) {
                try {
                    await db.collection("users").doc(uid).set({
                        entitlementSources: {
                            trial: {
                                tier: trialTier,
                                expiresAt: admin.firestore.Timestamp.fromMillis(trialExpiresAtMs)
                            }
                        }
                    }, { merge: true });
                    await refreshEntitlementSummary(uid);
                } catch (e) {
                    console.error("[resolveEntitlement] Erreur écriture reverse trial:", e);
                }
            }
            return {
                tier: trialTier,
                source: "trial",
                expiresAt: trialExpiresAtMs,
                isTrial: true
            };
        }
    }

    return {
        tier: defaultTier,
        source: "default",
        expiresAt: null,
        isTrial: false
    };
}

// ── Recalcule et met à jour le résumé d'entitlement de l'utilisateur ──
export async function refreshEntitlementSummary(uid: string): Promise<{
    tier: string;
    source: string;
    expiresAt: number | null;
    isTrial: boolean;
}> {
    const cfg = await loadBillingConfig();
    const userDoc = await db.collection("users").doc(uid).get();
    const userData = userDoc.data() || {};

    const resolved = await resolveEntitlement(uid, userData, cfg);

    const summaryData = {
        tier: resolved.tier,
        source: resolved.source,
        expiresAt: resolved.expiresAt ? admin.firestore.Timestamp.fromMillis(resolved.expiresAt) : null,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
    };

    await db.collection("users").doc(uid).set({
        entitlement: summaryData,
        subscriptionTier: resolved.tier,
        subscriptionSource: resolved.source,
        subscriptionExpiresAt: summaryData.expiresAt,
        subscriptionUpdatedAt: summaryData.updatedAt
    }, { merge: true });

    return resolved;
}

export function activePromotions(uid: string, tier: string, createdAt: any, now: number, cfg: any): any[] {
    const campaigns = cfg.promotions?.campaigns || [];
    let userCreatedAtMs = now;
    if (createdAt) {
        userCreatedAtMs = typeof createdAt.toMillis === "function" ? createdAt.toMillis() : Number(createdAt);
    }
    const accountAgeDays = Math.floor((now - userCreatedAtMs) / 86400000);

    return campaigns
        .filter((c: any) => {
            if (!c.active) return false;
            const startsMs = new Date(c.startsAt).getTime();
            const endsMs = new Date(c.endsAt).getTime();
            if (now < startsMs || now > endsMs) return false;

            const aud = c.audience || {};
            if (aud.all) return true;
            if (Array.isArray(aud.uids) && aud.uids.includes(uid)) return true;
            if (Array.isArray(aud.tiers) && aud.tiers.includes(tier)) return true;
            if (typeof aud.newUsersWithinDays === "number" && accountAgeDays <= aud.newUsersWithinDays) return true;

            return false;
        })
        .sort((a: any, b: any) => (b.priority || 0) - (a.priority || 0));
}

export function computePageCharge(pages: number, tierKey: string, activePromos: any[], cfg: any): {
    pages: number;
    freePages: number;
    credits: number;
    appliedPromoIds: string[];
} {
    const tierConfig = cfg.tiers[tierKey] || cfg.tiers[cfg.billing.defaultTier];

    let freePageEvery = cfg.billing.freePageEvery || 0;
    if (tierConfig && typeof tierConfig.freePageEvery === "number") {
        freePageEvery = tierConfig.freePageEvery;
    }

    const promoFreePageEvery = activePromos.find(p => p.effect && p.effect.type === "free_page_every");
    if (promoFreePageEvery && typeof promoFreePageEvery.effect.value === "number") {
        freePageEvery = promoFreePageEvery.effect.value;
    }

    const freePages = (freePageEvery > 0) ? Math.floor(pages / freePageEvery) : 0;
    const remainingPages = Math.max(0, pages - freePages);

    let discountPercent = 0;
    const promoDiscount = activePromos.find(p => p.effect && p.effect.type === "page_discount_percent");
    if (promoDiscount && typeof promoDiscount.effect.value === "number") {
        discountPercent = promoDiscount.effect.value;
    }

    const credits = Math.max(0, Math.ceil(remainingPages * (1 - (discountPercent / 100))));

    const appliedPromoIds: string[] = [];
    if (promoFreePageEvery) appliedPromoIds.push(promoFreePageEvery.id);
    if (promoDiscount) appliedPromoIds.push(promoDiscount.id);

    return {
        pages,
        freePages,
        credits,
        appliedPromoIds
    };
}

export async function getWallet(uid: string, tx?: admin.firestore.Transaction): Promise<any> {
    const walletRef = db.collection("users").doc(uid).collection("wallet").doc("main");
    const walletDoc = tx ? await tx.get(walletRef) : await walletRef.get();

    const cfg = await loadBillingConfig();
    const userDoc = await db.collection("users").doc(uid).get();
    const userData = userDoc.data() || {};

    const entitlement = await resolveEntitlement(uid, userData, cfg);
    const tierConfig = cfg.tiers[entitlement.tier] || cfg.tiers[cfg.billing.defaultTier];

    const now = Date.now();
    let wallet = walletDoc.exists ? walletDoc.data()! : {
        monthlyBalance: 0,
        purchasedBalance: 0,
        reserved: 0,
        monthlyResetAt: 0,
        claimedGiftPromoIds: [],
        reservations: {}
    };

    let needsUpdate = !walletDoc.exists;

    // Remise à zéro mensuelle paresseuse
    if (now >= (wallet.monthlyResetAt || 0)) {
        const monthlyGrant = tierConfig.monthlyBookPages || 0;
        let newMonthly = monthlyGrant;

        if (cfg.billing.monthlyRollover && cfg.billing.monthlyRollover.enabled && wallet.monthlyBalance > 0) {
            const capMonths = cfg.billing.monthlyRollover.capMonths || 3;
            const maxCap = monthlyGrant * capMonths;
            newMonthly = Math.min(maxCap, wallet.monthlyBalance + monthlyGrant);
        }

        const nextReset = new Date(now);
        nextReset.setMonth(nextReset.getMonth() + 1);
        nextReset.setDate(1);
        nextReset.setHours(0, 0, 0, 0);

        wallet.monthlyBalance = newMonthly;
        wallet.monthlyResetAt = nextReset.getTime();
        needsUpdate = true;

        const ledgerRef = db.collection("users").doc(uid).collection("creditLedger").doc();
        const ledgerItem = {
            type: "monthly_grant",
            amount: monthlyGrant,
            balanceAfter: wallet.monthlyBalance + wallet.purchasedBalance,
            ref: "lazy_monthly_reset",
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        };
        if (tx) tx.set(ledgerRef, ledgerItem);
        else await ledgerRef.set(ledgerItem);
    }

    // Gestion des promotions gift_pages
    const promos = activePromotions(uid, entitlement.tier, userData.createdAt, now, cfg);
    const giftPromos = promos.filter(p => p.effect && p.effect.type === "gift_pages");

    for (const giftP of giftPromos) {
        const claimed = wallet.claimedGiftPromoIds || [];
        if (!claimed.includes(giftP.id)) {
            const giftAmount = giftP.effect.value || 0;
            wallet.purchasedBalance += giftAmount;
            wallet.claimedGiftPromoIds = [...claimed, giftP.id];
            needsUpdate = true;

            const ledgerRef = db.collection("users").doc(uid).collection("creditLedger").doc();
            const ledgerItem = {
                type: "gift",
                amount: giftAmount,
                balanceAfter: wallet.monthlyBalance + wallet.purchasedBalance,
                ref: `gift_promo_${giftP.id}`,
                createdAt: admin.firestore.FieldValue.serverTimestamp()
            };
            if (tx) tx.set(ledgerRef, ledgerItem);
            else await ledgerRef.set(ledgerItem);
        }
    }

    if (needsUpdate) {
        if (tx) tx.set(walletRef, wallet, { merge: true });
        else await walletRef.set(wallet, { merge: true });
    }

    return { ...wallet, tier: entitlement.tier };
}

// ── Réservation de crédits (sans déduire deux fois "reserved") ──
export async function reserveCredits(uid: string, creditsNeeded: number): Promise<{
    success: boolean;
    reservationId: string | null;
    monthlySpent: number;
    purchasedSpent: number;
    available: number;
}> {
    const walletRef = db.collection("users").doc(uid).collection("wallet").doc("main");
    const reservationId = `res_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`;

    let result = {
        success: false,
        reservationId: null as string | null,
        monthlySpent: 0,
        purchasedSpent: 0,
        available: 0
    };

    await db.runTransaction(async (tx) => {
        const wallet = await getWallet(uid, tx);
        // Les soldes monthlyBalance et purchasedBalance sont déduits à la réservation.
        // Le solde disponible immédiat est donc (monthlyBalance + purchasedBalance), SANS retirer reserved.
        const available = wallet.monthlyBalance + wallet.purchasedBalance;
        result.available = available;

        if (available < creditsNeeded) {
            result.success = false;
            return;
        }

        let remainingNeeded = creditsNeeded;
        let monthlySpent = Math.min(wallet.monthlyBalance, remainingNeeded);
        remainingNeeded -= monthlySpent;
        let purchasedSpent = Math.min(wallet.purchasedBalance, remainingNeeded);

        wallet.monthlyBalance -= monthlySpent;
        wallet.purchasedBalance -= purchasedSpent;
        wallet.reserved = (wallet.reserved || 0) + creditsNeeded;

        if (!wallet.reservations) wallet.reservations = {};
        wallet.reservations[reservationId] = {
            monthly: monthlySpent,
            purchased: purchasedSpent,
            createdAt: Date.now()
        };

        tx.set(walletRef, wallet, { merge: true });

        result.success = true;
        result.reservationId = reservationId;
        result.monthlySpent = monthlySpent;
        result.purchasedSpent = purchasedSpent;
    });

    return result;
}

// ── Régularisation finale des crédits réservés ──
export async function settleCredits(uid: string, reservationId: string | null, actualCreditsNeeded: number, refInfo: string): Promise<void> {
    const walletRef = db.collection("users").doc(uid).collection("wallet").doc("main");

    await db.runTransaction(async (tx) => {
        const walletDoc = await tx.get(walletRef);
        if (!walletDoc.exists) return;
        const wallet = walletDoc.data()!;

        let resMonthly = 0;
        let resPurchased = 0;

        if (reservationId && wallet.reservations && wallet.reservations[reservationId]) {
            const resData = wallet.reservations[reservationId];
            resMonthly = resData.monthly || 0;
            resPurchased = resData.purchased || 0;
            delete wallet.reservations[reservationId];
        }

        const reservedAmount = resMonthly + resPurchased;
        wallet.reserved = Math.max(0, (wallet.reserved || 0) - reservedAmount);

        const diff = actualCreditsNeeded - reservedAmount;

        if (diff < 0) {
            // Surplus réservé : on rend d'abord aux crédits ACHETÉS (jusqu'à resPurchased), puis le reste aux crédits mensuels
            const refundTotal = Math.abs(diff);
            const refundPurchased = Math.min(refundTotal, resPurchased);
            const refundMonthly = refundTotal - refundPurchased;

            wallet.purchasedBalance += refundPurchased;
            wallet.monthlyBalance += refundMonthly;
        } else if (diff > 0) {
            // Besoin supplémentaire : mensuel d'abord, puis achetés
            let extraNeeded = diff;
            const extraMonthly = Math.min(wallet.monthlyBalance, extraNeeded);
            extraNeeded -= extraMonthly;
            const extraPurchased = Math.min(wallet.purchasedBalance, extraNeeded);

            wallet.monthlyBalance -= extraMonthly;
            wallet.purchasedBalance -= extraPurchased;
        }

        const updateData: any = {
            monthlyBalance: wallet.monthlyBalance,
            purchasedBalance: wallet.purchasedBalance,
            reserved: wallet.reserved
        };

        if (reservationId) {
            updateData.reservations = {
                [reservationId]: admin.firestore.FieldValue.delete()
            };
        }

        tx.set(walletRef, updateData, { merge: true });

        const ledgerRef = db.collection("users").doc(uid).collection("creditLedger").doc();
        tx.set(ledgerRef, {
            type: "spend",
            amount: actualCreditsNeeded,
            balanceAfter: wallet.monthlyBalance + wallet.purchasedBalance,
            ref: refInfo,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });
    });
}

// ── Libération complète de la réservation en cas d'échec ──
export async function releaseCredits(uid: string, reservationId: string | null): Promise<void> {
    if (!reservationId) return;
    const walletRef = db.collection("users").doc(uid).collection("wallet").doc("main");

    await db.runTransaction(async (tx) => {
        const walletDoc = await tx.get(walletRef);
        if (!walletDoc.exists) return;
        const wallet = walletDoc.data()!;

        if (wallet.reservations && wallet.reservations[reservationId]) {
            const resData = wallet.reservations[reservationId];
            const resMonthly = resData.monthly || 0;
            const resPurchased = resData.purchased || 0;
            const reservedAmount = resMonthly + resPurchased;

            delete wallet.reservations[reservationId];
            wallet.reserved = Math.max(0, (wallet.reserved || 0) - reservedAmount);

            // Rendre les crédits exactement d'où ils sont venus
            wallet.monthlyBalance += resMonthly;
            wallet.purchasedBalance += resPurchased;

            const updateData: any = {
                monthlyBalance: wallet.monthlyBalance,
                purchasedBalance: wallet.purchasedBalance,
                reserved: wallet.reserved,
                reservations: {
                    [reservationId]: admin.firestore.FieldValue.delete()
                }
            };

            tx.set(walletRef, updateData, { merge: true });

            const ledgerRef = db.collection("users").doc(uid).collection("creditLedger").doc();
            tx.set(ledgerRef, {
                type: "release",
                amount: reservedAmount,
                balanceAfter: wallet.monthlyBalance + wallet.purchasedBalance,
                ref: "reservation_released",
                createdAt: admin.firestore.FieldValue.serverTimestamp()
            });
        }
    });
}

export async function logAiUsage(data: {
    uid: string;
    fn: string;
    words: number;
    pages: number;
    scenes: number;
    credits: number;
    enforced: boolean;
}): Promise<void> {
    try {
        await db.collection("aiUsageLog").add({
            uid: data.uid,
            fn: data.fn,
            words: data.words,
            pages: data.pages,
            scenes: data.scenes,
            credits: data.credits,
            enforced: data.enforced,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });
    } catch (e) {
        console.error("[logAiUsage] Erreur écriture log IA:", e);
    }
}

// ── Callables publiques avec options d'appel ──

export const getMyBillingState = onCall({ region: "us-central1", invoker: "public" }, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Authentification requise");
    }
    const uid = request.auth.uid;

    const summary = await refreshEntitlementSummary(uid);
    const cfg = await loadBillingConfig();
    const userDoc = await db.collection("users").doc(uid).get();
    const userData = userDoc.data() || {};

    const enforced = isEnforced(uid, cfg);
    const wallet = await getWallet(uid);
    const promos = activePromotions(uid, summary.tier, userData.createdAt, Date.now(), cfg);

    return {
        enforced,
        tier: summary.tier,
        entitlement: summary,
        wallet,
        tiers: cfg.tiers,
        activePromotions: promos.map((p: any) => ({
            id: p.id,
            effect: p.effect,
            bannerText: p.bannerText
        }))
    };
});

export const redeemPromoCode = onCall({ region: "us-central1", invoker: "public" }, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Authentification requise");
    }
    const uid = request.auth.uid;
    const rawCode = request.data?.code;
    if (!rawCode || typeof rawCode !== "string") {
        throw new HttpsError("invalid-argument", "Code promo requis");
    }

    const code = rawCode.trim().toUpperCase();
    const promoRef = db.collection("promoCodes").doc(code);
    const cfg = await loadBillingConfig();

    // Résolution du palier actuel avant utilisation du code
    const currentSummary = await refreshEntitlementSummary(uid);
    const currentTierObj = cfg.tiers[currentSummary.tier];
    const currentRank = currentTierObj ? (currentTierObj.rank ?? 0) : 0;

    await db.runTransaction(async (tx) => {
        const promoDoc = await tx.get(promoRef);
        if (!promoDoc.exists) {
            throw new HttpsError("not-found", "Code promo invalide");
        }

        const promo = promoDoc.data()!;
        const now = Date.now();

        if (!promo.active) {
            throw new HttpsError("failed-precondition", "Code promo inactif");
        }

        if (promo.expiresAt) {
            const expMs = typeof promo.expiresAt.toMillis === "function" ? promo.expiresAt.toMillis() : Number(promo.expiresAt);
            if (now > expMs) {
                throw new HttpsError("failed-precondition", "Code promo expiré");
            }
        }

        if (typeof promo.maxUses === "number" && promo.usedCount >= promo.maxUses) {
            throw new HttpsError("resource-exhausted", "Code promo épuisé");
        }

        const usedBy = promo.usedBy || [];
        if (usedBy.includes(uid)) {
            throw new HttpsError("already-exists", "Code promo déjà utilisé par votre compte");
        }

        const grant = promo.grant || {};
        if (grant.type === "tier") {
            const targetTierObj = cfg.tiers[grant.tier];
            const targetRank = targetTierObj ? (targetTierObj.rank ?? 0) : 0;

            if (targetRank <= currentRank) {
                throw new HttpsError("failed-precondition", "already-higher-tier");
            }

            const days = grant.days || 30;
            const expMs = now + (days * 86400000);
            const userRef = db.collection("users").doc(uid);

            tx.set(userRef, {
                entitlementSources: {
                    promo: {
                        tier: grant.tier,
                        expiresAt: admin.firestore.Timestamp.fromMillis(expMs),
                        code
                    }
                }
            }, { merge: true });
        } else if (grant.type === "pages") {
            const pages = grant.pages || 0;
            const walletRef = db.collection("users").doc(uid).collection("wallet").doc("main");
            const walletDoc = await tx.get(walletRef);
            const wallet = walletDoc.data() || { monthlyBalance: 0, purchasedBalance: 0, reserved: 0 };
            wallet.purchasedBalance = (wallet.purchasedBalance || 0) + pages;
            tx.set(walletRef, wallet, { merge: true });

            const ledgerRef = db.collection("users").doc(uid).collection("creditLedger").doc();
            tx.set(ledgerRef, {
                type: "promo_bonus",
                amount: pages,
                balanceAfter: (wallet.monthlyBalance || 0) + wallet.purchasedBalance,
                ref: `promo_code_${code}`,
                createdAt: admin.firestore.FieldValue.serverTimestamp()
            });
        }

        tx.update(promoRef, {
            usedCount: (promo.usedCount || 0) + 1,
            usedBy: [...usedBy, uid]
        });
    });

    await refreshEntitlementSummary(uid);
    return { success: true };
});

export const adminGrantEntitlement = onCall({ region: "us-central1", invoker: "public" }, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Authentification requise");
    }
    const callerUid = request.auth.uid;
    const adminConfigDoc = await db.collection("serverConfig").doc("billingAdmin").get();
    const adminUids = adminConfigDoc.data()?.adminUids || [];

    if (!Array.isArray(adminUids) || !adminUids.includes(callerUid)) {
        throw new HttpsError("permission-denied", "Accès administrateur refusé");
    }

    const { targetUid, tier, days, note } = request.data;
    if (!targetUid || !tier) {
        throw new HttpsError("invalid-argument", "Champs targetUid et tier requis");
    }

    const daysVal = days || 30;
    const expMs = Date.now() + (daysVal * 86400000);

    await db.collection("users").doc(targetUid).set({
        entitlementSources: {
            admin: {
                tier,
                expiresAt: admin.firestore.Timestamp.fromMillis(expMs),
                note: note || "Attribution admin"
            }
        }
    }, { merge: true });

    await refreshEntitlementSummary(targetUid);
    return { success: true };
});

/**
 * Tâche planifiée quotidienne (LOT F)
 * Crédite le Capital Média mensuel pour tous les abonnés PRESTIGE actifs,
 * en lisant les seuils et recharges depuis appConfig/billing (mediaCapitalConfig).
 * Gère également la péremption à 12 mois des anciens crédits non utilisés.
 */
export const monthlyMediaCapitalRefill = onSchedule("every 24 hours", async () => {
    const cfg = await loadBillingConfig();
    const mediaCapCfg = cfg.billing?.mediaCapitalConfig || {
        initial: { photos: 500, videos: 50, audios: 100 },
        monthlyRefill: { photos: 100, videos: 10, audios: 20 },
        expiryMonths: 12
    };

    const now = Date.now();
    const expiryMs = mediaCapCfg.expiryMonths * 30 * 24 * 3600 * 1000;

    // Récupérer les abonnés PRESTIGE
    const prestigeSnap = await db.collection("users")
        .where("subscriptionTier", "==", "PRESTIGE")
        .get();

    const nowObj = new Date();
    const currentMonthId = `${nowObj.getFullYear()}-${String(nowObj.getMonth() + 1).padStart(2, "0")}`;

    for (const doc of prestigeSnap.docs) {
        const uid = doc.id;
        const ledgerRef = db.collection("users").doc(uid).collection("mediaLedger");
        const currentMonthDoc = await ledgerRef.doc(currentMonthId).get();

        if (!currentMonthDoc.exists) {
            // Déterminer s'il s'agit du tout premier crédit PRESTIGE pour ce compte
            const existingLedger = await ledgerRef.limit(1).get();
            const isFirstGrant = existingLedger.empty;

            const grantValues = isFirstGrant ? mediaCapCfg.initial : mediaCapCfg.monthlyRefill;

            await ledgerRef.doc(currentMonthId).set({
                monthId: currentMonthId,
                photosGranted: grantValues.photos,
                videosGranted: grantValues.videos,
                audiosGranted: grantValues.audios,
                photosUsed: 0,
                videosUsed: 0,
                audiosUsed: 0,
                grantedAt: admin.firestore.FieldValue.serverTimestamp(),
                expiresAt: admin.firestore.Timestamp.fromMillis(now + expiryMs)
            });

            console.log(`[mediaCapitalRefill] Crédité ${currentMonthId} pour ${uid} (${isFirstGrant ? "initial" : "mensuel"})`);
        }
    }
});

/**
 * Retourne le solde de Capital Média disponible (PRESTIGE) (LOT F)
 */
export const getUserMediaCapital = onCall({ region: "us-central1", invoker: "public" }, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Authentification requise");
    }
    const uid = request.auth.uid;

    const now = admin.firestore.Timestamp.now();
    const ledgerSnap = await db.collection("users").doc(uid).collection("mediaLedger")
        .where("expiresAt", ">", now)
        .get();

    let totalPhotosAvailable = 0;
    let totalVideosAvailable = 0;
    let totalAudiosAvailable = 0;

    ledgerSnap.docs.forEach(doc => {
        const data = doc.data();
        const pRem = Math.max(0, (data.photosGranted || 0) - (data.photosUsed || 0));
        const vRem = Math.max(0, (data.videosGranted || 0) - (data.videosUsed || 0));
        const aRem = Math.max(0, (data.audiosGranted || 0) - (data.audiosUsed || 0));

        totalPhotosAvailable += pRem;
        totalVideosAvailable += vRem;
        totalAudiosAvailable += aRem;
    });

    return {
        photos: totalPhotosAvailable,
        videos: totalVideosAvailable,
        audios: totalAudiosAvailable
    };
});

export const adminGrantCredits = onCall({ region: "us-central1", invoker: "public" }, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Authentification requise");
    }
    const callerUid = request.auth.uid;
    const adminConfigDoc = await db.collection("serverConfig").doc("billingAdmin").get();
    const adminUids = adminConfigDoc.data()?.adminUids || [];

    if (!Array.isArray(adminUids) || !adminUids.includes(callerUid)) {
        throw new HttpsError("permission-denied", "Accès administrateur refusé");
    }

    const { targetUid, pages, note } = request.data;
    if (!targetUid || typeof pages !== "number") {
        throw new HttpsError("invalid-argument", "Champs targetUid et pages requis");
    }

    const walletRef = db.collection("users").doc(targetUid).collection("wallet").doc("main");
    await db.runTransaction(async (tx) => {
        const walletDoc = await tx.get(walletRef);
        const wallet = walletDoc.data() || { monthlyBalance: 0, purchasedBalance: 0, reserved: 0 };
        wallet.purchasedBalance = (wallet.purchasedBalance || 0) + pages;
        tx.set(walletRef, wallet, { merge: true });

        const ledgerRef = db.collection("users").doc(targetUid).collection("creditLedger").doc();
        tx.set(ledgerRef, {
            type: "admin",
            amount: pages,
            balanceAfter: (wallet.monthlyBalance || 0) + wallet.purchasedBalance,
            ref: note || "Crédit manuel admin",
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });
    });

    return { success: true };
});
