import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import * as crypto from "crypto";
import { db } from "./admin";
import { propagateUidLiaison } from "./lifecycle";

/**
 * PHOEN-X Invitations Module - Role and invitation management
 */

// Fonctions d'invitation Dépositaire
export const generateDepositaryInviteToken = onCall(async (request) => {
    if (request.auth?.uid !== request.data.creatorId) {
        throw new HttpsError("permission-denied", "Accès refusé");
    }
    const { creatorId, depositaryId } = request.data;
    const token = crypto.randomBytes(32).toString('hex');
    await db.collection("users").doc(creatorId).collection("depositaries").doc(depositaryId).set({ inviteToken: token, inviteTokenUsed: false }, { merge: true });
    return { token };
});

export const generateDepositaryShortCode = onCall(async (request) => {
    if (request.auth?.uid !== request.data.creatorId) {
        throw new HttpsError("permission-denied", "Accès refusé");
    }
    const { creatorId, depositaryId } = request.data;
    const code = crypto.randomBytes(4).toString('hex');
    await db.collection("depositaryInviteCodes").doc(code).set({ creatorId, depositaryId, expiresAt: admin.firestore.Timestamp.fromMillis(Date.now() + 900000), used: false });
    return { shortCode: code };
});

export const redeemDepositaryShortCode = onCall(async (request) => {
    // ═══ SÉCURITÉ v9.3.6 : Authentification requise ═══
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const { shortCode } = request.data;
    const uid = request.auth.uid;

    try {
        const result = await db.runTransaction(async (transaction) => {
            const now = Date.now();
            const windowStart = now - (15 * 60 * 1000); // 15 mins

            // 1. Lectures (v9.3.8 : TOUTES les lectures avant les écritures)
            const limitRef = db.collection("rateLimits").doc(uid);
            const codeRef = db.collection("depositaryInviteCodes").doc(shortCode);

            const [limitDoc, codeDoc] = await Promise.all([
                transaction.get(limitRef),
                transaction.get(codeRef)
            ]);

            // 2. Évaluation Rate Limit (v9.4.5)
            const lData = limitDoc.data();
            const lastAttempt = lData?.lastAttemptAt?.toMillis() || 0;
            const alreadyBlocked = limitDoc.exists && lastAttempt > windowStart && lData!.shortCodeAttempts >= 10;

            // 3. Incrément SYSTÉMATIQUE (Placé avant tout retour pour garantir l'écriture)
            updateRateLimitTransactional(transaction, limitRef, limitDoc, windowStart);

            // 4. Logique de sortie transactionnelle (Sans throw)
            if (alreadyBlocked) return { ok: false, reason: "rate_limited" };
            if (!codeDoc.exists) return { ok: false, reason: "not_found" };

            const cData = codeDoc.data()!;
            if (cData.expiresAt.toMillis() < now || cData.used) {
                return { ok: false, reason: "expired_or_used" };
            }

            // 5. Marquage comme utilisé (Uniquement si tout est OK)
            transaction.update(codeRef, {
                used: true,
                redeemedByUid: uid,
                redeemedAt: admin.firestore.FieldValue.serverTimestamp()
            });

            return {
                ok: true,
                creatorId: cData.creatorId,
                depositaryId: cData.depositaryId
            };
        });

        // ═══ TRADUCTION DES ERREURS HORS TRANSACTION (v9.4.5) ═══
        if (!result.ok) {
            const errorMap: Record<string, { code: any, msg: string }> = {
                "rate_limited": { code: "resource-exhausted", msg: "Trop de tentatives. Réessayez dans 15 minutes." },
                "not_found": { code: "not-found", msg: "Code invalide" },
                "expired_or_used": { code: "permission-denied", msg: "Code expiré ou déjà utilisé" }
            };
            const err = errorMap[result.reason!];
            throw new HttpsError(err.code, err.msg);
        }

        const { creatorId, depositaryId } = result;
        const creatorDoc = await db.collection("users").doc(creatorId).get();
        const dDoc = await db.collection("users").doc(creatorId).collection("depositaries").doc(depositaryId).get();

        return {
            creatorId: creatorId,
            depositaryId: depositaryId,
            token: dDoc.data()?.inviteToken,
            creatorName: creatorDoc.data()?.displayName || "Ton proche"
        };

    } catch (error: any) {
        if (error instanceof HttpsError) throw error;
        throw new HttpsError("internal", error.message);
    }
});

export function updateRateLimitTransactional(transaction: admin.firestore.Transaction, limitRef: admin.firestore.DocumentReference, limitDoc: admin.firestore.DocumentSnapshot, windowStart: number) {
    if (limitDoc.exists) {
        const lData = limitDoc.data()!;
        const lastAttempt = lData.lastAttemptAt?.toMillis() || 0;
        if (lastAttempt <= windowStart) {
            transaction.set(limitRef, { shortCodeAttempts: 1, lastAttemptAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
        } else {
            transaction.set(limitRef, { shortCodeAttempts: admin.firestore.FieldValue.increment(1), lastAttemptAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
        }
    } else {
        transaction.set(limitRef, { shortCodeAttempts: 1, lastAttemptAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
    }
}

export const joinAsDepositary = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }
    const { creatorId, depositaryId, token } = request.data;
    const depositaryUid = request.auth.uid;

    try {
        await db.runTransaction(async (transaction) => {
            const ref = db.collection("users").doc(creatorId).collection("depositaries").doc(depositaryId);
            const doc = await transaction.get(ref);

            if (!doc.exists || doc.data()?.inviteToken !== token || doc.data()?.inviteTokenUsed) {
                throw new HttpsError("permission-denied", "Invalide");
            }

            const creatorDoc = await transaction.get(db.collection("users").doc(creatorId));
            const creatorName = creatorDoc.data()?.displayName || "Votre proche";

            // 1. Liaison sur le document du Créateur
            transaction.update(ref, {
                depositaryUid: depositaryUid,
                status: "active",
                inviteTokenUsed: true,
                inviteToken: admin.firestore.FieldValue.delete() // v9.3.9 : Suppression du token après usage
            });

            // 2. Lien inverse sur le document du Dépositaire (Système myRoles v9.3.5)
            const depositaryUserRef = db.collection("users").doc(depositaryUid);
            const roleKey = `${creatorId}_depositary`;
            const newRoleData = {
                creatorId: creatorId,
                creatorName: creatorName,
                role: "depositary",
                status: "active",
                label: "Gardien de confiance",
                joinedAt: admin.firestore.FieldValue.serverTimestamp(),
                sourceId: depositaryId
            };

            transaction.set(depositaryUserRef, {
                myRoles: { [roleKey]: newRoleData }
            }, { merge: true });
        });

        return { success: true };
    } catch (error: any) {
        if (error instanceof HttpsError) throw error;
        throw new HttpsError("internal", error.message);
    }
});

/**
 * PHOEN-X v7.2 - Liaison Universelle
 */
export const generateUniversalInvitation = onCall(async (request) => {
    const { email, role, sourceId, label, expiresHours } = request.data;
    const auth = request.auth;
    if (!auth) throw new HttpsError("unauthenticated", "Non authentifié");
    if (!sourceId) throw new HttpsError("invalid-argument", "sourceId manquant");

    // Récupérer le nom du créateur pour dénormalisation
    const creatorDoc = await db.collection("users").doc(auth.uid).get();
    const creatorName = creatorDoc.data()?.displayName || "Votre proche";

    // Sécurisation v7.2 : On construit le chemin nous-mêmes pour éviter les injections
    let sourcePath = "";
    if (role === "depositary") sourcePath = `users/${auth.uid}/depositaries/${sourceId}`;
    else if (role === "witness") sourcePath = `users/${auth.uid}/witnesses/${sourceId}`;
    else if (role === "recipient") sourcePath = `users/${auth.uid}/recipients/${sourceId}`;
    else throw new HttpsError("invalid-argument", "Rôle invalide");

    const tokenId = crypto.randomBytes(32).toString('hex');
    const expiresAt = admin.firestore.Timestamp.fromMillis(Date.now() + (expiresHours || 168) * 3600000);

    const inviteData = {
        email: email.toLowerCase(),
        creatorId: auth.uid,
        creatorName,
        role,
        sourceId,
        sourcePath,
        label,
        expiresAt,
        used: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
    };

    await db.collection("invitations").doc(tokenId).set(inviteData);

    return { tokenId };
});

export const getInvitationDetails = onCall(async (request) => {
    const { tokenId } = request.data;
    if (!tokenId) throw new HttpsError("invalid-argument", "Token manquant");

    const inviteDoc = await db.collection("invitations").doc(tokenId).get();
    if (!inviteDoc.exists) throw new HttpsError("not-found", "Invitation introuvable");

    const inviteData = inviteDoc.data()!;
    if (inviteData.expiresAt.toDate() < new Date()) throw new HttpsError("permission-denied", "Invitation expirée");
    if (inviteData.used) throw new HttpsError("already-exists", "Invitation déjà utilisée");

    const creatorDoc = await db.collection("users").doc(inviteData.creatorId).get();

    return {
        creatorName: creatorDoc.data()?.displayName || "Votre proche",
        creatorId: inviteData.creatorId,
        role: inviteData.role,
        label: inviteData.label,
        targetEmail: inviteData.email
    };
});

export const acceptUniversalInvitation = onCall(async (request) => {
    const { tokenId } = request.data;
    const auth = request.auth;

    if (!auth || !auth.token.email) {
        throw new HttpsError("unauthenticated", "Vous devez être connecté avec un email valide.");
    }

    const userEmail = auth.token.email.toLowerCase();
    const inviteRef = db.collection("invitations").doc(tokenId);
    const userRef = db.collection("users").doc(auth.uid);

    try {
        const result = await db.runTransaction(async (transaction) => {
            const inviteDoc = await transaction.get(inviteRef);

            // 1. Existence
            if (!inviteDoc.exists) {
                throw new HttpsError("not-found", "Invitation introuvable.");
            }

            const inviteData = inviteDoc.data()!;
            const { creatorId, role } = inviteData;

            // 2. Expiration
            if (inviteData.expiresAt && inviteData.expiresAt.toDate() < new Date()) {
                throw new HttpsError("permission-denied", "Cette invitation a expiré.");
            }

            // 3. Idempotence & Réutilisation
            if (inviteData.used) {
                if (inviteData.acceptedByUid === auth.uid) {
                    return { status: "already_accepted", message: "Vous avez déjà accepté ce rôle." };
                } else {
                    throw new HttpsError("already-exists", "Cette invitation a déjà été utilisée par un autre compte.");
                }
            }

            // 4. Vérification Identitaire (Normalisée)
            if (inviteData.email.toLowerCase() !== userEmail) {
                throw new HttpsError("permission-denied", `Cette invitation est destinée à ${inviteData.email}.`);
            }

            // 6. FIX isCreator : On vérifie si l'utilisateur est déjà Créateur
            const userDoc = await transaction.get(userRef);
            const userData = userDoc.data();
            const currentIsCreator = userData?.isCreator === true;

            // 7. MISE À JOUR ATOMIQUE
            const roleKey = `${creatorId}_${role}`;
            const newRoleData = {
                creatorId: creatorId,
                creatorName: inviteData.creatorName || "Votre proche",
                role: role,
                status: "active",
                label: inviteData.label || role,
                joinedAt: admin.firestore.FieldValue.serverTimestamp(),
                sourceId: inviteData.sourceId || null
            };

            // Mise à jour du profil de l'invité
            transaction.set(userRef, {
                myRoles: { [roleKey]: newRoleData },
                isCreator: currentIsCreator // On préserve le statut true s'il existe
            }, { merge: true });

            // Marquage du token comme consommé
            transaction.update(inviteRef, {
                used: true,
                acceptedByUid: auth.uid,
                acceptedAt: admin.firestore.FieldValue.serverTimestamp()
            });

            // 8. Mise à jour du statut dans la liste du Créateur
            if (inviteData.sourcePath) {
                const sourceRef = db.doc(inviteData.sourcePath);
                const sourceUpdates: any = {
                    status: "active",
                    linkedUid: auth.uid,
                    linkedAt: admin.firestore.FieldValue.serverTimestamp()
                };

                // v9.1 : Champ spécifique requis par les Security Rules pour les Dépositaires
                if (role === "depositary") {
                    sourceUpdates.depositaryUid = auth.uid;
                    // v9.4.4 : Dénormalisation sur le document Créateur
                    transaction.update(db.collection("users").doc(creatorId), {
                        depositaryUids: admin.firestore.FieldValue.arrayUnion(auth.uid)
                    });
                }

                transaction.update(sourceRef, sourceUpdates);
            }

            return { status: "success", role: role };
        });

        // ═══ PROPAGATION DE LIAISON (v9.3.2) ═══
        if (result.status === "success" || result.status === "already_accepted") {
            const inviteDocAfter = await inviteRef.get();
            const inviteData = inviteDocAfter.data();
            if (inviteData && inviteData.sourceId && inviteData.role === "recipient") {
                await propagateUidLiaison(inviteData.creatorId, inviteData.sourceId, auth.uid);
            }
        }

        return result;

    } catch (error: any) {
        if (error instanceof HttpsError) throw error;
        console.error("Erreur acceptUniversalInvitation:", error);
        throw new HttpsError("internal", error.message || "Erreur lors de l'acceptation de l'invitation");
    }
});

export const migrateLegacyRoles = onCall(async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Utilisateur non connecté");

    const userRef = db.collection("users").doc(uid);
    const doc = await userRef.get();
    const data = doc.data();

    if (!doc.exists || data?.myRoles) return { status: "already_migrated" };

    const legacyIds = data?.protectedCreatorIds || [];
    const newRoles: any = {};

    for (const creatorId of legacyIds) {
        const creatorDoc = await db.collection("users").doc(creatorId).get();
        const creatorName = creatorDoc.data()?.displayName || "Votre proche";

        newRoles[`${creatorId}_depositary`] = {
            creatorId: creatorId,
            creatorName: creatorName,
            role: "depositary",
            status: "active",
            label: "Gardien de confiance",
            joinedAt: admin.firestore.FieldValue.serverTimestamp(),
            migratedAt: admin.firestore.FieldValue.serverTimestamp()
        };
    }

    await userRef.update({
        myRoles: newRoles,
        isCreator: data?.isCreator === true, // v9.3.5 : Sécurisation du rôle Créateur (pas de true par défaut)
        migrationVersion: 7.2
    });

    return { status: "success", count: legacyIds.length };
});

/**
 * PHOEN-X v9.4.10 - Envoi de mail via Cloud Function (Relais sécurisé)
 */
export const sendMail = onCall(
    { region: "us-central1", invoker: "public" },
    async (request) => {
        if (!request.auth) {
            throw new HttpsError("unauthenticated", "Authentification requise.");
        }

        const { to, subject, text } = request.data as {
            to?: string;
            subject?: string;
            text?: string;
        };

        if (!to || !subject || !text) {
            throw new HttpsError(
                "invalid-argument",
                "Les champs 'to', 'subject' et 'text' sont requis."
            );
        }

        await db.collection("mail").add({
            to,
            message: { subject, text },
        });

        return { success: true };
    }
);

export const becomeCreator = onCall(async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Utilisateur non connecté");

    const { rhythmDays } = request.data;
    if (!rhythmDays || typeof rhythmDays !== "number") {
        throw new HttpsError("invalid-argument", "Rythme de silence invalide");
    }

    try {
        const userRef = db.collection("users").doc(uid);
        await userRef.update({
            isCreator: true,
            "silenceConfig.rhythmDays": rhythmDays,
            "silenceConfig.lastCheckInAt": admin.firestore.FieldValue.serverTimestamp(),
            "silenceConfig.missedCycles": 0,
            "silenceConfig.escalationLevel": 0,
            "silenceConfig.lastSilenceStatus": "present",
            convertedAt: admin.firestore.FieldValue.serverTimestamp()
        });

        console.log(`[CONVERSION] L'utilisateur ${uid} est maintenant Créateur.`);
        return { success: true };
    } catch (error: any) {
        console.error(`[CONVERSION ERROR] Échec pour ${uid}:`, error);
        throw new HttpsError("internal", "Impossible de valider le statut Créateur");
    }
});
